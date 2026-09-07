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
// أبيض نقي في الفاتح وأسود نقي في الداكن؛ الهوية الوردية تبقى في الأزرار والتمييز
// لا في الخلفيات، فتبدو الواجهة أنظف ويستفيد الداكن من شاشات OLED.
val HalaBgLight = Color(0xFFFFFFFF)
val HalaBgDark = Color(0xFF000000)

// البطاقات: أبيض على أبيض يتمايز بحدّ خفيف (outline)، وفي الداكن طبقة رمادية دافئة
val HalaCardLight = Color(0xFFFFFFFF)
val HalaCardDark = Color(0xFF141216)

// الحقول والفقاعات الواردة: رمادي وردي باهت جداً / رمادي دافئ داكن
val HalaInputLight = Color(0xFFF6F1F4)
val HalaInputDark = Color(0xFF1F1C23)

// ─── Containers (خلفيات مُلوّنة للشارات والرقائق) ───
val HalaPrimaryContainerLight = Color(0xFFFDE4F0)
val HalaPrimaryContainerDark = Color(0xFF3A1230)

val HalaErrorContainerLight = Color(0xFFFDECEA)
val HalaErrorContainerDark = Color(0xFF3B1A1A)

// ─── Text ───
val HalaTextPrimaryLight = Color(0xFF1A171D)
val HalaTextPrimaryDark = Color(0xFFF5F2F7)

// نص ثانوي محايد بلمسة بنفسجية بدل البنفسجي الصريح — أهدأ وأوضح تراتبياً
val HalaTextSecondaryLight = Color(0xFF6E6479)
val HalaTextSecondaryDark = Color(0xFFA9A2B3)

val HalaTextInvertedLight = Color(0xFFFFFFFF)
val HalaTextInvertedDark = Color(0xFF1A0020)

// ─── Borders & Dividers ───
val HalaBorderLight = Color(0xFFE6DAE2)
val HalaBorderDark = Color(0xFF3A3441)

val HalaDividerLight = Color(0xFFEFE9ED)
val HalaDividerDark = Color(0xFF26222B)

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
