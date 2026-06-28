package com.chathala.hala.core.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * اختصارات لاهتزاز خفيف/متوسط/طويل من داخل Compose.
 * `HapticFeedbackType` يوفّر `LongPress` و`TextHandleMove` — نحن نعيد تعيين
 * الدلالة لاستخدامات شائعة (light = select، medium = success).
 */
object HapticHelper {

    fun light(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun medium(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
