package com.chathala.hala.feature.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** رسالة واردة جاهزة للعرض كشريط داخل التطبيق. */
data class InAppMessageAlert(
    val id: String,
    val conversationId: String,
    val senderName: String?,
    val senderImage: String?,
    val type: String?,
    val content: String?
)

/**
 * إشعارات داخل التطبيق للرسائل الواردة.
 *
 * لماذا لا نكتفي بإشعار النظام؟ لأن أندرويد يعرضه في شريط الحالة حتى والتطبيق
 * مفتوح أمام المستخدم — وهو مكان لا ينظر إليه أحد وهو يستعمل التطبيق، ويصل
 * متأخّراً عن السوكِت. فالمصدر هنا `SocketEvent.NewMessage` مباشرةً: يصل لحظياً،
 * ويحمل المُرسِل والمحتوى، ولا يعتمد على FCM أصلاً.
 *
 * كل الحالة هنا لأن المُنتِج ([com.chathala.hala.HalaApp]) والمستهلك (الواجهة)
 * والكاتم ([HalaMessagingService]) في طبقات لا يعرف بعضها بعضاً.
 */
object InAppAlerts {

    private val _current = MutableStateFlow<InAppMessageAlert?>(null)
    val current: StateFlow<InAppMessageAlert?> = _current.asStateFlow()

    /** المحادثة المفتوحة الآن — رسائلها تظهر أمام المستخدم فلا شريط لها. */
    @Volatile
    private var activeConversationId: String? = null

    /** معرّف المستخدم الحالي — لا شريط لرسالة أرسلها هو من جهاز آخر. */
    @Volatile
    private var currentUserId: String? = null

    /**
     * هل في التطبيق نشاطٌ مرئي؟ يقرأها [HalaMessagingService] ليكتم إشعار النظام
     * للرسائل حين يكون الشريط الداخلي هو الذي سيظهر — وإلا رأى المستخدم إشعارين
     * للرسالة الواحدة.
     */
    @Volatile
    var appInForeground: Boolean = false
        internal set

    fun setActiveConversation(id: String?) {
        activeConversationId = id
        // مغادرة الشريط الحاليّ إن كان لنفس المحادثة التي فُتحت للتوّ.
        if (id != null && _current.value?.conversationId == id) _current.value = null
    }

    fun setCurrentUser(id: String?) {
        currentUserId = id
    }

    /**
     * يبني الشريط من حمولة `new-message` ويعرضه — أو يتجاهلها بصمت.
     *
     * شكل الحمولة مطابق لما يقرؤه `ChatsViewModel.applyIncomingMessage`: قد تأتي
     * الرسالة ملفوفة في `message` أو في الجذر مباشرةً.
     */
    fun onNewMessage(json: JSONObject) {
        val msg = json.optJSONObject("message") ?: json
        val convId = msg.optString("conversation").takeIf { it.isNotBlank() } ?: return
        if (convId == activeConversationId) return

        val senderObj = msg.optJSONObject("sender")
        val senderId = senderObj?.optString("_id")?.takeIf { it.isNotBlank() }
            ?: msg.optString("sender").takeIf { it.isNotBlank() }
        if (senderId != null && senderId == currentUserId) return

        _current.value = InAppMessageAlert(
            id = msg.optString("_id").takeIf { it.isNotBlank() } ?: convId + System.currentTimeMillis(),
            conversationId = convId,
            senderName = senderObj?.optString("name")?.takeIf { it.isNotBlank() },
            senderImage = senderObj?.optString("profileImage")?.takeIf { it.isNotBlank() },
            type = msg.optString("type").takeIf { it.isNotBlank() },
            content = msg.optString("content").takeIf { it.isNotBlank() }
        )
    }

    /** [id] فارغاً يُخفي أيّ شريط؛ وبقيمة يُخفي ذلك الشريط وحده (مؤقّت انتهى بعد وصول غيره). */
    fun dismiss(id: String? = null) {
        if (id == null || _current.value?.id == id) _current.value = null
    }
}
