package com.chathala.hala.feature.main.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.chathala.hala.R

/**
 * تبويبات الشريط السفلي الأربعة.
 * الترتيب هنا = ترتيب الظهور في NavigationBar.
 */
enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    DISCOVER(R.string.tab_discover, Icons.Rounded.Explore),
    CHATS(R.string.tab_chats, Icons.AutoMirrored.Rounded.Chat),
    NOTIFICATIONS(R.string.tab_notifications, Icons.Rounded.Notifications),
    PROFILE(R.string.tab_profile, Icons.Rounded.Person)
}
