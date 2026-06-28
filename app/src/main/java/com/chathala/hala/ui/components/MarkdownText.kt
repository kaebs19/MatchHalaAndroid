package com.chathala.hala.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * عارض Markdown خفيف (بدون تبعيات) — يكفي لمحتوى الشروط/الخصوصية القادم من السيرفر.
 * يدعم: العناوين (#، ##، ###)، النصّ العريض (**)، القوائم (- / *)، والفقرات.
 * أي صيغة أخرى تُعرض كنصّ عادي.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    Column(modifier = modifier) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(8.dp))

                line.startsWith("### ") -> Heading(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleMedium,
                    topPadding = 10.dp
                )
                line.startsWith("## ") -> Heading(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.titleLarge,
                    topPadding = 14.dp
                )
                line.startsWith("# ") -> Heading(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineSmall,
                    topPadding = 8.dp
                )

                line.startsWith("- ") || line.startsWith("* ") -> BulletRow(line.drop(2))

                else -> Text(
                    text = parseInline(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun Heading(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    topPadding: androidx.compose.ui.unit.Dp
) {
    Text(
        text = parseInline(text),
        style = style.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
    )
}

@Composable
private fun BulletRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "•  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = parseInline(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** يحوّل **النص العريض** إلى AnnotatedString. */
private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val start = text.indexOf("**", i)
        if (start == -1) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, start))
        val end = text.indexOf("**", start + 2)
        if (end == -1) {
            append(text.substring(start))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 2, end))
        }
        i = end + 2
    }
}
