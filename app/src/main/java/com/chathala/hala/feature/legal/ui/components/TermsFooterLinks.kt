package com.chathala.hala.feature.legal.ui.components

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import com.chathala.hala.R

/** سطر واحد صغير: «باستخدامك … توافق على [الشروط] و[الخصوصية]» — الروابط قابلة للنقر داخل النص. */
@Composable
fun TermsFooterLinks(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    val text = buildAnnotatedString {
        append(S.get(R.string.terms_footer_prefix))
        append(" ")
        withLink(LinkAnnotation.Clickable("terms", TextLinkStyles(style = linkStyle)) { onOpenTerms() }) {
            append(S.get(R.string.terms_title))
        }
        append(" ")
        append(S.get(R.string.terms_and))
        append(" ")
        withLink(LinkAnnotation.Clickable("privacy", TextLinkStyles(style = linkStyle)) { onOpenPrivacy() }) {
            append(S.get(R.string.privacy_title))
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
