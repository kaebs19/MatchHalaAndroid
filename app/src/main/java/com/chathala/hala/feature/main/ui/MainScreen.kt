package com.chathala.hala.feature.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.HalaApp
import com.chathala.hala.feature.chats.ui.list.ChatsScreen
import com.chathala.hala.feature.discover.ui.DiscoverScreen
import com.chathala.hala.feature.notifications.ui.NotificationsScreen
import com.chathala.hala.feature.profile.ui.ProfileScreen
import com.chathala.hala.feature.push.PushIntentCoordinator
import com.chathala.hala.feature.push.RequestNotificationPermissionEffect
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(
    onLoggedOut: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onOpenRequestPreview: (String) -> Unit = {},
    onOpenChatRequests: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenUserSearch: () -> Unit = {},
    onOpenRequests: () -> Unit = {}
) {
    var selected by rememberSaveable { mutableStateOf(MainTab.DISCOVER) }

    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as HalaApp }
    val unreadCountFlow: StateFlow<Int> = app.notificationsRepository.unreadCount
    val unreadCount by unreadCountFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        app.notificationsRepository.refreshUnreadCount()
    }

    val pendingPushTab by PushIntentCoordinator.pendingTab.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPushTab) {
        val target = pendingPushTab ?: return@LaunchedEffect
        selected = target
        PushIntentCoordinator.consumeTab()
    }

    RequestNotificationPermissionEffect()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomBar(
                selected = selected,
                unreadNotifications = unreadCount,
                onSelect = { selected = it }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (selected) {
                MainTab.DISCOVER -> DiscoverScreen(
                    onOpenConversation = onOpenConversation,
                    onOpenUserProfile = onOpenUserProfile,
                    onOpenSearch = onOpenUserSearch,
                    onOpenRequests = onOpenRequests
                )
                MainTab.CHATS -> ChatsScreen(
                    onOpenConversation = onOpenConversation,
                    onOpenRequestPreview = onOpenRequestPreview,
                    onOpenRequests = onOpenChatRequests
                )
                MainTab.NOTIFICATIONS -> NotificationsScreen(
                    onOpenConversation = onOpenConversation,
                    onOpenRequestPreview = onOpenRequestPreview,
                    onOpenUserProfile = onOpenUserProfile
                )
                MainTab.PROFILE -> ProfileScreen(
                    onLoggedOut = onLoggedOut,
                    onEditProfile = onEditProfile,
                    onOpenSettings = onOpenSettings,
                    onOpenVerification = onOpenVerification,
                    onOpenPremium = onOpenPremium
                )
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    selected: MainTab,
    unreadNotifications: Int,
    onSelect: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            // نبضة تكبير خفيفة للأيقونة المختارة
            val iconScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                ),
                label = "tab-scale"
            )
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    val iconMod = Modifier.graphicsLayer {
                        scaleX = iconScale; scaleY = iconScale
                    }
                    if (tab == MainTab.NOTIFICATIONS && unreadNotifications > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(
                                        text = if (unreadNotifications > 99) "99+" else unreadNotifications.toString()
                                    )
                                }
                            }
                        ) {
                            Icon(imageVector = tab.icon, contentDescription = null, modifier = iconMod)
                        }
                    } else {
                        Icon(imageVector = tab.icon, contentDescription = null, modifier = iconMod)
                    }
                },
                label = { Text(text = stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
