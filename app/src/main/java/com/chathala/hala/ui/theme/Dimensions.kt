package com.chathala.hala.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * مقاسات موحّدة للتطبيق — استخدمها بدل الأرقام اليدوية.
 *
 * ```
 * Modifier.padding(HalaDimens.Spacing.md)
 * Modifier.size(HalaDimens.Icon.md)
 * ```
 */
object HalaDimens {

    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
    }

    object Corner {
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 28.dp
        val pill = 50.dp
    }

    object Icon {
        val sm = 16.dp
        val md = 20.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
    }

    object Height {
        val fieldRow = 58.dp
        val button = 56.dp
        val bar = 64.dp
    }

    object Avatar {
        val sm = 48.dp
        val md = 80.dp
        val lg = 140.dp
    }

    object Elevation {
        val sm = 4.dp
        val md = 8.dp
        val lg = 12.dp
        val xl = 16.dp
    }

    object Text {
        val tiny = 12.sp
        val small = 14.sp
        val body = 16.sp
        val title = 20.sp
        val headline = 28.sp
    }
}
