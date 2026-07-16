package com.chathala.hala.feature.profile.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.core.util.showToast
import kotlinx.coroutines.launch
import com.chathala.hala.feature.profile.ui.components.PhotosGalleryCard
import com.chathala.hala.feature.profile.ui.components.ProfileAvatarSection
import com.chathala.hala.feature.profile.ui.components.ProfileHeaderBar
import com.chathala.hala.feature.profile.ui.components.ProfileInfoRow
import com.chathala.hala.feature.profile.ui.components.ProfilePalette
import com.chathala.hala.feature.profile.ui.components.ProfilePillsRow
import com.chathala.hala.feature.profile.ui.components.ProfileSectionCard
import com.chathala.hala.feature.profile.ui.components.ReadOnlyInterestsChips
import com.chathala.hala.feature.profile.ui.components.VerificationCard
import com.chathala.hala.feature.user.data.User

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHost = com.chathala.hala.ui.components.rememberHalaSnackbarHost()
    var showQrSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val part = com.chathala.hala.core.util.MediaUploadHelper.uriToImagePart(
                    context = context,
                    uri = uri,
                    fieldName = "profileImage"
                )
                if (part != null) {
                    viewModel.uploadPhoto(part)
                } else {
                    context.showToast("تعذّرت قراءة الصورة")
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHost.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.pullToRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeaderBar(
            title = stringResource(R.string.profile_screen_title),
            onSettings = onOpenSettings,
            onShowQr = { showQrSheet = true },
            onTheme = { showThemeSheet = true },
            onEdit = onEditProfile,
            onLogout = { viewModel.logout(onLoggedOut) }
        )

        val currentUser = user
        if (currentUser == null) {
            com.chathala.hala.feature.profile.ui.components.ProfileSkeleton()
        } else {
            val age = ProfileFormatter.computeAge(currentUser.birthDate)
            val countryLabel = currentUser.country?.let { code ->
                Countries.list.firstOrNull { it.code == code }?.let { "${it.flag}  ${it.nameAr}" }
            }

            Spacer(Modifier.height(8.dp))
            ProfileAvatarSection(
                name = currentUser.name,
                age = age,
                imageUrl = currentUser.profileImage,
                isVerified = currentUser.isVerified,
                isPremium = currentUser.isPremium,
                isUploading = uploading,
                onChangePhoto = {
                    photoPicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts
                                .PickVisualMedia.ImageOnly
                        )
                    )
                }
            )
            Spacer(Modifier.height(14.dp))
            ProfilePillsRow(
                countryLabel = countryLabel,
                gender = currentUser.gender
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // بانر الاشتراك الذهبي — لغير المشتركين فقط
                if (!currentUser.isPremium) {
                    com.chathala.hala.feature.profile.ui.components.PremiumBanner(
                        onClick = onOpenPremium
                    )
                }
                // ملاحظة: بطاقة توثيق الحساب (VerificationCard) أُلغيت حالياً من الملف الشخصي
                IdentityCard(user = currentUser)
                BasicInfoCard(user = currentUser)
                if (currentUser.photos.isNotEmpty()) {
                    PhotosGalleryCard(photos = currentUser.photos)
                }
                BioCard(bio = currentUser.bio)
                InterestsCard(interests = currentUser.interests)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    }
    com.chathala.hala.ui.components.HalaSnackbarHost(
        hostState = snackbarHost,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }

    val currentUserSnapshot = user
    if (showQrSheet && currentUserSnapshot != null) {
        com.chathala.hala.feature.profile.ui.components.QrCodeSheet(
            userId = currentUserSnapshot.id,
            displayName = currentUserSnapshot.name,
            onDismiss = { showQrSheet = false }
        )
    }
    if (showThemeSheet) {
        com.chathala.hala.feature.settings.ui.ThemePickerSheet(
            onDismiss = { showThemeSheet = false }
        )
    }
}

