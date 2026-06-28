package com.chathala.hala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chathala.hala.HalaApp
import com.chathala.hala.feature.push.PushIntentCoordinator
import kotlinx.coroutines.launch
import com.chathala.hala.feature.auth.ui.ForgotPasswordScreen
import com.chathala.hala.feature.auth.ui.LoginScreen
import com.chathala.hala.feature.auth.ui.RegisterScreen
import com.chathala.hala.feature.auth.ui.ResetPasswordScreen
import com.chathala.hala.feature.blocking.ui.BlockedUsersScreen
import com.chathala.hala.feature.verification.ui.VerificationScreen
import com.chathala.hala.feature.chats.ui.chat.ChatScreen
import com.chathala.hala.feature.chats.ui.pending.PendingRequestsScreen
import com.chathala.hala.feature.chats.ui.request.RequestPreviewScreen
import com.chathala.hala.feature.legal.data.LegalDocType
import com.chathala.hala.feature.legal.ui.LegalDocScreen
import com.chathala.hala.feature.discover.ui.search.UserSearchScreen
import com.chathala.hala.feature.main.ui.MainScreen
import com.chathala.hala.feature.profile.ui.EditProfileScreen
import com.chathala.hala.feature.profile.ui.ProfileCompletionScreen
import com.chathala.hala.feature.settings.ui.SettingsScreen
import com.chathala.hala.feature.settings.ui.account.AccountSettingsScreen
import com.chathala.hala.feature.settings.ui.about.AboutScreen
import com.chathala.hala.feature.settings.ui.contact.ContactScreen
import com.chathala.hala.feature.settings.ui.discover.DiscoverSettingsScreen
import com.chathala.hala.feature.settings.ui.notifications.NotificationSettingsScreen
import com.chathala.hala.feature.settings.ui.content.ContentPreferencesScreen
import com.chathala.hala.feature.settings.ui.privacy.PrivacyScreen
import com.chathala.hala.feature.settings.ui.security.ChangePasswordScreen
import com.chathala.hala.feature.splash.ui.SplashScreen
import com.chathala.hala.feature.suspension.data.SuspensionMode
import com.chathala.hala.feature.suspension.ui.SuspendedScreen
import com.chathala.hala.feature.userprofile.ui.UserProfileScreen

