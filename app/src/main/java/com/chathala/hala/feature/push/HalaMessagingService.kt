package com.chathala.hala.feature.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.chathala.hala.HalaApp
import com.chathala.hala.MainActivity
import com.chathala.hala.R
import com.chathala.hala.core.network.NetworkResult
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * يتعامل مع FCM:
 *  - onNewToken → يُسجّله مع الباك إند إذا كان المستخدم مسجل دخول
 *  - onMessageReceived → يعرض إشعار في الـ tray (مع deep link data)
 *
 * ملاحظة: عند وصول رسالة `notification` من السيرفر والتطبيق في الـ foreground،
 * Firebase يمرّرها لـ onMessageReceived. عند الـ background، Firebase يُنشئ
 * الإشعار تلقائياً (إذا كان payload يحتوي `notification`). لذا نعرض إشعاراً
 * يدوياً هنا فقط للـ data-only messages + للتأكد من التحكم الكامل بالـ channels.
 */
class HalaMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken: $token")
        val app = applicationContext as? HalaApp ?: return
        scope.launch {
            // نُسجّل فقط لو المستخدم مسجّل دخول (لدينا access token)
            val accessToken = app.tokenStorage.token.first()
            if (accessToken.isNullOrBlank()) {
                Log.d(TAG, "onNewToken: no active session — skip register")
                return@launch
            }
            when (val r = app.deviceTokenRepository.register(token)) {
                is NetworkResult.Success -> Log.d(TAG, "FCM token registered")
                is NetworkResult.Error -> Log.w(TAG, "Register failed: ${r.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val notif = message.notification

        val title = notif?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = notif?.body ?: data["body"] ?: ""
        val type = data["type"]

        // badge: تحديث عدّاد غير المقروء بشكل خفيف — بدون ربط بـ VM
        (applicationContext as? HalaApp)?.let { app ->
            scope.launch {
                runCatching { app.notificationsRepository.refreshUnreadCount() }
            }
        }

        showSystemNotification(
            context = applicationContext,
            title = title,
            body = body,
            type = type,
            extras = data
        )
    }

    companion object {
        private const val TAG = "HalaMessagingService"

        /** يُعرض إشعار نظام (tray) مع deep link عبر MainActivity. */
        fun showSystemNotification(
            context: Context,
            title: String,
            body: String,
            type: String?,
            extras: Map<String, String>
        ) {
            val channelId = HalaNotificationChannels.channelForType(type)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_FROM_PUSH, true)
                if (type != null) putExtra(EXTRA_TYPE, type)
                // تمرير data كـ extras (mn السيرفر) للـ deep link
                extras.forEach { (k, v) -> putExtra("data_$k", v) }
            }

            val pending = PendingIntent.getActivity(
                context,
                Random.nextInt(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Large Icon:
            //  - إشعار من مستخدم (فيه senderImage) → صورة المُرسِل دائرية (احترافي مثل تطبيقات المراسلة)
            //  - غير ذلك → الشعار الرسمي الملوّن
            // الأيقونة الصغيرة تبقى أحادية اللون بإلزام النظام.
            val senderImage = extras["senderImage"]?.takeIf { it.isNotBlank() }
            val largeIcon = (senderImage?.let { loadCircularBitmap(it, 128) })
                ?: runCatching {
                    ContextCompat.getDrawable(context, R.drawable.dardasha_hala_log)?.toBitmap(128, 128)
                }.getOrNull()

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_hala)
                .setColor(context.getColor(R.color.notification_accent))
                .apply { largeIcon?.let { setLargeIcon(it) } }
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(
                    if (channelId == HalaNotificationChannels.MESSAGES ||
                        channelId == HalaNotificationChannels.SYSTEM
                    )
                        NotificationCompat.PRIORITY_HIGH
                    else
                        NotificationCompat.PRIORITY_DEFAULT
                )

            val manager = context.getSystemService<NotificationManager>() ?: return
            manager.notify(Random.nextInt(), builder.build())
        }

        /**
         * يحمّل صورة من URL (متزامن — يُستدعى من خيط FCM الخلفي) ويقصّها دائرية.
         * يُعيد null عند الفشل/المهلة فنرجع للشعار الرسمي.
         */
        private fun loadCircularBitmap(url: String, sizePx: Int): android.graphics.Bitmap? = runCatching {
            val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                doInput = true
                instanceFollowRedirects = true
                connect()
            }
            val raw = conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                ?: return null
            circleCrop(raw, sizePx)
        }.getOrNull()

        private fun circleCrop(src: android.graphics.Bitmap, size: Int): android.graphics.Bitmap {
            val square = android.media.ThumbnailUtils.extractThumbnail(src, size, size)
            val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.BitmapShader(
                    square,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)
            return output
        }

        const val EXTRA_FROM_PUSH = "hala_from_push"
        const val EXTRA_TYPE = "hala_notif_type"
    }
}
