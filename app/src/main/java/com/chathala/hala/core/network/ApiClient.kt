package com.chathala.hala.core.network

import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.core.storage.UserStorage
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
    private lateinit var userStorage: UserStorage

    fun init(tokenStorage: TokenStorage, userStorage: UserStorage) {
        this.tokenStorage = tokenStorage
        this.userStorage = userStorage
    }

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
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
            .addInterceptor(SuspensionInterceptor())
            .addInterceptor(logging)
            .authenticator(
                TokenAuthenticator(
                    tokenStorage = tokenStorage,
                    userStorage = userStorage,
                    refreshService = refreshService
                )
            )
            .build()
    }

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
