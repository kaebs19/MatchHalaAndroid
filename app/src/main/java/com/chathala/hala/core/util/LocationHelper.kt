package com.chathala.hala.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * مساعد لجلب آخر موقع معروف للمستخدم عبر Fused Location API.
 *
 *  - يفحص الإذن أولاً (COARSE أو FINE) — يُرجع null لو لم يُمنح
 *  - يطلب `getCurrentLocation` (Priority.BALANCED) حتى نحصل على قيمة حديثة دون تجميد
 *  - يرجع فقط lat/lng
 */
data class LatLng(val lat: Double, val lng: Double)

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
                        cont.resume(LatLng(loc.latitude, loc.longitude))
                    } else {
                        // fallback: lastLocation
                        client.lastLocation
                            .addOnSuccessListener { l ->
                                cont.resume(l?.let { LatLng(it.latitude, it.longitude) })
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
}
