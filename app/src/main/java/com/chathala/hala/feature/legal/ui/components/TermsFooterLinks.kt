package com.chathala.hala.feature.legal.ui.components

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.chathala.hala.R
import com.chathala.hala.ui.components.TextLink

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermsFooterLinks(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    // FlowRow لا Row: النص الإنجليزي أطول من العربي فكان آخر رابط يُقصّ ويلتفّ بحرف واحد.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = S.get(R.string.terms_footer_prefix),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextLink(
            text = S.get(R.string.terms_title),
            onClick = onOpenTerms
        )
        Text(
            text = " " + S.get(R.string.terms_and) + " ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextLink(
            text = S.get(R.string.privacy_title),
            onClick = onOpenPrivacy
        )
    }
}
