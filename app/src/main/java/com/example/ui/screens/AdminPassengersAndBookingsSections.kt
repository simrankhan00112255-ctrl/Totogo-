package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AdminAnalytics
import com.example.core.AppStrings
import com.example.core.LocalAppStrings
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Complaint
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.Base64PhotoThumbnail
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LazyListScope.adminPassengerManagementSection(
  strings: AppStrings,
  users: List<UserProfile>,
  bookings: List<Booking>,
  complaints: List<Complaint>,
  showAllUsers: Boolean,
  isBusy: Boolean,
  onToggleUserFilter: (Boolean) -> Unit,
  onInspectPassenger: (UserProfile) -> Unit,
  onToggleSuspend: (userId: String, suspend: Boolean) -> Unit,
) {
  val filteredUsers = if (showAllUsers) users else users.filter { it.role == UserRole.PASSENGER }
  val bookingsByUser = bookings.groupBy { it.passengerId }
  val complaintsByUser = complaints.groupBy { it.reporterId }

  item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "${strings.passengerManagementTitle} (${filteredUsers.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TotoDeepEmerald,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !showAllUsers,
          onClick = { onToggleUserFilter(false) },
          label = { Text("${strings.totalPassengers} (${users.count { it.role == UserRole.PASSENGER }})") },
        )
        FilterChip(
          selected = showAllUsers,
          onClick = { onToggleUserFilter(true) },
          label = { Text("${strings.filterAll} (${users.size})") },
        )
      }
    }
  }

  if (filteredUsers.isEmpty()) {
    item {
      Text("No registered users yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  } else {
    items(filteredUsers, key = { it.userId }) { u ->
      val rideCount = bookingsByUser[u.userId]?.size ?: 0
      val complaintCount = complaintsByUser[u.userId]?.size ?: 0
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Base64PhotoThumbnail(
              base64 = u.photoBase64,
              contentDescription = u.fullName,
              size = 46.dp,
              isCircle = true,
              emptyLabel = "User",
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(u.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (u.isSuspended) {
                  Surface(color = TotoRedContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(
                      text = strings.suspendedBadge,
                      style = MaterialTheme.typography.labelSmall,
                      color = TotoRedError,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
              }
              Text("${u.role} • ${u.mobileNumber.ifBlank { "No phone" }}", style = MaterialTheme.typography.bodySmall)
              if (u.email.isNotBlank()) {
                Text(u.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                text = "${strings.totalBookingsCount}: $rideCount • ${strings.adminTabSupport}: $complaintCount",
                style = MaterialTheme.typography.labelSmall,
                color = TotoDeepEmerald,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { onInspectPassenger(u) },
              modifier = Modifier.weight(1f),
            ) {
              Text(strings.viewHistoryAndComplaints, style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { onToggleSuspend(u.userId, !u.isSuspended) },
              enabled = !isBusy,
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (u.isSuspended) TotoDeepEmerald else TotoRedError,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("admin_suspend_user_${u.userId}"),
            ) {
              Text(if (u.isSuspended) strings.unsuspendUser else strings.suspendUser, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}

fun LazyListScope.adminLiveBookingsSection(
  strings: AppStrings,
  bookings: List<Booking>,
  bookingFilter: Int,
  onSelectFilter: (Int) -> Unit,
) {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.US)

  item {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(
        strings.filterActive,
        strings.filterAll,
        strings.filterCompleted,
        strings.filterCancelled,
      ).forEachIndexed { idx, label ->
        FilterChip(
          selected = bookingFilter == idx,
          onClick = { onSelectFilter(idx) },
          label = { Text(label) },
        )
      }
    }
  }

  val filtered = bookings.filter { b ->
    when (bookingFilter) {
      0 -> b.status in listOf(
        BookingStatus.SEARCHING,
        BookingStatus.ASSIGNED,
        BookingStatus.ARRIVING,
        BookingStatus.STARTED,
      )
      2 -> b.status == BookingStatus.COMPLETED
      3 -> b.status == BookingStatus.CANCELLED
      else -> true
    }
  }

  if (filtered.isEmpty()) {
    item {
      Text(strings.noBookingsYet, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  } else {
    items(filtered, key = { it.id }) { b ->
      val createdText = b.createdAt?.seconds?.let { sec ->
        Instant.ofEpochSecond(sec).atZone(AdminAnalytics.IST_ZONE).format(dateFormatter)
      } ?: ""
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = "${b.pickupName} → ${b.destinationName}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            Surface(
              color = when (b.status) {
                BookingStatus.COMPLETED -> TotoLightMintSurface
                BookingStatus.CANCELLED -> TotoRedContainer
                else -> TotoAmberContainer
              },
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = b.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (b.status == BookingStatus.CANCELLED) TotoRedError else TotoDeepEmerald,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          Text(
            text = "Passenger: ${b.passengerName}${b.passengerMobile?.let { " ($it)" }.orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
          )
          if (!b.driverName.isNullOrBlank()) {
            Text(
              text = "Driver: ${b.driverName}${b.driverPhone?.let { " ($it)" }.orEmpty()} • ${b.vehicleRegNumber.orEmpty()}",
              style = MaterialTheme.typography.bodySmall,
              color = TotoDeepEmerald,
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = "₹${(b.finalFare ?: b.estimatedFare).toInt()} • ${String.format(Locale.US, "%.1f", b.distanceKm)} km",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
            if (createdText.isNotBlank()) {
              Text(createdText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = if (b.driverLat != null && b.driverLng != null) TotoEmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = if (b.driverLat != null && b.driverLng != null) {
                "${strings.realGpsAvailable}: ${String.format(Locale.US, "%.4f, %.4f", b.driverLat, b.driverLng)}"
              } else {
                strings.noLiveGpsYet
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (!b.cancelReason.isNullOrBlank()) {
            Text(
              text = "Reason: ${b.cancelReason}",
              style = MaterialTheme.typography.labelSmall,
              color = TotoRedError,
            )
          }
        }
      }
    }
  }
}

@Composable
fun AdminPassengerDetailDialog(
  passenger: UserProfile,
  passengerBookings: List<Booking>,
  passengerComplaints: List<Complaint>,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${passenger.fullName} — ${strings.viewHistoryAndComplaints}", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${passenger.role} • ${passenger.mobileNumber.ifBlank { "No phone" }}", style = MaterialTheme.typography.bodySmall)
        Text("${strings.totalBookingsCount}: ${passengerBookings.size}", fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
        if (passengerBookings.isEmpty()) {
          Text(strings.noBookingsYet, style = MaterialTheme.typography.bodySmall)
        } else {
          passengerBookings.take(5).forEach { b ->
            Text(
              text = "• ${b.pickupName} → ${b.destinationName} (${b.status}, ₹${(b.finalFare ?: b.estimatedFare).toInt()})",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        Text("${strings.complaintsTitle}: ${passengerComplaints.size}", fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
        if (passengerComplaints.isEmpty()) {
          Text("No complaints filed.", style = MaterialTheme.typography.bodySmall)
        } else {
          passengerComplaints.take(5).forEach { c ->
            Text("• [${c.status}] ${c.category}: ${c.description}", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.closeLabel)
      }
    },
  )
}
