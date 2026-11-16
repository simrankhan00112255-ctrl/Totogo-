package com.example.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class GpsReading(
  val lat: Double,
  val lng: Double,
  val isLiveGps: Boolean = true,
)

object LocationHelper {
  fun hasLocationPermission(context: Context): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentLocationOrNull(context: Context): GpsReading? {
    if (!hasLocationPermission(context)) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
      val token = CancellationTokenSource()
      val current = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
      if (current != null) {
        GpsReading(current.latitude, current.longitude, isLiveGps = true)
      } else {
        val last = client.lastLocation.await()
        last?.let { GpsReading(it.latitude, it.longitude, isLiveGps = true) }
      }
    } catch (_: Exception) {
      null
    }
  }

  @SuppressLint("MissingPermission")
  fun observeLocationUpdates(
    context: Context,
    intervalMs: Long = 10_000L,
  ): Flow<GpsReading> = callbackFlow {
    if (!hasLocationPermission(context)) {
      close()
      return@callbackFlow
    }
    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
      .setMinUpdateIntervalMillis(intervalMs / 2)
      .setMinUpdateDistanceMeters(15f)
      .build()

    val callback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val loc = result.lastLocation ?: return
        trySend(GpsReading(loc.latitude, loc.longitude, isLiveGps = true))
      }
    }

    try {
      client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: Exception) {
      close()
      return@callbackFlow
    }

    awaitClose {
      runCatching { client.removeLocationUpdates(callback) }
    }
  }
}
