package com.chathala.hala.feature.profile.ui.components

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * صفّ الاشتراك المميّز على نمط iOS: تدرّج وردي → بنفسجي بظلّ وردي، تاج أصفر في
 * دائرة زجاجية، ونص أبيض. يُعرض لغير المشتركين فقط؛ النقر يفتح شاشة الاشتراك.
 */
@Composable
fun PremiumBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val gradient = Brush.horizontalGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color(0xFFE91E63).copy(alpha = 0.35f), spotColor = Color(0xFFE91E63).copy(alpha = 0.35f))
            .clip(shape)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFFFEB3B),
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = S.get(R.string.premium_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(text = "👑", fontSize = 14.sp)
            }
            Text(
                text = S.get(R.string.premium_subtitle),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * شريط هلا+ الرفيع قبل الإحصائيات: تدرّج وردي-ذهبي، «اعرف من زارك · 10 قصص · بلا
 * إعلانات» وزرّ «اشترك». يفتح الاشتراك **ورقةً** (لا تنقّلاً).
 */
@Composable
fun PremiumStrip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFE91E63), Color(0xFFFFA000))))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "👑", fontSize = 15.sp)
        Text(
            text = S.get(R.string.premium_strip_text),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = S.get(R.string.premium_strip_cta),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE91E63),
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
