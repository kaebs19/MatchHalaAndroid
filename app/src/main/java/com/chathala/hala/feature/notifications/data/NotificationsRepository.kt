package com.chathala.hala.feature.notifications.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * استدعاءات /api/mobile/notifications:
 *  - جلب القائمة مع pagination + filter + grouping + auto mark-read
 *  - تعليم فردي / جماعي كمقروء
 *  - حذف فردي / جماعي
 *
 *  كذلك يحفظ `unreadCount` بشكل تشاركي في الذاكرة — تراه التبويبات
 *  (الـ MainScreen) كـ badge دون الحاجة لربطها بـ ViewModel.
 */
class NotificationsRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun updateUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    fun decrementUnread(by: Int = 1) {
        _unreadCount.value = (_unreadCount.value - by).coerceAtLeast(0)
    }

    fun clearUnread() {
        _unreadCount.value = 0
    }

    suspend fun fetchNotifications(
        page: Int = 1,
        limit: Int = 20,
        filter: String = "all",
        group: Boolean = true,
        markRead: Boolean = true
    ): NetworkResult<NotificationsData> = safeApiCall {
        val resp = api.getNotifications(
            bearer = bearer(),
            page = page,
            limit = limit,
            filter = filter,
            group = group,
            markRead = markRead
        )
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    /** مزامنة خفيفة لعدد غير المقروء فقط — للـ badge في شريط التبويبات. */
    suspend fun refreshUnreadCount(): NetworkResult<Int> = safeApiCall {
        val resp = api.getNotifications(
            bearer = bearer(),
            page = 1,
            limit = 1,
            filter = "all",
            group = false,
            markRead = false
        )
        val count = resp.data?.unreadCount ?: 0
        _unreadCount.value = count
        count
    }

    suspend fun markRead(id: String): NetworkResult<String> = safeApiCall {
        val resp = api.markNotificationRead(bearer(), id)
        resp.message ?: "تم تحديث الإشعار"
    }

    suspend fun markAllRead(): NetworkResult<String> = safeApiCall {
        val resp = api.markAllNotificationsRead(bearer())
        resp.message ?: "تم تحديث الإشعارات"
    }

    suspend fun delete(id: String): NetworkResult<String> = safeApiCall {
        val resp = api.deleteNotification(bearer(), id)
        resp.message ?: "تم حذف الإشعار"
    }

    suspend fun deleteAll(): NetworkResult<String> = safeApiCall {
        val resp = api.deleteAllNotifications(bearer())
        resp.message ?: "تم حذف جميع الإشعارات"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
