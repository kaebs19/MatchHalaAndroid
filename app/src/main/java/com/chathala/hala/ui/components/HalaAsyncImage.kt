package com.chathala.hala.ui.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * صورة شبكية احترافية موحّدة:
 *  - أثناء التحميل: تأثير shimmer ([SkeletonBlock]).
 *  - عند الفشل: أيقونة «صورة معطوبة» على خلفية هادئة.
 *
 * تُستخدم بدل [coil.compose.AsyncImage] المباشر لتوحيد التجربة.
 *
 * @param fallbackName اسم المستخدم — يُمرَّر في الأفاتار فقط. عند تمريره يحلّ
 * [InitialAvatar] (الحرف الأول) محلّ أيقونة «الصورة المعطوبة»، سواء كان
 * [model] فارغاً أصلاً أو فشل تحميله. مستخدمو فيسبوك/Google بلا صورة كانوا
 * يظهرون بأيقونة عطل توحي بخلل في التطبيق.
 */
@Composable
fun HalaAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackName: String? = null
) {
    // رابط فارغ = لا صورة أصلاً؛ لا داعي لتمريره على Coil ليفشل ثم نعالج الفشل.
    val hasModel = when (model) {
        null -> false
        is String -> model.isNotBlank()
        else -> true
    }
    if (!hasModel && fallbackName != null) {
        InitialAvatar(name = fallbackName, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading ->
                SkeletonBlock(modifier = Modifier.fillMaxSize())
            is AsyncImagePainter.State.Error ->
                if (fallbackName != null) {
                    InitialAvatar(name = fallbackName, modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = S.get(R.string.err_image_load_failed),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            else -> SubcomposeAsyncImageContent()
        }
    }
}
