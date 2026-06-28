package com.chathala.hala.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private val displayFormat by lazy {
        SimpleDateFormat("dd / MM / yyyy", Locale("ar"))
    }

    private val isoFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun formatDisplay(date: Date): String = displayFormat.format(date)

    fun formatIso(date: Date): String = isoFormat.format(date)
}
