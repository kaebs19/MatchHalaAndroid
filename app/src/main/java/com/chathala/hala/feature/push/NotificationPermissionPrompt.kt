package com.chathala.hala.feature.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.storage.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * طلب إذن الإشعارات بشاشة تمهيدية قبل نافذة النظام.
 *
 * لماذا لا نُطلق نافذة النظام مباشرة: نافذة أندرويد لا تشرح الفائدة، ورفضها
 * مرّتين يُغلق الطلب نهائياً — بعدها لا يعود `launch` يُظهر شيئاً أبداً، ويصبح
 * المستخدم بلا إشعارات بلا طريق للعودة. الشاشة التمهيدية تشرح أولاً، وتكشف
 * حالة الرفض النهائي فتحوّل المستخدم لإعدادات النظام.
 */
@Composable
fun NotificationPermissionPrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { AppPreferences(context.applicationContext) }

    // 0 = مخفي، 1 = الشاشة التمهيدية، 2 = تحويل لإعدادات النظام (رفض نهائي)
    var stage by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        stage = when {
            granted -> 0
            // النظام رفض دون إظهار نافذة ⇒ رفض دائم، الطريق الوحيد هو الإعدادات
            !shouldShowRationale(context) -> 2
            else -> 0
        }
        if (!granted) scope.launch { prefs.setNotificationPromptDismissedNow() }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        if (hasNotificationPermission(context)) return@LaunchedEffect
        val dismissedAt = prefs.notificationPromptDismissedAt.first()
        if (System.currentTimeMillis() - dismissedAt < REASK_INTERVAL_MS) return@LaunchedEffect
        stage = 1
    }

    if (stage == 0) return

    val settingsMode = stage == 2
    PromptDialog(
        settingsMode = settingsMode,
        onConfirm = {
            if (settingsMode) {
                openNotificationSettings(context)
                stage = 0
            } else {
                stage = 0
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismiss = {
            stage = 0
            scope.launch { prefs.setNotificationPromptDismissedNow() }
        }
    )
}

@Composable
private fun PromptDialog(
    settingsMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = tween(220),
        label = "notif-prompt-scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // هيدر متدرّج بهوية التطبيق — نفس تدرّج شاشات الدخول
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = S.get(
                            if (settingsMode) R.string.notif_prompt_settings_title
                            else R.string.notif_prompt_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = S.get(
                            if (settingsMode) R.string.notif_prompt_settings_subtitle
                            else R.string.notif_prompt_subtitle
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))
                    BenefitRow(Icons.AutoMirrored.Filled.Chat, S.get(R.string.notif_prompt_benefit_messages))
                    Spacer(Modifier.height(10.dp))
                    BenefitRow(Icons.Filled.Favorite, S.get(R.string.notif_prompt_benefit_likes))
                    Spacer(Modifier.height(10.dp))
                    BenefitRow(Icons.Filled.PersonAdd, S.get(R.string.notif_prompt_benefit_requests))

                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = S.get(
                                if (settingsMode) R.string.notif_prompt_open_settings
                                else R.string.notif_prompt_enable
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = S.get(R.string.notif_prompt_later),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun shouldShowRationale(context: Context): Boolean {
    val activity = context as? Activity ?: return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS
    )
}

/** يفتح إعدادات إشعارات التطبيق مباشرة، مع رجوع لصفحة التطبيق إن تعذّر. */
private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        }
    }
}

/** ثلاثة أيام بين محاولتين — يكفي لتغيّر رأي المستخدم دون أن يصير إلحاحاً. */
private const val REASK_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000
