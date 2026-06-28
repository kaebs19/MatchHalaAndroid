package com.chathala.hala.feature.chats.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val UrlRegex = Regex("(?i)\\b((?:https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)")
private val PhoneRegex = Regex("(?<![\\d+])\\+?\\d[\\d\\s\\-]{6,15}\\d")

/**
 * نص الرسالة مع تمييز الروابط وأرقام الهاتف وجعلها قابلة للنقر.
 * النقر على رابط → يفتح المتصفح، النقر على رقم → dialer.
 */
@Composable
fun LinkifiedText(
    text: String,
    color: Color,
    linkColor: Color = MaterialTheme.colorScheme.tertiary,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotated = remember(text, color, linkColor) {
        buildLinkified(text, color, linkColor)
    }
    ClickableText(
        text = annotated,
        style = style.copy(color = color),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                runCatching {
                    val url = if (ann.item.startsWith("http", ignoreCase = true)) ann.item
                              else "https://${ann.item}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return@ClickableText
            }
            annotated.getStringAnnotations("PHONE", offset, offset).firstOrNull()?.let { ann ->
                runCatching {
                    val tel = ann.item.replace(Regex("[\\s\\-]"), "")
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                }
            }
        }
    )
}

private fun buildLinkified(text: String, baseColor: Color, linkColor: Color): AnnotatedString =
    buildAnnotatedString {
        val combined = (UrlRegex.findAll(text).map { it to "URL" } +
                PhoneRegex.findAll(text).map { it to "PHONE" })
            .sortedBy { it.first.range.first }
            .toList()
        var cursor = 0
        for ((m, tag) in combined) {
            val s = m.range.first
            val e = m.range.last + 1
            if (s < cursor) continue
            if (s > cursor) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(cursor, s))
                }
            }
            val raw = m.value
            pushStringAnnotation(tag = tag, annotation = raw)
            withStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(raw)
            }
            pop()
            cursor = e
        }
        if (cursor < text.length) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(cursor))
            }
        }
    }
