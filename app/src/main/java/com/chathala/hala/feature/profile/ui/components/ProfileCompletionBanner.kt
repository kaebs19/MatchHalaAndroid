package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.feature.user.data.User

/** نسبة اكتمال الملف من سبعة حقول + أسماء الحقول الناقصة (بترتيب ثابت). */
data class ProfileCompletion(val fraction: Float, val missingLabels: List<String>)

fun computeProfileCompletion(user: User): ProfileCompletion {
    val checks = listOf(
        user.name.isNotBlank() to R.string.profile_missing_name,
        !user.profileImage.isNullOrBlank() to R.string.profile_missing_photo,
        !user.bio.isNullOrBlank() to R.string.profile_missing_bio,
        !user.gender.isNullOrBlank() to R.string.profile_missing_gender,
        !user.birthDate.isNullOrBlank() to R.string.profile_missing_birth,
        !user.country.isNullOrBlank() to R.string.profile_missing_country,
        user.interests.isNotEmpty() to R.string.profile_missing_interests
    )
    val filled = checks.count { it.first }
    return ProfileCompletion(
        fraction = filled / checks.size.toFloat(),
        missingLabels = checks.filterNot { it.first }.map { S.get(it.second) }
    )
}

private val WarningColor = Color(0xFFD48A00)

/** بانر «أكمل ملفك» — دائرة تقدّم + الحقول الناقصة كرقائق؛ النقر يفتح التعديل. */
@Composable
fun ProfileCompletionBanner(
    completion: ProfileCompletion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, primary.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                val track = primary.copy(alpha = 0.2f)
                Canvas(modifier = Modifier.size(48.dp)) {
                    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    val inset = stroke.width / 2
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = stroke)
                    drawArc(primary, -90f, 360f * completion.fraction, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = stroke)
                }
                Text(
                    text = "${(completion.fraction * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = S.get(R.string.profile_completion_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = S.get(R.string.profile_completion_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        if (completion.missingLabels.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(completion.missingLabels) { label ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(WarningColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = WarningColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WarningColor)
                    }
                }
            }
        }
    }
}