@Composable
fun HalaNavGraph() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as HalaApp
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun goTo(route: String, clearBackstack: Boolean = false) {
        nav.navigate(route) {
            if (clearBackstack) popUpTo(nav.graph.id) { inclusive = true }
        }
    }

    // ── Session expiry watcher ───────────────────────────────
    // إذا تم مسح الـ token (من TokenAuthenticator بعد فشل refresh)
    // وكان المستخدم في شاشة محمية → نُعيده لـ Login.
    val backStack by nav.currentBackStackEntryAsState()
    LaunchedEffect(Unit) {
        app.tokenStorage.token.collect { token ->
            val currentRoute = backStack?.destination?.route
            val onProtectedRoute = currentRoute in PROTECTED_ROUTES
            if (token.isNullOrBlank() && onProtectedRoute) {
                goTo(Routes.LOGIN, clearBackstack = true)
            }
        }
    }

    // ── مراقب التعليق أثناء الجلسة ───────────────────────────
    // معترِض الشبكة يلتقط 403 (تعليق/حظر) من أي طلب ويُطلق حدثاً هنا.
    LaunchedEffect(Unit) {
        com.chathala.hala.feature.suspension.data.SuspensionGate.events.collect { mode ->
            val currentRoute = backStack?.destination?.route
            if (currentRoute?.startsWith("suspended") != true) {
                goTo(Routes.suspended(mode.routeArg), clearBackstack = true)
            }
        }
    }

    // ── Push deep link: فتح محادثة محدّدة من tap إشعار ────────
    // تعمل بغض النظر عن الشاشة الحالية — لو المستخدم في Settings ستفتح الـ chat.
    // يُشترَط وجود جلسة صالحة (token غير فارغ).
    val token by app.tokenStorage.token.collectAsStateWithLifecycle(initialValue = null)
    val pendingConversationId by PushIntentCoordinator.pendingConversationId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingConversationId, token) {
        val convId = pendingConversationId ?: return@LaunchedEffect
        if (token.isNullOrBlank()) return@LaunchedEffect
        nav.navigate(Routes.chat(convId))
        PushIntentCoordinator.consumeConversation()
    }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        // انتقالات احترافية: انزلاق أفقي خفيف + تلاشٍ بين الشاشات
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300))
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { -it / 8 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300))
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        }
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onAuthenticated = { goTo(Routes.HOME, clearBackstack = true) },
                onUnauthenticated = { goTo(Routes.LOGIN, clearBackstack = true) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onRegister = { nav.navigate(Routes.REGISTER) },
                onForgot = { nav.navigate(Routes.FORGOT) },
                onOpenTerms = { nav.navigate(Routes.LEGAL_TERMS) },
                onOpenPrivacy = { nav.navigate(Routes.LEGAL_PRIVACY) },
                onBanned = { mode -> nav.navigate(Routes.suspended(mode.routeArg)) },
                onSuccess = { goTo(Routes.HOME, clearBackstack = true) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { nav.popBackStack() },
                onOpenTerms = { nav.navigate(Routes.LEGAL_TERMS) },
                onOpenPrivacy = { nav.navigate(Routes.LEGAL_PRIVACY) },
                onBanned = { mode -> nav.navigate(Routes.suspended(mode.routeArg)) },
                onSuccess = { goTo(Routes.PROFILE_COMPLETE, clearBackstack = true) }
            )
        }

        composable(
            Routes.SUSPENDED,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { entry ->
            val mode = SuspensionMode.fromArg(entry.arguments?.getString("mode"))
            SuspendedScreen(
                mode = mode,
                onBackToLogin = {
                    // أنهِ الجلسة (إن وُجدت — حالة التعليق أثناء الجلسة) قبل العودة لتسجيل الدخول
                    scope.launch {
                        runCatching { app.socket.disconnect() }
                        app.tokenStorage.clear()
                    }
                    goTo(Routes.LOGIN, clearBackstack = true)
                },
                onLifted = {
                    // رُفِع التعليق — البيانات حُدّثت بالفعل عبر /me؛ أعِد الاتصال وادخل التطبيق
                    scope.launch {
                        runCatching { app.deviceTokenRepository.ensureSynced() }
                        runCatching { app.socket.connect() }
                    }
                    goTo(Routes.HOME, clearBackstack = true)
                },
                onOpenTerms = { nav.navigate(Routes.LEGAL_TERMS) },
                onOpenPrivacy = { nav.navigate(Routes.LEGAL_PRIVACY) },
                onOpenContact = { nav.navigate(Routes.SETTINGS_CONTACT) }
            )
        }

        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                onBack = { nav.popBackStack() },
                onCodeSent = { email -> nav.navigate(Routes.reset(email)) }
            )
        }

        composable(Routes.RESET) { entry ->
            val email = entry.arguments?.getString("email").orEmpty()
            ResetPasswordScreen(
                email = email,
                onBack = { nav.popBackStack() },
                onSuccess = { goTo(Routes.LOGIN, clearBackstack = true) }
            )
        }

        composable(Routes.PROFILE_COMPLETE) {
            ProfileCompletionScreen(
                onSkip = { goTo(Routes.CONTENT_POLICY, clearBackstack = true) },
                onDone = { goTo(Routes.CONTENT_POLICY, clearBackstack = true) }
            )
        }

        composable(Routes.CONTENT_POLICY) {
            com.chathala.hala.feature.auth.ui.ContentPolicyScreen(
                onAgree = { goTo(Routes.HOME, clearBackstack = true) },
                onDecline = {
                    scope.launch { app.authRepository.logout() }
                    goTo(Routes.LOGIN, clearBackstack = true)
                }
            )
        }

        composable(Routes.HOME) {
            MainScreen(
                onLoggedOut = { goTo(Routes.LOGIN, clearBackstack = true) },
                onEditProfile = { nav.navigate(Routes.EDIT_PROFILE) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenConversation = { id -> nav.navigate(Routes.chat(id)) },
                onOpenRequestPreview = { id -> nav.navigate(Routes.requestPreview(id)) },
                onOpenChatRequests = { nav.navigate(Routes.CHAT_REQUESTS) },
                onOpenVerification = { nav.navigate(Routes.VERIFICATION) },
                onOpenUserProfile = { userId -> nav.navigate(Routes.userProfile(userId)) },
                onOpenUserSearch = { nav.navigate(Routes.USER_SEARCH) },
                onOpenRequests = { nav.navigate(Routes.SETTINGS_REQUESTS) }
            )
        }

        composable(Routes.USER_SEARCH) {
            UserSearchScreen(
                onBack = { nav.popBackStack() },
                onOpenUserProfile = { userId -> nav.navigate(Routes.userProfile(userId)) }
            )
        }

        composable(Routes.USER_PROFILE) { entry ->
            val userId = entry.arguments?.getString("userId").orEmpty()
            UserProfileScreen(
                userId = userId,
                onBack = { nav.popBackStack() },
                onOpenConversation = { convId ->
                    nav.navigate(Routes.chat(convId)) {
                        popUpTo(Routes.USER_PROFILE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            ChatScreen(
                conversationId = id,
                onBack = { nav.popBackStack() },
                onOpenRequestPreview = { convId ->
                    nav.navigate(Routes.requestPreview(convId)) {
                        popUpTo(Routes.chat(id)) { inclusive = true }
                    }
                },
                onOpenUserProfile = { userId ->
                    nav.navigate(Routes.userProfile(userId))
                },
                onOpenContentSettings = { nav.navigate(Routes.SETTINGS_CONTENT) },
                onOpenRequests = { nav.navigate(Routes.SETTINGS_REQUESTS) }
            )
        }

        composable(Routes.CHAT_REQUESTS) {
            PendingRequestsScreen(
                onBack = { nav.popBackStack() },
                onOpenConversation = { id ->
                    nav.navigate(Routes.chat(id)) {
                        popUpTo(Routes.CHAT_REQUESTS) { inclusive = true }
                    }
                },
                onOpenRequestPreview = { id ->
                    nav.navigate(Routes.requestPreview(id))
                }
            )
        }

        composable(Routes.REQUEST_PREVIEW) { entry ->
            val id = entry.arguments?.getString("conversationId").orEmpty()
            RequestPreviewScreen(
                conversationId = id,
                onBack = { nav.popBackStack() },
                onOpenConversation = { convId ->
                    nav.navigate(Routes.chat(convId)) {
                        popUpTo(Routes.REQUEST_PREVIEW) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BLOCKED_USERS) {
            BlockedUsersScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.VERIFICATION) {
            VerificationScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenTheme = { /* shown as sheet inside SettingsScreen via state */ },
                onOpenTerms = { nav.navigate(Routes.LEGAL_TERMS) },
                onOpenPrivacy = { nav.navigate(Routes.LEGAL_PRIVACY) },
                onOpenPrivacySettings = { nav.navigate(Routes.SETTINGS_PRIVACY) },
                onOpenNotificationSettings = { nav.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                onOpenDiscoverSettings = { nav.navigate(Routes.SETTINGS_DISCOVER) },
                onOpenAbout = { nav.navigate(Routes.SETTINGS_ABOUT) },
                onOpenContact = { nav.navigate(Routes.SETTINGS_CONTACT) },
                onOpenAccountSettings = { nav.navigate(Routes.SETTINGS_ACCOUNT) },
                onOpenContentSettings = { nav.navigate(Routes.SETTINGS_CONTENT) },
                onLoggedOut = { goTo(Routes.LOGIN, clearBackstack = true) }
            )
        }

        composable(Routes.SETTINGS_ACCOUNT) {
            AccountSettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenBlockedUsers = { nav.navigate(Routes.BLOCKED_USERS) },
                onOpenChangePassword = { nav.navigate(Routes.SETTINGS_CHANGE_PASSWORD) },
                onLoggedOut = { goTo(Routes.LOGIN, clearBackstack = true) },
                onOpenViolations = { nav.navigate(Routes.SETTINGS_VIOLATIONS) },
                onOpenStanding = { nav.navigate(Routes.SETTINGS_STANDING) },
                onOpenRequests = { nav.navigate(Routes.SETTINGS_REQUESTS) }
            )
        }

        composable(Routes.SETTINGS_VIOLATIONS) {
            com.chathala.hala.feature.settings.ui.account.ViolationsHistoryScreen(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_STANDING) {
            com.chathala.hala.feature.settings.ui.account.AccountStandingScreen(
                onBack = { nav.popBackStack() },
                onOpenViolations = { nav.navigate(Routes.SETTINGS_VIOLATIONS) }
            )
        }

        composable(Routes.SETTINGS_REQUESTS) {
            com.chathala.hala.feature.settings.ui.account.MyRequestsScreen(
                onBack = { nav.popBackStack() },
                onOpenRequest = { id -> nav.navigate(Routes.requestDetail(id)) },
                onOpenPrivacy = { nav.navigate(Routes.LEGAL_PRIVACY) },
                onOpenTerms = { nav.navigate(Routes.LEGAL_TERMS) },
                onOpenContact = { nav.navigate(Routes.SETTINGS_CONTACT) },
                onOpenViolations = { nav.navigate(Routes.SETTINGS_VIOLATIONS) }
            )
        }

        composable(
            Routes.SETTINGS_REQUEST_DETAIL,
            arguments = listOf(navArgument("appealId") { type = NavType.StringType })
        ) { entry ->
            val appealId = entry.arguments?.getString("appealId").orEmpty()
            com.chathala.hala.feature.settings.ui.account.RequestDetailScreen(
                appealId = appealId,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_PRIVACY) {
            PrivacyScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_NOTIFICATIONS) {
            NotificationSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_DISCOVER) {
            DiscoverSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_CONTENT) {
            ContentPreferencesScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_CONTACT) {
            ContactScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS_CHANGE_PASSWORD) {
            ChangePasswordScreen(
                onBack = { nav.popBackStack() },
                onSuccess = { nav.popBackStack() }
            )
        }

        composable(Routes.LEGAL_PRIVACY) {
            LegalDocScreen(
                type = LegalDocType.PRIVACY,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.LEGAL_TERMS) {
            LegalDocScreen(
                type = LegalDocType.TERMS,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
