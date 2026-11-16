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
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.ChopraLandmark
import com.example.core.ChopraLocations
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.core.LocationHelper
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverPublicProfile
import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import com.example.data.model.UserProfile
import com.example.ui.components.Base64PhotoThumbnail
import com.example.ui.components.BookingStatusTimeline
import com.example.ui.components.ChopraAreaInteractiveMap
import com.example.ui.components.DriverIdentityVerificationCard
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

@Composable
fun PassengerHomeScreen(
  userProfile: UserProfile,
  onlineDrivers: List<DriverPublicProfile>,
  bookings: List<Booking>,
  activeDriverPublic: DriverPublicProfile?,
  fareSettings: FareSettings,
  fixedRoutes: List<FixedRoute>,
  isBusy: Boolean,
  onToggleLanguage: () -> Unit,
  onBookToto: (
    pickupName: String,
    pickupLat: Double,
    pickupLng: Double,
    destName: String,
    destLat: Double,
    destLng: Double,
    onBooked: () -> Unit,
  ) -> Unit,
  onCancelBooking: (Booking, String) -> Unit,
  onSubmitRating: (bookingId: String, driverId: String, stars: Int, comment: String?) -> Unit,
  onReportDriver: (targetDriverId: String, bookingId: String, category: String, desc: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val strings = LocalAppStrings.current
  val lang = LocalAppLanguage.current
  val isBengali = lang == AppLanguage.BENGALI

  var pickupLandmark by remember { mutableStateOf(ChopraLocations.landmarks[0]) }
  var destLandmark by remember { mutableStateOf(ChopraLocations.landmarks[3]) }
  var customPickupText by remember { mutableStateOf("") }
  var customDestText by remember { mutableStateOf("") }
  var gpsLat by remember { mutableStateOf<Double?>(null) }
  var gpsLng by remember { mutableStateOf<Double?>(null) }
  var useGpsForPickup by remember { mutableStateOf(false) }
  var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
  var hasLocationPerm by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }

  var showConfirmBookingDialog by remember { mutableStateOf(false) }
  var cancelDialogTarget by remember { mutableStateOf<Booking?>(null) }
  var previewDriver by remember { mutableStateOf<DriverPublicProfile?>(null) }
  var showReportDialogFor by remember { mutableStateOf<Booking?>(null) }

  val dialPhone: (String) -> Unit = { phone ->
    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
    runCatching { context.startActivity(dialIntent) }
  }

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
          gpsLat = fix.lat
          gpsLng = fix.lng
          useGpsForPickup = true
          customPickupText = ""
          gpsStatusMessage = null
        } else {
          useGpsForPickup = false
          gpsStatusMessage = strings.gpsUnavailable
        }
      }
    } else {
      useGpsForPickup = false
      gpsStatusMessage = strings.gpsPermissionNeeded
    }
  }

  LaunchedEffect(hasLocationPerm) {
    if (hasLocationPerm && gpsLat == null) {
      val fix = LocationHelper.getCurrentLocationOrNull(context)
      if (fix != null) {
        gpsLat = fix.lat
        gpsLng = fix.lng
        useGpsForPickup = true
      }
    }
  }

  val usingLiveGps = useGpsForPickup && gpsLat != null && gpsLng != null && customPickupText.isBlank()
  val effectivePickupName = when {
    customPickupText.isNotBlank() -> customPickupText.trim()
    usingLiveGps -> if (isBengali) {
      "বর্তমান GPS অবস্থান (${String.format("%.4f, %.4f", gpsLat, gpsLng)})"
    } else {
      "Current GPS (${String.format("%.4f, %.4f", gpsLat, gpsLng)})"
    }
    else -> pickupLandmark.displayName(isBengali)
  }
  val effectivePickupLat = if (usingLiveGps) gpsLat!! else pickupLandmark.lat
  val effectivePickupLng = if (usingLiveGps) gpsLng!! else pickupLandmark.lng
  val effectiveDestName = customDestText.trim().ifBlank { destLandmark.displayName(isBengali) }

  val fareCalc = remember(
    effectivePickupName,
    effectivePickupLat,
    effectivePickupLng,
    effectiveDestName,
    destLandmark,
    fareSettings,
    fixedRoutes,
  ) {
    ChopraLocations.calculateFare(
      pickupName = effectivePickupName,
      pickupLat = effectivePickupLat,
      pickupLng = effectivePickupLng,
      destinationName = effectiveDestName,
      destinationLat = destLandmark.lat,
      destinationLng = destLandmark.lng,
      settings = fareSettings,
      fixedRoutes = fixedRoutes,
    )
  }

  val activeBooking = bookings.firstOrNull {
    it.status in listOf(
      BookingStatus.SEARCHING,
      BookingStatus.ASSIGNED,
      BookingStatus.ARRIVING,
      BookingStatus.STARTED,
    ) || (it.status == BookingStatus.COMPLETED && !it.hasRated)
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      TotoLogoHeader(onToggleLanguage = onToggleLanguage)
    }

    // Current GPS status & passenger identity greeting bar
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = TotoLightMintSurface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Filled.MyLocation,
              contentDescription = strings.currentLocation,
              tint = TotoDeepEmerald,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
              Text(
                text = "${userProfile.fullName} • ${strings.currentLocation}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = when {
                  gpsLat != null && gpsLng != null ->
                    String.format("GPS: %.4f, %.4f", gpsLat, gpsLng)
                  gpsStatusMessage != null -> gpsStatusMessage!!
                  !hasLocationPerm -> strings.gpsPermissionNeeded
                  else -> strings.gpsUnavailable
                },
                style = MaterialTheme.typography.labelMedium,
                color = TotoDeepEmerald,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
          TextButton(
            onClick = {
              if (!hasLocationPerm) {
                locationLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              } else {
                scope.launch {
                  val fix = LocationHelper.getCurrentLocationOrNull(context)
                  if (fix != null) {
                    gpsLat = fix.lat
                    gpsLng = fix.lng
                    useGpsForPickup = true
                    customPickupText = ""
                    gpsStatusMessage = null
                  } else {
                    useGpsForPickup = false
                    gpsStatusMessage = strings.gpsUnavailable
                  }
                }
              }
            },
            modifier = Modifier.testTag("use_gps_button"),
          ) {
            Text(
              text = if (hasLocationPerm) strings.useMyGps else strings.grantPermission,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Chopra interactive map with live online TOTOs & route
    item {
      val mapPickupLat = activeBooking?.pickupLat ?: effectivePickupLat
      val mapPickupLng = activeBooking?.pickupLng ?: effectivePickupLng
      val mapDestLat = activeBooking?.destinationLat ?: destLandmark.lat
      val mapDestLng = activeBooking?.destinationLng ?: destLandmark.lng
      val mapPickupLabel = activeBooking?.pickupName ?: effectivePickupName
      val mapDestLabel = activeBooking?.destinationName ?: effectiveDestName

      ChopraAreaInteractiveMap(
        pickupLat = mapPickupLat,
        pickupLng = mapPickupLng,
        destLat = mapDestLat,
        destLng = mapDestLng,
        onlineDrivers = onlineDrivers,
        pickupLabel = mapPickupLabel,
        destLabel = mapDestLabel,
        activeDriverLat = activeBooking?.driverLat,
        activeDriverLng = activeBooking?.driverLng,
        onLandmarkClicked = if (activeBooking == null) {
          { landmark ->
            destLandmark = landmark
            customDestText = ""
          }
        } else null,
        onOpenExternalMaps = {
          val routeUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&origin=$mapPickupLat,$mapPickupLng&destination=$mapDestLat,$mapDestLng"
          )
          val intent = Intent(Intent.ACTION_VIEW, routeUri)
          try {
            context.startActivity(intent)
          } catch (_: Exception) {
            runCatching {
              context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:$mapDestLat,$mapDestLng?q=$mapDestLat,$mapDestLng"))
              )
            }
          }
        },
      )
    }

    // Active Booking Card (if ride in progress or awaiting rating)
    if (activeBooking != null) {
      item {
        ActivePassengerBookingCard(
          booking = activeBooking,
          driverPublic = activeDriverPublic,
          isBusy = isBusy,
          onCallDriver = dialPhone,
          onCancel = { cancelDialogTarget = activeBooking },
          onRate = { stars, comment ->
            val driverId = activeBooking.driverId
            if (!driverId.isNullOrBlank()) {
              onSubmitRating(activeBooking.id, driverId, stars, comment)
            }
          },
          onReport = { showReportDialogFor = activeBooking },
        )
      }
    } else {
      // Ride Booking Composer Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, TotoCardBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = strings.pickupLocation,
                style = MaterialTheme.typography.titleSmall,
                color = TotoDeepEmerald,
                fontWeight = FontWeight.Bold,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  color = if (usingLiveGps) TotoEmeraldPrimary else TotoLightMintSurface,
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = if (usingLiveGps) strings.gpsPickupSelected else strings.manualPickupSelected,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (usingLiveGps) TotoPureWhite else TotoDeepEmerald,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
                IconButton(
                  onClick = {
                    val oldPickup = pickupLandmark
                    pickupLandmark = destLandmark
                    destLandmark = oldPickup
                    val oldCustom = customPickupText
                    customPickupText = customDestText
                    customDestText = oldCustom
                    useGpsForPickup = false
                  },
                  modifier = Modifier.size(32.dp).testTag("swap_locations_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = strings.swapLocations,
                    tint = TotoDeepEmerald,
                  )
                }
              }
            }

            LandmarkChipStrip(
              landmarks = ChopraLocations.landmarks,
              selected = if (usingLiveGps) null else pickupLandmark,
              isBengali = isBengali,
              onSelect = {
                pickupLandmark = it
                customPickupText = ""
                useGpsForPickup = false
              },
            )

            OutlinedTextField(
              value = customPickupText,
              onValueChange = {
                customPickupText = it
                if (it.isNotBlank()) useGpsForPickup = false
              },
              label = { Text("${strings.pickupLocation} (${strings.enterCustomAddress})") },
              placeholder = { Text(effectivePickupName) },
              leadingIcon = {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TotoDeepEmerald)
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pickup_input"),
            )

            Text(
              text = strings.destinationLocation,
              style = MaterialTheme.typography.titleSmall,
              color = TotoDeepEmerald,
              fontWeight = FontWeight.Bold,
            )

            LandmarkChipStrip(
              landmarks = ChopraLocations.landmarks,
              selected = destLandmark,
              isBengali = isBengali,
              onSelect = {
                destLandmark = it
                customDestText = ""
              },
            )

            OutlinedTextField(
              value = customDestText,
              onValueChange = { customDestText = it },
              label = { Text("${strings.destinationLocation} (${strings.enterCustomAddress})") },
              placeholder = { Text(destLandmark.displayName(isBengali)) },
              leadingIcon = {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = TotoRedError)
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("destination_input"),
            )

            // Fare & Distance summary
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = TotoLightMintSurface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(strings.estimatedDistance, style = MaterialTheme.typography.labelSmall)
                    Text(
                      text = "${fareCalc.distanceKm} km",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(strings.estimatedFare, style = MaterialTheme.typography.labelSmall)
                    Text(
                      text = "₹${fareCalc.estimatedFare.toInt()}",
                      style = MaterialTheme.typography.headlineSmall,
                      color = TotoDeepEmerald,
                      fontWeight = FontWeight.ExtraBold,
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Payments,
                      contentDescription = null,
                      tint = TotoDeepEmerald,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                      text = strings.cashPaymentOnly,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                  if (fareCalc.isFixedRoute) {
                    Surface(
                      color = TotoEmeraldPrimary,
                      shape = RoundedCornerShape(8.dp),
                    ) {
                      Text(
                        text = strings.fixedRouteBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = TotoPureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  } else {
                    Text(
                      text = strings.upiFutureNote,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            Button(
              onClick = { showConfirmBookingDialog = true },
              enabled = !isBusy && !userProfile.isSuspended && effectivePickupName != effectiveDestName,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("book_toto_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
            ) {
              if (isBusy) {
                CircularProgressIndicator(color = TotoPureWhite, modifier = Modifier.size(22.dp))
              } else {
                Icon(Icons.Filled.ElectricRickshaw, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                  text = strings.bookTotoNow,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    // Nearby available & admin-approved TOTOs
    item {
      Text(
        text = "${strings.nearbyTotos} (${onlineDrivers.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    if (onlineDrivers.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, TotoCardBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = strings.noDriversOnline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    } else {
      items(onlineDrivers, key = { it.driverId }) { driver ->
        val distLabel = if (driver.hasGpsFix) {
          val km = ChopraLocations.haversineKm(
            effectivePickupLat,
            effectivePickupLng,
            driver.currentLat,
            driver.currentLng,
          )
          "$km km away"
        } else {
          driver.serviceArea
        }
        Surface(
          onClick = { previewDriver = driver },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, TotoCardBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Base64PhotoThumbnail(
              base64 = driver.profilePhotoBase64,
              contentDescription = driver.fullName,
              size = 52.dp,
              isCircle = true,
              emptyLabel = "Driver",
            )
            Spacer(Modifier.width(10.dp))
            Base64PhotoThumbnail(
              base64 = driver.frontPhotoBase64,
              contentDescription = strings.totoFrontPhoto,
              size = 52.dp,
              isCircle = false,
              emptyLabel = "TOTO",
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = driver.fullName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Filled.Verified,
                  contentDescription = strings.verifiedDriverBadge,
                  tint = TotoEmeraldPrimary,
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text = "${driver.registrationNumber} • ${driver.vehicleType}",
                style = MaterialTheme.typography.bodySmall,
                color = TotoDeepEmerald,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = distLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Star, contentDescription = null, tint = TotoAmber, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(3.dp))
              Text(
                text = String.format("%.1f", driver.ratingAvg),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // Recent bookings preview
    item {
      Text(
        text = strings.recentBookings,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    if (bookings.isEmpty()) {
      item {
        Text(
          text = strings.noBookingsYet,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(bookings.take(4), key = { it.id }) { booking ->
        BookingHistoryRowCard(
          booking = booking,
          onCancel = if (booking.status in CANCELLABLE_STATUSES) {
            { cancelDialogTarget = booking }
          } else null,
          onCallDriver = booking.driverPhone?.takeIf { it.isNotBlank() }?.let { phone ->
            { dialPhone(phone) }
          },
          onReport = { showReportDialogFor = booking },
        )
      }
    }
  }

  // Confirm Booking Dialog
  if (showConfirmBookingDialog) {
    AlertDialog(
      onDismissRequest = { if (!isBusy) showConfirmBookingDialog = false },
      title = { Text(strings.confirmBookingTitle, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(strings.confirmBookingSubtitle, style = MaterialTheme.typography.bodySmall)
          Surface(
            color = TotoLightMintSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("${strings.pickupLocation}: $effectivePickupName", fontWeight = FontWeight.SemiBold)
              Text("${strings.destinationLocation}: $effectiveDestName", fontWeight = FontWeight.SemiBold)
              Text("${strings.estimatedDistance}: ${fareCalc.distanceKm} km")
              Text(
                text = "${strings.estimatedFare}: ₹${fareCalc.estimatedFare.toInt()} (${strings.cashPaymentOnly})",
                color = TotoDeepEmerald,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "${strings.nearbyTotos}: ${onlineDrivers.size}",
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onBookToto(
              effectivePickupName,
              effectivePickupLat,
              effectivePickupLng,
              effectiveDestName,
              destLandmark.lat,
              destLandmark.lng,
            ) {
              showConfirmBookingDialog = false
            }
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
          modifier = Modifier.testTag("confirm_booking_dialog_button"),
        ) {
          if (isBusy) {
            CircularProgressIndicator(color = TotoPureWhite, modifier = Modifier.size(18.dp))
          } else {
            Text(strings.confirmAndRequest)
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showConfirmBookingDialog = false },
          enabled = !isBusy,
        ) {
          Text(strings.changeDetails)
        }
      },
    )
  }

  // Cancel Ride Reason Dialog
  cancelDialogTarget?.let { target ->
    CancelBookingDialog(
      isBusy = isBusy,
      onDismiss = { cancelDialogTarget = null },
      onConfirmCancel = { reason ->
        onCancelBooking(target, reason)
        cancelDialogTarget = null
      },
    )
  }

  // Approved Nearby Driver & TOTO Photo Preview Dialog
  previewDriver?.let { d ->
    AlertDialog(
      onDismissRequest = { previewDriver = null },
      title = { Text(strings.driverPreviewTitle, fontWeight = FontWeight.Bold) },
      text = {
        DriverIdentityVerificationCard(driverPublic = d)
      },
      confirmButton = {
        TextButton(onClick = { previewDriver = null }) {
          Text(strings.closeLabel)
        }
      },
    )
  }

  showReportDialogFor?.let { targetBooking ->
    ReportSafetyIssueDialog(
      onDismiss = { showReportDialogFor = null },
      onSubmit = { category, desc ->
        onReportDriver(
          targetBooking.driverId ?: "unassigned",
          targetBooking.id,
          category,
          desc,
        )
        showReportDialogFor = null
      },
    )
  }
}

@Composable
private fun LandmarkChipStrip(
  landmarks: List<ChopraLandmark>,
  selected: ChopraLandmark?,
  isBengali: Boolean,
  onSelect: (ChopraLandmark) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    landmarks.forEach { landmark ->
      FilterChip(
        selected = selected != null && landmark.id == selected.id,
        onClick = { onSelect(landmark) },
        label = { Text(landmark.displayName(isBengali), style = MaterialTheme.typography.labelMedium) },
      )
    }
  }
}

@Composable
private fun ActivePassengerBookingCard(
  booking: Booking,
  driverPublic: DriverPublicProfile?,
  isBusy: Boolean,
  onCallDriver: (String) -> Unit,
  onCancel: () -> Unit,
  onRate: (stars: Int, comment: String?) -> Unit,
  onReport: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var selectedStars by remember { mutableIntStateOf(5) }
  var ratingComment by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, TotoDeepEmerald),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier.testTag("active_booking_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = TotoLightMintSurface,
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = bookingStatusLabel(booking.status, strings),
            style = MaterialTheme.typography.labelLarge,
            color = TotoDeepEmerald,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
        Text(
          text = "₹${(booking.finalFare ?: booking.estimatedFare).toInt()} • ${booking.distanceKm} km",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TotoDeepEmerald,
        )
      }

      BookingStatusTimeline(
        status = booking.status,
        cancelReason = booking.cancelReason,
      )

      Text(
        text = "${booking.pickupName} → ${booking.destinationName}",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
      )

      if (booking.status == BookingStatus.SEARCHING) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(10.dp))
          Text(
            text = strings.searchingDriver,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Assigned verified driver identity, phone number & real TOTO photographs
      if (driverPublic != null && booking.driverId != null) {
        DriverIdentityVerificationCard(
          driverPublic = driverPublic,
          driverPhone = booking.driverPhone,
          bookingVehicleType = booking.vehicleType,
          onCallDriver = onCallDriver,
        )
      } else if (booking.driverId != null && !booking.driverName.isNullOrBlank()) {
        // Fallback info while driverPublic stream resolves (never uses fake photos)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = TotoLightMintSurface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(booking.driverName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${booking.vehicleRegNumber ?: ""} • ${booking.vehicleType ?: "Electric TOTO"}")
            if (!booking.driverPhone.isNullOrBlank()) {
              OutlinedButton(onClick = { onCallDriver(booking.driverPhone) }) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("${strings.callDriver}: ${booking.driverPhone}")
              }
            }
          }
        }
      }

      if (booking.status == BookingStatus.COMPLETED && !booking.hasRated && !booking.driverId.isNullOrBlank()) {
        Surface(
          color = TotoAmberContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.rateDriver, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row {
              for (star in 1..5) {
                IconButton(onClick = { selectedStars = star }) {
                  Icon(
                    imageVector = if (star <= selectedStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$star stars",
                    tint = TotoAmber,
                  )
                }
              }
            }
            OutlinedTextField(
              value = ratingComment,
              onValueChange = { ratingComment = it },
              label = { Text("Feedback (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Button(
              onClick = { onRate(selectedStars, ratingComment.takeIf { it.isNotBlank() }) },
              enabled = !isBusy,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(strings.submitRating)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (booking.status in CANCELLABLE_STATUSES) {
          OutlinedButton(
            onClick = onCancel,
            enabled = !isBusy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TotoRedError),
            modifier = Modifier
              .weight(1f)
              .testTag("cancel_booking_button"),
          ) {
            Text(strings.cancelRide)
          }
        }
        if (!booking.driverId.isNullOrBlank()) {
          OutlinedButton(
            onClick = onReport,
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.reportIssue)
          }
        }
      }
    }
  }
}

@Composable
fun PassengerRidesScreen(
  bookings: List<Booking>,
  isBusy: Boolean = false,
  onCancelBooking: ((Booking, String) -> Unit)? = null,
  onReportDriver: (targetDriverId: String, bookingId: String, category: String, desc: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  var filterIndex by remember { mutableIntStateOf(0) }
  var reportTarget by remember { mutableStateOf<Booking?>(null) }
  var cancelTarget by remember { mutableStateOf<Booking?>(null) }

  val filterLabels = listOf(
    strings.filterAll,
    strings.filterActive,
    strings.filterCompleted,
    strings.filterCancelled,
  )

  val filteredBookings = remember(bookings, filterIndex) {
    when (filterIndex) {
      1 -> bookings.filter {
        it.status in listOf(
          BookingStatus.SEARCHING,
          BookingStatus.ASSIGNED,
          BookingStatus.ARRIVING,
          BookingStatus.STARTED,
        )
      }
      2 -> bookings.filter { it.status == BookingStatus.COMPLETED }
      3 -> bookings.filter { it.status == BookingStatus.CANCELLED }
      else -> bookings
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = strings.recentBookings,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        filterLabels.forEachIndexed { idx, label ->
          FilterChip(
            selected = filterIndex == idx,
            onClick = { filterIndex = idx },
            label = { Text(label) },
          )
        }
      }
    }

    if (filteredBookings.isEmpty()) {
      item {
        Text(
          text = strings.noBookingsYet,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(filteredBookings, key = { it.id }) { booking ->
        BookingHistoryRowCard(
          booking = booking,
          onCancel = if (onCancelBooking != null && booking.status in CANCELLABLE_STATUSES) {
            { cancelTarget = booking }
          } else null,
          onCallDriver = booking.driverPhone?.takeIf { it.isNotBlank() }?.let { phone ->
            {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
              }
            }
          },
          onReport = { reportTarget = booking },
        )
      }
    }
  }

  cancelTarget?.let { target ->
    CancelBookingDialog(
      isBusy = isBusy,
      onDismiss = { cancelTarget = null },
      onConfirmCancel = { reason ->
        onCancelBooking?.invoke(target, reason)
        cancelTarget = null
      },
    )
  }

  reportTarget?.let { target ->
    ReportSafetyIssueDialog(
      onDismiss = { reportTarget = null },
      onSubmit = { category, desc ->
        onReportDriver(target.driverId ?: "unassigned", target.id, category, desc)
        reportTarget = null
      },
    )
  }
}

@Composable
fun BookingHistoryRowCard(
  booking: Booking,
  onCancel: (() -> Unit)? = null,
  onCallDriver: (() -> Unit)? = null,
  onReport: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val statusBg = when (booking.status) {
    BookingStatus.COMPLETED -> TotoLightMintSurface
    BookingStatus.CANCELLED -> TotoRedContainer
    else -> TotoAmberContainer
  }
  val statusFg = when (booking.status) {
    BookingStatus.COMPLETED -> TotoDeepEmerald
    BookingStatus.CANCELLED -> TotoRedError
    else -> MaterialTheme.colorScheme.onTertiaryContainer
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, TotoCardBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${booking.pickupName} → ${booking.destinationName}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "₹${(booking.finalFare ?: booking.estimatedFare).toInt()}",
          style = MaterialTheme.typography.titleSmall,
          color = TotoDeepEmerald,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = "${bookingStatusLabel(booking.status, strings)} • ${booking.distanceKm} km",
            style = MaterialTheme.typography.labelSmall,
            color = statusFg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        if (onReport != null && !booking.driverId.isNullOrBlank()) {
          Text(
            text = strings.reportIssue,
            style = MaterialTheme.typography.labelSmall,
            color = TotoRedError,
            modifier = Modifier.clickable { onReport() },
          )
        }
      }

      if (!booking.driverName.isNullOrBlank()) {
        val phonePart = booking.driverPhone?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
        Text(
          text = "${booking.driverName} (${booking.vehicleRegNumber ?: ""})$phonePart",
          style = MaterialTheme.typography.labelSmall,
          color = TotoEmeraldPrimary,
        )
      }

      if (!booking.cancelReason.isNullOrBlank() && booking.status == BookingStatus.CANCELLED) {
        Text(
          text = booking.cancelReason,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (onCancel != null || onCallDriver != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          if (onCallDriver != null && booking.status in CANCELLABLE_STATUSES) {
            OutlinedButton(
              onClick = onCallDriver,
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.callDriver, style = MaterialTheme.typography.labelSmall)
            }
          }
          if (onCancel != null) {
            OutlinedButton(
              onClick = onCancel,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TotoRedError),
              modifier = Modifier.weight(1f),
            ) {
              Text(strings.cancelRide, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CancelBookingDialog(
  isBusy: Boolean,
  onDismiss: () -> Unit,
  onConfirmCancel: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val isBengali = LocalAppLanguage.current == AppLanguage.BENGALI
  val quickReasons = if (isBengali) {
    listOf("পরিকল্পনা বদলেছে", "চালক দেরি করছেন", "ভুলবশত বুকিং হয়েছে", "অন্য টোটো পেয়েছি")
  } else {
    listOf("Changed plans", "Driver taking too long", "Booked by mistake", "Found another TOTO")
  }
  var reason by remember { mutableStateOf(quickReasons.first()) }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    title = { Text(strings.cancelReasonTitle, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickReasons.forEach { itemReason ->
          FilterChip(
            selected = reason == itemReason,
            onClick = { reason = itemReason },
            label = { Text(itemReason) },
          )
        }
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text(strings.cancelRide) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmCancel(reason.ifBlank { "Passenger cancelled" }) },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = TotoRedError),
        modifier = Modifier.testTag("confirm_cancel_dialog_button"),
      ) {
        Text(strings.confirmCancelRide)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text(strings.keepRide)
      }
    },
  )
}

@Composable
fun ReportSafetyIssueDialog(
  onDismiss: () -> Unit,
  onSubmit: (category: String, description: String) -> Unit,
) {
  val strings = LocalAppStrings.current
  var category by remember { mutableStateOf("Driver / Ride Behaviour") }
  var description by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.reportIssue) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category") },
          singleLine = true,
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Details") },
          minLines = 3,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (description.trim().length >= 3) onSubmit(category, description) },
      ) {
        Text(strings.saveAndContinue)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeLabel) }
    },
  )
}

fun bookingStatusLabel(status: String, strings: com.example.core.AppStrings): String =
  when (status) {
    BookingStatus.SEARCHING -> strings.searchingDriver
    BookingStatus.ASSIGNED -> strings.driverAssigned
    BookingStatus.ARRIVING -> strings.driverArriving
    BookingStatus.STARTED -> strings.tripStarted
    BookingStatus.COMPLETED -> strings.tripCompleted
    BookingStatus.CANCELLED -> strings.cancelled
    else -> status
  }

private val CANCELLABLE_STATUSES = setOf(
  BookingStatus.SEARCHING,
  BookingStatus.ASSIGNED,
  BookingStatus.ARRIVING,
)