// ────────────────────────────────────────────────────────────
// Cards
// ────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(user: User) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val userIdDisplay = ProfileFormatter.formatUserId(user.id)
    val joinDate = ProfileFormatter.formatJoinDate(user.joinDate)

    ProfileSectionCard {
        ProfileInfoRow(
            icon = Icons.Filled.Badge,
            iconTint = ProfilePalette.Id,
            iconBackground = ProfilePalette.bg(ProfilePalette.Id),
            label = stringResource(R.string.profile_label_id),
            value = userIdDisplay,
            trailing = {
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(userIdDisplay))
                        context.showToast("تم النسخ")
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
                label = stringResource(R.string.profile_label_joined),
                value = joinDate,
                showDivider = false
            )
        }
    }
}

@Composable
private fun BasicInfoCard(user: User) {
    val notSet = stringResource(R.string.profile_not_set)
    val age = ProfileFormatter.computeAge(user.birthDate)
    val countryLabel = user.country?.let { code ->
        val country = Countries.list.firstOrNull { it.code == code }
        country?.let { "${it.flag}  ${it.nameAr}" } ?: code
    }
    val genderValue = when (user.gender) {
        "male" -> stringResource(R.string.profile_gender_male)
        "female" -> stringResource(R.string.profile_gender_female)
        else -> notSet
    }
    val genderIcon = if (user.gender == "female") Icons.Filled.Female else Icons.Filled.Male
    val ageValue = age?.let { stringResource(R.string.profile_age_years, it) } ?: notSet
    val subscription = if (user.isPremium)
        stringResource(R.string.profile_subscription_premium)
    else
        stringResource(R.string.profile_subscription_free)

    ProfileSectionCard(
        title = stringResource(R.string.profile_section_basic),
        titleIcon = Icons.Filled.Info,
        titleIconTint = MaterialTheme.colorScheme.primary
    ) {
        ProfileInfoRow(
            icon = genderIcon,
            iconTint = ProfilePalette.Gender,
            iconBackground = ProfilePalette.bg(ProfilePalette.Gender),
            label = stringResource(R.string.profile_label_gender),
            value = genderValue
        )
        ProfileInfoRow(
            icon = Icons.Filled.Cake,
            iconTint = ProfilePalette.Age,
            iconBackground = ProfilePalette.bg(ProfilePalette.Age),
            label = stringResource(R.string.profile_label_age),
            value = ageValue
        )
        ProfileInfoRow(
            icon = Icons.Filled.Public,
            iconTint = ProfilePalette.Country,
            iconBackground = ProfilePalette.bg(ProfilePalette.Country),
            label = stringResource(R.string.profile_label_country),
            value = countryLabel ?: notSet
        )
        user.city?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoRow(
                icon = Icons.Filled.LocationCity,
                iconTint = ProfilePalette.Country,
                iconBackground = ProfilePalette.bg(ProfilePalette.Country),
                label = stringResource(R.string.profile_label_city),
                value = it
            )
        }
        user.zodiacSign?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoRow(
                icon = Icons.Filled.Stars,
                iconTint = ProfilePalette.Premium,
                iconBackground = ProfilePalette.bg(ProfilePalette.Premium),
                label = stringResource(R.string.profile_label_zodiac),
                value = it
            )
        }
        ProfileInfoRow(
            icon = Icons.Filled.WorkspacePremium,
            iconTint = ProfilePalette.Premium,
            iconBackground = ProfilePalette.bg(ProfilePalette.Premium),
            label = stringResource(R.string.profile_label_subscription),
            value = subscription,
            showDivider = false
        )
    }
}

@Composable
private fun BioCard(bio: String?) {
    ProfileSectionCard(
        title = stringResource(R.string.profile_section_bio),
        titleIcon = Icons.Filled.Description,
        titleIconTint = ProfilePalette.Bio
    ) {
        Text(
            text = bio?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.profile_no_bio),
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
private fun InterestsCard(interests: List<String>) {
    ProfileSectionCard(
        title = stringResource(R.string.profile_section_interests),
        titleIcon = Icons.Filled.AutoAwesome,
        titleIconTint = ProfilePalette.Interests,
        countBadge = interests.size.takeIf { it > 0 }
    ) {
        if (interests.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_no_interests),
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
