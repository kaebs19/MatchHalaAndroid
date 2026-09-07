package com.chathala.hala.feature.profile.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.profile.ui.components.ProfileInfoRow
import com.chathala.hala.feature.profile.ui.components.ProfilePalette
import com.chathala.hala.feature.profile.ui.components.ProfileSectionCard
import com.chathala.hala.feature.profile.ui.components.ReadOnlyInterestsChips
import com.chathala.hala.feature.user.data.User

// بطاقات تفاصيل الملف — كانت في ProfileScreen، وانتقلت إلى شاشة «ملفي التعريفي»
// (مثل iOS) لتبقى الشاشة الرئيسية للملف موجزة.

@Composable
internal fun IdentityCard(user: User) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val userIdDisplay = ProfileFormatter.formatUserId(user.id)
    val joinDate = ProfileFormatter.formatJoinDate(user.joinDate)

    ProfileSectionCard {
        ProfileInfoRow(
            icon = Icons.Filled.Badge,
            iconTint = ProfilePalette.Id,
            iconBackground = ProfilePalette.bg(ProfilePalette.Id),
            label = S.get(R.string.profile_label_id),
            value = userIdDisplay,
            trailing = {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(userIdDisplay))
                        context.showToast(S.get(R.string.action_copied))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
        if (joinDate != null) {
            ProfileInfoRow(
                icon = Icons.Filled.CalendarMonth,
                iconTint = ProfilePalette.Calendar,
                iconBackground = ProfilePalette.bg(ProfilePalette.Calendar),
                label = S.get(R.string.profile_label_joined),
                value = joinDate,
                showDivider = false
            )
        }
    }
}

@Composable
internal fun BasicInfoCard(user: User) {
    val notSet = S.get(R.string.profile_not_set)
    val age = ProfileFormatter.computeAge(user.birthDate)
    val countryLabel = user.country?.let { code ->
        val country = Countries.list.firstOrNull { it.code == code }
        country?.let { "${it.flag}  ${it.name}" } ?: code
    }
    val genderValue = when (user.gender) {
        "male" -> S.get(R.string.profile_gender_male)
        "female" -> S.get(R.string.profile_gender_female)
        else -> notSet
    }
    val genderIcon = if (user.gender == "female") Icons.Filled.Female else Icons.Filled.Male
    val ageValue = age?.let { S.get(R.string.profile_age_years, it) } ?: notSet
    val subscription = if (user.isPremium)
        S.get(R.string.profile_subscription_premium)
    else
        S.get(R.string.profile_subscription_free)

    ProfileSectionCard(
        title = S.get(R.string.profile_section_basic),
        titleIcon = Icons.Filled.Info,
        titleIconTint = MaterialTheme.colorScheme.primary
    ) {
        ProfileInfoRow(
            icon = genderIcon,
            iconTint = ProfilePalette.Gender,
            iconBackground = ProfilePalette.bg(ProfilePalette.Gender),
            label = S.get(R.string.profile_label_gender),
            value = genderValue
        )
        ProfileInfoRow(
            icon = Icons.Filled.Cake,
            iconTint = ProfilePalette.Age,
            iconBackground = ProfilePalette.bg(ProfilePalette.Age),
            label = S.get(R.string.profile_label_age),
            value = ageValue
        )
        ProfileInfoRow(
            icon = Icons.Filled.Public,
            iconTint = ProfilePalette.Country,
            iconBackground = ProfilePalette.bg(ProfilePalette.Country),
            label = S.get(R.string.profile_label_country),
            value = countryLabel ?: notSet
        )
        user.city?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoRow(
                icon = Icons.Filled.LocationCity,
                iconTint = ProfilePalette.Country,
                iconBackground = ProfilePalette.bg(ProfilePalette.Country),
                label = S.get(R.string.profile_label_city),
                value = it
            )
        }
        user.zodiacSign?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoRow(
                icon = Icons.Filled.Stars,
                iconTint = ProfilePalette.Premium,
                iconBackground = ProfilePalette.bg(ProfilePalette.Premium),
                label = S.get(R.string.profile_label_zodiac),
                value = it
            )
        }
        ProfileInfoRow(
            icon = Icons.Filled.WorkspacePremium,
            iconTint = ProfilePalette.Premium,
            iconBackground = ProfilePalette.bg(ProfilePalette.Premium),
            label = S.get(R.string.profile_label_subscription),
            value = subscription,
            showDivider = false
        )
    }
}

@Composable
internal fun BioCard(bio: String?) {
    ProfileSectionCard(
        title = S.get(R.string.profile_section_bio),
        titleIcon = Icons.Filled.Description,
        titleIconTint = ProfilePalette.Bio
    ) {
        Text(
            text = bio?.takeIf { it.isNotBlank() }
                ?: S.get(R.string.profile_no_bio),
            style = MaterialTheme.typography.bodyLarge,
            color = if (bio.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
internal fun InterestsCard(interests: List<String>) {
    ProfileSectionCard(
        title = S.get(R.string.profile_section_interests),
        titleIcon = Icons.Filled.AutoAwesome,
        titleIconTint = ProfilePalette.Interests,
        countBadge = interests.size.takeIf { it > 0 }
    ) {
        if (interests.isEmpty()) {
            Text(
                text = S.get(R.string.profile_no_interests),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            Spacer(Modifier.height(6.dp))
            ReadOnlyInterestsChips(interestKeys = interests)
        }
    }
}
