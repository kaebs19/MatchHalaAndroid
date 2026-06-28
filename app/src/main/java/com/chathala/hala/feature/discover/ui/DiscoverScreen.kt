package com.chathala.hala.feature.discover.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.core.ads.findActivity
import com.chathala.hala.core.util.LocationHelper
import com.chathala.hala.feature.discover.data.DiscoverCard
import com.chathala.hala.feature.discover.ui.components.CardDetailSheet
import com.chathala.hala.feature.discover.ui.components.FiltersSheet
import com.chathala.hala.feature.discover.ui.components.PremiumGateDialog
import com.chathala.hala.feature.discover.ui.components.SwipeActionButtons
import com.chathala.hala.feature.discover.ui.components.SwipeCardView
import com.chathala.hala.feature.discover.ui.components.SwipeDirection
import com.chathala.hala.feature.reporting.ui.ReportUserSheet
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaSnackbarHost
import com.chathala.hala.ui.components.rememberHalaSnackbarHost

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenConversation: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val snackbarHost = rememberHalaSnackbarHost()
    var detailCard by remember { mutableStateOf<DiscoverCard?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var premiumGate by remember { mutableStateOf<PremiumGateReason?>(null) }
    var reportCard by remember { mutableStateOf<DiscoverCard?>(null) }
    var showNativeOverlay by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) viewModel.refreshLocationAndReload(context)
    }

    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            viewModel.refreshLocationAndReload(context)
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHost.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.openConversation.collect { convId ->
            detailCard = null
            onOpenConversation(convId)
        }
    }

    // إعلان بيني بعد كل 10 بطاقات
    LaunchedEffect(Unit) {
        com.chathala.hala.core.ads.InterstitialAdManager.preload(context)
        viewModel.showInterstitial.collect {
            context.findActivity()?.let {
                com.chathala.hala.core.ads.InterstitialAdManager.showNow(it)
            }
        }
    }
    // إعلان مدمج بين البطاقات (كل 8)
    LaunchedEffect(Unit) {
        viewModel.showNativeAd.collect { showNativeOverlay = true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                availableLabel = availableUsersLabel(state.total),
                hasActiveFilters = hasActiveFilters(state.filters),
                isPremium = isPremium,
                onSearchClick = onOpenSearch,
                onFiltersClick = { showFilters = true },
                onVisitorsClick = {
                    if (isPremium) {
                        // TODO: ربط شاشة الزوار حين توفّر backend endpoint
                    } else {
                        premiumGate = PremiumGateReason.VISITORS
                    }
                }
            )

            // بانر التقييد العام (يظهر فقط عند تقييد/تعليق الحساب)
            com.chathala.hala.feature.settings.ui.account.AccountRestrictionBanner(
                onAppeal = onOpenRequests
            )

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                when {
                    state.initialLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                    state.error != null && state.cards.isEmpty() -> ErrorState(
                        message = state.error ?: "",
                        onRetry = viewModel::load
                    )

                    state.currentCard == null -> EmptyDiscover(onRefresh = viewModel::refresh)

                    else -> CardStack(
                        state = state,
                        isPremium = isPremium,
                        onTapCard = { onOpenUserProfile(it.id) },
                        onSwipe = { direction ->
                            when (direction) {
                                // السحب يُزيح البطاقة فعلياً → يتقدّم
                                SwipeDirection.RIGHT -> viewModel.likeCurrent(advance = true)
                                SwipeDirection.UP -> viewModel.superLikeCurrent(advance = true)
                                SwipeDirection.LEFT -> viewModel.skipCurrent()
                            }
                        },
                        onSkip = viewModel::skipCurrent,
                        // زر الإعجاب: يلوّن ولا يُغادر البطاقة
                        onLike = { viewModel.likeCurrent(advance = false) },
                        onSuperLike = {
                            if (isPremium) viewModel.superLikeCurrent(advance = true)
                            else premiumGate = PremiumGateReason.SUPER_LIKE
                        },
                        onUndo = {
                            if (isPremium) viewModel.undoLast()
                            else premiumGate = PremiumGateReason.UNDO
                        },
                        onMessage = { detailCard = state.currentCard }
                    )
                }
            }
        }

        HalaSnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showNativeOverlay) {
            NativeAdOverlay(onDismiss = { showNativeOverlay = false })
        }
    }

    detailCard?.let { card ->
        CardDetailSheet(
            card = card,
            alreadyRequested = card.id in state.requestSentForIds,
            sending = state.requestingUserId == card.id,
            blocking = state.blockingUserId == card.id,
            alreadyReported = card.id in state.reportedUserIds,
            onSendRequest = { greeting ->
                viewModel.requestConversation(card, greeting, openIfExisting = true)
            },
            onBlock = {
                detailCard = null
                viewModel.blockUser(card)
            },
            onReport = {
                reportCard = card
                detailCard = null
            },
            onDismiss = { detailCard = null }
        )
    }

    reportCard?.let { card ->
        ReportUserSheet(
            targetUserName = card.name,
            submitting = state.reportingUserId == card.id,
            onSubmit = { reason, desc ->
                viewModel.reportUser(card, reason, desc)
                reportCard = null
            },
            onDismiss = { reportCard = null }
        )
    }

    premiumGate?.let { reason ->
        PremiumGateDialog(
            title = reason.title,
            message = reason.message,
            onDismiss = { premiumGate = null }
        )
    }

    if (showFilters) {
        FiltersSheet(
            initial = state.filters,
            onApply = { f ->
                showFilters = false
                viewModel.applyFilters(f)
            },
            onDismiss = { showFilters = false }
        )
    }
}

