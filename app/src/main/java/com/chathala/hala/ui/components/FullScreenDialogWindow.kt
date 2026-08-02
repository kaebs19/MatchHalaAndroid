package com.chathala.hala.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

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
