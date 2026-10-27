package com.chathala.hala.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only

/**
 * يجعل نافذة [androidx.compose.ui.window.Dialog] الحاوية بملء الشاشة فعلياً.
 *
 * **لماذا:** نافذة الحوار «عائمة»، فيقتطع منها النظام أشرطة الحالة والتنقّل
 * (مثال: إطار بارتفاع 2274px على شاشة 2400px)، بينما يقيس Compose المحتوى
 * بارتفاع الشاشة الكامل حين `usePlatformDefaultWidth = false` → يُقصّ أسفل
 * المحتوى خلف شريط التنقّل. و`decorFitsSystemWindows = false` لا يُجدي هنا:
 * فهو بلا أثر للنوافذ العائمة على أندرويد 15+.
 *
 * يُستدعى داخل محتوى الحوار، ثم تُعالَج الحواف بـ`windowInsetsPadding` كالمعتاد.
 */
@Composable
fun FullScreenDialogWindow() {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}

/**
 * مثل `windowInsetsPadding(WindowInsets.safeDrawing)` لمحتوى حوار بملء الشاشة، مع
 * احتياط للحافة العلوية: نافذة الحوار مع FLAG_LAYOUT_NO_LIMITS قد لا تستلم إزاحة شريط
 * الحالة (صفر) فتُرسم الأزرار العلوية تحته — نأخذها حينها من نافذة النشاط.
 */
@Composable
fun Modifier.dialogSafeDrawing(): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safe = androidx.compose.foundation.layout.WindowInsets.safeDrawing
    val view = LocalView.current
    val activityTop = androidx.compose.runtime.remember(view) {
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        activity?.window?.decorView?.let { decor ->
            androidx.core.view.ViewCompat.getRootWindowInsets(decor)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
                ?.top
        } ?: 0
    }
    val top = safe.getTop(density)
    val withSafe = this.windowInsetsPadding(safe)
    return if (top == 0 && activityTop > 0) withSafe.padding(top = with(density) { activityTop.toDp() }) else withSafe
}

/**
 * الحافة العلوية وحدها (شريط الحالة/القطع) — لعناصر مثبّتة أعلى حوار بملء الشاشة وارتفاعها
 * بقدر محتواها. ⚠️ لا تستعمل [dialogSafeDrawing] لها: إزاحة لوحة المفاتيح السفلية تُطبَّق
 * عليها أيضاً فيُعطى المحتوى ارتفاعاً صفراً ويختفي (✕ والعدّاد في القصة النصّية).
 */
@Composable
fun Modifier.dialogSafeTop(): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sides = androidx.compose.foundation.layout.WindowInsetsSides.Top + androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
    val safe = androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(sides)
    val padded = this.windowInsetsPadding(safe)
    if (safe.getTop(density) > 0) return padded
    val view = LocalView.current
    val activityTop = androidx.compose.runtime.remember(view) {
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        activity?.window?.decorView?.let { decor ->
            androidx.core.view.ViewCompat.getRootWindowInsets(decor)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
                ?.top
        } ?: 0
    }
    return padded.padding(top = with(density) { activityTop.toDp() })
}

/** الحافة السفلية وحدها (شريط التنقّل ولوحة المفاتيح) — لعناصر مثبّتة أسفل الحوار. */
@Composable
fun Modifier.dialogSafeBottom(): Modifier = this.windowInsetsPadding(
    androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(
        androidx.compose.foundation.layout.WindowInsetsSides.Bottom + androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
    )
)
