package com.chathala.hala.core.session.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.PendingCountData
import com.chathala.hala.feature.notifications.data.NotificationsRepository
import com.chathala.hala.feature.user.data.UserRepository
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * ما يحتاجه التطبيق عند الفتح والعودة من الخلفية في نداء واحد:
 * `GET /api/mobile/app/bootstrap` ← كل جزء **بشكل** مساره القديم.
 * نداء جارٍ واحد + فاصل 5ث، وعند الفشل نرجع للطلبات المنفردة.
 */
class AppBootstrap(
    private val tokenStorage: TokenStorage,
    private val notifications: NotificationsRepository,
    private val conversations: ConversationsRepository,
    private val users: UserRepository,
    private val scope: CoroutineScope,
    private val api: BootstrapApi = ApiClient.bootstrapService
) {
    private var job: Job? = null
    private var lastAt = 0L

    fun refresh(force: Boolean = false) {
        if (job?.isActive == true) return
        val now = System.currentTimeMillis()
        if (!force && now - lastAt < MIN_INTERVAL_MS) return
        lastAt = now
        job = scope.launch {
            val token = tokenStorage.token.first() ?: return@launch
            when (val r = safeApiCall { api.bootstrap("Bearer $token").data ?: BootstrapData() }) {
                is NetworkResult.Success -> apply(r.data)
                is NetworkResult.Error -> {
                    // الخادم القديم/خطأ مؤقت ← الطلبات المنفردة
                    notifications.refreshUnreadCount()
                    conversations.refreshPendingCount()
                }
            }
        }
    }

    private suspend fun apply(data: BootstrapData) {
        data.notifications?.unreadCount?.let(notifications::updateUnreadCount)
        data.pendingRequests?.let(conversations::applyPendingCount)
        // اشتراك انتهى/تجدّد على الخادم ← نعيد قراءة المستخدم (الحدود، الإعلانات، القصص)
        data.subscription?.isPremium?.let { premium ->
            if (users.currentUser.first()?.isPremium != premium) users.refresh()
        }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 5_000L
    }
}

interface BootstrapApi {
    @GET("api/mobile/app/bootstrap")
    suspend fun bootstrap(@Header("Authorization") bearer: String): BootstrapResponse
}

@JsonClass(generateAdapter = true)
data class BootstrapResponse(val success: Boolean = false, val data: BootstrapData? = null)

@JsonClass(generateAdapter = true)
data class BootstrapData(
    val notifications: BootstrapUnread? = null,
    val pendingRequests: PendingCountData? = null,
    val subscription: BootstrapSubscription? = null
)

@JsonClass(generateAdapter = true)
data class BootstrapUnread(val unreadCount: Int? = null)

@JsonClass(generateAdapter = true)
data class BootstrapSubscription(val isPremium: Boolean? = null, val expired: Boolean? = null)
