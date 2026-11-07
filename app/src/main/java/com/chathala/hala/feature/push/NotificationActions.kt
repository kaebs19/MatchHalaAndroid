package com.chathala.hala.feature.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.chathala.hala.MainActivity
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.i18n.S
import kotlin.random.Random

/** بطاقة الملف المرفقة بإشعارات الإعجاب/الزيارة/التطابق (`card*` من الخادم). */
data class ProfileCard(
    val userId: String?,
    val name: String?,
    val image: String?,
    val age: String?,
    val country: String?,
    val verified: Boolean,
    val locked: Boolean
) {
    fun titleLine(): String = listOfNotNull(name, if (verified) "✓" else null).joinToString(" ")

    fun details(): String = listOfNotNull(
        age,
        country?.let { c -> Countries.byCode(c)?.let { "${it.flag} ${it.name}" } ?: c }
    ).joinToString("  ·  ")

    companion object {
        private val CARD_TYPES = setOf("new_like", "like", "super_like", "superlike", "profile_view", "new_match", "match")

        fun from(type: String?, extras: Map<String, String>): ProfileCard? {
            if (type !in CARD_TYPES) return null
            fun v(k: String) = extras[k]?.takeIf { it.isNotBlank() }
            val locked = v("cardLocked") == "1"
            val name = v("cardName") ?: v("senderName") ?: v("fromName")
            val image = v("cardImage") ?: if (locked) null else v("senderImage")
            if (name == null && image == null) return null
            return ProfileCard(
                userId = v("cardUserId") ?: v("fromUserId") ?: v("senderId") ?: v("viewerId"),
                name = name, image = image, age = v("cardAge"), country = v("cardCountry"),
                verified = v("cardVerified") == "1", locked = locked
            )
        }
    }
}

/** أزرار الإشعار حسب النوع (مقابل فئات UNNotificationCategory في iOS). */
object NotificationActions {

    fun attach(context: Context, builder: NotificationCompat.Builder, type: String?, extras: Map<String, String>, notifId: Int) {
        fun v(k: String) = extras[k]?.takeIf { it.isNotBlank() }
        when (type) {
            "friend_request" -> {
                val friendshipId = v("friendshipId") ?: return
                builder.addAction(0, S.get(R.string.notif_action_accept),
                    broadcast(context, NotificationActionReceiver.ACTION_FRIEND_ACCEPT, notifId) {
                        putExtra(NotificationActionReceiver.EXTRA_FRIENDSHIP_ID, friendshipId)
                    })
                builder.addAction(0, S.get(R.string.notif_action_decline),
                    broadcast(context, NotificationActionReceiver.ACTION_FRIEND_DECLINE, notifId) {
                        putExtra(NotificationActionReceiver.EXTRA_FRIENDSHIP_ID, friendshipId)
                    })
            }
            "friend_accepted" -> {
                val senderId = v("senderId") ?: return
                builder.addAction(0, S.get(R.string.notif_action_message), openProfile(context, senderId, type))
            }
            "new_like", "like", "profile_view" -> {
                val uid = v("senderId") ?: v("fromUserId") ?: v("cardUserId") ?: v("viewerId") ?: return
                // الإعجاب المقفل (غير مشترك): هوية المعجب مخفية — «أعجبني أيضاً» لا يُعرض
                if (v("cardLocked") != "1") {
                    builder.addAction(0, S.get(R.string.notif_action_like_back),
                        broadcast(context, NotificationActionReceiver.ACTION_LIKE_BACK, notifId) {
                            putExtra(NotificationActionReceiver.EXTRA_USER_ID, uid)
                        })
                    builder.addAction(0, S.get(R.string.notif_action_view_profile), openProfile(context, uid, type))
                }
            }
        }
    }

    private fun broadcast(context: Context, action: String, notifId: Int, fill: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            fill()
        }
        return PendingIntent.getBroadcast(
            context, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openProfile(context: Context, userId: String, type: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HalaMessagingService.EXTRA_FROM_PUSH, true)
            if (type != null) putExtra(HalaMessagingService.EXTRA_TYPE, type)
            putExtra(PushIntentCoordinator.EXTRA_OPEN_USER, userId)
        }
        return PendingIntent.getActivity(
            context, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
