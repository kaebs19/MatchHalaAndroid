package com.chathala.hala.feature.profile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * فهرس الاهتمامات المُحمَّلة من `/api/interests`، مفهرَساً بالمفتاح.
 *
 * الملف الشخصي يخزّن مفاتيح فقط (`football`)، وقائمة الأسماء تأتي من الخادم
 * ويحرّرها الأدمن — فلا نُضمّن نسخة ثابتة في التطبيق. يُملأ الفهرس عند أول جلب
 * ناجح ويُستخدم لعرض الاسم بلغة الواجهة الحالية.
 *
 * الحالة حالة Compose: تعبئة الفهرس تُعيد تركيب الشرائح التي تنتظر الأسماء.
 */
object InterestsCatalog {

    private var byKey by mutableStateOf<Map<String, Interest>>(emptyMap())

    fun update(interests: List<Interest>) {
        if (interests.isEmpty()) return
        byKey = interests.associateBy { it.key }
    }

    /**
     * اسم الاهتمام باللغة الحالية.
     * يعود للمفتاح الخام إذا لم يُحمَّل الفهرس بعد — أفضل من شريحة فارغة.
     */
    fun label(key: String): String = byKey[key]?.name ?: key
}
