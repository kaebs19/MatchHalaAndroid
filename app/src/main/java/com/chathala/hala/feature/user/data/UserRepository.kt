package com.chathala.hala.feature.user.data

import com.chathala.hala.core.i18n.S
import com.chathala.hala.R

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import com.chathala.hala.core.storage.UserStorage
import com.chathala.hala.feature.auth.data.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * يدير المستخدم الحالي:
 *  - `currentUser` Flow قابل للمراقبة في أي شاشة
 *  - `saveFromAuth(...)` بعد تسجيل الدخول/التسجيل (بيانات أساسية)
 *  - `refresh()` يجلب البيانات الكاملة من /api/auth/me
 *  - `clear()` عند تسجيل الخروج
 */
class UserRepository(
    private val api: ApiService = ApiClient.service,
    private val userStorage: UserStorage,
    private val tokenStorage: TokenStorage
) {

    val currentUser: Flow<User?> = userStorage.user

    /** يُحفظ من رد Login/Register الأساسي. */
    suspend fun saveFromAuth(authUser: AuthUser) {
        val id = authUser.id ?: return
        val name = authUser.name ?: return
        val email = authUser.email ?: return
        userStorage.save(
            User(
                id = id,
                name = name,
                email = email,
                role = authUser.role,
                profileImage = authUser.profileImage
            )
        )
    }

    /** يجلب بيانات المستخدم الكاملة من السيرفر ويحدّث الكاش. */
    @Suppress("UNCHECKED_CAST")
    suspend fun refresh(): NetworkResult<User> = safeApiCall {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))

        val raw = api.getMe("Bearer $token")
        val data = raw["data"] as? Map<String, Any?>
            ?: throw IllegalStateException(S.get(R.string.err_unexpected_data_missing))
        val userMap = data["user"] as? Map<String, Any?>
            ?: throw IllegalStateException(S.get(R.string.err_unexpected_user_missing))

        val user = userMap.toUserDomain()
            ?: throw IllegalStateException(S.get(R.string.err_user_data_incomplete))
        userStorage.save(user)
        user
    }

    suspend fun clear() {
        userStorage.clear()
    }

    /**
     * يرفع موقع المستخدم للخادم (لوحة التحكم + حساب المسافات).
     *
     * مكبوح زمنياً: الاكتشاف يطلب الموقع عند كل دخول للشاشة، وبلا كبح يصبح
     * الرفع طلب شبكة في كل مرة بلا فائدة. الفشل صامت — الموقع رفاهية لا وظيفة.
     */
    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        country: String? = null,
        accuracy: Float? = null
    ): NetworkResult<Unit> = safeApiCall {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException(S.get(R.string.auth_no_active_session))
        api.updateUserLocation(
            bearer = "Bearer $token",
            body = UpdateLocationRequest(
                latitude = latitude,
                longitude = longitude,
                city = city,
                country = country,
                accuracy = accuracy
            )
        )
        lastLocationSentAtMs = System.currentTimeMillis()
        Unit
    }

    /** هل مضى ما يكفي منذ آخر رفع؟ (يُفحص قبل استدعاء [updateLocation]) */
    fun shouldSendLocation(): Boolean =
        System.currentTimeMillis() - lastLocationSentAtMs >= LOCATION_MIN_INTERVAL_MS

    private companion object {
        /** آخر رفع ناجح — في الذاكرة فقط: إعادة الرفع بعد إعادة التشغيل مقبولة. */
        @Volatile
        var lastLocationSentAtMs: Long = 0L

        /** ساعة واحدة بين رفعَين — يكفي لتتبّع الانتقال بين المدن بلا إزعاج للشبكة. */
        const val LOCATION_MIN_INTERVAL_MS = 60 * 60 * 1000L
    }
}