/** تسمية احترافية لعدد المتاحين — مجمّعة بلا رقم دقيق ولا تنازل مع كل سحب. */
private fun availableUsersLabel(total: Int): String? = when {
    total <= 0 -> null
    total >= 100 -> "أكثر من 100 شخص متاح"
    total >= 50 -> "أكثر من 50 شخص متاح"
    total >= 20 -> "أكثر من 20 شخص متاح"
    total >= 10 -> "أكثر من 10 أشخاص متاحين"
    else -> "أشخاص متاحون الآن"
}

@Composable
private fun TopBar(
    availableLabel: String?,
    hasActiveFilters: Boolean,
    isPremium: Boolean,
    onSearchClick: () -> Unit,
    onFiltersClick: () -> Unit,
    onVisitorsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisitorsPill(isPremium = isPremium, onClick = onVisitorsClick)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "اكتشف",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (availableLabel != null) {
                Text(
                    text = availableLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "بحث عن مستخدمين",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Box {
            IconButton(onClick = onFiltersClick) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "تخصيص البحث",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            if (hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun VisitorsPill(isPremium: Boolean, onClick: () -> Unit) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "الزوار",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "الزوار",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!isPremium) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
            )
        }
    }
}

@Composable
private fun CardStack(
    state: DiscoverUiState,
    isPremium: Boolean,
    onTapCard: (DiscoverCard) -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onSkip: () -> Unit,
    onLike: () -> Unit,
    onSuperLike: () -> Unit,
    onUndo: () -> Unit,
    onMessage: () -> Unit
) {
    val current = state.currentCard ?: return
    val next = state.nextCard

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Next card peek (behind current) — صورة فقط بدون overlay
            if (next != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(0.94f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    SwipeCardView(
                        card = next,
                        onSwiped = {},
                        onTap = {},
                        enabled = false,
                        showOverlay = false
                    )
                }
            }

            SwipeCardView(
                card = current,
                onSwiped = onSwipe,
                onTap = { onTapCard(current) },
                onDoubleTap = onLike
            )
        }

        // بانر بين بطاقة المستخدم والأزرار السريعة
        com.chathala.hala.core.ads.BannerAd(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        SwipeActionButtons(
            onSkip = onSkip,
            onMessage = onMessage,
            onSuperLike = onSuperLike,
            onLike = onLike,
            onUndo = onUndo,
            isPremium = isPremium,
            isLiked = state.currentCard?.id in state.likedIds,
            canUndo = state.canUndo,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun NativeAdOverlay(onDismiss: () -> Unit) {
    val ad = com.chathala.hala.core.ads.rememberNativeAd()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* يبتلع النقرات حتى لا تصل للمكدّس */ },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (ad != null) {
                    com.chathala.hala.core.ads.NativeAdCard(ad)
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                }
                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "متابعة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDiscover(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "انتهت البطاقات",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "اسحب للأسفل للتحديث أو جرّب تعديل الفلاتر",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun hasActiveFilters(f: com.chathala.hala.feature.discover.data.DiscoverRepository.Filters): Boolean {
    return f.gender != null || f.minAge != null || f.maxAge != null || f.onlyRecent
}

private enum class PremiumGateReason(val title: String, val message: String) {
    SUPER_LIKE(
        title = "الإعجاب المميز للبريميوم",
        message = "اظهر باهتمامك الفوري واحصل على انتباه أكبر مع الإعجاب المميز. متاح لمشتركي البريميوم."
    ),
    UNDO(
        title = "التراجع للبريميوم",
        message = "سحبت بالخطأ؟ بإمكان مشتركي البريميوم التراجع عن آخر حركة واستعادة البطاقة."
    ),
    VISITORS(
        title = "شاهد من زار ملفك",
        message = "قائمة الزوار متاحة لمشتركي البريميوم — اكتشف من اهتم بك حتى لو لم يرسل رسالة."
    )
}
