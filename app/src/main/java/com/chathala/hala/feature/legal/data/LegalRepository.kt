package com.chathala.hala.feature.legal.data

import com.chathala.hala.core.network.ApiClient
import com.chathala.hala.core.network.ApiService
import com.chathala.hala.core.network.NetworkResult
import com.chathala.hala.core.network.safeApiCall

class LegalRepository(
    private val api: ApiService = ApiClient.service
) {

    suspend fun fetchPrivacyPolicy(): NetworkResult<String> =
        safeApiCall { api.getPrivacyPolicy().data?.content.orEmpty() }

    suspend fun fetchTerms(): NetworkResult<String> =
        safeApiCall { api.getTerms().data?.content.orEmpty() }
}
