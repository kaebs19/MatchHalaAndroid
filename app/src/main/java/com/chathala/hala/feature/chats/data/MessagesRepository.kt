package com.chathala.hala.feature.chats.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import android.content.Context
import android.net.Uri
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo
import com.chathala.hala.core.util.MediaUploadHelper
import java.io.File

class MessagesRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    suspend fun fetchMessages(
        conversationId: String,
        page: Int = 1,
        limit: Int = 50
    ): NetworkResult<MessagesData> = safeApiCall {
        val resp = api.getMessages(bearer(), conversationId, page, limit)
        resp.data ?: throw IllegalStateException(S.get(R.string.err_data_unavailable))
    }

    suspend fun sendText(
        conversationId: String,
        content: String,
        replyTo: String? = null,
        expectedSessionId: String? = null
    ): NetworkResult<Triple<Message?, ExternalPromoBlockedInfo?, MessagingLockedInfo?>> = safeApiCall {
        val resp = api.sendMessage(
            bearer = bearer(expectedSessionId),
            body = SendMessageRequest(
                conversationId = conversationId,
                content = content,
                type = "text",
                replyTo = replyTo
            )
        )
        // إذا كانت المراسلة مقيّدة — لا رسالة، نُرجع معلومات التقييد
        if (resp.messagingLocked != null) {
            return@safeApiCall Triple(null, null, resp.messagingLocked)
        }
        val msg = resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_send_failed))
        // تحذير الكلمات المحظورة — ورقة الحسابات الخارجية تغني عنه إن وُجدت
        if (resp.warning != null && resp.externalPromoBlocked == null) BannedWordNotice.show(resp.warning)
        Triple(msg, resp.externalPromoBlocked, null)
    }

    suspend fun sendImage(
        context: Context,
        conversationId: String,
        uri: Uri,
        caption: String? = null,
        imageSource: String = "gallery",
        expectedSessionId: String? = null
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.uriToImagePart(context, uri, fieldName = "image")
            ?: throw IllegalStateException(S.get(R.string.msg_image_read_failed))
        val resp = api.sendImageMessage(
            bearer = bearer(expectedSessionId),
            image = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            caption = caption?.let(MediaUploadHelper::plainText),
            imageSource = MediaUploadHelper.plainText(imageSource)
        )
        resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_image_send_failed))
    }

    suspend fun sendAudio(
        conversationId: String,
        file: File,
        durationSeconds: Int,
        waveform: List<Double> = emptyList(),
        expectedSessionId: String? = null
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.fileToAudioPart(file)
        // الخادم يتوقّع waveform كنصّ JSON (نفس ما يرسله iOS) — قيم 0..1
        val waveformJson = waveform
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "[", postfix = "]") { "%.3f".format(java.util.Locale.US, it) }
        val resp = api.sendAudioMessage(
            bearer = bearer(expectedSessionId),
            audio = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            duration = MediaUploadHelper.plainText(durationSeconds.toString()),
            waveform = waveformJson?.let { MediaUploadHelper.plainText(it) }
        )
        resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_audio_send_failed))
    }

    suspend fun react(messageId: String, emoji: String): NetworkResult<List<Reaction>> = safeApiCall {
        val resp = api.reactToMessage(
            bearer = bearer(),
            messageId = messageId,
            body = ReactRequest(emoji)
        )
        resp.data?.reactions ?: emptyList()
    }

    /** يعدّل نصّ رسالة ويُرجع النصّ بعد فلاتر الخادم (قد يُكتَم جزء منه). */
    suspend fun editMessage(messageId: String, content: String): NetworkResult<String> = safeApiCall {
        val resp = api.editMessage(bearer(), messageId, EditMessageRequest(content))
        resp.data?.content ?: throw IllegalStateException(S.serverOr(resp.message, R.string.msg_edit_failed))
    }

    suspend fun deleteMessage(messageId: String): NetworkResult<String> = safeApiCall {
        val resp = api.deleteMessage(bearer(), messageId)
        S.serverOr(resp.message, R.string.msg_deleted)
    }

    suspend fun forwardMessage(
        messageId: String,
        targetConversationId: String
    ): NetworkResult<Message> = safeApiCall {
        val resp = api.forwardMessage(
            bearer = bearer(),
            body = ForwardRequest(messageId = messageId, targetConversationId = targetConversationId)
        )
        resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_forward_failed))
    }

    suspend fun viewDisappearingPhoto(messageId: String): NetworkResult<Unit> = safeApiCall {
        val response = api.viewDisappearingPhoto(bearer(), messageId)
        check(response.success) { S.serverOr(response.message, R.string.err_unexpected) }
        Unit
    }

    /** تسجيل مشاهدة فيديو مؤقت — بعد تنزيله كاملاً (التسجيل يبدأ العدّ). 410 PHOTO_EXPIRED إن انتهى. */
    suspend fun viewDisappearingVideo(messageId: String): NetworkResult<ViewPhotoData> = safeApiCall {
        val response = api.viewDisappearingPhoto(bearer(), messageId)
        check(response.success) { S.serverOr(response.message, R.string.err_unexpected) }
        response.data ?: ViewPhotoData()
    }

    /** إغلاق فيديو «مرة واحدة» ← يُدمَّر على الخادم. */
    suspend fun expireDisappearingMedia(messageId: String): NetworkResult<Unit> = safeApiCall {
        api.expireDisappearingMedia(bearer(), messageId)
        Unit
    }

    /**
     * فيديو ≤15ث ≤6MB، مؤقت دائماً. رموز الرفض النهائي: VIDEO_LIMIT_REACHED (429)،
     * VIDEO_TOO_LARGE، VIDEO_TOO_LONG، VIDEO_INVALID — لا إعادة تلقائية.
     */
    suspend fun sendVideo(
        conversationId: String,
        file: File,
        durationSeconds: Double,
        mode: String,
        width: Int?,
        height: Int?,
        replyTo: String?,
        expectedSessionId: String? = null
    ): NetworkResult<Message> = safeApiCall {
        val part = MultipartBody.Part.createFormData(
            "video", file.name, file.asRequestBody("video/mp4".toMediaTypeOrNull())
        )
        val resp = api.sendVideoMessage(
            bearer = bearer(expectedSessionId),
            video = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            duration = MediaUploadHelper.plainText("%.1f".format(java.util.Locale.US, durationSeconds)),
            mode = MediaUploadHelper.plainText(mode),
            width = width?.let { MediaUploadHelper.plainText(it.toString()) },
            height = height?.let { MediaUploadHelper.plainText(it.toString()) },
            replyTo = replyTo?.let(MediaUploadHelper::plainText)
        )
        resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_video_send_failed))
    }

    suspend fun revealSensitiveContent(messageId: String): NetworkResult<String> = safeApiCall {
        val resp = api.revealSensitiveContent(bearer(), messageId)
        resp.data?.content ?: throw IllegalStateException(S.serverOr(resp.message, R.string.msg_reveal_failed))
    }

    suspend fun sendDisappearingImage(
        context: Context,
        conversationId: String,
        uri: Uri,
        durationSeconds: Int,
        imageSource: String = "gallery",
        expectedSessionId: String? = null
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.uriToImagePart(context, uri, fieldName = "image")
            ?: throw IllegalStateException(S.get(R.string.msg_image_read_failed))
        val resp = api.sendDisappearingImage(
            bearer = bearer(expectedSessionId),
            image = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            caption = null,
            imageSource = MediaUploadHelper.plainText(imageSource),
            disappearingDuration = MediaUploadHelper.plainText(durationSeconds.toString())
        )
        resp.data?.message ?: throw IllegalStateException(S.get(R.string.msg_image_send_failed))
    }

    suspend fun appealBlock(messageId: String, reason: String): NetworkResult<String> = safeApiCall {
        val resp = api.appealBlock(bearer(), messageId, AppealBlockRequest(reason))
        S.serverOr(resp.message, R.string.appeal_submitted)
    }

    /**
     * طلب مراجعة تقييد المراسلة — يستخدم نظام الاستئناف نفسه (POST /api/appeals)
     * بـ actionType="restriction" تماماً مثل iOS ولوحة الأدمن. التسمية «مراجعة»
     * في واجهة Android فقط — البنية الخلفية موحّدة.
     */
    suspend fun requestMessagingReview(reason: String): NetworkResult<String> = safeApiCall {
        val resp = api.submitAppeal(
            bearer(),
            com.chathala.hala.feature.suspension.data.AppealRequest(
                reason = reason,
                actionType = "restriction"
            )
        )
        S.serverOr(resp.message, R.string.request_submitted)
    }

    suspend fun fetchPromoKeywords(): NetworkResult<List<PromoKeyword>> = safeApiCall {
        val resp = api.getPromoKeywords(bearer())
        resp.data?.keywords ?: emptyList()
    }

    private suspend fun bearer(expectedSessionId: String? = null): String {
        val session = tokenStorage.snapshot()
            ?: throw kotlinx.coroutines.CancellationException("Session ended")
        if (expectedSessionId != null && session.sessionId != expectedSessionId) {
            throw kotlinx.coroutines.CancellationException("Session changed")
        }
        return "Bearer ${session.accessToken}"
    }
}
