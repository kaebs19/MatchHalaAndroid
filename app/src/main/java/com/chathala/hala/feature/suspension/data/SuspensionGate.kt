package com.chathala.hala.feature.suspension.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * حامل مؤقت + قناة أحداث للتعليق/الحظر بين طبقة الشبكة وشاشة الحظر.
 *
 * مساران يصلانه:
 *  1. تدفّق تسجيل الدخول (AuthViewModel) — يضبط [account] ثم يوجّه عبر حالة الشاشة.
 *  2. التعليق أثناء الجلسة — `SuspensionInterceptor` يلتقط 403 من أي طلب،
 *     يضبط [account] ويُطلق [events] ليلتقطها NavGraph ويوجّه لشاشة الحظر.
 *
 * تفاصيل حساب الإيقاف تأتي في جسم الرد (لا تُمرّر عبر وسائط التنقّل)؛
 * حظر الجهاز تجلب الشاشة تفاصيله من الشبكة.
 */
object SuspensionGate {

    @Volatile
    var account: AccountSuspensionInfo? = null

    private val _events = MutableSharedFlow<SuspensionMode>(extraBufferCapacity = 1)
    val events: SharedFlow<SuspensionMode> = _events.asSharedFlow()

    /** يُستدعى من معترِض الشبكة عند رصد تعليق أثناء الجلسة. */
    fun publish(info: AccountSuspensionInfo?, mode: SuspensionMode) {
        if (mode == SuspensionMode.ACCOUNT) account = info
        _events.tryEmit(mode)
    }

    fun consume(): AccountSuspensionInfo? {
        val v = account
        account = null
        return v
    }
}
