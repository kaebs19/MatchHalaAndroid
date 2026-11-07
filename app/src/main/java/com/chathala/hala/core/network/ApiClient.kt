package com.chathala.hala.core.network

import com.chathala.hala.BuildConfig
import com.chathala.hala.core.session.SessionTokens
import kotlinx.coroutines.runBlocking
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.feature.auth.data.ErrorBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton لعميل الشبكة. يُهيّأ من [HalaApp.onCreate] عبر [init] قبل أول استخدام.
 * - `service`: ApiService الرئيسي (مع Authenticator لتجديد الرموز تلقائياً)
 * - `refreshService`: عميل منفصل لاستدعاء /refresh-token (بدون authenticator)
 */
object ApiClient {

    const val BASE_URL: String = "https://matchhala.chathala.com/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            // يقبل sender ككائن أو نصّ (id) — يمنع خطأ BEGIN_OBJECT/STRING
            .add(com.chathala.hala.feature.chats.data.MessageSenderAdapterFactory)
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val errorParser: com.squareup.moshi.JsonAdapter<ErrorBody> by lazy {
        moshi.adapter(ErrorBody::class.java)
    }

    private lateinit var tokenStorage: TokenStorage

    fun init(tokenStorage: TokenStorage) {
        this.tokenStorage = tokenStorage
    }

    /**
     * سجلّ الشبكة — **في debug فقط**. المستوى BODY يطبع الترويسات والأجسام
     * كاملةً في logcat: أي أن رمز الوصول ورمز التجديد ونصّ كل رسالة خاصة
     * تُكتب بوضوح. ذلك مقبول أثناء التطوير، وتسريبٌ في نسخة المتجر (تقارير
     * الأخطاء وأدوات الجهاز تلتقط logcat). في release نستخدم NONE.
     */
    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // ── Refresh client (without authenticator) ──────────────────
    private val refreshOkHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DeviceHeaderInterceptor())
            .addInterceptor(logging)
            .build()
    }

    private val refreshRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(refreshOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val refreshService: RefreshApiService by lazy {
        refreshRetrofit.create(RefreshApiService::class.java)
    }

    // ── Main client (with TokenAuthenticator) ───────────────────
    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DeviceHeaderInterceptor())
            .addInterceptor { chain ->
                val request = chain.request()
                val tokens = runBlocking { tokenStorage.snapshot() }
                val tagged = if (tokens != null && request.header("Authorization") == "Bearer ${tokens.accessToken}") {
                    request.newBuilder().tag(SessionTokens::class.java, tokens).build()
                } else request
                chain.proceed(tagged)
            }
            .addInterceptor(SuspensionInterceptor())
            .addInterceptor(RestrictionHeaderInterceptor())
            .addInterceptor(logging)
            .authenticator(
                TokenAuthenticator(
                    tokenStorage = tokenStorage,
                    refreshService = refreshService
                )
            )
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val service: ApiService by lazy { retrofit.create(ApiService::class.java) }

    /** مسارات القصص — واجهة منفصلة كي لا يتضخّم [ApiService]. */
    val storyService: com.chathala.hala.feature.stories.data.StoryApiService by lazy {
        retrofit.create(com.chathala.hala.feature.stories.data.StoryApiService::class.java)
    }

    val stickerService: com.chathala.hala.feature.stickers.StickerApi by lazy {
        retrofit.create(com.chathala.hala.feature.stickers.StickerApi::class.java)
    }

    val bootstrapService: com.chathala.hala.core.session.data.BootstrapApi by lazy {
        retrofit.create(com.chathala.hala.core.session.data.BootstrapApi::class.java)
    }

    /** عميل HTTP المصادَق — لتنزيل ملفات (فيديو مؤقت) بنفس الترويسات. */
    val httpClient: OkHttpClient get() = okHttp
}
