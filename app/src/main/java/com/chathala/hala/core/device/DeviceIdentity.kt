package com.chathala.hala.core.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import com.chathala.hala.BuildConfig
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * يوفّر معرّفات ثابتة للجهاز تُرسل مع طلبات المصادقة لتفعيل نظام حظر الأجهزة في الباك إند
 * (نموذج BannedDevice + middleware/bannedDeviceCheck).
 *
 * ثلاث طبقات — كل طبقة تسدّ ثغرة تهرّب مختلفة:
 *  1. [deviceFingerprint] — SHA-256 من مواصفات العتاد (شركة + موديل + جهاز + شاشة + لغة + توقيت).
 *  2. [deviceToken]       — UUID ثابت يُولّد مرة ويُحفظ محلياً. يُمسح عند حذف التطبيق
 *                           (يطابق `keychainToken` على الباك إند).
 *  3. [vendorId]          — ANDROID_ID: يبقى ثابتاً حتى بعد حذف التطبيق وإعادة تثبيته
 *                           (مكافئ identifierForVendor على iOS — طبقة مكافحة التهرّب الأقوى).
 *
 * يُهيّأ مرة واحدة من [com.chathala.hala.HalaApp.onCreate] عبر [init].
 * القيم تبقى فارغة قبل [init] (يقرأها interceptor الشبكة بأمان).
 */
object DeviceIdentity {

    private const val PREFS = "hala_device_identity"
    private const val KEY_UUID = "device_uuid"

    @Volatile
    private var initialized = false

    var deviceFingerprint: String = ""
        private set

    var deviceToken: String = ""
        private set

    var vendorId: String = ""
        private set

    /** اسم الجهاز للعرض/التحليل (مثل: "Samsung SM-G991B"). */
    var deviceModel: String = ""
        private set

    /** إصدار نظام التشغيل (مثل: "Android 14"). */
    var osVersion: String = ""
        private set

    val appVersion: String get() = BuildConfig.VERSION_NAME

    const val platform: String = "android"

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            deviceToken = readOrCreateUuid(prefs)
            vendorId = readAndroidId(appCtx)
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            osVersion = "Android ${Build.VERSION.RELEASE}"
            deviceFingerprint = computeFingerprint(appCtx)
            initialized = true
        }
    }

    private fun readOrCreateUuid(prefs: SharedPreferences): String {
        prefs.getString(KEY_UUID, null)?.let { return it }
        val uuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_UUID, uuid).apply()
        return uuid
    }

    @SuppressLint("HardwareIds")
    private fun readAndroidId(context: Context): String = try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    } catch (_: Exception) {
        ""
    }

    private fun computeFingerprint(context: Context): String {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val raw = buildString {
            append(Build.MANUFACTURER).append('|')
            append(Build.MODEL).append('|')
            append(Build.DEVICE).append('|')
            append(metrics.widthPixels).append('x').append(metrics.heightPixels).append('|')
            append(Locale.getDefault().language).append('|')
            append(TimeZone.getDefault().id)
        }
        return sha256(raw)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
