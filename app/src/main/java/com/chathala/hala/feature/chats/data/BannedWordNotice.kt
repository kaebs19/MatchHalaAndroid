package com.chathala.hala.feature.chats.data

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chathala.hala.R
import com.chathala.hala.core.i18n.LocaleManager
import com.chathala.hala.core.i18n.S
import com.squareup.moshi.JsonClass

/** `warning` في ردّ الإرسال: كلمات محظورة حُجبت عن المستلم. */
@JsonClass(generateAdapter = true)
data class BannedWordWarning(
    val message: String? = null,
    val violations: Int? = null,
    val maxViolations: Int? = null,
    val banned: Boolean? = null
)

/**
 * المرسل يرى رسالته كاملة والمستلم يراها مطموسة — بلا هذا التنبيه لا يعرف أنها حُجبت.
 * مرة في اليوم، والمخالفة الأخيرة قبل الإيقاف (`violations >= max-1`) تظهر دائماً.
 * (مقابل `showBannedWordNotice` في iOS)
 */
object BannedWordNotice {
    private const val PREFS = "banned_word_notice"
    private const val KEY_LAST = "last_shown_at"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun show(warning: BannedWordWarning) {
        val ctx = LocaleManager.context
        val text = if (warning.banned == true) {
            S.get(R.string.banned_word_account_banned)
        } else {
            val count = warning.violations ?: 0
            val max = warning.maxViolations ?: 0
            val isLast = max > 0 && count >= max - 1
            val prefs = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (!isLast && now - prefs.getLong(KEY_LAST, 0L) < DAY_MS) return
            prefs.edit().putLong(KEY_LAST, now).apply()
            val counter = if (max > 0) " " + S.get(R.string.banned_word_counter, count, max) else ""
            if (isLast) S.get(R.string.banned_word_last_warning, counter)
            else S.get(R.string.banned_word_notice, counter)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, text, Toast.LENGTH_LONG).show()
        }
    }
}
