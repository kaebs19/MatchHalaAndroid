package com.chathala.hala.feature.settings.ui.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.feature.settings.ui.components.SettingsScaffold

@Composable
fun ContentPreferencesScreen(
    onBack: () -> Unit,
    viewModel: ContentPreferencesViewModel = viewModel(
        factory = ContentPreferencesViewModel.Factory(LocalContext.current)
    )
) {
    val enabled by viewModel.sensitiveContentEnabled.collectAsStateWithLifecycle(initialValue = false)

    SettingsScaffold(title = "المحتوى الحساس", onBack = onBack, scrollable = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // أيقونة العين مع علامة تحذير
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3D1A00)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👁️⚠️", style = MaterialTheme.typography.headlineLarge)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "عرض المحتوى الحساس",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "تطبيقنا يحجب تلقائياً الكلمات الجنسية في المحادثات لحمايتك.\nيمكنك (لو كنت بالغاً) السماح بعرضها بضغطة واحدة عند الحاجة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            Spacer(Modifier.height(24.dp))

            // بطاقة الحالة الحالية
            AnimatedVisibility(visible = enabled) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0D3320))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الميزة مفعّلة. يمكنك الكشف عن المحتوى المحجوب بضغطة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.size(10.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // مفتاح السماح بالعرض
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "السماح بالعرض",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE91E8C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "اضغط للتفعيل/التعطيل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setEnabled(it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ميزات/تفاصيل
            InfoCard(
                icon = Icons.Filled.Shield,
                iconTint = Color(0xFF2196F3),
                title = "حمايتك أولوية",
                description = "الحظر افتراضي. التفعيل اختياري بضغطة منك فقط."
            )
            Spacer(Modifier.height(8.dp))
            InfoCard(
                icon = Icons.Filled.People,
                iconTint = Color(0xFFE53935),
                title = "للبالغين فقط (+18)",
                description = "نتحقق من العمر بناءً على تاريخ ميلادك المحفوظ."
            )
            Spacer(Modifier.height(8.dp))
            InfoCard(
                icon = Icons.Filled.Loop,
                iconTint = Color(0xFF43A047),
                title = "يمكنك الإلغاء أي وقت",
                description = "أطفئ التوجيل وستعودون الكلمات للحجب فوراً."
            )
            Spacer(Modifier.height(8.dp))
            InfoCard(
                icon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                iconTint = Color(0xFFF57C00),
                title = "روابط خارجية تبقى محجوبة",
                description = "حسابات سناب/إنستا/واتساب تبقى دائماً محجوبة لحمايتك."
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(14.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
