package com.chathala.hala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.HalaApp
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.storage.AppLanguage
import kotlinx.coroutines.launch

/**
 * زر مُدمج لتبديل اللغة — يُستخدم في شاشات المصادقة قبل توفّر الإعدادات.
 *
 * التبديل فوري: نحدّث [LocaleManager] مباشرةً ثم نحفظ في DataStore، فتُعاد تركيب
 * كل النصوص واتجاه التخطيط (RTL/LTR) في نفس اللحظة دون إعادة تشغيل.
 */
@Composable
fun LanguageToggleChip(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as HalaApp
    val scope = rememberCoroutineScope()
    val current by app.appPreferences.language.collectAsState(initial = LocaleManager.current)

    // زر ثنائي: يعرض دائماً اللغة الأخرى — أي ما سيصير إليه عند الضغط
    val next = if (current == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    val nextLabel = if (next == AppLanguage.ARABIC) "العربية" else "English"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable {
                LocaleManager.setLanguage(next)
                scope.launch { app.appPreferences.setLanguage(next) }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = nextLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}
