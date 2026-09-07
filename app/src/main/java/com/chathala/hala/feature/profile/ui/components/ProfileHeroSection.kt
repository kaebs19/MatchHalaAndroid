package com.chathala.hala.feature.profile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chathala.hala.R
import com.chathala.hala.core.i18n.S
import com.chathala.hala.ui.components.HalaAsyncImage

/** الهامش السفلي للهيدر — بطاقة النشاط تطفو فوقه بهذا القدر ناقص 16dp. */
val HERO_BOTTOM_PADDING = 56.dp

/** مقدار تداخل قسم المعلومات مع الهيدر (مثل iOS: -40). */
val HERO_OVERLAP = 40.dp

/**
 * هيدر الملف الشخصي على نمط iOS: تدرّج من اللون الأساسي إلى خلفية الشاشة، الصورة
 * بحلقة ذهبية للمشتركين، الاسم والعمر والتاج، ثم رقائق الجنس/التوثيق/الدولة.
 *
 * [topBar] يُرسم داخل التدرّج في الأعلى (أيقونات الإجراءات).
 */
@Composable
fun ProfileHeroSection(
    name: String,
    age: Int?,
    imageUrl: String?,
    gender: String?,
    countryLabel: String?,
    isVerified: Boolean,
    isPremium: Boolean,
    isUploading: Boolean,
    onChangePhoto: () -> Unit,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val gradient = Brush.verticalGradient(
        colors = listOf(
            primary.copy(alpha = 0.85f),
            primary.copy(alpha = 0.40f),
            MaterialTheme.colorScheme.background
        )
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(bottom = HERO_BOTTOM_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topBar()
        Spacer(Modifier.height(12.dp))
        HeroAvatar(
            imageUrl = imageUrl,
            name = name,
            isPremium = isPremium,
            isUploading = isUploading,
            onChangePhoto = onChangePhoto
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (age != null) {
                Text(
                    text = age.toString(),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isPremium) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (gender) {
                "female" -> HeroChip(Icons.Filled.Female, S.get(R.string.gender_female_short), Color(0xFFFF2D8F))
                "male" -> HeroChip(Icons.Filled.Male, S.get(R.string.gender_male_short), Color(0xFF2196F3))
            }
            if (isVerified) {
                HeroChip(Icons.Filled.Verified, S.get(R.string.profile_verified_chip), Color(0xFF00BCD4))
            }
            if (!countryLabel.isNullOrBlank()) {
                HeroChip(Icons.Filled.Place, countryLabel, primary)
            }
        }
    }
}

@Composable
private fun HeroAvatar(
    imageUrl: String?,
    name: String,
    isPremium: Boolean,
    isUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    val ring: BorderStroke = if (isPremium) {
        BorderStroke(
            3.dp,
            Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFD700)))
        )
    } else {
        BorderStroke(3.dp, Color.White.copy(alpha = 0.9f))
    }
    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = if (isPremium) Color(0xFFFFA000) else Color.Black,
                    spotColor = if (isPremium) Color(0xFFFFA000) else Color.Black
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(ring, CircleShape)
                .padding(3.dp)
        ) {
            HalaAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallbackName = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(36.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable(enabled = !isUploading, onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroChip(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(5.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
