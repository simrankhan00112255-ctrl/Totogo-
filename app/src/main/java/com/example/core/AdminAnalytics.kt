package com.example.core

import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverProfile
import com.example.data.model.RideRating
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AdminBookingStats(
  val totalBookings: Int = 0,
  val activeBookings: Int = 0,
  val completedBookings: Int = 0,
  val cancelledBookings: Int = 0,
  val totalRevenue: Double = 0.0,
  val todayBookings: Int = 0,
  val todayCompletedBookings: Int = 0,
  val todayCancelledBookings: Int = 0,
  val todayRevenue: Double = 0.0,
  val monthBookings: Int = 0,
  val monthCompletedBookings: Int = 0,
  val monthCancelledBookings: Int = 0,
  val monthRevenue: Double = 0.0,
  val cancellationRatePercent: Double = 0.0,
  val cancellationReasons: List<Pair<String, Int>> = emptyList(),
)

data class DriverPerformanceRow(
  val driverId: String,
  val fullName: String,
  val registrationNumber: String,
  val serviceArea: String,
  val status: String,
  val totalTrips: Long,
  val completedFromBookings: Int,
  val totalEarnings: Double,
  val avgRating: Double?,
  val ratingCount: Int,
)

object AdminAnalytics {
  val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
  private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

  fun computeBookingStats(
    bookings: List<Booking>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = IST_ZONE,
  ): AdminBookingStats {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val thisMonth = YearMonth.from(today)

    var active = 0
    var completed = 0
    var cancelled = 0
    var totalRev = 0.0

    var todayAll = 0
    var todayComp = 0
    var todayCanc = 0
    var todayRev = 0.0

    var monthAll = 0
    var monthComp = 0
    var monthCanc = 0
    var monthRev = 0.0

    val reasonCounts = linkedMapOf<String, Int>()

    for (b in bookings) {
      val fare = b.finalFare ?: b.estimatedFare
      val isActive = b.status in listOf(
        BookingStatus.SEARCHING,
        BookingStatus.ASSIGNED,
        BookingStatus.ARRIVING,
        BookingStatus.STARTED,
      )
      val isCompleted = b.status == BookingStatus.COMPLETED
      val isCancelled = b.status == BookingStatus.CANCELLED

      if (isActive) active++
      if (isCompleted) {
        completed++
        totalRev += fare
      }
      if (isCancelled) {
        cancelled++
        val reason = b.cancelReason?.trim()?.takeIf { it.isNotBlank() } ?: "Unspecified"
        reasonCounts[reason] = (reasonCounts[reason] ?: 0) + 1
      }

      val createdSec = b.createdAt?.seconds
      if (createdSec != null) {
        val bookingDate: LocalDate = Instant.ofEpochSecond(createdSec).atZone(zoneId).toLocalDate()
        val bookingMonth: YearMonth = YearMonth.from(bookingDate)

        if (bookingDate == today) {
          todayAll++
          if (isCompleted) {
            todayComp++
            todayRev += fare
          }
          if (isCancelled) todayCanc++
        }
        if (bookingMonth == thisMonth) {
          monthAll++
          if (isCompleted) {
            monthComp++
            monthRev += fare
          }
          if (isCancelled) monthCanc++
        }
      }
    }

    val cancellationRate = if (bookings.isNotEmpty()) {
      (cancelled * 100.0) / bookings.size.toDouble()
    } else {
      0.0
    }

    return AdminBookingStats(
      totalBookings = bookings.size,
      activeBookings = active,
      completedBookings = completed,
      cancelledBookings = cancelled,
      totalRevenue = totalRev,
      todayBookings = todayAll,
      todayCompletedBookings = todayComp,
      todayCancelledBookings = todayCanc,
      todayRevenue = todayRev,
      monthBookings = monthAll,
      monthCompletedBookings = monthComp,
      monthCancelledBookings = monthCanc,
      monthRevenue = monthRev,
      cancellationRatePercent = cancellationRate,
      cancellationReasons = reasonCounts.entries
        .sortedByDescending { it.value }
        .map { it.key to it.value },
    )
  }

