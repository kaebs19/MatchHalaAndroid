package com.chathala.hala.feature.push

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chathala.hala.MainActivity
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

/**
 * «وحشتنا»: ثلاثة إشعارات **محلية** بعد 3 و7 و15 يوماً من آخر استخدام، 8:30 مساءً
 * بتوقيت الجهاز. تُجدوَل عند الخروج للخلفية وتُلغى عند العودة وتسجيل الخروج، ولا
 * تُجدوَل بلا إذن إشعارات أو لغير المسجّل. النوع `win_back` يفتح الاكتشاف.
 *
 * ⚠️ الجدولة تنتظر قراءة الجلسة (غير متزامنة): رقم جيل يمنع عودة سريعة أثناءها من
 * أن تترك التذكيرات مجدولة (في iOS بقيت).
 */
class WinBackReminders(
    private val context: Context,
    private val tokenStorage: TokenStorage,
    private val scope: CoroutineScope
) {
    private val generation = AtomicInteger(0)

    fun scheduleOnBackground() {
        val gen = generation.incrementAndGet()
        scope.launch {
            if (tokenStorage.token.first().isNullOrBlank() || !notificationsAllowed()) return@launch
            if (generation.get() != gen) return@launch
            val am = context.getSystemService(AlarmManager::class.java) ?: return@launch
            REMINDER_DAYS.forEach { day ->
                val at = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, day)
                    set(Calendar.HOUR_OF_DAY, HOUR); set(Calendar.MINUTE, MINUTE)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                // تقريبي يكفي (لا صلاحية منبّه دقيق)
                am.set(AlarmManager.RTC_WAKEUP, at, pending(day))
            }
        }
    }

    fun cancel() {
        generation.incrementAndGet()
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        REMINDER_DAYS.forEach { am.cancel(pending(it)) }
    }

    private fun notificationsAllowed(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun pending(day: Int): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_BASE + day,
        Intent(context, WinBackReceiver::class.java).putExtra(EXTRA_DAY, day),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val TYPE = "win_back"
        internal const val EXTRA_DAY = "win_back_day"
        private val REMINDER_DAYS = listOf(3, 7, 15)
        private const val HOUR = 20
        private const val MINUTE = 30
        private const val REQUEST_BASE = 7300
    }
}

/** يعرض تذكير «وحشتنا» — النصّ بلغة التطبيق وقت العرض. */
class WinBackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra(WinBackReminders.EXTRA_DAY, 3)
        val (title, body) = when (day) {
            3 -> R.string.winback_title_1 to R.string.winback_body_1
            7 -> R.string.winback_title_2 to R.string.winback_body_2
            else -> R.string.winback_title_3 to R.string.winback_body_3
        }
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HalaMessagingService.EXTRA_FROM_PUSH, true)
            putExtra(HalaMessagingService.EXTRA_TYPE, WinBackReminders.TYPE)
        }
        val tap = PendingIntent.getActivity(
            context, 7400 + day, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, HalaNotificationChannels.channelForType(WinBackReminders.TYPE))
            .setSmallIcon(R.drawable.ic_stat_hala)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(S.get(title))
            .setContentText(S.get(body))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(7500 + day, notification)
        }
    }
}
