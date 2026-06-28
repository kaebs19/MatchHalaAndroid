package com.chathala.hala.feature.splash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * شاشة البداية:
 *  - توكن موجود → Home مباشرة
 *  - غير موجود  → Login
 *  يُعرض أنميشن البداية مدة لائقة قبل الانتقال حتى لا يظهر كوميض سريع.
 */
private const val MIN_SPLASH_MILLIS = 2200L

@Composable
fun SplashScreen(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.hala_splash)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LaunchedEffect(Unit) {
        // نضمن ظهور الأنميشن مدة لائقة مع تنفيذ الفحص بالتوازي
        val start = System.currentTimeMillis()
        val token = app.tokenStorage.token.first()
        val authenticated = !token.isNullOrBlank()
        if (authenticated) {
            // Best-effort: مزامنة FCM token + افتح Socket
            runCatching { app.deviceTokenRepository.ensureSynced() }
            app.socket.connect()
        }
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < MIN_SPLASH_MILLIS) {
            delay(MIN_SPLASH_MILLIS - elapsed)
        }
        if (authenticated) onAuthenticated() else onUnauthenticated()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxWidth(0.82f),
            contentScale = ContentScale.Fit
        )
    }
}
