package com.chathala.hala.feature.auth.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

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
                GoogleSignInResult.Error(S.get(R.string.google_unexpected_credential))
            }
        } catch (e: GetCredentialCancellationException) {
            // ملاحظة: خدمات Google تُبلّغ أحياناً عن "إلغاء" حتى حين يكون السبب الحقيقي
            // عدم تطابق SHA-1/معرّف العميل — لذا نُسجّل التفاصيل لتشخيصها من logcat.
            android.util.Log.w(TAG, "Google sign-in cancelled/aborted: ${e.type} — ${e.message}", e)
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            android.util.Log.w(TAG, "No Google credential: ${e.message}", e)
            GoogleSignInResult.Error(S.get(R.string.google_no_accounts))
        } catch (e: GoogleIdTokenParsingException) {
            android.util.Log.e(TAG, "ID token parsing failed", e)
            GoogleSignInResult.Error(S.get(R.string.google_read_failed))
        } catch (e: GetCredentialException) {
            android.util.Log.e(TAG, "GetCredentialException: type=${e.type} msg=${e.message}", e)
            GoogleSignInResult.Error(e.message ?: S.get(R.string.google_signin_failed))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Unexpected Google sign-in error", e)
            GoogleSignInResult.Error(e.message ?: S.get(R.string.err_unexpected))
        }
    }

    private companion object {
        const val TAG = "GoogleSignIn"
    }
}
