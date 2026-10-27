package com.chathala.hala.feature.chats.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Outgoing copies live outside backup/cache, so failed voice messages survive cache eviction. */
class ChatAttachmentStorage(private val context: Context) : OutgoingAttachments {
    private val directory get() = File(context.noBackupFilesDir, "chat_outbox")

    override suspend fun prepare(entry: OutgoingMessage): OutgoingMessage = withContext(Dispatchers.IO) {
        if (entry.message.type == "text") return@withContext entry
        entry.attachmentPath?.let {
            if (!managedFile(it).isFile) throw IOException("Missing attachment")
            return@withContext entry
        }
        val source = entry.message.mediaUrl ?: throw IOException("Missing attachment")
        val audio = entry.message.type == "audio"
        val uri = if (audio) Uri.fromFile(File(source)) else Uri.parse(source)
        val mime = context.contentResolver.getType(uri)
        val extension = if (audio) "m4a" else MimeTypeMap.getSingleton().getExtensionFromMimeType(mime ?: "image/jpeg") ?: "jpg"
        check(directory.mkdirs() || directory.isDirectory)
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open attachment")
            if (audio) File(source).delete()
            entry.copy(
                attachmentPath = target.absolutePath,
                message = entry.message.copy(mediaUrl = if (audio) target.absolutePath else Uri.fromFile(target).toString())
            )
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    override suspend fun release(entry: OutgoingMessage) = withContext(Dispatchers.IO) {
        entry.attachmentPath?.let { managedFile(it).delete() }
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) { directory.deleteRecursively(); Unit }

    private fun managedFile(path: String): File = File(path).canonicalFile.also {
        require(it.parentFile == directory.canonicalFile) { "Attachment outside outbox" }
    }
}
