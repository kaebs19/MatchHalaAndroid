package com.chathala.hala.feature.push

import android.content.Intent
import com.chathala.hala.feature.main.ui.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * وسيط بين MainActivity.onCreate/onNewIntent وبين UI (MainScreen).
 *
 * عندما يضغط المستخدم على إشعار push:
 *   1. FCM يفتح MainActivity مع extras تحتوي EXTRA_FROM_PUSH + type (+ data_*)
 *   2. MainActivity ينادي handle(intent) → نُحدّث `pendingTab` و/أو `pendingConversationId`
 *   3. MainScreen يراقب الـ Flows ويُبدّل التبويب أو يفتح المحادثة ثم يستهلك الأحداث
 */
object PushIntentCoordinator {

    /** يضعه FCM في intent الإشعار الذي يعرضه النظام نيابةً عنا. */
    private const val FCM_MESSAGE_ID = "google.message_id"

    private val _pendingTab = MutableStateFlow<MainTab?>(null)
    val pendingTab: StateFlow<MainTab?> = _pendingTab.asStateFlow()

    /** conversationId المراد فتحه مباشرة عند فتح التطبيق من إشعار رسالة. */
    private val _pendingConversationId = MutableStateFlow<String?>(null)
    val pendingConversationId: StateFlow<String?> = _pendingConversationId.asStateFlow()

    /** رابط ويب مرفق بإشعار رسمي (من لوحة الإدارة) — يُفتح عند الضغط على الإشعار. */
    private val _pendingLink = MutableStateFlow<String?>(null)
    val pendingLink: StateFlow<String?> = _pendingLink.asStateFlow()

    /** يفحص الـ intent ويستخرج وجهة التنقل إن كان قادماً من push. */
    fun handle(intent: Intent?) {
        if (intent == null) return
        // مساران للضغط على الإشعار:
        //  - إشعار رسمناه نحن (onMessageReceived) → EXTRA_FROM_PUSH و data_*.
        //  - إشعار عرضه النظام في الخلفية (بثّ اللوحة يحمل notification block) →
        //    يفتح النظام التطبيق ومفاتيح data كما هي، مع google.message_id.
        val systemTray = intent.hasExtra(FCM_MESSAGE_ID)
        val fromPush = intent.getBooleanExtra(HalaMessagingService.EXTRA_FROM_PUSH, false) || systemTray
        if (!fromPush) return

        fun extra(key: String): String? =
            intent.getStringExtra("data_$key") ?: if (systemTray) intent.getStringExtra(key) else null

        val type = intent.getStringExtra(HalaMessagingService.EXTRA_TYPE) ?: extra("type")
        val isMessageType = type == "message" || type == "new_message"

        _pendingTab.value = if (isMessageType) MainTab.CHATS else MainTab.NOTIFICATIONS

        if (isMessageType) {
            val convId = extra("conversationId") ?: extra("conversation_id")
            if (!convId.isNullOrBlank()) {
                _pendingConversationId.value = convId
            }
        }

        extra("link")?.trim()
            ?.takeIf { com.chathala.hala.feature.notifications.ui.components.isSafeLink(it) }
            ?.let { _pendingLink.value = it }

        // استهلاك الـ extras حتى لا يُعاد تطبيقها عند rotate
        intent.removeExtra(HalaMessagingService.EXTRA_FROM_PUSH)
        intent.removeExtra(HalaMessagingService.EXTRA_TYPE)
        intent.removeExtra(FCM_MESSAGE_ID)
        intent.removeExtra("data_conversationId")
        intent.removeExtra("data_conversation_id")
        intent.removeExtra("data_link")
        intent.removeExtra("link")
    }

    /** يستهلك رابط الإشعار — يُستدعى بعد فتحه. */
    fun consumeLink() {
        _pendingLink.value = null
    }

    /**
     * يطلب فتح محادثة من داخل التطبيق (نقر على الشريط الداخلي).
     *
     * عبر نفس القناة التي يسلكها deep link الإشعار: `HalaNavGraph` يراقبها ويفتح
     * المحادثة أياً كانت الشاشة الحالية — فلا حاجة لتمرير `NavController` إلى شريط
     * يعيش فوق شجرة التنقّل كلّها.
     */
    fun requestConversation(conversationId: String) {
        if (conversationId.isBlank()) return
        _pendingConversationId.value = conversationId
    }

    /** يستهلك تبديل التبويب — يُستدعى من MainScreen بعد تطبيق التبديل. */
    fun consumeTab() {
        _pendingTab.value = null
    }

    /** يستهلك deep link المحادثة — يُستدعى بعد بدء navigation للمحادثة. */
    fun consumeConversation() {
        _pendingConversationId.value = null
    }
}
