package com.chathala.hala.navigation

/**
 * قائمة مركزية لمسارات الـ Navigation.
 * أضف أي شاشة جديدة هنا ثم أربطها في [HalaNavGraph].
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val RESET = "reset/{email}"
    const val WELCOME = "welcome"
    const val PROFILE_COMPLETE = "profile_complete"
    const val CONTENT_POLICY = "content_policy"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_DISCOVER = "settings/discover"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_CONTACT = "settings/contact"
    const val SETTINGS_CHANGE_PASSWORD = "settings/change_password"
    const val SETTINGS_ACCOUNT = "settings/account"
    const val SETTINGS_CONTENT = "settings/content"
    const val HOME = "home"
    const val LEGAL_PRIVACY = "legal/privacy"
    const val LEGAL_TERMS = "legal/terms"
    const val CHAT = "chat/{conversationId}"
    const val CHAT_REQUESTS = "chat/requests"
    const val REQUEST_PREVIEW = "chat/request/{conversationId}"
    const val BLOCKED_USERS = "settings/blocked"
    const val SETTINGS_VIOLATIONS = "settings/violations"
    const val SETTINGS_STANDING = "settings/standing"
    const val SETTINGS_REQUESTS = "settings/requests"
    const val SETTINGS_REQUEST_DETAIL = "settings/requests/{appealId}"
    const val VERIFICATION = "verification"
    const val USER_PROFILE = "user/{userId}"
    const val USER_SEARCH = "user/search"
    const val SUSPENDED = "suspended/{mode}"
    const val SUBSCRIPTION = "subscription"

    fun reset(email: String) = "reset/$email"
    fun chat(conversationId: String) = "chat/$conversationId"
    fun requestPreview(conversationId: String) = "chat/request/$conversationId"
    fun userProfile(userId: String) = "user/$userId"
    fun suspended(mode: String) = "suspended/$mode"
    fun requestDetail(appealId: String) = "settings/requests/$appealId"
}

/** مسارات تتطلب جلسة صالحة — لو فُقد التوكن نُعيد توجيه Login. */
internal val PROTECTED_ROUTES = setOf(
    Routes.HOME,
    Routes.WELCOME,
    Routes.PROFILE_COMPLETE,
    Routes.EDIT_PROFILE,
    Routes.SETTINGS,
    Routes.SETTINGS_PRIVACY,
    Routes.SETTINGS_NOTIFICATIONS,
    Routes.SETTINGS_DISCOVER,
    Routes.SETTINGS_ACCOUNT,
    Routes.SETTINGS_CHANGE_PASSWORD,
    Routes.SUBSCRIPTION
)
