package com.example.core

import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopraLocationsTest {

  @Test
  fun calculateFare_fixedRouteMatch_usesFixedFare() {
    val settings = FareSettings(minFare = 20.0, perKmFare = 12.0, minDistanceKm = 1.5)
    val fixedRoutes = listOf(
      FixedRoute(
        id = "r1",
        originName = "Chopra Bazar",
        destinationName = "Sonapur More",
        fixedFare = 25.0,
      )
    )
    val res = ChopraLocations.calculateFare(
      pickupName = "Chopra Bazar",
      pickupLat = 26.4042,
      pickupLng = 88.2485,
      destinationName = "Sonapur More",
      destinationLat = 26.4254,
      destinationLng = 88.2620,
      settings = settings,
      fixedRoutes = fixedRoutes,
    )
    assertTrue(res.isFixedRoute)
    assertEquals(25.0, res.estimatedFare, 0.01)
  }

  @Test
  fun calculateFare_shortTrip_respectsMinimumFare() {
    val settings = FareSettings(minFare = 20.0, perKmFare = 12.0, minDistanceKm = 1.5)
    val res = ChopraLocations.calculateFare(
      pickupName = "Chopra Bazar",
      pickupLat = 26.4042,
      pickupLng = 88.2485,
      destinationName = "Chopra Bus Stand",
      destinationLat = 26.4018,
      destinationLng = 88.2458,
      settings = settings,
      fixedRoutes = emptyList(),
    )
    assertFalse(res.isFixedRoute)
    assertEquals(20.0, res.estimatedFare, 0.01)
  }

  @Test
  fun calculateFare_longTripBeyond5Km_appliesAdditionalKmRateAndRoundsTo5() {
    val settings = FareSettings(
      minFare = 20.0,
      perKmFare = 12.0,
      minDistanceKm = 1.5,
      additionalDistanceFarePerKm = 10.0,
    )
    val res = ChopraLocations.calculateFare(
      pickupName = "Chopra Bazar",
      pickupLat = 26.4042,
      pickupLng = 88.2485,
      destinationName = "Daspara Haat",
      destinationLat = 26.4390,
      destinationLng = 88.2940,
      settings = settings,
      fixedRoutes = emptyList(),
    )
    assertFalse(res.isFixedRoute)
    assertTrue(res.distanceKm > 5.0)
    assertTrue(res.estimatedFare >= 60.0)
    assertEquals(0.0, res.estimatedFare % 5.0, 0.001)
  }
}
