package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AdminBookingStats
import com.example.core.AppStrings
import com.example.core.LocalAppStrings
import com.example.data.model.DriverProfile
import com.example.data.model.DriverPublicProfile
import com.example.data.model.DriverStatus
import com.example.data.model.RideRating
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VehicleRecord
import com.example.ui.components.Base64PhotoThumbnail
import com.example.ui.theme.TotoAmber
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError
import java.util.Locale

fun LazyListScope.adminOverviewSection(
  strings: AppStrings,
  users: List<UserProfile>,
  drivers: List<DriverProfile>,
  onlineDrivers: List<DriverPublicProfile>,
  stats: AdminBookingStats,
) {
  val passengersCount = users.count { it.role == UserRole.PASSENGER }
  val pendingCount = drivers.count { it.status == DriverStatus.PENDING_VERIFICATION }
  val approvedCount = drivers.count { it.status == DriverStatus.APPROVED }
  val rejectedOrSuspendedCount = drivers.count {
    it.status == DriverStatus.REJECTED || it.status == DriverStatus.SUSPENDED
  }

  item {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminKpiCard(strings.totalPassengers, "$passengersCount", Modifier.weight(1f))
        AdminKpiCard(strings.totalDrivers, "${drivers.size}", Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminKpiCard(strings.pendingApplications, "$pendingCount", Modifier.weight(1f))
        AdminKpiCard(strings.onlineDriversCount, "${onlineDrivers.size}", Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminKpiCard(strings.statusApproved, "$approvedCount", Modifier.weight(1f))
        AdminKpiCard("${strings.statusRejected} / ${strings.statusSuspended}", "$rejectedOrSuspendedCount", Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminKpiCard(strings.totalBookingsCount, "${stats.totalBookings}", Modifier.weight(1f))
        AdminKpiCard(strings.activeBookingsCount, "${stats.activeBookings}", Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AdminKpiCard(strings.completedBookingsCount, "${stats.completedBookings}", Modifier.weight(1f))
        AdminKpiCard(strings.cancelledBookingsCount, "${stats.cancelledBookings}", Modifier.weight(1f))
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = TotoLightMintSurface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.dailyBookingStatsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${strings.totalBookingsCount}: ${stats.todayBookings}", style = MaterialTheme.typography.bodySmall)
            Text("${strings.completedBookingsCount}: ${stats.todayCompletedBookings}", style = MaterialTheme.typography.bodySmall)
            Text("${strings.cancelledBookingsCount}: ${stats.todayCancelledBookings}", style = MaterialTheme.typography.bodySmall)
          }
          Text(
            text = "${strings.totalRevenueLabel}: ₹${stats.todayRevenue.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            color = TotoDeepEmerald,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.monthlyBookingStatsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${strings.totalBookingsCount}: ${stats.monthBookings}", style = MaterialTheme.typography.bodySmall)
            Text("${strings.completedBookingsCount}: ${stats.monthCompletedBookings}", style = MaterialTheme.typography.bodySmall)
            Text("${strings.cancelledBookingsCount}: ${stats.monthCancelledBookings}", style = MaterialTheme.typography.bodySmall)
          }
          Text(
            text = "${strings.totalRevenueLabel}: ₹${stats.monthRevenue.toInt()} • Lifetime ₹${stats.totalRevenue.toInt()}",
            style = MaterialTheme.typography.titleSmall,
            color = TotoEmeraldPrimary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

fun LazyListScope.adminDriverManagementSection(
  strings: AppStrings,
  drivers: List<DriverProfile>,
  vehicleByDriver: Map<String, VehicleRecord>,
  ratingsByDriver: Map<String, List<RideRating>>,
  driverFilter: Int,
  isBusy: Boolean,
  onSelectFilter: (Int) -> Unit,
  onInspectPhoto: (String, String?) -> Unit,
  onApprove: (DriverProfile, VehicleRecord?) -> Unit,
  onReject: (DriverProfile) -> Unit,
  onSuspend: (DriverProfile) -> Unit,
) {
  val pendingCount = drivers.count { it.status == DriverStatus.PENDING_VERIFICATION }
  val approvedCount = drivers.count { it.status == DriverStatus.APPROVED }

  item {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(
        "${strings.statusPending} ($pendingCount)",
        "${strings.filterAll} (${drivers.size})",
        "${strings.statusApproved} ($approvedCount)",
        "${strings.statusRejected} / ${strings.statusSuspended}",
      ).forEachIndexed { idx, label ->
        FilterChip(
          selected = driverFilter == idx,
          onClick = { onSelectFilter(idx) },
          label = { Text(label) },
        )
      }
    }
  }

  val filteredDrivers = drivers.filter { d ->
    when (driverFilter) {
      0 -> d.status == DriverStatus.PENDING_VERIFICATION
      2 -> d.status == DriverStatus.APPROVED
      3 -> d.status == DriverStatus.REJECTED || d.status == DriverStatus.SUSPENDED
      else -> true
    }
  }

  if (filteredDrivers.isEmpty()) {
    item {
      Text(
        text = "No driver profiles match this filter.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  } else {
    items(filteredDrivers, key = { it.driverId }) { driver ->
      val vehicle = vehicleByDriver[driver.driverId]
      val driverRatings = ratingsByDriver[driver.driverId].orEmpty()
      AdminDriverReviewCard(
        driver = driver,
        vehicle = vehicle,
        ratings = driverRatings,
        isBusy = isBusy,
        onInspectPhoto = onInspectPhoto,
        onApprove = { onApprove(driver, vehicle) },
        onReject = { onReject(driver) },
        onSuspend = { onSuspend(driver) },
      )
    }
  }
}

@Composable
fun AdminKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(4.dp))
      Text(value, style = MaterialTheme.typography.headlineSmall, color = TotoDeepEmerald, fontWeight = FontWeight.ExtraBold)
    }
  }
}

@Composable
fun AdminDriverReviewCard(
  driver: DriverProfile,
  vehicle: VehicleRecord?,
  ratings: List<RideRating>,
  isBusy: Boolean,
  onInspectPhoto: (String, String?) -> Unit,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onSuspend: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val avgRating = if (ratings.isNotEmpty()) ratings.sumOf { it.stars.toDouble() } / ratings.size else null

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Base64PhotoThumbnail(
          base64 = driver.profilePhotoBase64,
          contentDescription = driver.fullName,
          size = 56.dp,
          isCircle = true,
          emptyLabel = "Driver",
          modifier = Modifier.clickable { onInspectPhoto(strings.driverPhoto, driver.profilePhotoBase64) },
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(driver.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val vehicleSummary = buildString {
            append(driver.registrationNumber)
            append(" • ")
            append(driver.vehicleType)
            if (driver.vehicleModel.isNotBlank()) append(" (${driver.vehicleModel})")
          }
          Text(vehicleSummary, style = MaterialTheme.typography.bodySmall)
          Text("${driver.serviceArea} • ${driver.mobileNumber}", style = MaterialTheme.typography.labelSmall)
          if (!driver.licenceNumber.isNullOrBlank()) {
            Text("Licence: ${driver.licenceNumber}", style = MaterialTheme.typography.labelSmall)
          }
          if (!driver.permitNumber.isNullOrBlank()) {
            Text("Permit: ${driver.permitNumber}", style = MaterialTheme.typography.labelSmall)
          }
        }
        Surface(
          color = when (driver.status) {
            DriverStatus.APPROVED -> TotoLightMintSurface
            DriverStatus.REJECTED, DriverStatus.SUSPENDED -> TotoRedContainer
            else -> TotoAmberContainer
          },
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = driver.status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Surface(
        color = TotoLightMintSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${strings.totalTrips}: ${driver.totalTrips} • ₹${driver.totalEarnings.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = TotoDeepEmerald,
            fontWeight = FontWeight.Bold,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = TotoAmber, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(3.dp))
            Text(
              text = if (avgRating != null) String.format(Locale.US, "%.1f (%d)", avgRating, ratings.size) else "N/A",
              style = MaterialTheme.typography.labelMedium,
              color = TotoDeepEmerald,
            )
          }
        }
      }

      if (!driver.rejectionReason.isNullOrBlank() && driver.status != DriverStatus.APPROVED) {
        Text(
          text = "${strings.rejectionReasonLabel}: ${driver.rejectionReason}",
          style = MaterialTheme.typography.labelSmall,
          color = TotoRedError,
        )
      }

      if (vehicle != null) {
        Text(strings.inspectDocsAndPhotos, style = MaterialTheme.typography.labelSmall, color = TotoDeepEmerald, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Base64PhotoThumbnail(vehicle.frontPhotoBase64, "Front", size = 64.dp, emptyLabel = "Front", modifier = Modifier.clickable { onInspectPhoto(strings.totoFrontPhoto, vehicle.frontPhotoBase64) })
          Base64PhotoThumbnail(vehicle.rearPhotoBase64, "Rear", size = 64.dp, emptyLabel = "Rear", modifier = Modifier.clickable { onInspectPhoto(strings.totoRearPhoto, vehicle.rearPhotoBase64) })
          Base64PhotoThumbnail(vehicle.sidePhotoBase64, "Side", size = 64.dp, emptyLabel = "Side", modifier = Modifier.clickable { onInspectPhoto(strings.totoSidePhoto, vehicle.sidePhotoBase64) })
          Base64PhotoThumbnail(vehicle.licenceDocBase64, "Licence", size = 64.dp, emptyLabel = "Licence", modifier = Modifier.clickable { onInspectPhoto(strings.licenceDocPhoto, vehicle.licenceDocBase64) })
          Base64PhotoThumbnail(vehicle.permitDocBase64, "Permit", size = 64.dp, emptyLabel = "Permit", modifier = Modifier.clickable { onInspectPhoto(strings.permitDocPhoto, vehicle.permitDocBase64) })
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (driver.status != DriverStatus.APPROVED) {
          Button(
            onClick = onApprove,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = TotoEmeraldPrimary),
            modifier = Modifier
              .weight(1f)
              .testTag("admin_approve_driver_button"),
          ) {
            Text(strings.approveDriver)
          }
        }
        if (driver.status != DriverStatus.REJECTED) {
          OutlinedButton(
            onClick = onReject,
            enabled = !isBusy,
            modifier = Modifier.weight(1f),
          ) {
            Text(strings.rejectDriver)
          }
        }
        if (driver.status == DriverStatus.APPROVED) {
          OutlinedButton(
            onClick = onSuspend,
            enabled = !isBusy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TotoRedError),
            modifier = Modifier.weight(1f),
          ) {
            Text(strings.suspendDriver)
          }
        }
      }
    }
  }
}
