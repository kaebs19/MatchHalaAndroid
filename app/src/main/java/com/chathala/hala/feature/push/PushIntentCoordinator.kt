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

    private val _pendingTab = MutableStateFlow<MainTab?>(null)
    val pendingTab: StateFlow<MainTab?> = _pendingTab.asStateFlow()

    /** conversationId المراد فتحه مباشرة عند فتح التطبيق من إشعار رسالة. */
    private val _pendingConversationId = MutableStateFlow<String?>(null)
    val pendingConversationId: StateFlow<String?> = _pendingConversationId.asStateFlow()

    /** يفحص الـ intent ويستخرج وجهة التنقل إن كان قادماً من push. */
    fun handle(intent: Intent?) {
        if (intent == null) return
        val fromPush = intent.getBooleanExtra(HalaMessagingService.EXTRA_FROM_PUSH, false)
        if (!fromPush) return

        val type = intent.getStringExtra(HalaMessagingService.EXTRA_TYPE)
        val isMessageType = type == "message" || type == "new_message"

        _pendingTab.value = if (isMessageType) MainTab.CHATS else MainTab.NOTIFICATIONS

        if (isMessageType) {
            val convId = intent.getStringExtra("data_conversationId")
                ?: intent.getStringExtra("data_conversation_id")
            if (!convId.isNullOrBlank()) {
                _pendingConversationId.value = convId
            }
        }

        // استهلاك الـ extras حتى لا يُعاد تطبيقها عند rotate
        intent.removeExtra(HalaMessagingService.EXTRA_FROM_PUSH)
        intent.removeExtra(HalaMessagingService.EXTRA_TYPE)
        intent.removeExtra("data_conversationId")
        intent.removeExtra("data_conversation_id")
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
