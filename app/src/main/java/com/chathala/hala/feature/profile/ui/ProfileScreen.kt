package com.chathala.hala.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.i18n.S
import com.chathala.hala.core.util.MediaUploadHelper
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.core.util.showToast
import com.chathala.hala.feature.profile.ui.components.CountPill
import com.chathala.hala.feature.profile.ui.components.HERO_OVERLAP
import com.chathala.hala.feature.profile.ui.components.PendingBadge
import com.chathala.hala.feature.profile.ui.components.PremiumBanner
import com.chathala.hala.feature.profile.ui.components.ProfileCompletionBanner
import com.chathala.hala.feature.profile.ui.components.ProfileEntryRow
import com.chathala.hala.feature.profile.ui.components.ProfileHeaderBar
import com.chathala.hala.feature.profile.ui.components.ProfileHeroSection
import com.chathala.hala.feature.profile.ui.components.ProfileSkeleton
import com.chathala.hala.feature.profile.ui.components.ProfileStatsCard
import com.chathala.hala.feature.profile.ui.components.QrCodeSheet
import com.chathala.hala.feature.profile.ui.components.computeProfileCompletion
import com.chathala.hala.feature.settings.ui.ThemePickerSheet
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost
import kotlinx.coroutines.launch

/**
 * تبويب الملف الشخصي على نمط iOS: هيدر متدرّج، بطاقة نشاط تطفو فوق حافته، بانر
 * اكتمال الملف، مدخل «ملفي التعريفي» (التفاصيل في شاشة فرعية)، بطاقة الأصدقاء،
 * وصفّ الاشتراك. لا زر خروج هنا — موضعه الإعدادات.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenVisitors: () -> Unit = {},
    onOpenMyInfo: () -> Unit = {},
    onOpenChats: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = rememberHalaSnackbarHost()
    var showQrSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val part = MediaUploadHelper.uriToImagePart(
                    context = context,
                    uri = uri,
                    fieldName = "profileImage"
                )
                if (part != null) {
                    viewModel.uploadPhoto(part)
                } else {
                    context.showToast(S.get(R.string.profile_photo_read_failed))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.pullToRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val currentUser = user
                if (currentUser == null) {
                    ProfileSkeleton()
                    return@Column
                }

                val age = ProfileFormatter.computeAge(currentUser.birthDate)
                val countryLabel = currentUser.country?.let { code ->
                    Countries.list.firstOrNull { it.code == code }?.let { "${it.flag} ${it.name}" }
                }

                ProfileHeroSection(
                    name = currentUser.name,
                    age = age,
                    imageUrl = currentUser.profileImage,
                    gender = currentUser.gender,
                    countryLabel = countryLabel,
                    isVerified = currentUser.isVerified,
                    isPremium = currentUser.isPremium,
                    isUploading = uploading,
                    onChangePhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    topBar = {
                        ProfileHeaderBar(
                            title = S.get(R.string.profile_screen_title),
                            onSettings = onOpenSettings,
                            onShowQr = { showQrSheet = true },
                            onTheme = { showThemeSheet = true },
                            onEdit = onEditProfile
                        )
                    }
                )

                // قسم المعلومات يتداخل مع أسفل الهيدر (مثل iOS: -40)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = -HERO_OVERLAP)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileStatsCard(
                        stats = stats,
                        onFriends = onOpenFriends,
                        onChats = onOpenChats,
                        onVisitors = onOpenVisitors
                    )

                    val completion = computeProfileCompletion(currentUser)
                    if (completion.fraction < 0.8f) {
                        ProfileCompletionBanner(completion = completion, onClick = onEditProfile)
                    }

                    ProfileEntryRow(
                        icon = Icons.Filled.ContactPage,
                        iconColor = Color(0xFF9C27B0),
                        title = S.get(R.string.profile_my_info_title),
                        subtitle = S.get(R.string.profile_my_info_subtitle),
                        onClick = onOpenMyInfo
                    )

                    ProfileEntryRow(
                        icon = Icons.Filled.People,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = S.get(R.string.friends_title),
                        subtitle = S.get(R.string.profile_friends_subtitle),
                        onClick = onOpenFriends,
                        titleTrailing = if (stats.friends > 0) ({ CountPill(stats.friends) }) else null,
                        trailing = if (stats.pendingRequests > 0) ({
                            PendingBadge(S.get(R.string.profile_friend_requests_badge, stats.pendingRequests))
                        }) else null
                    )

                    if (!currentUser.isPremium) {
                        PremiumBanner(onClick = onOpenPremium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    val currentUserSnapshot = user
    if (showQrSheet && currentUserSnapshot != null) {
        QrCodeSheet(
            userId = currentUserSnapshot.id,
            displayName = currentUserSnapshot.name,
            onDismiss = { showQrSheet = false }
        )
    }
    if (showThemeSheet) {
        ThemePickerSheet(onDismiss = { showThemeSheet = false })
    }
}
