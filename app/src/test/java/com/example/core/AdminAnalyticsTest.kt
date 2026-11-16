package com.example.core

import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverProfile
import com.example.data.model.DriverStatus
import com.example.data.model.RideRating
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class AdminAnalyticsTest {

  @Test
  fun computeBookingStats_calculatesDailyMonthlyRevenueAndCancellations() {
    val now = ZonedDateTime.of(2026, 11, 15, 14, 0, 0, 0, AdminAnalytics.IST_ZONE)
    val todaySec = now.minusHours(2).toEpochSecond()
    val earlierMonthSec = now.withDayOfMonth(2).toEpochSecond()
    val lastMonthSec = now.minusMonths(1).toEpochSecond()

    val bookings = listOf(
      Booking(
        id = "b1",
        status = BookingStatus.COMPLETED,
        estimatedFare = 40.0,
        finalFare = 45.0,
        createdAt = Timestamp(todaySec, 0),
      ),
      Booking(
        id = "b2",
        status = BookingStatus.CANCELLED,
        estimatedFare = 30.0,
        cancelReason = "Driver delayed",
        createdAt = Timestamp(todaySec, 0),
      ),
      Booking(
        id = "b3",
        status = BookingStatus.COMPLETED,
        estimatedFare = 60.0,
        finalFare = 60.0,
        createdAt = Timestamp(earlierMonthSec, 0),
      ),
      Booking(
        id = "b4",
        status = BookingStatus.SEARCHING,
        estimatedFare = 25.0,
        createdAt = Timestamp(todaySec, 0),
      ),
      Booking(
        id = "b5",
        status = BookingStatus.COMPLETED,
        estimatedFare = 100.0,
        finalFare = 100.0,
        createdAt = Timestamp(lastMonthSec, 0),
      ),
    )

    val stats = AdminAnalytics.computeBookingStats(bookings, nowMillis = now.toInstant().toEpochMilli())

    assertEquals(5, stats.totalBookings)
    assertEquals(1, stats.activeBookings)
    assertEquals(3, stats.completedBookings)
    assertEquals(1, stats.cancelledBookings)
    assertEquals(205.0, stats.totalRevenue, 0.01)

    assertEquals(3, stats.todayBookings)
    assertEquals(1, stats.todayCompletedBookings)
    assertEquals(1, stats.todayCancelledBookings)
    assertEquals(45.0, stats.todayRevenue, 0.01)

    assertEquals(4, stats.monthBookings)
    assertEquals(2, stats.monthCompletedBookings)
    assertEquals(105.0, stats.monthRevenue, 0.01)

    assertEquals(20.0, stats.cancellationRatePercent, 0.01)
    assertEquals(listOf("Driver delayed" to 1), stats.cancellationReasons)
  }

  @Test
  fun computeDriverPerformance_andCsvExport_escapesAndFormatsAccurately() {
    val drivers = listOf(
      DriverProfile(
        driverId = "d1",
        fullName = "Karim Ali",
        registrationNumber = "WB-59-T-1010",
        serviceArea = "Chopra Bazar",
        status = DriverStatus.APPROVED,
        totalTrips = 3,
        totalEarnings = 140.0,
      ),
      DriverProfile(
        driverId = "d2",
        fullName = "Raju \"Pro\" Mia",
        registrationNumber = "WB-59-T-2020",
        serviceArea = "Daspara, Haat",
        status = DriverStatus.APPROVED,
        totalTrips = 1,
        totalEarnings = 50.0,
      ),
    )
    val bookings = listOf(
      Booking(
        id = "bk1",
        passengerName = "Suman Das",
        driverId = "d1",
        driverName = "Karim Ali",
        pickupName = "Chopra Bazar",
        destinationName = "Sonapur, More",
        distanceKm = 2.5,
        estimatedFare = 35.0,
        finalFare = 40.0,
        status = BookingStatus.COMPLETED,
      )
    )
    val ratings = listOf(
      RideRating(id = "r1", driverId = "d1", stars = 5),
      RideRating(id = "r2", driverId = "d1", stars = 4),
    )

    val perf = AdminAnalytics.computeDriverPerformance(drivers, bookings, ratings)
    assertEquals(2, perf.size)
    assertEquals("d1", perf[0].driverId)
    assertEquals(4.5, perf[0].avgRating ?: 0.0, 0.01)
    assertEquals(2, perf[0].ratingCount)

    val bookingsCsv = AdminAnalytics.buildBookingsCsv(bookings)
    assertTrue(bookingsCsv.contains("Booking ID,Date (IST),Status"))
    assertTrue(bookingsCsv.contains("\"Sonapur, More\""))

    val driversCsv = AdminAnalytics.buildDriverPerformanceCsv(perf)
    assertTrue(driversCsv.contains("\"Raju \"\"Pro\"\" Mia\""))
    assertTrue(driversCsv.contains("\"Daspara, Haat\""))
  }
}
