package com.chathala.hala.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * مساعد لجلب آخر موقع معروف للمستخدم عبر Fused Location API.
 *
 *  - يفحص الإذن أولاً (COARSE أو FINE) — يُرجع null لو لم يُمنح
 *  - يطلب `getCurrentLocation` (Priority.BALANCED) حتى نحصل على قيمة حديثة دون تجميد
 *  - يرجع فقط lat/lng
 */
data class LatLng(val lat: Double, val lng: Double, val accuracyMeters: Float? = null)

/** اسم المكان بعد الترجمة العكسية للإحداثيات على الجهاز. */
data class PlaceName(val city: String?, val country: String?)

object LocationHelper {

    private const val TAG = "LocationHelper"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchLastLocation(context: Context): LatLng? {
        if (!hasLocationPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (!cont.isActive) return@addOnSuccessListener
                    if (loc != null) {
                        cont.resume(LatLng(loc.latitude, loc.longitude, loc.accuracy))
                    } else {
                        // fallback: lastLocation
                        client.lastLocation
                            .addOnSuccessListener { l ->
                                cont.resume(l?.let { LatLng(it.latitude, it.longitude, it.accuracy) })
                            }
                            .addOnFailureListener {
                                Log.w(TAG, "lastLocation fallback failed: ${it.message}")
                                cont.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    Log.w(TAG, "getCurrentLocation failed: ${it.message}")
                    cont.resume(null)
                }
        }
    }

    /**
     * يترجم الإحداثيات إلى مدينة/دولة على الجهاز.
     *
     * الخادم لا يملك مترجماً عكسياً، فبدون هذه الخطوة تصل لوحة التحكم إحداثيات
     * فقط. تُنفَّذ خارج الخيط الرئيسي، وترجع null بهدوء إن غاب مزوّد الترجمة.
     */
    suspend fun resolvePlace(context: Context, point: LatLng): PlaceName? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale("en"))
            val address = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(point.lat, point.lng, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(point.lat, point.lng, 1)?.firstOrNull()
                }
            }.onFailure { Log.w(TAG, "geocoder failed: ${it.message}") }.getOrNull()
                ?: return@withContext null

            PlaceName(
                city = address.locality ?: address.subAdminArea ?: address.adminArea,
                country = address.countryCode ?: address.countryName
            )
        }
}
