package com.chathala.hala.feature.push

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
                "التفاعلات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات الإعجابات والمطابقات وزيارات البروفايل"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                MESSAGES,
                "الرسائل",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل الواردة من المحادثات"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                SYSTEM,
                "تنبيهات النظام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الحساب والأمان والتحقق"
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
