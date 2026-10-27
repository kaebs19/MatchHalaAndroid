package com.chathala.hala.core.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.source
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * يحوّل [Uri] من Photo Picker إلى [MultipartBody.Part] لإرساله إلى Retrofit.
 * يبث الصورة مباشرة إلى الطلب دون تحميل الملف كاملاً في ذاكرة التطبيق.
 */
object MediaUploadHelper {

    suspend fun uriToImagePart(
        context: Context,
        uri: Uri,
        fieldName: String = "profileImage"
    ): MultipartBody.Part? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
            ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(uri.lastPathSegment?.substringAfterLast('.'))
            ?: "image/jpeg"
        val body = object : RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                val stream = resolver.openInputStream(uri)
                    ?: throw IOException("Cannot open selected image")
                stream.source().use { sink.writeAll(it) }
            }
        }
        val ext = mimeType.substringAfter('/', "jpg")
        val fileName = "upload_${System.currentTimeMillis()}.$ext"
        MultipartBody.Part.createFormData(fieldName, fileName, body)
    }

    /** يحوّل ملف محلي (مثل تسجيل صوتي) إلى Multipart part. */
    fun fileToAudioPart(
        file: File,
        fieldName: String = "audio",
        mimeType: String = "audio/mp4"
    ): MultipartBody.Part {
        val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, file.name, body)
    }

    fun plainText(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaTypeOrNull())
}
