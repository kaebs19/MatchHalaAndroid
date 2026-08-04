package com.chathala.hala.feature.push

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * قنوات الإشعارات — Android 8+ يُلزم تصنيفها.
 *
 *  - social  → تفاعلات (إعجابات، مطابقات، زيارات…)
 *  - system  → تنبيهات الحساب (تحقق، تحذيرات، إلخ)
 *  - messages → رسائل الدردشة
 */
object HalaNotificationChannels {

    const val SOCIAL = "hala_social"
    const val SYSTEM = "hala_system"
    const val MESSAGES = "hala_messages"

    fun registerAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                SOCIAL,
                S.get(R.string.channel_social_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = S.get(R.string.channel_social_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                MESSAGES,
                S.get(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = S.get(R.string.channel_messages_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                SYSTEM,
                S.get(R.string.channel_system_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = S.get(R.string.channel_system_desc)
            }
        )
    }

    /** يختار القناة المناسبة حسب نوع الإشعار (يطابق types الباك إند). */
    fun channelForType(type: String?): String = when (type) {
        "message", "new_message" -> MESSAGES
        "like", "new_like", "match", "new_match",
        "super_like", "profile_view", "new_follower", "comment" -> SOCIAL
        else -> SYSTEM
    }
}
