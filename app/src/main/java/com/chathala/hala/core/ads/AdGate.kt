package com.chathala.hala.core.ads

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chathala.hala.feature.user.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * بوابة واحدة تقرّر: هل تُعرض الإعلانات لهذا المستخدم؟
 *
 * لا إعلانات للمشتركين (بريميوم) — وهذا جزء من قيمة الاشتراك.
 * القيمة الابتدائية `false` عمداً: لا نعرض إعلاناً قبل معرفة حالة المستخدم،
 * حتى لا يومض بانر للمشترك عند فتح التطبيق. يُحدَّثها [HalaApp] من `currentUser`.
 */
object AdGate {

    private val _adsEnabled = MutableStateFlow(false)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

    /** يُستدعى مع كل تغيّر في المستخدم الحالي (دخول/خروج/تجديد اشتراك). */
    fun update(user: User?) {
        _adsEnabled.value = user != null && !user.isPremium
    }

    val enabled: Boolean get() = _adsEnabled.value

    @Composable
    fun rememberEnabled(): Boolean = adsEnabled.collectAsStateWithLifecycle().value
}
