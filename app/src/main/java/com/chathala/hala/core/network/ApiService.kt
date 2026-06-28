package com.chathala.hala.core.network

import com.chathala.hala.feature.auth.data.AuthResponse
import com.chathala.hala.feature.auth.data.ForgotPasswordRequest
import com.chathala.hala.feature.auth.data.GoogleAuthRequest
import com.chathala.hala.feature.profile.data.InterestsResponse
import com.chathala.hala.feature.legal.data.LegalResponse
import com.chathala.hala.feature.auth.data.LoginRequest
import com.chathala.hala.feature.auth.data.RegisterRequest
import com.chathala.hala.feature.auth.data.ResetPasswordRequest
import com.chathala.hala.feature.auth.data.SimpleResponse
import com.chathala.hala.feature.auth.data.DeleteAccountRequest
import com.chathala.hala.feature.auth.data.RefreshTokenRequest
import com.chathala.hala.feature.auth.data.RefreshTokenResponse
import com.chathala.hala.feature.settings.data.AboutResponse
import com.chathala.hala.feature.settings.data.ChangePasswordRequest
import com.chathala.hala.feature.settings.data.ContactResponse
import com.chathala.hala.feature.settings.data.NotificationPrefsResponse
import com.chathala.hala.feature.settings.data.PrivacySettingsResponse
import com.chathala.hala.feature.settings.data.DoNotDisturbResponse
import com.chathala.hala.feature.settings.data.UpdateAcceptingRequestsRequest
import com.chathala.hala.feature.settings.data.UpdateDoNotDisturbRequest
import com.chathala.hala.feature.settings.data.UpdatePauseDiscoveryRequest
import com.chathala.hala.feature.settings.data.UpdateNotificationPrefRequest
import com.chathala.hala.feature.settings.data.UpdatePremiumOnlyRequestsRequest
import com.chathala.hala.feature.settings.data.UpdateShowAgeRequest
import com.chathala.hala.feature.settings.data.UpdateShowCountryRequest
import com.chathala.hala.feature.settings.data.UpdateDistanceRequest
import com.chathala.hala.feature.settings.data.UpdateStealthRequest
import com.chathala.hala.feature.chats.data.AcceptWithMessageRequest
import com.chathala.hala.feature.chats.data.AcceptWithMessageResponse
import com.chathala.hala.feature.chats.data.ChatModeInfoResponse
import com.chathala.hala.feature.chats.data.ChatModeRequest
import com.chathala.hala.feature.chats.data.ChatModeResponse
import com.chathala.hala.feature.chats.data.ConversationsResponse
import com.chathala.hala.feature.chats.data.ForwardRequest
import com.chathala.hala.feature.chats.data.ForwardResponse
import com.chathala.hala.feature.chats.data.MessagesResponse
import com.chathala.hala.feature.chats.data.MuteRequest
import com.chathala.hala.feature.chats.data.MuteResponse
import com.chathala.hala.feature.chats.data.PendingCountResponse
import com.chathala.hala.feature.chats.data.PendingRequestsResponse
import com.chathala.hala.feature.chats.data.ReactRequest
import com.chathala.hala.feature.chats.data.ReactResponse
import com.chathala.hala.feature.chats.data.SendMessageRequest
import com.chathala.hala.feature.chats.data.SendMessageResponse
import com.chathala.hala.feature.chats.data.ViewPhotoResponse
import com.chathala.hala.feature.blocking.data.BlockedUsersResponse
import com.chathala.hala.feature.verification.data.VerificationStatusResponse
import com.chathala.hala.feature.verification.data.VerificationSubmitResponse
import com.chathala.hala.feature.discover.data.DiscoverCardsResponse
import com.chathala.hala.feature.discover.data.RequestConversationRequest
import com.chathala.hala.feature.discover.data.RequestConversationResponse
import com.chathala.hala.feature.notifications.data.NotificationsResponse
import com.chathala.hala.feature.push.data.DeviceTokenRequest
import com.chathala.hala.feature.profile.data.UpdateProfileRequest
import com.chathala.hala.feature.profile.data.UpdateProfileResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): SimpleResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): SimpleResponse

    @POST("api/auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): AuthResponse

    // ── حظر الجهاز / الاستئناف ────────────────────────────────────

    @POST("api/auth/check-device-ban")
    suspend fun checkDeviceBan(
        @Body body: com.chathala.hala.feature.suspension.data.CheckDeviceBanRequest
    ): com.chathala.hala.feature.suspension.data.CheckDeviceBanResponse

    @POST("api/appeals")
    suspend fun submitAppeal(
        @Header("Authorization") bearer: String,
        @Body body: com.chathala.hala.feature.suspension.data.AppealRequest
    ): com.chathala.hala.feature.suspension.data.AppealResponse

    @POST("api/appeals/public/device-ban")
    suspend fun submitPublicDeviceBanAppeal(
        @Body body: com.chathala.hala.feature.suspension.data.PublicDeviceBanAppealRequest
    ): com.chathala.hala.feature.suspension.data.AppealResponse

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): RefreshTokenResponse

    @retrofit2.http.HTTP(method = "DELETE", path = "api/auth/delete-account", hasBody = true)
    suspend fun deleteAccount(
        @Header("Authorization") bearer: String,
        @Body body: DeleteAccountRequest
    ): SimpleResponse

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") bearer: String): Map<String, Any?>

    @PUT("api/auth/update-profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest
    ): UpdateProfileResponse

    @Multipart
    @PUT("api/auth/upload-profile-image")
    suspend fun uploadProfileImage(
        @Header("Authorization") bearer: String,
        @Part profileImage: MultipartBody.Part
    ): UpdateProfileResponse

    @retrofit2.http.DELETE("api/auth/profile-image")
    suspend fun deleteProfileImage(
        @Header("Authorization") bearer: String
    ): UpdateProfileResponse

    @GET("api/interests")
    suspend fun getInterests(): InterestsResponse

    @GET("api/settings/privacy-policy")
    suspend fun getPrivacyPolicy(): LegalResponse

    @GET("api/settings/terms")
    suspend fun getTerms(): LegalResponse

    @GET("api/settings/about")
    suspend fun getAbout(): AboutResponse

    @GET("api/settings/contact-us")
    suspend fun getContact(): ContactResponse

    @GET("api/mobile/privacy/settings")
    suspend fun getPrivacySettings(
        @Header("Authorization") bearer: String
    ): PrivacySettingsResponse

    @retrofit2.http.PATCH("api/mobile/privacy/distance")
    suspend fun updateShowDistance(
        @Header("Authorization") bearer: String,
        @Body body: UpdateDistanceRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/stealth")
    suspend fun updateStealthMode(
        @Header("Authorization") bearer: String,
        @Body body: UpdateStealthRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/show-age")
    suspend fun updateShowAge(
        @Header("Authorization") bearer: String,
        @Body body: UpdateShowAgeRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/show-country")
    suspend fun updateShowCountry(
        @Header("Authorization") bearer: String,
        @Body body: UpdateShowCountryRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/accepting-requests")
    suspend fun updateAcceptingRequests(
        @Header("Authorization") bearer: String,
        @Body body: UpdateAcceptingRequestsRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/premium-only-requests")
    suspend fun updatePremiumOnlyRequests(
        @Header("Authorization") bearer: String,
        @Body body: UpdatePremiumOnlyRequestsRequest
    ): SimpleResponse

    @retrofit2.http.PATCH("api/mobile/privacy/do-not-disturb")
    suspend fun updateDoNotDisturb(
        @Header("Authorization") bearer: String,
        @Body body: UpdateDoNotDisturbRequest
    ): DoNotDisturbResponse

    @retrofit2.http.PATCH("api/mobile/privacy/pause-discovery")
    suspend fun updatePauseDiscovery(
        @Header("Authorization") bearer: String,
        @Body body: UpdatePauseDiscoveryRequest
    ): SimpleResponse

    @GET("api/mobile/notifications/preferences")
    suspend fun getNotificationPrefs(
        @Header("Authorization") bearer: String
    ): NotificationPrefsResponse

    @retrofit2.http.PATCH("api/mobile/notifications/preferences")
    suspend fun updateNotificationPref(
        @Header("Authorization") bearer: String,
        @Body body: UpdateNotificationPrefRequest
    ): NotificationPrefsResponse

    @PUT("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") bearer: String,
        @Body body: ChangePasswordRequest
    ): SimpleResponse

    // ── Notifications ─────────────────────────────────────────────

    @GET("api/mobile/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("filter") filter: String,
        @Query("group") group: Boolean,
        @Query("markRead") markRead: Boolean
    ): NotificationsResponse

    @PUT("api/mobile/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    @PUT("api/mobile/notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") bearer: String
    ): SimpleResponse

    @retrofit2.http.DELETE("api/mobile/notifications/{id}")
    suspend fun deleteNotification(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    @retrofit2.http.DELETE("api/mobile/notifications")
    suspend fun deleteAllNotifications(
        @Header("Authorization") bearer: String
    ): SimpleResponse

    // ── Push Notifications (FCM) ──────────────────────────────────

    @PUT("api/mobile/device-token")
    suspend fun registerDeviceToken(
        @Header("Authorization") bearer: String,
        @Body body: DeviceTokenRequest
    ): SimpleResponse

    @retrofit2.http.DELETE("api/mobile/device/unregister-token")
    suspend fun unregisterDeviceToken(
        @Header("Authorization") bearer: String
    ): SimpleResponse

    // ── Chats / Conversations ─────────────────────────────────────

    @GET("api/mobile/conversations")
    suspend fun getConversations(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        // accepted = القائمة الرئيسية فقط (الطلبات لها endpoint/شاشة مستقلة)
        @Query("status") status: String? = null
    ): ConversationsResponse

    @GET("api/mobile/users/search")
    suspend fun searchUsers(
        @Header("Authorization") bearer: String,
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("isPremium") isPremium: Boolean? = null,
        @Query("online") online: Boolean? = null,
        @Query("gender") gender: String? = null,
        @Query("country") country: String? = null,
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("random") random: Boolean? = null
    ): com.chathala.hala.feature.discover.data.UserSearchResponse

    @GET("api/mobile/conversations/pending-count")
    suspend fun getPendingCount(
        @Header("Authorization") bearer: String
    ): PendingCountResponse

    @GET("api/mobile/conversations/pending")
    suspend fun getPendingRequests(
        @Header("Authorization") bearer: String
    ): PendingRequestsResponse

    @PUT("api/mobile/conversations/{id}/accept")
    suspend fun acceptConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    @POST("api/mobile/conversations/{id}/accept-with-message")
    suspend fun acceptWithMessage(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: AcceptWithMessageRequest
    ): AcceptWithMessageResponse

    @PUT("api/mobile/conversations/{id}/reject")
    suspend fun rejectConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    @PUT("api/mobile/conversations/{id}/read")
    suspend fun markConversationRead(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    @retrofit2.http.DELETE("api/mobile/conversations/{id}")
    suspend fun deleteConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): SimpleResponse

    // ── Messages ──────────────────────────────────────────────────

    @GET("api/mobile/messages/{conversationId}")
    suspend fun getMessages(
        @Header("Authorization") bearer: String,
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): MessagesResponse

    @POST("api/mobile/messages/send")
    suspend fun sendMessage(
        @Header("Authorization") bearer: String,
        @Body body: SendMessageRequest
    ): SendMessageResponse

    @Multipart
    @POST("api/mobile/messages/send-image")
    suspend fun sendImageMessage(
        @Header("Authorization") bearer: String,
        @Part image: MultipartBody.Part,
        @Part("conversationId") conversationId: okhttp3.RequestBody,
        @Part("caption") caption: okhttp3.RequestBody?,
        @Part("imageSource") imageSource: okhttp3.RequestBody?
    ): SendMessageResponse

    @Multipart
    @POST("api/mobile/messages/send-audio")
    suspend fun sendAudioMessage(
        @Header("Authorization") bearer: String,
        @Part audio: MultipartBody.Part,
        @Part("conversationId") conversationId: okhttp3.RequestBody,
        @Part("duration") duration: okhttp3.RequestBody?
    ): SendMessageResponse

    @POST("api/mobile/messages/{messageId}/react")
    suspend fun reactToMessage(
        @Header("Authorization") bearer: String,
        @Path("messageId") messageId: String,
        @Body body: ReactRequest
    ): ReactResponse

    @PUT("api/mobile/conversations/{id}/mute")
    suspend fun muteConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: MuteRequest
    ): MuteResponse

    @retrofit2.http.DELETE("api/mobile/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") bearer: String,
        @Path("messageId") messageId: String
    ): SimpleResponse

    @POST("api/mobile/messages/forward")
    suspend fun forwardMessage(
        @Header("Authorization") bearer: String,
        @Body body: ForwardRequest
    ): ForwardResponse

    @PUT("api/mobile/conversations/{id}/chat-mode")
    suspend fun setChatMode(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: ChatModeRequest
    ): ChatModeResponse

    @GET("api/mobile/conversations/{id}/chat-mode")
    suspend fun getChatMode(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): ChatModeInfoResponse

    @POST("api/v2/mobile/messages/{messageId}/reveal")
    suspend fun revealSensitiveContent(
        @Header("Authorization") bearer: String,
        @Path("messageId") messageId: String
    ): com.chathala.hala.feature.chats.data.RevealContentResponse

    @retrofit2.http.PATCH("api/mobile/privacy/allow-sensitive-content")
    suspend fun updateAllowSensitiveContent(
        @Header("Authorization") bearer: String,
        @Body body: com.chathala.hala.feature.settings.data.UpdateAllowSensitiveContentRequest
    ): com.chathala.hala.feature.settings.data.SimpleSettingsResponse

    @POST("api/mobile/messages/{messageId}/view-photo")
    suspend fun viewDisappearingPhoto(
        @Header("Authorization") bearer: String,
        @Path("messageId") messageId: String
    ): ViewPhotoResponse

    @Multipart
    @POST("api/mobile/messages/send-image")
    suspend fun sendDisappearingImage(
        @Header("Authorization") bearer: String,
        @Part image: MultipartBody.Part,
        @Part("conversationId") conversationId: okhttp3.RequestBody,
        @Part("caption") caption: okhttp3.RequestBody?,
        @Part("imageSource") imageSource: okhttp3.RequestBody?,
        @Part("disappearingDuration") disappearingDuration: okhttp3.RequestBody
    ): SendMessageResponse

    @GET("api/mobile/violations-history")
    suspend fun getViolationsHistory(
        @Header("Authorization") bearer: String
    ): com.chathala.hala.feature.settings.data.ViolationsHistoryResponse

    @GET("api/mobile/account-standing")
    suspend fun getAccountStanding(
        @Header("Authorization") bearer: String
    ): com.chathala.hala.feature.settings.data.AccountStandingResponse

    @GET("api/appeals/my")
    suspend fun getMyAppeals(
        @Header("Authorization") bearer: String
    ): com.chathala.hala.feature.settings.data.MyAppealsResponse

    @GET("api/appeals/{id}")
    suspend fun getAppealDetail(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): com.chathala.hala.feature.settings.data.AppealDetailResponse

    @POST("api/appeals/{id}/reply")
    suspend fun replyToAppeal(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: com.chathala.hala.feature.settings.data.AppealReplyRequest
    ): com.chathala.hala.feature.settings.data.AppealReplyResponse

    @POST("api/v2/mobile/messages/{messageId}/appeal-block")
    suspend fun appealBlock(
        @Header("Authorization") bearer: String,
        @Path("messageId") messageId: String,
        @Body body: com.chathala.hala.feature.chats.data.AppealBlockRequest
    ): com.chathala.hala.feature.chats.data.AppealBlockResponse

    @GET("api/mobile/config/promo-keywords")
    suspend fun getPromoKeywords(
        @Header("Authorization") bearer: String
    ): com.chathala.hala.feature.chats.data.PromoKeywordsResponse

    // ── Discover ─────────────────────────────────────────────────

    @GET("api/swipes/cards")
    suspend fun getDiscoverCards(
        @Header("Authorization") bearer: String,
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 100,
        @retrofit2.http.Query("gender") gender: String? = null,
        @retrofit2.http.Query("minAge") minAge: Int? = null,
        @retrofit2.http.Query("maxAge") maxAge: Int? = null,
        @retrofit2.http.Query("lastActiveWithin") lastActiveWithin: String? = null,
        @retrofit2.http.Query("latitude") latitude: Double? = null,
        @retrofit2.http.Query("longitude") longitude: Double? = null
    ): DiscoverCardsResponse

    @POST("api/mobile/conversations/request")
    suspend fun requestConversation(
        @Header("Authorization") bearer: String,
        @Body body: RequestConversationRequest
    ): RequestConversationResponse

    @POST("api/swipes")
    suspend fun swipe(
        @Header("Authorization") bearer: String,
        @Body body: com.chathala.hala.feature.discover.data.SwipeRequest
    ): com.chathala.hala.feature.discover.data.SwipeResponse

    // ── Blocking ─────────────────────────────────────────────────

    @GET("api/mobile/users/blocked")
    suspend fun getBlockedUsers(
        @Header("Authorization") bearer: String
    ): BlockedUsersResponse

    @POST("api/mobile/users/block/{userId}")
    suspend fun blockUser(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): SimpleResponse

    @POST("api/mobile/users/unblock/{userId}")
    suspend fun unblockUser(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: String
    ): SimpleResponse

    // ── Reporting ────────────────────────────────────────────────

    @POST("api/mobile/reports")
    suspend fun reportUser(
        @Header("Authorization") bearer: String,
        @Body body: com.chathala.hala.feature.reporting.data.CreateReportRequest
    ): SimpleResponse

    // ── User profile view ────────────────────────────────────────

    @GET("api/mobile/users/{id}/profile")
    suspend fun getUserProfile(
        @Header("Authorization") bearer: String,
        @Path("id") userId: String
    ): com.chathala.hala.feature.userprofile.data.UserProfileResponse

    // ── Verification ─────────────────────────────────────────────

    @GET("api/mobile/verification/status")
    suspend fun getVerificationStatus(
        @Header("Authorization") bearer: String
    ): VerificationStatusResponse

    @Multipart
    @POST("api/mobile/verification/submit")
    suspend fun submitVerification(
        @Header("Authorization") bearer: String,
        @Part selfie: MultipartBody.Part
    ): VerificationSubmitResponse
}
