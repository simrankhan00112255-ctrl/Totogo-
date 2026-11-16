package com.example.core

import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ChopraLandmark(
  val id: String,
  val nameEn: String,
  val nameBn: String,
  val lat: Double,
  val lng: Double,
  val areaType: String = "Hub",
) {
  fun displayName(isBengali: Boolean): String = if (isBengali) nameBn else nameEn
}

object ChopraLocations {
  // Real coordinates for Chopra & surrounding areas, Uttar Dinajpur, West Bengal
  val landmarks = listOf(
    ChopraLandmark("chopra_bazar", "Chopra Bazar", "চোপড়া বাজার", 26.4042, 88.2485, "Market"),
    ChopraLandmark("chopra_bus_stand", "Chopra Bus Stand (NH-27)", "চোপড়া বাস স্ট্যান্ড (NH-27)", 26.4018, 88.2458, "Transit"),
    ChopraLandmark("chopra_hospital", "Chopra Rural Hospital", "চোপড়া গ্রামীণ হাসপাতাল", 26.4066, 88.2512, "Hospital"),
    ChopraLandmark("sonapur_more", "Sonapur More", "সোনাপুর মোড়", 26.4254, 88.2620, "Junction"),
    ChopraLandmark("lakhipur", "Lakhipur Bazar", "লক্ষ্মীপুর বাজার", 26.3895, 88.2290, "Market"),
    ChopraLandmark("daspara", "Daspara Haat", "দাসপাড়া হাট", 26.4390, 88.2940, "Market"),
    ChopraLandmark("haptiagachh", "Haptiagachh", "হাপতিয়াগছ", 26.3810, 88.2715, "Village"),
    ChopraLandmark("tinmile_hat", "Tinmile Hat", "তিনমাইল হাট", 26.3695, 88.2155, "Market"),
    ChopraLandmark("dalua", "Dalua More", "ডালুয়া মোড়", 26.4160, 88.2255, "Junction"),
    ChopraLandmark("majhiali", "Majhiali", "মাঝিয়ালি", 26.4495, 88.2345, "Village"),
    ChopraLandmark("islampur_more", "Islampur More", "ইসলামপুর মোড়", 26.2660, 88.1890, "City Hub"),
  )

  const val CENTER_LAT = 26.4042
  const val CENTER_LNG = 88.2485

  fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a =
      sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    // Road factor (~1.22x) for rural/NH-27 road curvature
    val roadKm = earthRadiusKm * c * 1.22
    return ((roadKm * 10.0).roundToInt()) / 10.0
  }

  data class FareCalculation(
    val distanceKm: Double,
    val estimatedFare: Double,
    val isFixedRoute: Boolean,
  )

  fun calculateFare(
    pickupName: String,
    pickupLat: Double,
    pickupLng: Double,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double,
    settings: FareSettings,
    fixedRoutes: List<FixedRoute>,
  ): FareCalculation {
    val distanceKm = haversineKm(pickupLat, pickupLng, destinationLat, destinationLng)
    val matchedFixed =
      fixedRoutes.firstOrNull { route ->
        (route.originName.equals(pickupName, ignoreCase = true) &&
          route.destinationName.equals(destinationName, ignoreCase = true)) ||
          (route.originName.equals(destinationName, ignoreCase = true) &&
            route.destinationName.equals(pickupName, ignoreCase = true))
      }

    if (matchedFixed != null) {
      return FareCalculation(
        distanceKm = distanceKm,
        estimatedFare = matchedFixed.fixedFare,
        isFixedRoute = true,
      )
    }

    val rawFare =
      if (distanceKm <= settings.minDistanceKm) {
        settings.minFare
      } else {
        val extraKm = distanceKm - settings.minDistanceKm
        val standardThresholdKm = 5.0
        if (distanceKm <= standardThresholdKm) {
          settings.minFare + extraKm * settings.perKmFare
        } else {
          val baseExtra = (standardThresholdKm - settings.minDistanceKm).coerceAtLeast(0.0) * settings.perKmFare
          val longDistanceExtra = (distanceKm - standardThresholdKm) * settings.additionalDistanceFarePerKm
          settings.minFare + baseExtra + longDistanceExtra
        }
      }
    // Round to nearest 5 INR for convenient local cash change
    val roundedFare = ((rawFare / 5.0).roundToInt() * 5.0).coerceAtLeast(settings.minFare)
    return FareCalculation(
      distanceKm = distanceKm,
      estimatedFare = roundedFare,
      isFixedRoute = false,
    )
  }
}
