package com.chathala.hala.feature.stories.data

import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * طبقة الشبكة للقصص. الأخطاء تحمل `code` من الخادم (STORY_*)، ولرمز
 * [StoryErrorCodes.LIMIT] يحمل `payload` قيمة [StoryLimitData].
 */
class StoryRepository(
    private val tokenStorage: TokenStorage,
    private val api: StoryApiService = ApiClient.storyService
) {

    suspend fun publishImage(jpeg: ByteArray): NetworkResult<StoryPublishData> = call {
        val body = jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", "story_${System.currentTimeMillis()}.jpg", body)
        api.publishImage(bearer(), part).requireData()
    }

    suspend fun publishText(text: String, background: Int): NetworkResult<StoryPublishData> = call {
        api.publishText(bearer(), TextStoryRequest(text, background)).requireData()
    }

    suspend fun myStories(): NetworkResult<MyStoriesData> = call { api.myStories(bearer()).requireData() }

    suspend fun userStories(userId: String): NetworkResult<UserStoriesData> =
        call { api.userStories(bearer(), userId).data ?: UserStoriesData() }

    suspend fun statuses(userIds: List<String>): NetworkResult<Map<String, StoryStatus>> =
        call { api.statuses(bearer(), StoryStatusRequest(userIds)).data?.statuses.orEmpty() }

    suspend fun view(storyId: String): NetworkResult<Boolean> =
        call { api.view(bearer(), storyId).data?.recorded == true }

    suspend fun screenshot(storyId: String): NetworkResult<Boolean> =
        call { api.screenshot(bearer(), storyId).data?.recorded == true }

    suspend fun viewers(storyId: String): NetworkResult<StoryViewersData> =
        call { api.viewers(bearer(), storyId).data ?: StoryViewersData() }

    suspend fun delete(storyId: String): NetworkResult<Unit> = call { api.delete(bearer(), storyId); Unit }

    suspend fun setMuted(userId: String, muted: Boolean): NetworkResult<Boolean> = call {
        val r = if (muted) api.mute(bearer(), userId) else api.unmute(bearer(), userId)
        r.data?.muted ?: muted
    }

    private fun <T> StoryEnvelope<T>.requireData(): T =
        data ?: throw IllegalStateException(message ?: S.get(R.string.err_unexpected))

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
        return "Bearer $token"
    }

    /**
     * مثل `safeApiCall` لكن يحتفظ بـ `data` الخطأ: `ErrorBody` العام يحلّلها
     * كبيانات إيقاف فتضيع حقول STORY_LIMIT.
     */
    private inline fun <T> call(block: () -> T): NetworkResult<T> = try {
        NetworkResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val raw = e.response()?.errorBody()?.string()
        val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
        val code = json?.optString("code")?.takeIf { it.isNotBlank() }
        val payload: Any? = if (code == StoryErrorCodes.LIMIT) {
            json.optJSONObject("data")?.let { d ->
                StoryLimitData(
                    max = d.optInt("max", FREE_MAX),
                    active = d.optInt("active", 0),
                    freeMax = d.optInt("freeMax", FREE_MAX),
                    premiumMax = d.optInt("premiumMax", PREMIUM_MAX),
                    isPremium = d.optBoolean("isPremium", false)
                )
            }
        } else null
        NetworkResult.Error(
            message = json?.optString("message")?.takeIf { it.isNotBlank() }
                ?: S.get(R.string.err_generic_code, e.code().toString()),
            code = code,
            payload = payload,
            httpCode = e.code()
        )
    } catch (e: IOException) {
        NetworkResult.Error(S.get(R.string.err_check_connection))
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: S.get(R.string.err_unexpected))
    }
}
