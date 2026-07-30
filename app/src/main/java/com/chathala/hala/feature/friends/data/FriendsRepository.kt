package com.chathala.hala.feature.friends.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

/** يجمّع كل استدعاءات ميزة الأصدقاء (طلبات/قبول/قائمة/حالة/خصوصية). */
class FriendsRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    suspend fun status(userId: String): NetworkResult<FriendStatusData> = safeApiCall {
        api.getFriendStatus(bearer(), userId).data ?: FriendStatusData(status = "none")
    }

    suspend fun sendRequest(userId: String): NetworkResult<FriendActionData> = safeApiCall {
        api.sendFriendRequest(bearer(), FriendRequestBody(userId)).data
            ?: FriendActionData(status = "pending_sent")
    }

    suspend fun accept(friendshipId: String): NetworkResult<FriendActionData> = safeApiCall {
        api.acceptFriendRequest(bearer(), friendshipId).data
            ?: FriendActionData(status = "friends")
    }

    suspend fun decline(friendshipId: String): NetworkResult<String> = safeApiCall {
        api.declineFriendRequest(bearer(), friendshipId).message ?: "تم رفض الطلب"
    }

    suspend fun remove(userId: String): NetworkResult<String> = safeApiCall {
        api.removeFriend(bearer(), userId).message ?: "تمت الإزالة"
    }

    suspend fun list(): NetworkResult<FriendsListData> = safeApiCall {
        api.getFriends(bearer()).data ?: FriendsListData()
    }

    suspend fun requests(): NetworkResult<FriendRequestsData> = safeApiCall {
        api.getFriendRequests(bearer()).data ?: FriendRequestsData()
    }

    suspend fun setFriendRequestsPrivacy(value: String): NetworkResult<String> = safeApiCall {
        api.updateFriendRequestsPrivacy(bearer(), UpdateFriendRequestsPrivacyBody(value)).message
            ?: "تم التحديث"
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
