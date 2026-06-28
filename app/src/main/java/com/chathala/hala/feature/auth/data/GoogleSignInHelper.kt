package com.chathala.hala.feature.auth.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.chathala.hala.core.config.AppConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * دخول بزر "المتابعة باستخدام Google".
     * يستخدم GetSignInWithGoogleOption الذي يفتح منتقي الحسابات دائماً
     * (مناسب للأزرار الصريحة، بخلاف GetGoogleIdOption للدخول التلقائي).
     */
    suspend fun signIn(): GoogleSignInResult {
        return try {
            val option = GetSignInWithGoogleOption.Builder(
                serverClientId = AppConfig.GOOGLE_WEB_CLIENT_ID
            ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(google.idToken)
            } else {
                GoogleSignInResult.Error("نوع بيانات الاعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Error("لا توجد حسابات Google على الجهاز. أضف حساباً من الإعدادات وحاول مجدداً.")
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error("فشل قراءة بيانات Google")
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(e.message ?: "فشل الدخول بـ Google")
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "خطأ غير متوقع")
        }
    }
}
