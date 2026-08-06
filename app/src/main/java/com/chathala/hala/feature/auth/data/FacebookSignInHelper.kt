package com.chathala.hala.feature.auth.data

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class FacebookSignInResult {
    data class Success(val accessToken: String) : FacebookSignInResult()
    data class Error(val message: String) : FacebookSignInResult()
    data object Cancelled : FacebookSignInResult()
}

/**
 * دخول بزر "المتابعة باستخدام فيسبوك".
 *
 * SDK فيسبوك يعمل بنمط callback على ActivityResultRegistry، فنغلّفه بـ coroutine
 * ليطابق شكل [GoogleSignInHelper] ويُستدعى من Compose بنفس الطريقة.
 */
class FacebookSignInHelper(private val context: Context) {

    private val callbackManager = CallbackManager.Factory.create()

    suspend fun signIn(): FacebookSignInResult = suspendCancellableCoroutine { cont ->
        val activity = context.findActivity()
        if (activity == null) {
            cont.resume(FacebookSignInResult.Error(S.get(R.string.facebook_signin_failed)))
            return@suspendCancellableCoroutine
        }

        val loginManager = LoginManager.getInstance()
        loginManager.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    loginManager.unregisterCallback(callbackManager)
                    val token = result.accessToken.token
                    if (cont.isActive) {
                        cont.resume(
                            if (token.isBlank()) {
                                FacebookSignInResult.Error(S.get(R.string.facebook_no_token))
                            } else {
                                FacebookSignInResult.Success(token)
                            }
                        )
                    }
                }

                override fun onCancel() {
                    loginManager.unregisterCallback(callbackManager)
                    if (cont.isActive) cont.resume(FacebookSignInResult.Cancelled)
                }

                override fun onError(error: FacebookException) {
                    loginManager.unregisterCallback(callbackManager)
                    android.util.Log.e(TAG, "Facebook login failed: ${error.message}", error)
                    if (cont.isActive) {
                        cont.resume(
                            FacebookSignInResult.Error(
                                S.serverOr(error.message, R.string.facebook_signin_failed)
                            )
                        )
                    }
                }
            }
        )

        cont.invokeOnCancellation { loginManager.unregisterCallback(callbackManager) }

        // جلسة جديدة في كل مرة — يمنع بقاء المستخدم عالقاً بحساب فيسبوك سابق بعد تسجيل الخروج.
        loginManager.logOut()
        // نمرّرها كـ ActivityResultRegistryOwner لا كـ Activity: هذا المسار يسجّل النتيجة
        // بنفسه فلا نحتاج تمرير onActivityResult يدوياً من MainActivity.
        loginManager.logInWithReadPermissions(
            activity as ActivityResultRegistryOwner,
            callbackManager,
            listOf("public_profile", "email")
        )
    }

    private fun Context.findActivity(): ComponentActivity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is ComponentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private companion object {
        const val TAG = "FacebookSignIn"
    }
}
