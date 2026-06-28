package com.chathala.hala.feature.profile.ui.components

import androidx.compose.ui.graphics.Color

/**
 * ألوان أيقونات الشارات في صفحة الملف الشخصي.
 * كل علامة ثابتة بصرياً بغض النظر عن theme (تضاف alpha للخلفية).
 */
internal object ProfilePalette {
    val Id = Color(0xFFE91E8C)         // وردي
    val Calendar = Color(0xFF34C759)   // أخضر
    val Gender = Color(0xFF2196F3)     // أزرق
    val Age = Color(0xFFFF9500)        // برتقالي
    val Country = Color(0xFFE91E8C)    // وردي
    val Premium = Color(0xFFFFC107)    // ذهبي
    val Bio = Color(0xFFE91E8C)        // وردي
    val Interests = Color(0xFFE91E8C)  // وردي

    fun bg(color: Color): Color = color.copy(alpha = 0.15f)
}
