package com.chathala.hala.feature.visitors.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall
import com.chathala.hala.core.storage.TokenStorage
import kotlinx.coroutines.flow.first

/** زيارات الملف الشخصي: تسجيل الزيارة + جلب قائمة الزوّار. */
class VisitorsRepository(
    private val api: ApiService = ApiClient.service,
    private val tokenStorage: TokenStorage
) {

    /**
     * يسجّل زيارة ملف مستخدم آخر (best-effort — لا يُعطّل فتح الشاشة عند الفشل).
     * الخادم يتجاهل التكرار خلال 24 ساعة ويحترم الوضع المخفي.
     */
    suspend fun recordView(viewedUserId: String): NetworkResult<String> = safeApiCall {
        api.recordProfileView(bearer(), RecordViewBody(viewedUserId)).message ?: "ok"
    }

    suspend fun fetchVisitors(page: Int = 1): NetworkResult<ProfileViewsData> = safeApiCall {
        api.getProfileViews(bearer(), page = page).data ?: ProfileViewsData()
    }

    private suspend fun bearer(): String {
        val token = tokenStorage.token.first()
            ?: throw IllegalStateException("لا يوجد جلسة نشطة")
        return "Bearer $token"
    }
}
