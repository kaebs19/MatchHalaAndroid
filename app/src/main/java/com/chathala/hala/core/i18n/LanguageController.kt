package com.chathala.hala.core.i18n

import com.chathala.hala.core.storage.AppLanguage
import com.chathala.hala.core.storage.AppPreferences
import com.chathala.hala.feature.push.data.DeviceTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * نقطة واحدة لتغيير لغة الواجهة.
 *
 * تغيير اللغة ثلاث خطوات لا واحدة: تحديث الحالة الحيّة، والحفظ، وإبلاغ الخادم
 * كي يبني نصّ الإشعارات باللغة الجديدة. توزيعها على نقاط الاستدعاء (الإعدادات
 * وزر شاشة الدخول) كان سيجعل نسيان إحداها عيباً صامتاً — لا يظهر إلا كإشعار
 * بلغة خاطئة بعد أيام.
 */
object LanguageController {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun apply(
        language: AppLanguage,
        prefs: AppPreferences,
        deviceTokens: DeviceTokenRepository
    ) {
        // أولاً الحالة الحيّة: التبديل يجب أن يظهر في نفس الإطار
        LocaleManager.setLanguage(language)
        scope.launch {
            prefs.setLanguage(language)
            // best-effort: يفشل بهدوء إن لم يكن المستخدم مسجّلاً — يُعاد الإرسال
            // عند أول تسجيل دخول لأن register يحمل اللغة الحالية.
            runCatching { deviceTokens.ensureSynced() }
        }
    }
}
