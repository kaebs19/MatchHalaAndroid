package com.chathala.hala

import android.app.Application
import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.core.storage.UserStorage
import com.chathala.hala.core.util.NetworkMonitor
import com.chathala.hala.feature.auth.data.AuthRepository
import com.chathala.hala.feature.chats.data.ChatsCacheStorage
import com.chathala.hala.feature.chats.data.ConversationsRepository
import com.chathala.hala.feature.chats.data.MessagesRepository
import com.chathala.hala.feature.blocking.data.BlockingRepository
import com.chathala.hala.feature.discover.data.DiscoverCacheStorage
import com.chathala.hala.feature.discover.data.DiscoverRepository
import com.chathala.hala.feature.verification.data.VerificationRepository
import com.chathala.hala.feature.chats.socket.HalaSocket
import com.chathala.hala.feature.chats.socket.SocketEvent
import com.chathala.hala.feature.legal.data.LegalRepository
import com.chathala.hala.feature.notifications.data.NotificationsRepository
import com.chathala.hala.feature.profile.data.ProfileRepository
import com.chathala.hala.feature.push.HalaNotificationChannels
import com.chathala.hala.feature.reporting.data.ReportRepository
import com.chathala.hala.feature.userprofile.data.UserProfileRepository
import com.chathala.hala.feature.push.data.DeviceTokenRepository
import com.chathala.hala.feature.settings.data.SettingsRepository
import com.chathala.hala.feature.user.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Application — يُهيّئ الـ singletons (Storage + Repositories).
 * ViewModels تصل لها عبر AndroidViewModelFactory.APPLICATION_KEY.
 */
class HalaApp : Application(), coil.ImageLoaderFactory {

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% من RAM للأفاتارات والصور
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            // معترض يحوّل المسارات النسبية مثل "/uploads/foo.jpg" إلى رابط كامل
            .components {
                add(com.chathala.hala.core.network.RelativeUrlMapper())
            }
            .build()
    }


    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var userStorage: UserStorage
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var legalRepository: LegalRepository
        private set

    lateinit var suspensionRepository: com.chathala.hala.feature.suspension.data.SuspensionRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var notificationsRepository: NotificationsRepository
        private set

    lateinit var deviceTokenRepository: DeviceTokenRepository
        private set

    lateinit var socket: HalaSocket
        private set

    lateinit var conversationsRepository: ConversationsRepository
        private set

    lateinit var messagesRepository: MessagesRepository
        private set

    lateinit var discoverRepository: DiscoverRepository
        private set

    lateinit var discoverCacheStorage: DiscoverCacheStorage
        private set

    lateinit var chatsCacheStorage: ChatsCacheStorage
        private set

    lateinit var blockingRepository: BlockingRepository
        private set

    lateinit var verificationRepository: VerificationRepository
        private set

    lateinit var reportRepository: ReportRepository
        private set

    lateinit var userProfileRepository: UserProfileRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var appPreferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        // يجب أن تُهيّأ معرّفات الجهاز قبل أي طلب شبكة (interceptor + repositories يقرؤونها)
        com.chathala.hala.core.device.DeviceIdentity.init(applicationContext)
        // تهيئة AdMob (best-effort، لا تُعطّل الإقلاع)
        runCatching {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        }
        tokenStorage = TokenStorage(applicationContext)
        userStorage = UserStorage(applicationContext, ApiClient.moshi)
        ApiClient.init(tokenStorage = tokenStorage, userStorage = userStorage)
        networkMonitor = NetworkMonitor(applicationContext)
        appPreferences = AppPreferences(applicationContext)
        userRepository = UserRepository(
            userStorage = userStorage,
            tokenStorage = tokenStorage
        )
        deviceTokenRepository = DeviceTokenRepository(tokenStorage = tokenStorage)
        socket = HalaSocket(tokenStorage = tokenStorage)
        discoverCacheStorage = DiscoverCacheStorage(applicationContext, ApiClient.moshi)
        chatsCacheStorage = ChatsCacheStorage(applicationContext, ApiClient.moshi)
        authRepository = AuthRepository(
            tokenStorage = tokenStorage,
            userRepository = userRepository,
            deviceTokenRepository = deviceTokenRepository,
            socket = socket,
            discoverCache = discoverCacheStorage,
            chatsCache = chatsCacheStorage
        )
        profileRepository = ProfileRepository(storage = tokenStorage)
        legalRepository = LegalRepository()
        suspensionRepository = com.chathala.hala.feature.suspension.data.SuspensionRepository(tokenStorage = tokenStorage)
        settingsRepository = SettingsRepository(tokenStorage = tokenStorage)
        notificationsRepository = NotificationsRepository(tokenStorage = tokenStorage)
        conversationsRepository = ConversationsRepository(tokenStorage = tokenStorage)
        messagesRepository = MessagesRepository(tokenStorage = tokenStorage)
        discoverRepository = DiscoverRepository(tokenStorage = tokenStorage)
        blockingRepository = BlockingRepository(tokenStorage = tokenStorage)
        verificationRepository = VerificationRepository(tokenStorage = tokenStorage)
        reportRepository = ReportRepository(tokenStorage = tokenStorage)
        userProfileRepository = UserProfileRepository(tokenStorage = tokenStorage)
        HalaNotificationChannels.registerAll(this)

        // الاستجابة لطلب السيرفر بتحديث FCM token (عند connect لـ Socket)
        socket.incoming
            .onEach { event ->
                if (event is SocketEvent.RequestFcmToken) {
                    runCatching { deviceTokenRepository.ensureSynced() }
                }
            }
            .launchIn(appScope)
    }
}
