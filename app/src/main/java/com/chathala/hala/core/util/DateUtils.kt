package com.chathala.hala.core.util

import com.chathala.hala.core.i18n.LocaleManager

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    // يُبنى عند الطلب لا `by lazy` — وإلا تجمّدت اللغة على أول استدعاء.
    private val displayFormat: SimpleDateFormat
        get() = SimpleDateFormat("dd / MM / yyyy", LocaleManager.locale)

    private val isoFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun formatDisplay(date: Date): String = displayFormat.format(date)

    fun formatIso(date: Date): String = isoFormat.format(date)
}
