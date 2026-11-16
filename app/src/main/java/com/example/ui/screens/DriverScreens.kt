package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.core.LocationHelper
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverProfile
import com.example.data.model.DriverPublicProfile
import com.example.data.model.DriverStatus
import com.example.data.model.RideRating
import com.example.data.model.UserProfile
import com.example.data.model.VehicleRecord
import com.example.ui.components.Base64PhotoThumbnail
import com.example.ui.components.BookingStatusTimeline
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.TotoLogoHeader
import com.example.ui.theme.TotoAmber
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoPureWhite
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun DriverDashboardScreen(
  userProfile: UserProfile,
  driverProfile: DriverProfile?,
  vehicleRecord: VehicleRecord?,
  driverPublic: DriverPublicProfile?,
  searchingBookings: List<Booking>,
  driverBookings: List<Booking>,
  driverRatings: List<RideRating>,
  dismissedBookingIds: Set<String>,
  isBusy: Boolean,
  onToggleLanguage: () -> Unit,
  onSubmitRegistration: (
    fullName: String,
    mobile: String,
    mobileVerified: Boolean,
    regNo: String,
    vehicleType: String,
    vehicleModel: String,
    serviceArea: String,
    licenceNo: String?,
    permitNo: String?,
    driverPhoto: String?,
    frontPhoto: String?,
    rearPhoto: String?,
    sidePhoto: String?,
    licenceDoc: String?,
    permitDoc: String?,
  ) -> Unit,
  onToggleOnline: (online: Boolean, lat: Double?, lng: Double?, hasGpsFix: Boolean) -> Unit,
  onPushLiveGps: (lat: Double, lng: Double) -> Unit,
  onPushActiveTripGps: (bookingId: String, lat: Double, lng: Double) -> Unit,
  onAcceptBooking: (Booking, DriverProfile) -> Unit,
  onDeclineBooking: (String) -> Unit,
  onProgressBooking: (Booking, newStatus: String, finalFare: Double?, lat: Double?, lng: Double?) -> Unit,
  onReportPassenger: (passengerId: String, bookingId: String, category: String, desc: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val strings = LocalAppStrings.current
  val isBengali = LocalAppLanguage.current == AppLanguage.BENGALI

  var showEditForm by remember { mutableStateOf(false) }
  var reportTarget by remember { mutableStateOf<Booking?>(null) }
  var previewPhotoTitleAndData by remember { mutableStateOf<Pair<String, String?>?>(null) }
  var gpsNote by remember { mutableStateOf<String?>(null) }
  var hasLocationPerm by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
  var selectedDeskTab by remember { mutableIntStateOf(0) }
  var historyFilter by remember { mutableIntStateOf(0) }

  val locationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasLocationPerm = granted
    if (granted) {
      scope.launch {
        val fix = LocationHelper.getCurrentLocationOrNull(context)
        if (fix != null) {
          onToggleOnline(true, fix.lat, fix.lng, true)
          gpsNote = null
        } else {
          onToggleOnline(true, null, null, false)
          gpsNote = strings.gpsUnavailable
        }
      }
    } else {
      gpsNote = strings.gpsPermissionNeeded
    }
  }

  if (driverProfile == null || showEditForm) {
    DriverRegistrationForm(
      defaultName = driverProfile?.fullName ?: userProfile.fullName,
      defaultMobile = driverProfile?.mobileNumber ?: userProfile.mobileNumber,
      existingDriver = driverProfile,
      existingVehicle = vehicleRecord,
      isBusy = isBusy,
      onToggleLanguage = onToggleLanguage,
      onCancelEdit = if (driverProfile != null) ({ showEditForm = false }) else null,
      onSubmit = { name, mob, verified, reg, vType, vModel, area, lic, perm, dPhoto, fPhoto, rPhoto, sPhoto, lDoc, pDoc ->
        onSubmitRegistration(name, mob, verified, reg, vType, vModel, area, lic, perm, dPhoto, fPhoto, rPhoto, sPhoto, lDoc, pDoc)
        showEditForm = false
      },
      modifier = modifier,
    )
    return
  }

  val activeTrip = driverBookings.firstOrNull {
    it.status in listOf(BookingStatus.ASSIGNED, BookingStatus.ARRIVING, BookingStatus.STARTED)
  }
  val isApproved = driverProfile.status == DriverStatus.APPROVED
  val isOnline = driverPublic?.isOnline == true

  // Real continuous GPS updates while driver is approved, online, and granted permission
  LaunchedEffect(isApproved, isOnline, hasLocationPerm, activeTrip?.id) {
    if (isApproved && isOnline && hasLocationPerm) {
      LocationHelper.observeLocationUpdates(context, intervalMs = 10_000L).collect { fix ->
        onPushLiveGps(fix.lat, fix.lng)
        gpsNote = null
        activeTrip?.let { trip ->
          onPushActiveTripGps(trip.id, fix.lat, fix.lng)
        }
      }
    }
  }

  // Real today's trips & earnings derived strictly from actual completed Firestore bookings
  val (todayTripsCount, todayEarningsAmount) = remember(driverBookings) {
    val startOfDayCal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startSec = startOfDayCal.timeInMillis / 1000L
    val todaysCompleted = driverBookings.filter { b ->
      b.status == BookingStatus.COMPLETED &&
        ((b.updatedAt?.seconds ?: b.createdAt?.seconds ?: 0L) >= startSec)
    }
    todaysCompleted.size to todaysCompleted.sumOf { it.finalFare ?: it.estimatedFare }
  }

  val avgRating = remember(driverRatings, driverPublic) {
    when {
      driverRatings.isNotEmpty() -> driverRatings.map { it.stars }.average()
      driverPublic != null && driverPublic.ratingCount > 0 -> driverPublic.ratingAvg
      else -> 5.0
    }
  }
  val ratingCount = maxOf(driverRatings.size.toLong(), driverPublic?.ratingCount ?: 0L)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { TotoLogoHeader(onToggleLanguage = onToggleLanguage) }

    // Driver Identity, Verification Gate & Online/Offline Toggle Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Base64PhotoThumbnail(
              base64 = driverProfile.profilePhotoBase64,
              contentDescription = driverProfile.fullName,
              size = 62.dp,
              isCircle = true,
              emptyLabel = "Driver",
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(driverProfile.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isApproved) {
                  Spacer(Modifier.width(6.dp))
                  Icon(Icons.Filled.Verified, contentDescription = strings.statusApproved, tint = TotoEmeraldPrimary, modifier = Modifier.size(18.dp))
                }
              }
              val vehicleLine = buildString {
                append(driverProfile.registrationNumber)
                append(" • ")
                append(driverProfile.vehicleType)
                if (driverProfile.vehicleModel.isNotBlank()) append(" (${driverProfile.vehicleModel})")
              }
              Text(vehicleLine, style = MaterialTheme.typography.bodySmall, color = TotoDeepEmerald, fontWeight = FontWeight.SemiBold)
              Text(driverProfile.serviceArea, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
              onClick = { showEditForm = true },
              modifier = Modifier.testTag("edit_driver_profile_button"),
            ) {
              Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(4.dp))
              Text("Edit", style = MaterialTheme.typography.labelSmall)
            }
          }

          val (badgeColor, badgeText) = when (driverProfile.status) {
            DriverStatus.APPROVED -> TotoLightMintSurface to strings.statusApproved
            DriverStatus.REJECTED -> TotoRedContainer to strings.statusRejected
            DriverStatus.SUSPENDED -> TotoRedContainer to strings.statusSuspended
            else -> TotoAmberContainer to strings.statusPending
          }
          Surface(color = badgeColor, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(badgeText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              if (!isApproved) {
                Text(driverProfile.rejectionReason ?: strings.approvalRequiredForOnline, style = MaterialTheme.typography.bodySmall)
                if (driverProfile.status == DriverStatus.REJECTED) {
                  OutlinedButton(onClick = { showEditForm = true }) {
                    Text(strings.resubmitForVerification, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }

          // Online / Offline Toggle + Live GPS Status (strictly gated on Admin approval)
          if (isApproved) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isOnline) strings.onlineStatus else strings.offlineStatus,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isOnline) TotoEmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                )
                if (isOnline && driverPublic?.hasGpsFix == true) {
                  Text(
                    text = "${strings.liveGpsActiveLabel} (${String.format("%.4f, %.4f", driverPublic.currentLat, driverPublic.currentLng)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TotoEmeraldPrimary,
                  )
                } else if (gpsNote != null) {
                  Text(gpsNote!!, style = MaterialTheme.typography.labelSmall, color = TotoAmber)
                }
              }
              Switch(
                checked = isOnline,
                onCheckedChange = { targetOnline ->
                  if (!targetOnline) {
                    onToggleOnline(false, null, null, false)
                  } else if (LocationHelper.hasLocationPermission(context)) {
                    scope.launch {
                      val fix = LocationHelper.getCurrentLocationOrNull(context)
                      if (fix != null) {
                        onToggleOnline(true, fix.lat, fix.lng, true)
                        gpsNote = null
                      } else {
                        onToggleOnline(true, null, null, false)
                        gpsNote = strings.gpsUnavailable
                      }
                    }
                  } else {
                    locationLauncher.launch(
                      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                  }
                },
                modifier = Modifier.testTag("driver_online_switch"),
              )
            }
            if (!hasLocationPerm || gpsNote != null) {
              OutlinedButton(
                onClick = {
                  if (!LocationHelper.hasLocationPermission(context)) {
                    locationLauncher.launch(
                      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                  } else {
                    scope.launch {
                      val fix = LocationHelper.getCurrentLocationOrNull(context)
                      if (fix != null) {
                        onPushLiveGps(fix.lat, fix.lng)
                        gpsNote = null
                      } else {
                        gpsNote = strings.gpsUnavailable
                      }
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (hasLocationPerm) strings.refreshGps else strings.grantPermission, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }
    }

    // KPI Row: Today's Earnings, Today's/Total Trips, Rating
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = TotoLightMintSurface, modifier = Modifier.weight(1f)) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(strings.todayEarningsLabel, style = MaterialTheme.typography.labelSmall)
            Text("₹${todayEarningsAmount.toInt()}", style = MaterialTheme.typography.titleLarge, color = TotoDeepEmerald, fontWeight = FontWeight.ExtraBold)
            Text("${strings.lifetimeEarningsLabel}: ₹${driverProfile.totalEarnings.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = TotoLightMintSurface, modifier = Modifier.weight(1f)) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(strings.todayTripsLabel, style = MaterialTheme.typography.labelSmall)
            Text("$todayTripsCount", style = MaterialTheme.typography.titleLarge, color = TotoDeepEmerald, fontWeight = FontWeight.ExtraBold)
            Text("${strings.totalTrips}: ${driverProfile.totalTrips}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = TotoAmberContainer, modifier = Modifier.weight(0.85f)) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Rating", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Star, contentDescription = null, tint = TotoAmber, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(String.format("%.1f", avgRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            Text("($ratingCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    // Dashboard Section Switcher
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val tabs = listOf(strings.driverTabRideDesk, strings.driverTabEarnings, strings.driverTabProfileDocs)
        tabs.forEachIndexed { idx, label ->
          FilterChip(
            selected = selectedDeskTab == idx,
            onClick = { selectedDeskTab = idx },
            label = { Text(label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = TotoDeepEmerald,
              selectedLabelColor = TotoPureWhite,
            ),
          )
        }
      }
    }

    when (selectedDeskTab) {
      0 -> {
        if (activeTrip != null) {
          item {
            DriverActiveTripCard(
              activeTrip = activeTrip,
              isBusy = isBusy,
              onCallPassenger = { phone ->
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                runCatching { context.startActivity(dialIntent) }
              },
              onNavigateMaps = { lat, lng ->
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
                try {
                  context.startActivity(mapIntent)
                } catch (_: Exception) {
                  runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) }
                }
              },
              onProgress = { nextStatus ->
                scope.launch {
                  val fix = LocationHelper.getCurrentLocationOrNull(context)
                  onProgressBooking(
                    activeTrip,
                    nextStatus,
                    if (nextStatus == BookingStatus.COMPLETED) activeTrip.estimatedFare else null,
                    fix?.lat,
                    fix?.lng,
                  )
                }
              },
              onReport = { reportTarget = activeTrip },
            )
          }
        }

        if (isApproved && isOnline && activeTrip == null) {
          item {
            Text(strings.incomingRequests, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          val visibleRequests = searchingBookings.filter { it.id !in dismissedBookingIds }
          if (visibleRequests.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, TotoCardBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = strings.noIncomingRequests,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          } else {
            items(visibleRequests, key = { it.id }) { req ->
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, TotoEmeraldPrimary),
                modifier = Modifier.testTag("incoming_request_card"),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                      Text(req.passengerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                      if (!req.passengerMobile.isNullOrBlank()) {
                        Text(req.passengerMobile, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                    Surface(color = TotoLightMintSurface, shape = RoundedCornerShape(10.dp)) {
                      Text(
                        text = "₹${req.estimatedFare.toInt()} • ${req.distanceKm} km",
                        style = MaterialTheme.typography.titleSmall,
                        color = TotoDeepEmerald,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                      )
                    }
                  }
                  Text("${strings.pickupLocation}: ${req.pickupName}", style = MaterialTheme.typography.bodyMedium)
                  Text("${strings.destinationLocation}: ${req.destinationName}", style = MaterialTheme.typography.bodyMedium)
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                      onClick = { onDeclineBooking(req.id) },
                      modifier = Modifier.weight(1f).testTag("decline_ride_button"),
                    ) { Text(strings.rejectRide) }
                    Button(
                      onClick = { onAcceptBooking(req, driverProfile) },
                      enabled = !isBusy,
                      modifier = Modifier.weight(1f).testTag("accept_ride_button"),
                      colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
                    ) { Text(strings.acceptRide) }
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        item {
          Text(strings.driverRatingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (driverRatings.isEmpty()) {
          item {
            Text(strings.noRatingsYet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          items(driverRatings, key = { it.id }) { r ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, TotoCardBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  repeat(r.stars.toInt().coerceIn(1, 5)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = TotoAmber, modifier = Modifier.size(16.dp))
                  }
                  Spacer(Modifier.width(6.dp))
                  Text("${r.stars}/5", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (!r.comment.isNullOrBlank()) {
                  Text(r.comment, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        item {
          Text(strings.recentBookings, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(strings.filterAll, strings.filterCompleted, strings.filterCancelled).forEachIndexed { idx, label ->
              FilterChip(selected = historyFilter == idx, onClick = { historyFilter = idx }, label = { Text(label) })
            }
          }
        }
        val filteredHistory = driverBookings.filter {
          when (historyFilter) {
            1 -> it.status == BookingStatus.COMPLETED
            2 -> it.status == BookingStatus.CANCELLED
            else -> true
          }
        }
        if (filteredHistory.isEmpty()) {
          item { Text(strings.noBookingsYet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
          items(filteredHistory, key = { it.id }) { booking -> BookingHistoryRowCard(booking = booking) }
        }
      }

      2 -> {
        item {
          DriverProfileAndDocsCard(
            driverProfile = driverProfile,
            vehicleRecord = vehicleRecord,
            isBengali = isBengali,
            onInspectPhoto = { title, data -> previewPhotoTitleAndData = title to data },
            onEdit = { showEditForm = true },
          )
        }
      }
    }
  }

  reportTarget?.let { target ->
    ReportSafetyIssueDialog(
      onDismiss = { reportTarget = null },
      onSubmit = { category, desc ->
        onReportPassenger(target.passengerId, target.id, category, desc)
        reportTarget = null
      },
    )
  }

  previewPhotoTitleAndData?.let { (title, data) ->
    PhotoPreviewDialog(
      title = title,
      base64 = data,
      onDismiss = { previewPhotoTitleAndData = null },
    )
  }
}

@Composable
private fun DriverActiveTripCard(
  activeTrip: Booking,
  isBusy: Boolean,
  onCallPassenger: (String) -> Unit,
  onNavigateMaps: (Double, Double) -> Unit,
  onProgress: (String) -> Unit,
  onReport: () -> Unit,
) {
  val strings = LocalAppStrings.current
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(2.dp, TotoDeepEmerald),
    modifier = Modifier.testTag("driver_active_trip_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = "${bookingStatusLabel(activeTrip.status, strings)} • ₹${activeTrip.estimatedFare.toInt()} (${activeTrip.distanceKm} km)",
        style = MaterialTheme.typography.titleMedium,
        color = TotoDeepEmerald,
        fontWeight = FontWeight.Bold,
      )
      BookingStatusTimeline(status = activeTrip.status, cancelReason = activeTrip.cancelReason)
      Text("${strings.passengerRole}: ${activeTrip.passengerName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text("${strings.pickupLocation}: ${activeTrip.pickupName}", style = MaterialTheme.typography.bodyMedium)
      Text("${strings.destinationLocation}: ${activeTrip.destinationName}", style = MaterialTheme.typography.bodyMedium)

      if (!activeTrip.passengerMobile.isNullOrBlank()) {
        OutlinedButton(
          onClick = { onCallPassenger(activeTrip.passengerMobile) },
          modifier = Modifier.fillMaxWidth().testTag("driver_call_passenger_button"),
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("${strings.callPassenger}: ${activeTrip.passengerMobile}", style = MaterialTheme.typography.labelMedium)
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = {
            val targetLat = if (activeTrip.status == BookingStatus.STARTED) activeTrip.destinationLat else activeTrip.pickupLat
            val targetLng = if (activeTrip.status == BookingStatus.STARTED) activeTrip.destinationLng else activeTrip.pickupLng
            onNavigateMaps(targetLat, targetLng)
          },
          modifier = Modifier.weight(1f).testTag("driver_maps_nav_button"),
        ) {
          Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(strings.openGoogleMaps, style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(onClick = onReport, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(strings.reportIssue, style = MaterialTheme.typography.labelSmall)
        }
      }

      val (nextStatus, nextLabel) = when (activeTrip.status) {
        BookingStatus.ASSIGNED -> BookingStatus.ARRIVING to strings.markArriving
        BookingStatus.ARRIVING -> BookingStatus.STARTED to strings.startTrip
        BookingStatus.STARTED -> BookingStatus.COMPLETED to strings.completeTrip
        else -> null to ""
      }
      if (nextStatus != null) {
        Button(
          onClick = { onProgress(nextStatus) },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("driver_progress_trip_button"),
          colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
        ) { Text(nextLabel, fontWeight = FontWeight.Bold) }
      }
    }
  }
}

@Composable
private fun DriverProfileAndDocsCard(
  driverProfile: DriverProfile,
  vehicleRecord: VehicleRecord?,
  isBengali: Boolean,
  onInspectPhoto: (String, String?) -> Unit,
  onEdit: () -> Unit,
) {
  val strings = LocalAppStrings.current
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(strings.driverTabProfileDocs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
      Text("${strings.mobileNumber}: ${driverProfile.mobileNumber} ${if (driverProfile.mobileVerified) "✓" else ""}", style = MaterialTheme.typography.bodySmall)
      if (!driverProfile.licenceNumber.isNullOrBlank()) {
        Text("${strings.licenceNumber}: ${driverProfile.licenceNumber}", style = MaterialTheme.typography.bodySmall)
      }
      if (!driverProfile.permitNumber.isNullOrBlank()) {
        Text("${strings.permitNumber}: ${driverProfile.permitNumber}", style = MaterialTheme.typography.bodySmall)
      }

      val photoVerifiedLabel = if (vehicleRecord?.photosVerified == true) {
        if (isBengali) "ছবি যাচাইকৃত ✓" else "Vehicle Photos Verified ✓"
      } else {
        strings.statusPending
      }
      val docsVerifiedLabel = if (vehicleRecord?.docsVerified == true) {
        if (isBengali) "নথি যাচাইকৃত ✓" else "Documents Verified ✓"
      } else {
        strings.statusPending
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = TotoLightMintSurface, shape = RoundedCornerShape(8.dp)) {
          Text(photoVerifiedLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Surface(color = TotoLightMintSurface, shape = RoundedCornerShape(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = TotoDeepEmerald, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(docsVerifiedLabel, style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val items = listOf(
          strings.driverPhoto to driverProfile.profilePhotoBase64,
          strings.totoFrontPhoto to vehicleRecord?.frontPhotoBase64,
          strings.totoRearPhoto to vehicleRecord?.rearPhotoBase64,
          strings.totoSidePhoto to vehicleRecord?.sidePhotoBase64,
          strings.licenceDocPhoto to vehicleRecord?.licenceDocBase64,
          strings.permitDocPhoto to vehicleRecord?.permitDocBase64,
        )
        items.forEach { (label, data) ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onInspectPhoto(label, data) },
          ) {
            Base64PhotoThumbnail(base64 = data, contentDescription = label, size = 72.dp, emptyLabel = "—")
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
          }
        }
      }

      OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(strings.editProfile)
      }
    }
  }
}
