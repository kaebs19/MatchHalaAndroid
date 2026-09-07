package com.chathala.hala.feature.profile.data

import com.squareup.moshi.JsonClass

/** ردّ GET /api/mobile/stats — عدّادات بطاقة النشاط في الملف الشخصي. */
@JsonClass(generateAdapter = true)
data class MyStatsResponse(
    val success: Boolean,
    val data: MyStatsData? = null
)

@JsonClass(generateAdapter = true)
data class MyStatsData(
    val visitors: Int = 0,
    val likes: Int = 0,
    val conversations: Int = 0
)

/** ما تعرضه بطاقة النشاط: الإحصاءات + عدّ الأصدقاء من نقطة الأصدقاء. */
data class ProfileStats(
    val friends: Int = 0,
    val pendingRequests: Int = 0,
    val conversations: Int = 0,
    val likes: Int = 0,
    val visitors: Int = 0
)
