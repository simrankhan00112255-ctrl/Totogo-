package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AdminAnalytics
import com.example.core.LocalAppStrings
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.Booking
import com.example.data.model.Complaint
import com.example.data.model.DriverProfile
import com.example.data.model.DriverPublicProfile
import com.example.data.model.DriverStatus
import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import com.example.data.model.RideRating
import com.example.data.model.UserProfile
import com.example.data.model.VehicleRecord
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.TotoLogoHeader
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoRedError

@Composable
fun AdminDashboardScreen(
  currentAdminUid: String,
  users: List<UserProfile>,
  drivers: List<DriverProfile>,
  vehicles: List<VehicleRecord>,
  onlineDrivers: List<DriverPublicProfile>,
  bookings: List<Booking>,
  complaints: List<Complaint>,
  ratings: List<RideRating>,
  auditLogs: List<AdminAuditLog>,
  admins: List<AdminRecord>,
  fareSettings: FareSettings,
  fixedRoutes: List<FixedRoute>,
  isBusy: Boolean,
  onToggleLanguage: () -> Unit,
  onApproveDriver: (DriverProfile, VehicleRecord?) -> Unit,
  onSetDriverStatus: (driverId: String, status: String, reason: String?) -> Unit,
  onToggleUserSuspend: (userId: String, suspend: Boolean) -> Unit,
  onSaveFareSettings: (FareSettings) -> Unit,
  onAddFixedRoute: (origin: String, dest: String, fare: Double) -> Unit,
  onDeleteFixedRoute: (routeId: String) -> Unit,
  onUpdateComplaint: (complaintId: String, status: String, notes: String?, reporterId: String?) -> Unit,
  onGrantAdmin: (uid: String, email: String) -> Unit,
  onRevokeAdmin: (uid: String) -> Unit,
  onNotifyMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var selectedSection by remember { mutableIntStateOf(0) }
  var driverFilter by remember { mutableIntStateOf(0) }
  var bookingFilter by remember { mutableIntStateOf(0) }
  var complaintFilter by remember { mutableIntStateOf(0) }
  var showAllUsers by remember { mutableStateOf(false) }

  var previewPhoto by remember { mutableStateOf<Pair<String, String?>?>(null) }
  var statusReasonTarget by remember { mutableStateOf<Pair<DriverProfile, String>?>(null) }
  var inspectedPassenger by remember { mutableStateOf<UserProfile?>(null) }
  var replyComplaintTarget by remember { mutableStateOf<Complaint?>(null) }

  val sections = listOf(
    strings.adminTabOverview,
    strings.adminTabDrivers,
    strings.adminTabPassengers,
    strings.adminTabLiveRides,
    strings.adminTabFares,
    strings.adminTabReports,
    strings.adminTabSupport,
    strings.adminTabSecurity,
  )

  val vehicleByDriver = remember(vehicles) { vehicles.associateBy { it.driverId } }
  val ratingsByDriver = remember(ratings) { ratings.groupBy { it.driverId } }
  val stats = remember(bookings) { AdminAnalytics.computeBookingStats(bookings) }
  val driverPerformance = remember(drivers, bookings, ratings) {
    AdminAnalytics.computeDriverPerformance(drivers, bookings, ratings)
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      TotoLogoHeader(onToggleLanguage = onToggleLanguage)
    }
    item {
      Text(
        text = strings.adminDashboardTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TotoDeepEmerald,
      )
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        sections.forEachIndexed { idx, title ->
          FilterChip(
            selected = selectedSection == idx,
            onClick = { selectedSection = idx },
            label = { Text(title) },
            modifier = Modifier.testTag("admin_tab_$idx"),
          )
        }
      }
    }

    when (selectedSection) {
      0 -> adminOverviewSection(
        strings = strings,
        users = users,
        drivers = drivers,
        onlineDrivers = onlineDrivers,
        stats = stats,
      )

      1 -> adminDriverManagementSection(
        strings = strings,
        drivers = drivers,
        vehicleByDriver = vehicleByDriver,
        ratingsByDriver = ratingsByDriver,
        driverFilter = driverFilter,
        isBusy = isBusy,
        onSelectFilter = { driverFilter = it },
        onInspectPhoto = { title, data -> previewPhoto = title to data },
        onApprove = onApproveDriver,
        onReject = { driver -> statusReasonTarget = driver to DriverStatus.REJECTED },
        onSuspend = { driver -> statusReasonTarget = driver to DriverStatus.SUSPENDED },
      )

      2 -> adminPassengerManagementSection(
        strings = strings,
        users = users,
        bookings = bookings,
        complaints = complaints,
        showAllUsers = showAllUsers,
        isBusy = isBusy,
        onToggleUserFilter = { showAllUsers = it },
        onInspectPassenger = { inspectedPassenger = it },
        onToggleSuspend = onToggleUserSuspend,
      )

      3 -> adminLiveBookingsSection(
        strings = strings,
        bookings = bookings,
        bookingFilter = bookingFilter,
        onSelectFilter = { bookingFilter = it },
      )

      4 -> {
        item {
          AdminFareEditorCard(
            fareSettings = fareSettings,
            fixedRoutes = fixedRoutes,
            isBusy = isBusy,
            onSaveSettings = onSaveFareSettings,
            onAddFixedRoute = onAddFixedRoute,
            onDeleteFixedRoute = onDeleteFixedRoute,
          )
        }
      }

      5 -> {
        item {
          AdminReportsAndAnalyticsCard(
            stats = stats,
            bookings = bookings,
            driverPerformance = driverPerformance,
            onNotifyMessage = onNotifyMessage,
          )
        }
      }

      6 -> adminComplaintsSection(
        strings = strings,
        complaints = complaints,
        statusFilter = complaintFilter,
        onSelectFilter = { complaintFilter = it },
        onOpenReplyDialog = { replyComplaintTarget = it },
      )

      7 -> adminSecurityAndAuditSection(
        strings = strings,
        currentAdminUid = currentAdminUid,
        admins = admins,
        auditLogs = auditLogs,
        isBusy = isBusy,
        onGrantAdmin = onGrantAdmin,
        onRevokeAdmin = onRevokeAdmin,
      )
    }
  }

  previewPhoto?.let { (title, data) ->
    PhotoPreviewDialog(
      title = title,
      base64 = data,
      onDismiss = { previewPhoto = null },
    )
  }

  statusReasonTarget?.let { (driver, targetStatus) ->
    var reasonText by remember {
      mutableStateOf(
        if (targetStatus == DriverStatus.REJECTED) "Blurry TOTO photos or missing permit document"
        else "Account suspended by administrator",
      )
    }
    AlertDialog(
      onDismissRequest = { statusReasonTarget = null },
      title = {
        Text(
          text = if (targetStatus == DriverStatus.REJECTED) strings.rejectDriver else strings.suspendDriver,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${driver.fullName} (${driver.registrationNumber})", style = MaterialTheme.typography.labelLarge)
          OutlinedTextField(
            value = reasonText,
            onValueChange = { reasonText = it },
            label = { Text(strings.rejectionReasonLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSetDriverStatus(driver.driverId, targetStatus, reasonText.trim().ifBlank { "Reviewed by admin" })
            statusReasonTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = TotoRedError),
        ) {
          Text(if (targetStatus == DriverStatus.REJECTED) strings.rejectDriver else strings.suspendDriver)
        }
      },
      dismissButton = {
        TextButton(onClick = { statusReasonTarget = null }) {
          Text(strings.closeLabel)
        }
      },
    )
  }

  inspectedPassenger?.let { passenger ->
    AdminPassengerDetailDialog(
      passenger = passenger,
      passengerBookings = bookings.filter { it.passengerId == passenger.userId },
      passengerComplaints = complaints.filter { it.reporterId == passenger.userId },
      onDismiss = { inspectedPassenger = null },
    )
  }

  replyComplaintTarget?.let { complaint ->
    AdminComplaintReplyDialog(
      complaint = complaint,
      isBusy = isBusy,
      onSubmit = { status, notes ->
        onUpdateComplaint(complaint.id, status, notes, complaint.reporterId)
        replyComplaintTarget = null
      },
      onDismiss = { replyComplaintTarget = null },
    )
  }
}
