package com.chathala.hala.core.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * يحوّل [Uri] من Photo Picker إلى [MultipartBody.Part] لإرساله إلى Retrofit.
 * يقرأ البايتات إلى الذاكرة — مناسب للصور (< بضع MB).
 */
object MediaUploadHelper {

    fun uriToImagePart(
        context: Context,
        uri: Uri,
        fieldName: String = "profileImage"
    ): MultipartBody.Part? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull(), 0, bytes.size)
        val ext = mimeType.substringAfter('/', "jpg")
        val fileName = "upload_${System.currentTimeMillis()}.$ext"
        return MultipartBody.Part.createFormData(fieldName, fileName, body)
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
