package com.chathala.hala.feature.discover.ui.search

import com.chathala.hala.core.i18n.S

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import com.chathala.hala.core.util.HapticHelper
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chathala.hala.R
import com.chathala.hala.core.ads.AdConfig
import com.chathala.hala.core.ads.NativeAdGridItem
import com.chathala.hala.core.ads.NativeAdListItem
import com.chathala.hala.core.data.Countries
import com.chathala.hala.core.util.ProfileFormatter
import com.chathala.hala.feature.discover.data.SearchUser
import com.chathala.hala.feature.discover.ui.components.PremiumGateDialog
import com.chathala.hala.ui.components.ErrorState
import com.chathala.hala.ui.components.HalaAsyncImage
import com.chathala.hala.ui.components.SkeletonBlock

private val GoldColor = Color(0xFFE6B800)
private val LikeColor = Color(0xFFFF3B6B)
private val OnlineColor = Color(0xFF4CAF50)

@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenPremium: () -> Unit = {},
    viewModel: UserSearchViewModel = viewModel(factory = UserSearchViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var showFilters by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // «متصل الآن» يفسد بمرور الوقت، وViewModel يبقى حيّاً في مكدّس التنقّل — فنُحدّث
    // الاقتراحات عند كل عودة للشاشة (الكبح داخل ViewModel).
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    val openProfile: (String) -> Unit = { id ->
        viewModel.rememberQuery()
        onOpenUserProfile(id)
    }
    // الفلترة ميزة مدفوعة
    val onFiltersClick: () -> Unit = {
        if (state.isPremium) showFilters = true else showGate = true
    }

    if (showFilters) {
        SearchFiltersSheet(
            initial = state.filters,
            onApply = { viewModel.applyFilters(it); showFilters = false },
            onDismiss = { showFilters = false }
        )
    }
    if (showGate) {
        PremiumGateDialog(
            title = S.get(R.string.premium_filter_gate_title),
            message = S.get(R.string.premium_filter_gate_msg),
            onDismiss = { showGate = false },
            onUpgrade = {
                showGate = false
                onOpenPremium()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // الضغط على أي فراغ خارج الحقول يُخفي لوحة المفاتيح
            .pointerInput(Unit) {
                detectTapGestures(onTap = { dismissKeyboard() })
            }
    ) {
        // ── شريط البحث ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = S.get(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text(S.get(R.string.user_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = S.get(R.string.cancel))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.large
            )
            // زر الفلاتر (مدفوع) مع نقطة عند وجود فلتر نشط
            Box {
                IconButton(onClick = onFiltersClick) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = S.get(R.string.search_filters_title),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (state.filters.isActive) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp)
                            .size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            // تبديل شبكة/قائمة — يشمل نتائج البحث وقائمة «المتصلون الآن» معاً
            run {
                IconButton(onClick = viewModel::toggleLayout) {
                    Icon(
                        imageVector = if (state.gridLayout)
                            Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = S.get(
                            if (state.gridLayout) R.string.search_view_list else R.string.search_view_grid
                        ),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // وضع البحث
                state.isSearchMode -> when {
                    state.loading -> SkeletonList()
                    state.error != null -> ErrorState(message = state.error ?: "", onRetry = viewModel::retry)
                    state.searched && state.results.isEmpty() -> EmptyState(
                        icon = Icons.Filled.PersonSearch,
                        title = S.get(R.string.user_search_empty_title),
                        subtitle = S.get(R.string.user_search_empty_desc)
                    )
                    else -> if (state.gridLayout) {
                        ResultsGrid(
                            results = state.results,
                            isLiked = state::isLiked,
                            onToggleLike = viewModel::toggleLike,
                            loadingMore = state.loadingMore,
                            onLoadMore = viewModel::loadMore,
                            onOpen = openProfile,
                            onScroll = dismissKeyboard
                        )
                    } else {
                        ResultsList(
                            results = state.results,
                            isLiked = state::isLiked,
                            onToggleLike = viewModel::toggleLike,
                            loadingMore = state.loadingMore,
                            onLoadMore = viewModel::loadMore,
                            onOpen = openProfile,
                            onScroll = dismissKeyboard
                        )
                    }
                }

                // وضع الاقتراحات (قبل الكتابة)
                state.suggestionsLoading -> SkeletonList()

                state.premium.isEmpty() && state.online.isEmpty() &&
                    state.recentlyActive.isEmpty() && state.recent.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = S.get(R.string.user_search_prompt_title),
                    subtitle = S.get(R.string.user_search_prompt_desc)
                )

                else -> SuggestionsList(
                    gridLayout = state.gridLayout,
                    isLiked = state::isLiked,
                    onToggleLike = viewModel::toggleLike,
                    recent = state.recent,
                    premium = state.premium,
                    online = state.online,
                    recentlyActive = state.recentlyActive,
                    showPromo = !state.isPremium,
                    onlineLoadingMore = state.onlineLoadingMore,
                    onLoadMoreOnline = viewModel::loadMoreOnline,
                    onOpen = openProfile,
                    onApplyRecent = viewModel::applyRecent,
                    onRemoveRecent = viewModel::removeRecent,
                    onClearRecent = viewModel::clearRecent,
                    // بطاقة الترقية → صفحة المشتريات مباشرة
                    onPromoClick = onOpenPremium,
                    onScroll = dismissKeyboard
                )
            }
        }
    }
}

/**
 * شبكة نتائج البحث — عمودان، البطاقة صورة بملء الإطار مع اسم/عمر/دولة فوق تدرّج داكن.
 *
 * الصورة هي المحتوى الأساسي هنا (بخلاف القائمة التي تُظهر أفاتاراً صغيراً)، فالشبكة
 * تعرض ضِعف عدد النتائج في نفس المساحة وتُبرز الصور — وهو ما يناسب شاشة تعارف.
 */
@Composable
private fun ResultsGrid(
    results: List<SearchUser>,
    isLiked: (SearchUser) -> Boolean,
    onToggleLike: (SearchUser) -> Unit,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpen: (String) -> Unit,
    onScroll: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= results.size - 4
        }
    }
    LaunchedEffect(reachedEnd, results.size) {
        if (reachedEnd && results.isNotEmpty()) onLoadMore()
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { if (it) onScroll() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        results.forEachIndexed { index, user ->
            item(key = user.id) {
                SearchResultCard(
                    user = user,
                    onClick = { onOpen(user.id) },
                    liked = isLiked(user),
                    onToggleLike = { onToggleLike(user) }
                )
            }
            // إعلان مدمج بين المستخدمين — يشغل خانة واحدة كبطاقة مستخدم فلا يكسر الشبكة
            if ((index + 1) % AdConfig.SEARCH_NATIVE_EVERY == 0) {
                item(key = "ad_$index") {
                    NativeAdGridItem(slot = "search_grid_${(index + 1) / AdConfig.SEARCH_NATIVE_EVERY}")
                }
            }
        }
        if (loadingMore) {
            item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/** حالة الحضور المعروضة على البطاقة. */
private sealed interface Presence {
    /** متصل الآن. */
    data object Online : Presence
    /** آخر ظهور قريب — [label] نصّ نسبي جاهز ("قبل 5 دقائق"). */
    data class Recent(val label: String) : Presence
}

/**
 * الحضور المعروض: الاتصال الآن يسبق آخر ظهور، وآخر ظهور القديم (أكثر من أسبوع)
 * لا يُعرض أصلاً — «نشط قبل 40 يوماً» معلومة تُنفّر ولا تُفيد.
 */
private fun presenceOf(user: SearchUser): Presence? = when {
    user.isOnline == true -> Presence.Online
    else -> ProfileFormatter.lastActiveLabel(user.lastLogin)?.let { Presence.Recent(it) }
}

/** شارة حضور مدمجة فوق الصورة — نقطة ملوّنة + نصّ على خلفية داكنة شبه شفافة. */
@Composable
private fun PresencePill(presence: Presence, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (presence is Presence.Online) OnlineColor else Color.White.copy(alpha = 0.65f))
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = when (presence) {
                Presence.Online -> S.get(R.string.user_search_badge_online)
                is Presence.Recent -> presence.label
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** زرّ الإعجاب الدائري — نبضة قصيرة عند التفعيل تُعطي إحساساً بالاستجابة. */
@Composable
private fun LikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onImage: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (liked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )
    Box(
        modifier = modifier
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (onImage) Color.Black.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .clickable {
                HapticHelper.light(haptic)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = S.get(if (liked) R.string.action_unlike else R.string.action_like),
            tint = when {
                liked -> LikeColor
                onImage -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * بطاقة مستخدم في الشبكة: صورة بملء الإطار، شارة حضور أعلاها، واسم/عمر/دولة
 * فوق تدرّج داكن أسفلها.
 *
 * التدرّج بثلاث محطات لا محطتين: التدرّج الخطّي البسيط يترك منتصف البطاقة رمادياً
 * على الصور الفاتحة بينما يبقى النصّ ضعيف التباين — المحطة الوسطى تُبقي الصورة
 * صافية في أعلاها وتُعتّم بسرعة تحت النصّ فقط.
 */
@Composable
private fun SearchResultCard(
    user: SearchUser,
    onClick: () -> Unit,
    liked: Boolean = false,
    onToggleLike: (() -> Unit)? = null
) {
    val isPremium = user.isPremium == true
    val age = ProfileFormatter.computeAge(user.birthDate)
    val country = countryText(user.country)
    val presence = presenceOf(user)
    val shape = RoundedCornerShape(20.dp)

    // انكماش خفيف عند اللمس بدل موجة الريبل: الموجة تضيع فوق صورة، والانكماش
    // يُشعر أنّ البطاقة كلّها زرّ واحد.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "cardPress"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .scale(pressScale)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        HalaAsyncImage(
            model = user.profileImage,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            fallbackName = user.name,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.30f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        if (presence != null) {
            PresencePill(
                presence = presence,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        // التاج في الطرف المقابل للشارة فلا يتزاحمان على بطاقة ضيّقة
        if (isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = GoldColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (onToggleLike != null) {
            LikeButton(
                liked = liked,
                onToggle = onToggleLike,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                // نترك مساحة لزرّ القلب فلا يركبه الاسم الطويل
                .padding(start = 12.dp, end = 54.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name ?: "—",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (age != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = age.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                if (user.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (country != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = country,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // إطار ذهبي للمشتركين — يُرسم أخيراً فلا تبتلعه الصورة أو التدرّج
        if (isPremium) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(GoldColor, Color(0xFFFFE9A8), GoldColor)),
                        shape = shape
                    )
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<SearchUser>,
    isLiked: (SearchUser) -> Boolean,
    onToggleLike: (SearchUser) -> Unit,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onOpen: (String) -> Unit,
    onScroll: () -> Unit
) {
    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= results.size - 3
        }
    }
    LaunchedEffect(reachedEnd, results.size) {
        if (reachedEnd && results.isNotEmpty()) onLoadMore()
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { if (it) onScroll() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        results.forEachIndexed { index, user ->
            item(key = user.id) {
                SearchResultRow(
                    user = user,
                    onClick = { onOpen(user.id) },
                    liked = isLiked(user),
                    onToggleLike = { onToggleLike(user) }
                )
            }
            // ✅ إعلان مدمج كل SEARCH_NATIVE_EVERY نتيجة (بين بطاقات المستخدمين)
            if ((index + 1) % AdConfig.SEARCH_NATIVE_EVERY == 0) {
                item(key = "ad_$index") {
                    NativeAdListItem(slot = "search_list_${(index + 1) / AdConfig.SEARCH_NATIVE_EVERY}")
                }
            }
        }
        if (loadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/** مفتاح العنصر الفاصل بين «متصلون الآن» وما بعده — منه يُشتقّ تحميل المزيد. */
private const val ONLINE_TAIL_KEY = "online_tail"

@Composable
private fun SuggestionsList(
    gridLayout: Boolean,
    isLiked: (SearchUser) -> Boolean,
    onToggleLike: (SearchUser) -> Unit,
    recent: List<String>,
    premium: List<SearchUser>,
    online: List<SearchUser>,
    recentlyActive: List<SearchUser>,
    showPromo: Boolean,
    onlineLoadingMore: Boolean,
    onLoadMoreOnline: () -> Unit,
    onOpen: (String) -> Unit,
    onApplyRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onPromoClick: () -> Unit,
    onScroll: () -> Unit
) {
    // شبكة بعمود واحد = قائمة. توحيد المسارين في LazyVerticalGrid واحد يتجنّب
    // تكرار كل الأقسام (الترويج/الأخيرة/المشتركون) مرتين، ويمنع تعشيش شبكة
    // داخل LazyColumn وهو غير مسموح (ارتفاع غير محدود).
    val gridState = rememberLazyGridState()
    // «تحميل المزيد» يُشتقّ من ظهور ذيل قسم المتصلين لا من نهاية الشبكة: قسم
    // «نشطون مؤخراً» صار بعده، فالنهاية لم تعد تعني أنّ المتصلين نفدوا.
    val onlineTailVisible by remember {
        derivedStateOf { gridState.layoutInfo.visibleItemsInfo.any { it.key == ONLINE_TAIL_KEY } }
    }
    LaunchedEffect(onlineTailVisible, online.size) {
        if (onlineTailVisible && online.isNotEmpty()) onLoadMoreOnline()
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { if (it) onScroll() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (gridLayout) 2 else 1),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (gridLayout) 12.dp else 0.dp,
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(if (gridLayout) 10.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(if (gridLayout) 10.dp else 4.dp)
    ) {
        if (showPromo) {
            item(key = "promo", span = { GridItemSpan(maxLineSpan) }) {
                PremiumPromoCard(
                    onClick = onPromoClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "recent_header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(icon = Icons.Filled.History, text = S.get(R.string.user_search_section_recent))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onClearRecent) { Text(S.get(R.string.user_search_clear_recent)) }
                }
            }
            item(key = "recent_chips", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recent.forEach { term ->
                        RecentChip(term = term, onClick = { onApplyRecent(term) }, onRemove = { onRemoveRecent(term) })
                    }
                }
            }
        }

        if (premium.isNotEmpty()) {
            item(key = "premium_header", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)) {
                    SectionHeader(icon = Icons.Filled.WorkspacePremium, text = S.get(R.string.user_search_section_premium), iconTint = GoldColor)
                }
            }
            item(key = "premium_carousel", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(premium, key = { "p_${it.id}" }) { user ->
                        PremiumChip(user = user, onClick = { onOpen(user.id) })
                    }
                }
            }
        }

        item(key = "online_header", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)) {
                SectionHeader(
                    icon = Icons.Filled.FiberManualRecord,
                    text = S.get(R.string.user_search_section_online),
                    iconTint = OnlineColor,
                    count = online.size
                )
            }
        }
        if (online.isEmpty()) {
            // سطر صريح بدل قسم يختفي: القسم الفارغ الصامت يبدو عطلاً في التطبيق.
            item(key = "online_empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = S.get(R.string.user_search_online_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        } else {
            userSection(
                keyPrefix = "online",
                users = online,
                gridLayout = gridLayout,
                isLiked = isLiked,
                onToggleLike = onToggleLike,
                onOpen = onOpen
            )
        }
        // علامة نهاية قسم المتصلين — ظهورها هو ما يُطلق تحميل الدفعة التالية.
        item(key = ONLINE_TAIL_KEY, span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.fillMaxWidth().height(1.dp))
        }
        if (onlineLoadingMore) {
            item(key = "online_loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
            }
        }

        if (recentlyActive.isNotEmpty()) {
            item(key = "recent_active_header", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)) {
                    SectionHeader(
                        icon = Icons.Filled.Schedule,
                        text = S.get(R.string.user_search_section_recently_active),
                        count = recentlyActive.size
                    )
                }
            }
            userSection(
                keyPrefix = "active",
                users = recentlyActive,
                gridLayout = gridLayout,
                isLiked = isLiked,
                onToggleLike = onToggleLike,
                onOpen = onOpen
            )
        }
    }
}

/**
 * قسم مستخدمين داخل شبكة الاقتراحات — بطاقة/صف حسب النمط، وإعلان مدمج كل
 * [AdConfig.SEARCH_NATIVE_EVERY]. مشترك بين «متصلون الآن» و«نشطون مؤخراً» فلا
 * يُكرَّر التخطيط نفسه (ومعه ترقيم مفاتيح الإعلانات) مرتين.
 *
 * @param keyPrefix يفصل مفاتيح العناصر وخانات الإعلانات بين القسمين.
 */
private fun LazyGridScope.userSection(
    keyPrefix: String,
    users: List<SearchUser>,
    gridLayout: Boolean,
    isLiked: (SearchUser) -> Boolean,
    onToggleLike: (SearchUser) -> Unit,
    onOpen: (String) -> Unit
) {
    users.forEachIndexed { index, user ->
        item(key = "${keyPrefix}_${user.id}") {
            if (gridLayout) {
                SearchResultCard(
                    user = user,
                    onClick = { onOpen(user.id) },
                    liked = isLiked(user),
                    onToggleLike = { onToggleLike(user) }
                )
            } else {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SearchResultRow(
                        user = user,
                        onClick = { onOpen(user.id) },
                        liked = isLiked(user),
                        onToggleLike = { onToggleLike(user) }
                    )
                }
            }
        }
        if ((index + 1) % AdConfig.SEARCH_NATIVE_EVERY == 0) {
            val adOrdinal = (index + 1) / AdConfig.SEARCH_NATIVE_EVERY
            if (gridLayout) {
                item(key = "${keyPrefix}_ad_$index") {
                    NativeAdGridItem(slot = "${keyPrefix}_grid_$adOrdinal")
                }
            } else {
                item(key = "${keyPrefix}_ad_$index", span = { GridItemSpan(maxLineSpan) }) {
                    NativeAdListItem(
                        slot = "${keyPrefix}_list_$adOrdinal",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/** أفاتار دائري بحلقة ذهبية + شارة تاج للمشتركين، في شريط أفقي. */
@Composable
private fun PremiumChip(user: SearchUser, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(76.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            // الحلقة الذهبية
            HalaAsyncImage(
                model = user.profileImage,
                contentDescription = user.name,
                fallbackName = user.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(GoldColor, Color(0xFFFFE082))),
                        shape = CircleShape
                    )
            )
            // التاج فوق الأفاتار
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.dp)
            )
            if (user.isOnline == true) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp)
                        .size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp).clip(CircleShape).background(OnlineColor)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.name ?: "—",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        countryText(user.country)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentChip(term: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(term, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = S.get(R.string.cancel),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).clip(CircleShape).clickable(onClick = onRemove).padding(1.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    /** عدد عناصر القسم — 0 يعني لا شارة (القسم فارغ أو العدّ غير مفيد). */
    count: Int = 0
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count > 0) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = iconTint,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * صف مستخدم في نمط القائمة: أفاتار + اسم/بيانات + سطر حضور + زرّ إعجاب.
 *
 * سطر الحضور هو الفرق الأهم عن النسخة السابقة: كانت القائمة تكتفي بنقطة خضراء
 * صغيرة على الأفاتار، فيستحيل تمييز «متصل الآن» من «نشط قبل ساعة» — وهو تحديداً
 * ما يبني عليه المستخدم قرار بدء محادثة.
 */
@Composable
private fun SearchResultRow(
    user: SearchUser,
    onClick: () -> Unit,
    liked: Boolean = false,
    onToggleLike: (() -> Unit)? = null
) {
    val isPremium = user.isPremium == true
    val age = ProfileFormatter.computeAge(user.birthDate)
    val presence = presenceOf(user)
    val meta = listOfNotNull(
        age?.toString(),
        countryText(user.country),
        user.distanceLabel?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isPremium) Modifier.border(1.dp, GoldColor.copy(alpha = 0.55f), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            HalaAsyncImage(
                model = user.profileImage,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                fallbackName = user.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .then(
                        if (isPremium) Modifier.border(2.dp, GoldColor, CircleShape)
                        else Modifier
                    )
            )
            if (user.isOnline == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(OnlineColor)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPremium) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = user.name ?: "—",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (presence != null) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (presence is Presence.Online) OnlineColor
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when (presence) {
                            Presence.Online -> S.get(R.string.user_search_badge_online)
                            is Presence.Recent -> S.get(R.string.user_search_active_ago, presence.label)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (presence is Presence.Online) OnlineColor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (onToggleLike != null) {
            Spacer(Modifier.width(8.dp))
            LikeButton(liked = liked, onToggle = onToggleLike, onImage = false)
        }
    }
}

@Composable
private fun SkeletonList() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(modifier = Modifier.size(52.dp), shape = CircleShape)
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.25f).height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** كود الدولة → "علم اسم" بالعربي، أو الكود الخام كحل أخير. */
private fun countryText(code: String?): String? {
    if (code.isNullOrBlank()) return null
    val c = Countries.byCode(code)
    return if (c != null) "${c.flag} ${c.name}" else code
}
