package com.chathala.hala

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chathala.hala.core.storage.AppTheme
import com.chathala.hala.feature.push.PushIntentCoordinator
import com.chathala.hala.navigation.HalaNavGraph
import com.chathala.hala.ui.components.OfflineBanner
import com.chathala.hala.ui.theme.HalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // edge-to-edge: نتحكم نحن بحواف النظام (insets) — مطلوب على Android 15+
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = applicationContext as HalaApp
        PushIntentCoordinator.handle(intent)
        setContent {
            val theme by app.appPreferences.theme.collectAsState(initial = AppTheme.SYSTEM)
            HalaTheme(themeMode = theme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            OfflineBanner()
                            HalaNavGraph()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushIntentCoordinator.handle(intent)
    }
}
