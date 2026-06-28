package com.chathala.hala.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════
// نظام ألوان هلا — متطابق مع نسخة iOS
// ════════════════════════════════════════════════════════════

// ─── Primary (الوردي) ───
val HalaPrimaryLight = Color(0xFFE91E8C)
val HalaPrimaryDark = Color(0xFFFF4DB8)

// ─── Secondary (البنفسجي) ───
val HalaSecondaryLight = Color(0xFF7B1FA2)
val HalaSecondaryDark = Color(0xFFCE93D8)

// ─── Accent (الأحمر المميز) ───
val HalaAccentLight = Color(0xFFD32F2F)
val HalaAccentDark = Color(0xFFFF5252)

// ─── Backgrounds ───
val HalaBgLight = Color(0xFFFFF0F5)          // وردي فاتح جداً
val HalaBgDark = Color(0xFF0D0010)            // بنفسجي غامق

val HalaCardLight = Color(0xFFFFFFFF)
val HalaCardDark = Color(0xFF1A0020)

val HalaInputLight = Color(0xFFFCE4EC)
val HalaInputDark = Color(0xFF2D0035)

// ─── Text ───
val HalaTextPrimaryLight = Color(0xFF1A0020)
val HalaTextPrimaryDark = Color(0xFFFFFFFF)

val HalaTextSecondaryLight = Color(0xFF7B1FA2)
val HalaTextSecondaryDark = Color(0xFFCE93D8)

val HalaTextInvertedLight = Color(0xFFFFFFFF)
val HalaTextInvertedDark = Color(0xFF1A0020)

// ─── Borders & Dividers ───
val HalaBorderLight = Color(0xFFF48FB1)
val HalaBorderDark = Color(0xFF4A0050)

val HalaDividerLight = Color(0xFFFCE4EC)
val HalaDividerDark = Color(0xFF2D0035)

// ─── Semantic ───
val HalaSuccessLight = Color(0xFF4CAF50)
val HalaSuccessDark = Color(0xFF81C784)

val HalaWarningLight = Color(0xFFFFC107)
val HalaWarningDark = Color(0xFFFFD54F)

val HalaErrorLight = Color(0xFFD32F2F)
val HalaErrorDark = Color(0xFFFF5252)

val HalaInfoLight = Color(0xFF7B1FA2)
val HalaInfoDark = Color(0xFFCE93D8)

// ─── Utilities ───
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ─── حدّ متباين مع الثيم: أبيض ناعم في الداكن، بنّي أنيق في الفاتح ───
// يُستخدم لإبراز كرت الهيدر وحقل كتابة الرسالة بشكل احترافي.
val HalaBorderBrown = Color(0xFF8D6E63)   // بنّي دافئ (Material Brown 400)

@androidx.compose.runtime.Composable
@androidx.compose.runtime.ReadOnlyComposable
fun contrastBorderColor(): Color =
    if (androidx.compose.foundation.isSystemInDarkTheme()) {
        White.copy(alpha = 0.30f)
    } else {
        HalaBorderBrown.copy(alpha = 0.55f)
    }
