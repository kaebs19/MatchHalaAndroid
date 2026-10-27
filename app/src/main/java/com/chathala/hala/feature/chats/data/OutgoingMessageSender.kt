package com.chathala.hala.feature.chats.data

import android.content.Context
import android.net.Uri
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.NetworkResult
import java.io.File

class OutgoingMessageSender(private val context: Context, private val repository: MessagesRepository) {
    suspend fun send(sessionId: String, entry: OutgoingMessage): OutgoingSendResult {
        val message = entry.message
        val conversation = requireNotNull(message.conversation)
        val outcome = when (message.type) {
            "text" -> when (val sent = repository.sendText(conversation, message.content.orEmpty(), message.replyTo?.id, sessionId)) {
                is NetworkResult.Error -> OutgoingSendResult(sent)
                is NetworkResult.Success -> {
                    val (real, promo, locked) = sent.data
                    if (real != null && locked == null) OutgoingSendResult(NetworkResult.Success(real), promo)
                    else OutgoingSendResult(NetworkResult.Error(
                        locked?.message ?: S.get(R.string.chat_locked_promo), code = "MESSAGING_RESTRICTED", httpCode = 403
                    ), promo, locked)
                }
            }
            "audio" -> OutgoingSendResult(repository.sendAudio(
                conversation, File(requireNotNull(entry.attachmentPath)),
                message.audioDuration?.toInt() ?: 1, message.audioWaveform.orEmpty(), sessionId
            ))
            "image" -> {
                val uri = Uri.fromFile(File(requireNotNull(entry.attachmentPath)))
                OutgoingSendResult(if (message.disappearing?.enabled == true) {
                    repository.sendDisappearingImage(context, conversation, uri, message.disappearing.duration ?: 10, message.imageSource ?: "gallery", sessionId)
                } else {
                    repository.sendImage(context, conversation, uri, imageSource = message.imageSource ?: "gallery", expectedSessionId = sessionId)
                })
            }
            else -> error("Unsupported outgoing type")
        }
        if (message.type == "text" && outcome.result is NetworkResult.Success) {
            com.chathala.hala.core.review.AppRating.notePositiveEvent()
        }
        val result = outcome.result
        return if (result is NetworkResult.Error) outcome.copy(result = result.copy(
            message = com.chathala.hala.core.network.ErrorMessages.friendly(result)
        )) else outcome

    }
}
