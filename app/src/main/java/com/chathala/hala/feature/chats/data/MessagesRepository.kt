package com.chathala.hala.feature.chats.data

import android.content.Context
import android.net.Uri
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.chats.data.ExternalPromoBlockedInfo
import com.chathala.hala.core.util.MediaUploadHelper
import kotlinx.coroutines.flow.first
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
        resp.data ?: throw IllegalStateException("بيانات غير متوفرة")
    }

    suspend fun sendText(
        conversationId: String,
        content: String,
        replyTo: String? = null
    ): NetworkResult<Triple<Message?, ExternalPromoBlockedInfo?, MessagingLockedInfo?>> = safeApiCall {
        val resp = api.sendMessage(
            bearer = bearer(),
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
        val msg = resp.data?.message ?: throw IllegalStateException("فشل إرسال الرسالة")
        Triple(msg, resp.externalPromoBlocked, null)
    }

    suspend fun sendImage(
        context: Context,
        conversationId: String,
        uri: Uri,
        caption: String? = null,
        imageSource: String = "gallery"
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.uriToImagePart(context, uri, fieldName = "image")
            ?: throw IllegalStateException("تعذّر قراءة الصورة")
        val resp = api.sendImageMessage(
            bearer = bearer(),
            image = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            caption = caption?.let(MediaUploadHelper::plainText),
            imageSource = MediaUploadHelper.plainText(imageSource)
        )
        resp.data?.message ?: throw IllegalStateException("فشل إرسال الصورة")
    }

    suspend fun sendAudio(
        conversationId: String,
        file: File,
        durationSeconds: Int
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.fileToAudioPart(file)
        val resp = api.sendAudioMessage(
            bearer = bearer(),
            audio = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            duration = MediaUploadHelper.plainText(durationSeconds.toString())
        )
        resp.data?.message ?: throw IllegalStateException("فشل إرسال الصوت")
    }

    suspend fun react(messageId: String, emoji: String): NetworkResult<List<Reaction>> = safeApiCall {
        val resp = api.reactToMessage(
            bearer = bearer(),
            messageId = messageId,
            body = ReactRequest(emoji)
        )
        resp.data?.reactions ?: emptyList()
    }

    suspend fun deleteMessage(messageId: String): NetworkResult<String> = safeApiCall {
        val resp = api.deleteMessage(bearer(), messageId)
        resp.message ?: "تم الحذف"
    }

    suspend fun forwardMessage(
        messageId: String,
        targetConversationId: String
    ): NetworkResult<Message> = safeApiCall {
        val resp = api.forwardMessage(
            bearer = bearer(),
            body = ForwardRequest(messageId = messageId, targetConversationId = targetConversationId)
        )
        resp.data?.message ?: throw IllegalStateException("فشل إعادة التوجيه")
    }

    suspend fun viewDisappearingPhoto(messageId: String): NetworkResult<Unit> = safeApiCall {
        api.viewDisappearingPhoto(bearer(), messageId)
        Unit
    }

    suspend fun revealSensitiveContent(messageId: String): NetworkResult<String> = safeApiCall {
        val resp = api.revealSensitiveContent(bearer(), messageId)
        resp.data?.content ?: throw IllegalStateException(resp.message ?: "فشل الكشف")
    }

    suspend fun sendDisappearingImage(
        context: Context,
        conversationId: String,
        uri: Uri,
        durationSeconds: Int,
        imageSource: String = "gallery"
    ): NetworkResult<Message> = safeApiCall {
        val part = MediaUploadHelper.uriToImagePart(context, uri, fieldName = "image")
            ?: throw IllegalStateException("تعذّر قراءة الصورة")
        val resp = api.sendDisappearingImage(
            bearer = bearer(),
            image = part,
            conversationId = MediaUploadHelper.plainText(conversationId),
            caption = null,
            imageSource = MediaUploadHelper.plainText(imageSource),
            disappearingDuration = MediaUploadHelper.plainText(durationSeconds.toString())
        )
        resp.data?.message ?: throw IllegalStateException("فشل إرسال الصورة")
    }

    suspend fun appealBlock(messageId: String, reason: String): NetworkResult<String> = safeApiCall {
        val resp = api.appealBlock(bearer(), messageId, AppealBlockRequest(reason))
        resp.message ?: "تم تقديم الاستئناف، سيتم مراجعته في أقرب فرصة ممكنة"
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
        resp.message ?: "تم إرسال طلبك، سيراجعه المشرف في أقرب فرصة ممكنة"
    }

    suspend fun fetchPromoKeywords(): NetworkResult<List<PromoKeyword>> = safeApiCall {
        val resp = api.getPromoKeywords(bearer())
        resp.data?.keywords ?: emptyList()
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
