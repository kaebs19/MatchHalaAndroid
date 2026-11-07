package com.chathala.hala.feature.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.chathala.hala.HalaApp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.feature.friends.data.FriendsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * أزرار الإشعار التي تعمل دون فتح التطبيق (مقابل UNNotificationAction في iOS):
 * قبول/رفض طلب الصداقة بـ `friendshipId`، و«أعجبني أيضاً» = `POST /api/swipes` إعجاب.
 * بعد التنفيذ يُستبدل الإشعار بتأكيد قصير يختفي وحده.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? HalaApp ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val pending = goAsync()
        scope.launch {
            val ok: Boolean
            val doneText: String
            when (intent.action) {
                ACTION_FRIEND_ACCEPT, ACTION_FRIEND_DECLINE -> {
                    val id = intent.getStringExtra(EXTRA_FRIENDSHIP_ID).orEmpty()
                    val repo = FriendsRepository(tokenStorage = app.tokenStorage)
                    val accept = intent.action == ACTION_FRIEND_ACCEPT
                    val r = if (id.isBlank()) null else if (accept) repo.accept(id) else repo.decline(id)
                    ok = r is NetworkResult.Success
                    doneText = S.get(if (accept) R.string.notif_friend_accepted_done else R.string.notif_friend_declined_done)
                }
                ACTION_LIKE_BACK -> {
                    val uid = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
                    val r = if (uid.isBlank()) null else app.discoverRepository.recordSwipe(uid, "like")
                    ok = r is NetworkResult.Success
                    doneText = S.get(R.string.notif_like_back_done)
                }
                else -> { pending.finish(); return@launch }
            }
            runCatching { showResult(context, notifId, if (ok) doneText else S.get(R.string.notif_action_failed)) }
            runCatching { app.notificationsRepository.refreshUnreadCount() }
            pending.finish()
        }
    }

    private fun showResult(context: Context, notifId: Int, text: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val n = NotificationCompat.Builder(context, HalaNotificationChannels.channelForType("friend_request"))
            .setSmallIcon(R.drawable.ic_stat_hala)
            .setColor(context.getColor(R.color.notification_accent))
            .setContentTitle(text)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(4_000)
            .build()
        manager.notify(notifId, n)
    }

    companion object {
        const val ACTION_FRIEND_ACCEPT = "com.chathala.hala.action.FRIEND_ACCEPT"
        const val ACTION_FRIEND_DECLINE = "com.chathala.hala.action.FRIEND_DECLINE"
        const val ACTION_LIKE_BACK = "com.chathala.hala.action.LIKE_BACK"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_FRIENDSHIP_ID = "friendship_id"
        const val EXTRA_USER_ID = "user_id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
