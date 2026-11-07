package com.chathala.hala.feature.stickers

import android.content.Context
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.chats.data.SendMessageResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class StickerItem(@Json(name = "_id") val id: String, val imageUrl: String)

@JsonClass(generateAdapter = true)
data class StickerCategory(
    @Json(name = "_id") val id: String,
    val name: String = "",
    val premium: Boolean? = null,
    val stickers: List<StickerItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StickersData(val categories: List<StickerCategory> = emptyList())

@JsonClass(generateAdapter = true)
data class StickersResponse(val success: Boolean = false, val data: StickersData? = null)

@JsonClass(generateAdapter = true)
data class SendStickerRequest(val conversationId: String, val stickerId: String)

interface StickerApi {
    @GET("api/mobile/stickers")
    suspend fun stickers(@Header("Authorization") bearer: String): StickersResponse

    @POST("api/mobile/messages/send-sticker")
    suspend fun send(@Header("Authorization") bearer: String, @Body body: SendStickerRequest): SendMessageResponse
}

/**
 * ملصقات لوحة التحكم (مقابل `StickerLibrary` في iOS): لا ملصقات مضمّنة عمداً — المُزال من
 * اللوحة يختفي بلا تحديث. تُحدَّث كل 5 دقائق على الأكثر، وتُخزَّن الفئات محلياً للفتح الفوري.
 * المفضلة والأخيرة على الجهاز لكل مستخدم.
 */
object StickerLibrary {
    private const val PREFS = "stickers"
    private const val KEY_CACHE = "categories.v1"
    private const val RECENT_LIMIT = 16
    private const val REFRESH_MS = 5 * 60 * 1000L

    private lateinit var appContext: Context
    private lateinit var tokens: TokenStorage
    private var lastFetch = 0L
    private var owner = "guest"

    private val _categories = MutableStateFlow<List<StickerCategory>>(emptyList())
    val categories: StateFlow<List<StickerCategory>> = _categories.asStateFlow()
    private val _favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteIds: StateFlow<List<String>> = _favoriteIds.asStateFlow()
    private val _recentIds = MutableStateFlow<List<String>>(emptyList())
    val recentIds: StateFlow<List<String>> = _recentIds.asStateFlow()

    private val listAdapter by lazy {
        ApiClient.moshi.adapter<List<StickerCategory>>(
            Types.newParameterizedType(List::class.java, StickerCategory::class.java)
        )
    }

    fun init(context: Context, tokenStorage: TokenStorage) {
        appContext = context.applicationContext
        tokens = tokenStorage
        val prefs = prefs()
        prefs.getString(KEY_CACHE, null)?.let { json ->
            runCatching { listAdapter.fromJson(json) }.getOrNull()?.let { _categories.value = it }
        }
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** المفضلة والأخيرة لكل مستخدم — تُقرأ عند معرفة المستخدم الحالي. */
    fun setOwner(userId: String?) {
        val o = userId ?: "guest"
        if (o == owner && (_favoriteIds.value.isNotEmpty() || _recentIds.value.isNotEmpty())) return
        owner = o
        val p = prefs()
        _favoriteIds.value = p.getString("favorites.$o", null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        _recentIds.value = p.getString("recents.$o", null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
    }

    private fun allowed(isPremium: Boolean) =
        _categories.value.filter { it.premium != true || isPremium }.flatMap { it.stickers }

    fun items(ids: List<String>, isPremium: Boolean): List<StickerItem> {
        val all = allowed(isPremium)
        return ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }

    /** رسالة المحادثة تحمل الرابط لا المعرّف. */
    fun stickerForUrl(url: String?): StickerItem? =
        url?.let { u -> _categories.value.asSequence().flatMap { it.stickers }.firstOrNull { it.imageUrl == u } }

    fun isFavorite(id: String) = id in _favoriteIds.value

    fun toggleFavorite(sticker: StickerItem) {
        val list = _favoriteIds.value.toMutableList()
        if (!list.remove(sticker.id)) list.add(0, sticker.id)
        _favoriteIds.value = list
        prefs().edit().putString("favorites.$owner", list.joinToString(",")).apply()
    }

    fun noteSent(sticker: StickerItem) {
        val list = (listOf(sticker.id) + _recentIds.value.filter { it != sticker.id }).take(RECENT_LIMIT)
        _recentIds.value = list
        prefs().edit().putString("recents.$owner", list.joinToString(",")).apply()
    }

    suspend fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastFetch < REFRESH_MS) return
        val token = tokens.token.first() ?: return
        val fresh = runCatching { ApiClient.stickerService.stickers("Bearer $token").data?.categories }.getOrNull() ?: return
        lastFetch = System.currentTimeMillis()
        _categories.value = fresh
        prefs().edit().putString(KEY_CACHE, listAdapter.toJson(fresh)).apply()
    }

    suspend fun send(conversationId: String, sticker: StickerItem): NetworkResult<com.chathala.hala.feature.chats.data.Message> = safeApiCall {
        val token = tokens.token.first() ?: error("no session")
        val resp = ApiClient.stickerService.send("Bearer $token", SendStickerRequest(conversationId, sticker.id))
        resp.data?.message ?: error(resp.message ?: "send failed")
    }.also { if (it is NetworkResult.Success) noteSent(sticker) }
}
