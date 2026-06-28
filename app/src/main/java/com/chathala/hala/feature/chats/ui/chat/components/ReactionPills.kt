package com.chathala.hala.feature.chats.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chathala.hala.feature.chats.data.Reaction

/**
 * يُجمّع الـ reactions حسب الإيموجي ويعرضها كـ pills صغيرة.
 * إذا كان currentUserId ضمنهم → شريط بلون primary مميّز.
 */
@Composable
fun ReactionPills(
    reactions: List<Reaction>,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return
    val grouped = reactions.groupBy { it.emoji }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        grouped.entries.take(6).forEach { (emoji, list) ->
            val mine = currentUserId != null && list.any { it.user == currentUserId }
            // pop-in: ينطّ الـ pill عند ظهوره أول مرة
            val scale = remember(emoji) { androidx.compose.animation.core.Animatable(0f) }
            androidx.compose.runtime.LaunchedEffect(emoji) {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                )
            }
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value; scaleY = scale.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (mine)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (mine)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, style = MaterialTheme.typography.labelMedium)
                if (list.size > 1) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = list.size.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (mine)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
