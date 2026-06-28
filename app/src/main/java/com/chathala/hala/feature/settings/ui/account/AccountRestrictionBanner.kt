package com.chathala.hala.feature.settings.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.HalaApp
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.feature.settings.data.AccountStandingData
import kotlinx.coroutines.flow.first

private val Red = Color(0xFFE53935)
private val Orange = Color(0xFFFF8C00)

/** يحوّل ISO (2026-06-28T03:30:00.000Z) إلى millis، أو null عند الفشل. */
private fun parseIsoMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrNull()
}

/** ينسّق المدة المتبقية: «1 يوم 23:59:58» أو «47:59:58» أو «انتهى». */
private fun formatCountdown(remainingMs: Long): String {
    if (remainingMs <= 0) return "انتهى"
    val totalSec = remainingMs / 1000
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val hms = "%02d:%02d:%02d".format(hours, minutes, seconds)
    return if (days > 0) "$days يوم $hms" else hms
}

/** ترجمة سبب التقييد إلى نص عربي واضح. */
private fun reasonLabel(reason: String?): String = when (reason) {
    "external_promotion" -> "نشر أو مشاركة حسابات خارجية"
    else -> "مخالفة سياسة الاستخدام"
}

/**
 * بانر عام يظهر أعلى الشاشات (الاكتشاف…) عندما يكون حساب المستخدم مقيّداً/معلّقاً.
 * يجلب حالة الحساب بنفسه عبر /account-standing ويعرض النوع + السبب + المدة + زر استئناف.
 *
 * @param refreshKey غيّره لإعادة الجلب (مثلاً بعد تقديم استئناف)
 */
@Composable
fun AccountRestrictionBanner(
    onAppeal: () -> Unit,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp

    // يُعاد الجلب عند بدء التشغيل أو عند ورود حدث رفع التقييد فوراً
    var liftTick by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        app.socket.incoming.collect { ev ->
            if (ev is com.chathala.hala.feature.chats.socket.SocketEvent.RestrictionLifted) {
                liftTick++
            }
        }
    }

    val standing by produceState<AccountStandingData?>(initialValue = null, refreshKey, liftTick) {
        value = runCatching {
            val token = app.tokenStorage.token.first() ?: return@runCatching null
            ApiClient.service.getAccountStanding("Bearer $token").data
        }.getOrNull()
    }

    val s = standing ?: return
    val restricted = s.restriction?.active == true
    val suspended = s.suspension?.active == true
    if (!restricted && !suspended) return

    var expanded by remember { mutableStateOf(true) }

    val title = if (suspended) "حسابك معلّق مؤقتاً" else "تقييد كامل للمراسلة"
    val hoursLeft = s.restriction?.hoursLeft ?: 0
    val reason = reasonLabel(s.restriction?.reason)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Red.copy(alpha = 0.92f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // زر الطي
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.size(10.dp))

            // زر المراجعة — يظهر فقط إن كانت المخالفة قابلة للمراجعة
            if (s.reviewable) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable(onClick = onAppeal)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        "مراجعة",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Red
                    )
                }
            } else {
                Text(
                    "غير قابلة للمراجعة",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // العنوان + الأيقونة
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    "لا يمكنك إرسال رسائل أو طلبات",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // التفاصيل (السبب + المدة) عند التوسيع
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.size(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        DetailRow("السبب", reason)
                        // عدّاد حيّ يتناقص كل ثانية من وقت الانتهاء
                        val untilMs = remember(s.restriction?.until) {
                            parseIsoMillis(s.restriction?.until)
                        }
                        if (!suspended && untilMs != null) {
                            val countdown by produceState(initialValue = "", untilMs) {
                                while (true) {
                                    val remaining = untilMs - System.currentTimeMillis()
                                    value = formatCountdown(remaining)
                                    if (remaining <= 0) break
                                    kotlinx.coroutines.delay(1000)
                                }
                            }
                            Spacer(Modifier.size(6.dp))
                            DetailRow("المدة المتبقية", countdown)
                        } else if (!suspended && hoursLeft > 0) {
                            Spacer(Modifier.size(6.dp))
                            DetailRow("المدة المتبقية", "$hoursLeft ساعة")
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (s.reviewable)
                                "إذا كنت ترى أن التقييد غير صحيح، قدّم طلب مراجعة وسيراجعه المشرف."
                            else (s.nonReviewableNote
                                ?: "مخالفات الصور الإباحية أو الجنسية والأسماء المخالفة غير قابلة للمراجعة."),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