  fun computeDriverPerformance(
    drivers: List<DriverProfile>,
    bookings: List<Booking>,
    ratings: List<RideRating>,
  ): List<DriverPerformanceRow> {
    val ratingsByDriver = ratings.groupBy { it.driverId }
    val completedByDriver = bookings
      .filter { it.status == BookingStatus.COMPLETED && !it.driverId.isNullOrBlank() }
      .groupBy { it.driverId!! }

    return drivers.map { d ->
      val dRatings = ratingsByDriver[d.driverId].orEmpty()
      val avg = if (dRatings.isNotEmpty()) {
        dRatings.sumOf { it.stars.toDouble() } / dRatings.size.toDouble()
      } else {
        null
      }
      val completedBookings = completedByDriver[d.driverId].orEmpty()
      val effectiveTrips = maxOf(d.totalTrips, completedBookings.size.toLong())
      val completedSum = completedBookings.sumOf { it.finalFare ?: it.estimatedFare }
      val effectiveEarnings = maxOf(d.totalEarnings, completedSum)

      DriverPerformanceRow(
        driverId = d.driverId,
        fullName = d.fullName,
        registrationNumber = d.registrationNumber,
        serviceArea = d.serviceArea,
        status = d.status,
        totalTrips = effectiveTrips,
        completedFromBookings = completedBookings.size,
        totalEarnings = effectiveEarnings,
        avgRating = avg,
        ratingCount = dRatings.size,
      )
    }.sortedWith(
      compareByDescending<DriverPerformanceRow> { it.totalTrips }
        .thenByDescending { it.totalEarnings }
    )
  }

  fun escapeCsv(raw: String?): String {
    val value = raw ?: ""
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    return if (needsQuotes) {
      "\"${value.replace("\"", "\"\"")}\""
    } else {
      value
    }
  }

  fun buildBookingsCsv(
    bookings: List<Booking>,
    zoneId: ZoneId = IST_ZONE,
  ): String {
    val sb = StringBuilder()
    sb.appendLine(
      "Booking ID,Date (IST),Status,Passenger Name,Passenger Phone,Driver Name,Driver Phone,Vehicle Reg,Pickup,Destination,Distance (km),Estimated Fare (INR),Final Fare (INR),Cancel Reason"
    )
    for (b in bookings) {
      val dateStr = b.createdAt?.seconds?.let { sec ->
        Instant.ofEpochSecond(sec).atZone(zoneId).format(dateTimeFormatter)
      } ?: ""
      val row = listOf(
        b.id,
        dateStr,
        b.status,
        b.passengerName,
        b.passengerMobile.orEmpty(),
        b.driverName.orEmpty(),
        b.driverPhone.orEmpty(),
        b.vehicleRegNumber.orEmpty(),
        b.pickupName,
        b.destinationName,
        String.format(Locale.US, "%.2f", b.distanceKm),
        String.format(Locale.US, "%.2f", b.estimatedFare),
        b.finalFare?.let { String.format(Locale.US, "%.2f", it) }.orEmpty(),
        b.cancelReason.orEmpty(),
      ).joinToString(",") { escapeCsv(it) }
      sb.appendLine(row)
    }
    return sb.toString()
  }

  fun buildDriverPerformanceCsv(rows: List<DriverPerformanceRow>): String {
    val sb = StringBuilder()
    sb.appendLine(
      "Driver ID,Full Name,Registration No,Service Area,Status,Completed Trips,Total Earnings (INR),Average Rating,Total Ratings"
    )
    for (r in rows) {
      val row = listOf(
        r.driverId,
        r.fullName,
        r.registrationNumber,
        r.serviceArea,
        r.status,
        r.totalTrips.toString(),
        String.format(Locale.US, "%.2f", r.totalEarnings),
        r.avgRating?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A",
        r.ratingCount.toString(),
      ).joinToString(",") { escapeCsv(it) }
      sb.appendLine(row)
    }
    return sb.toString()
  }
}
