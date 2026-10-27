package com.chathala.hala.core.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chathala.hala.BuildConfig
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.google.android.gms.ads.MobileAds

/** Developer-only entry point. The inspector still requires a registered AdMob test device. */
@Composable
fun AdInspectorButton(modifier: Modifier = Modifier) {
    if (!BuildConfig.DEBUG) return
    val activity = LocalContext.current.findActivity() ?: return
    var message by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        TextButton(onClick = {
            if (!AdGate.sdkInitialized) {
                message = S.get(R.string.ad_inspector_not_ready)
            } else {
                message = null
                MobileAds.openAdInspector(activity) { error ->
                    if (error != null) message = S.get(R.string.ad_inspector_error, error.message)
                }
            }
        }) { Text(S.get(R.string.ad_inspector_title)) }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
