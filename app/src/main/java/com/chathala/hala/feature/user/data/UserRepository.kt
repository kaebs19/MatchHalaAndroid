package com.chathala.hala.feature.user.data

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
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")

        val raw = api.getMe("Bearer $token")
        val data = raw["data"] as? Map<String, Any?>
            ?: throw IllegalStateException("استجابة غير متوقّعة (data مفقود)")
        val userMap = data["user"] as? Map<String, Any?>
            ?: throw IllegalStateException("استجابة غير متوقّعة (user مفقود)")

        val user = userMap.toUserDomain()
            ?: throw IllegalStateException("بيانات المستخدم غير مكتملة")
        userStorage.save(user)
        user
    }

    suspend fun clear() {
        userStorage.clear()
    }
}
