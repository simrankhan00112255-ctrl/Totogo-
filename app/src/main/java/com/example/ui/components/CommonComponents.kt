package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.ChopraLandmark
import com.example.core.ChopraLocations
import com.example.core.ImageCompression
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.data.model.BookingStatus
import com.example.data.model.DriverPublicProfile
import com.example.ui.theme.TotoAmber
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoMintAccent
import com.example.ui.theme.TotoPureWhite
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Composable
fun TotoLogoHeader(
  modifier: Modifier = Modifier,
  onToggleLanguage: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val lang = LocalAppLanguage.current
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(id = R.drawable.img_toto_launcher),
        contentDescription = strings.appName,
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
      )
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
          text = strings.appName,
          style = MaterialTheme.typography.titleLarge,
          color = TotoDeepEmerald,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = strings.tagline,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (onToggleLanguage != null) {
      Surface(
        onClick = onToggleLanguage,
        shape = RoundedCornerShape(20.dp),
        color = TotoLightMintSurface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.testTag("language_toggle_button"),
      ) {
        Text(
          text = if (lang == AppLanguage.BENGALI) "EN / বাংলা" else "বাংলা / EN",
          style = MaterialTheme.typography.labelMedium,
          color = TotoDeepEmerald,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
fun Base64PhotoThumbnail(
  base64: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  isCircle: Boolean = false,
  emptyLabel: String = "No Photo",
) {
  val bitmap = remember(base64) { ImageCompression.decodeBase64ToImageBitmap(base64) }
  val shape = if (isCircle) CircleShape else RoundedCornerShape(14.dp)
  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(TotoLightMintSurface)
      .border(1.dp, TotoCardBorder, shape),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.ElectricRickshaw,
          contentDescription = null,
          tint = TotoDeepEmerald.copy(alpha = 0.55f),
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = emptyLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun PhotoUploadField(
  label: String,
  base64: String?,
  onPhotoEncoded: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    uri?.let {
      ImageCompression.compressUriToBase64(context, it)?.let(onPhotoEncoded)
    }
  }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    if (success) {
      pendingCameraUri?.let { uri ->
        ImageCompression.compressUriToBase64(context, uri)?.let(onPhotoEncoded)
      }
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Base64PhotoThumbnail(
        base64 = base64,
        contentDescription = label,
        size = 56.dp,
        emptyLabel = "—",
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier.height(36.dp),
          ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.pickFromGallery, style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              val uri = ImageCompression.createTempCameraUri(context)
              pendingCameraUri = uri
              cameraLauncher.launch(uri)
            },
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier.height(36.dp),
          ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.captureWithCamera, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

@Composable
fun MobileOtpVerificationSection(
  mobileNumber: String,
  onMobileChange: (String) -> Unit,
  isVerified: Boolean,
  onVerified: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var generatedOtp by remember { mutableStateOf<String?>(null) }
  var enteredOtp by remember { mutableStateOf("") }
  var otpError by remember { mutableStateOf<String?>(null) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = mobileNumber,
        onValueChange = {
          onMobileChange(it)
          otpError = null
        },
        label = { Text(strings.mobileNumber) },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("mobile_number_input"),
        trailingIcon = {
          if (isVerified) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = strings.otpVerifiedBadge,
              tint = TotoEmeraldPrimary,
            )
          }
        },
      )
      if (!isVerified) {
        OutlinedButton(
          onClick = {
            val digits = mobileNumber.filter { it.isDigit() }
            if (digits.length >= 10) {
              generatedOtp = Random.nextInt(100000, 999999).toString()
              otpError = null
            } else {
              otpError = "Enter 10-digit mobile number"
            }
          },
          modifier = Modifier.testTag("send_otp_button"),
        ) {
          Text(strings.sendOtp, style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    if (generatedOtp != null && !isVerified) {
      Surface(
        color = TotoAmberContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "${strings.otpTestingNotice}: ${generatedOtp}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = enteredOtp,
          onValueChange = { enteredOtp = it },
          label = { Text(strings.otpCodeHint) },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("otp_code_input"),
        )
        TextButton(
          onClick = {
            if (enteredOtp.trim() == generatedOtp) {
              onVerified()
              otpError = null
            } else {
              otpError = "Invalid OTP"
            }
          },
          modifier = Modifier.testTag("verify_otp_button"),
        ) {
          Text(strings.verifyOtp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (isVerified) {
      Text(
        text = "✓ ${strings.otpVerifiedBadge}",
        style = MaterialTheme.typography.labelMedium,
        color = TotoEmeraldPrimary,
        fontWeight = FontWeight.SemiBold,
      )
    }
    if (otpError != null) {
      Text(
        text = otpError!!,
        style = MaterialTheme.typography.labelSmall,
        color = TotoRedError,
      )
    }
  }
}

@Composable
fun DriverIdentityVerificationCard(
  driverPublic: DriverPublicProfile,
  driverPhone: String? = null,
  bookingVehicleType: String? = null,
  onCallDriver: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val defaultVehicleLabel = buildString {
    append(driverPublic.vehicleType.ifBlank { "Electric TOTO" })
    if (driverPublic.vehicleModel.isNotBlank()) {
      append(" (${driverPublic.vehicleModel})")
    }
  }
  val vehicleSummary = (bookingVehicleType?.takeIf { it.isNotBlank() } ?: defaultVehicleLabel)
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("driver_identity_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, TotoEmeraldPrimary.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Base64PhotoThumbnail(
          base64 = driverPublic.profilePhotoBase64,
          contentDescription = driverPublic.fullName,
          size = 70.dp,
          isCircle = true,
          emptyLabel = "Driver",
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = driverPublic.fullName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            if (driverPublic.isVerified) {
              Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = strings.verifiedDriverBadge,
                tint = TotoEmeraldPrimary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Surface(
            color = TotoLightMintSurface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, TotoEmeraldPrimary.copy(alpha = 0.35f)),
          ) {
            Text(
              text = driverPublic.registrationNumber,
              style = MaterialTheme.typography.labelLarge,
              color = TotoDeepEmerald,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          Text(
            text = "${strings.vehicleDetailsLabel}: $vehicleSummary • ${driverPublic.serviceArea}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = null,
              tint = TotoAmber,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = String.format("%.1f (%d)", driverPublic.ratingAvg, driverPublic.ratingCount),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (!driverPhone.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = TotoLightMintSurface,
          border = BorderStroke(1.dp, TotoCardBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.driverPhoneLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = driverPhone,
                style = MaterialTheme.typography.titleSmall,
                color = TotoDeepEmerald,
                fontWeight = FontWeight.Bold,
              )
            }
            if (onCallDriver != null) {
              Button(
                onClick = { onCallDriver(driverPhone) },
                colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("call_driver_button"),
              ) {
                Icon(Icons.Filled.Call, contentDescription = strings.callDriver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.callDriver, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      Surface(
        color = TotoAmberContainer.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = strings.assignedTotoVerifyNote,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }

      // Prominent Actual TOTO Front Photo + optional Side & Rear Photos belonging strictly to the approved assigned driver
      val frontBitmap = remember(driverPublic.frontPhotoBase64) {
        ImageCompression.decodeBase64ToImageBitmap(driverPublic.frontPhotoBase64)
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = strings.totoFrontPhoto,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = TotoDeepEmerald,
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TotoLightMintSurface)
            .border(1.dp, TotoCardBorder, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          if (frontBitmap != null) {
            Image(
              bitmap = frontBitmap,
              contentDescription = strings.totoFrontPhoto,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Filled.ElectricRickshaw,
                contentDescription = null,
                tint = TotoDeepEmerald.copy(alpha = 0.55f),
                modifier = Modifier.size(28.dp),
              )
              Text(
                text = "Front TOTO photo not uploaded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Base64PhotoThumbnail(
              base64 = driverPublic.sidePhotoBase64,
              contentDescription = strings.totoSidePhoto,
              size = 76.dp,
              emptyLabel = "Side",
            )
            Text(strings.totoSidePhoto, style = MaterialTheme.typography.labelSmall, maxLines = 1)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Base64PhotoThumbnail(
              base64 = driverPublic.rearPhotoBase64,
              contentDescription = strings.totoRearPhoto,
              size = 76.dp,
              emptyLabel = "Rear",
            )
            Text(strings.totoRearPhoto, style = MaterialTheme.typography.labelSmall, maxLines = 1)
          }
        }
      }
    }
  }
}

@Composable
fun BookingStatusTimeline(
  status: String,
  cancelReason: String? = null,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  if (status == BookingStatus.CANCELLED) {
    Surface(
      color = TotoRedContainer,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = strings.cancelled,
          style = MaterialTheme.typography.labelLarge,
          color = TotoRedError,
          fontWeight = FontWeight.Bold,
        )
        if (!cancelReason.isNullOrBlank()) {
          Text(
            text = cancelReason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
    return
  }

  val steps = listOf(
    BookingStatus.SEARCHING to strings.searchingDriver,
    BookingStatus.ASSIGNED to strings.driverAssigned,
    BookingStatus.ARRIVING to strings.driverArriving,
    BookingStatus.STARTED to strings.tripStarted,
    BookingStatus.COMPLETED to strings.tripCompleted,
  )
  val currentIdx = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      steps.forEachIndexed { idx, _ ->
        val reached = idx <= currentIdx
        Box(
          modifier = Modifier
            .weight(1f)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (reached) TotoEmeraldPrimary else TotoCardBorder),
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      steps.forEachIndexed { idx, (_, label) ->
        val active = idx == currentIdx
        val reached = idx <= currentIdx
        Text(
          text = label.substringBefore("..."),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
          color = if (reached) TotoDeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
fun ChopraAreaInteractiveMap(
  pickupLat: Double,
  pickupLng: Double,
  destLat: Double?,
  destLng: Double?,
  onlineDrivers: List<DriverPublicProfile>,
  pickupLabel: String = "Pickup",
  destLabel: String? = null,
  activeDriverLat: Double? = null,
  activeDriverLng: Double? = null,
  onLandmarkClicked: ((ChopraLandmark) -> Unit)? = null,
  onOpenExternalMaps: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val lang = LocalAppLanguage.current
  val strings = LocalAppStrings.current
  val isBengali = lang == AppLanguage.BENGALI

  // Fit viewport cleanly around Chopra landmarks, pickup, and destination coordinates
  val allLats = buildList {
    add(26.365)
    add(26.452)
    add(pickupLat)
    destLat?.let { add(it) }
    activeDriverLat?.let { add(it) }
  }
  val allLngs = buildList {
    add(88.205)
    add(88.295)
    add(pickupLng)
    destLng?.let { add(it) }
    activeDriverLng?.let { add(it) }
  }
  val minLat = (allLats.minOrNull() ?: 26.355) - 0.012
  val maxLat = (allLats.maxOrNull() ?: 26.460) + 0.012
  val minLng = (allLngs.minOrNull() ?: 88.185) - 0.012
  val maxLng = (allLngs.maxOrNull() ?: 88.305) + 0.012
  val latSpan = max(maxLat - minLat, 0.02)
  val lngSpan = max(maxLng - minLng, 0.02)

  fun toNormalizedOffset(lat: Double, lng: Double): Pair<Float, Float> {
    val x = ((lng - minLng) / lngSpan).toFloat().coerceIn(0.06f, 0.94f)
    val y = (1f - ((lat - minLat) / latSpan).toFloat()).coerceIn(0.14f, 0.84f)
    return x to y
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(226.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFFE6F4EC), Color(0xFFD6EDE0)),
          )
        )
        .border(1.dp, TotoCardBorder, RoundedCornerShape(20.dp)),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle grid lines
        val stepX = size.width / 6f
        val stepY = size.height / 4f
        for (i in 1..5) {
          drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(stepX * i, 0f),
            end = Offset(stepX * i, size.height),
            strokeWidth = 1.5f,
          )
        }
        for (j in 1..3) {
          drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(0f, stepY * j),
            end = Offset(size.width, stepY * j),
            strokeWidth = 1.5f,
          )
        }

        // Main NH-27 highway & Chopra arterial roads
        val bazar = toNormalizedOffset(26.4042, 88.2485)
        val busStand = toNormalizedOffset(26.4018, 88.2458)
        val sonapur = toNormalizedOffset(26.4254, 88.2620)
        val daspara = toNormalizedOffset(26.4390, 88.2940)
        val islampur = toNormalizedOffset(26.2660, 88.1890)
        val lakhipur = toNormalizedOffset(26.3895, 88.2290)

        fun pt(p: Pair<Float, Float>) = Offset(p.first * size.width, p.second * size.height)

        drawLine(Color.White, pt(islampur), pt(busStand), strokeWidth = 10f)
        drawLine(Color.White, pt(busStand), pt(bazar), strokeWidth = 10f)
        drawLine(Color.White, pt(bazar), pt(sonapur), strokeWidth = 9f)
        drawLine(Color.White, pt(sonapur), pt(daspara), strokeWidth = 7f)
        drawLine(Color.White, pt(bazar), pt(lakhipur), strokeWidth = 7f)

        // Route polyline between pickup & destination
        val pStart = toNormalizedOffset(pickupLat, pickupLng)
        if (destLat != null && destLng != null) {
          val pEnd = toNormalizedOffset(destLat, destLng)
          drawLine(
            color = TotoDeepEmerald,
            start = pt(pStart),
            end = pt(pEnd),
            strokeWidth = 7f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f),
          )
          // Destination pin (Red)
          drawCircle(
            color = TotoRedError.copy(alpha = 0.2f),
            radius = 24f,
            center = pt(pEnd),
          )
          drawCircle(
            color = TotoRedError,
            radius = 12f,
            center = pt(pEnd),
          )
          drawCircle(
            color = Color.White,
            radius = 5f,
            center = pt(pEnd),
          )
        }

        // Pickup pin (Deep Green)
        drawCircle(
          color = TotoDeepEmerald.copy(alpha = 0.22f),
          radius = 24f,
          center = pt(pStart),
        )
        drawCircle(
          color = TotoDeepEmerald,
          radius = 12f,
          center = pt(pStart),
        )
        drawCircle(
          color = Color.White,
          radius = 5f,
          center = pt(pStart),
        )

        // Online verified TOTOs (only real Firestore records!)
        onlineDrivers.forEach { driver ->
          val dPt = pt(toNormalizedOffset(driver.currentLat, driver.currentLng))
          drawCircle(
            color = TotoMintAccent,
            radius = 11f,
            center = dPt,
          )
          drawCircle(
            color = Color.White,
            radius = 11f,
            center = dPt,
            style = Stroke(width = 3f),
          )
        }

        // Live active driver pin (only when real GPS coordinates are pushed)
        if (activeDriverLat != null && activeDriverLng != null) {
          val livePt = pt(toNormalizedOffset(activeDriverLat, activeDriverLng))
          drawCircle(
            color = TotoAmber,
            radius = 14f,
            center = livePt,
          )
          drawCircle(
            color = Color.White,
            radius = 5f,
            center = livePt,
          )
        }
      }

      // Overlays: header pill, Google Maps launcher, P & D route legend, and quick hub chips
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = TotoPureWhite.copy(alpha = 0.94f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text = if (isBengali) "চোপড়া রুট ম্যাপ • ${onlineDrivers.size} টোটো অনলাইন"
              else "Chopra Area Map • ${onlineDrivers.size} TOTOs Online",
              style = MaterialTheme.typography.labelSmall,
              color = TotoDeepEmerald,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }

          if (onOpenExternalMaps != null) {
            Surface(
              onClick = onOpenExternalMaps,
              color = TotoDeepEmerald,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("open_external_maps_button"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                  contentDescription = strings.openRouteInMaps,
                  tint = TotoPureWhite,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "Maps",
                  style = MaterialTheme.typography.labelSmall,
                  color = TotoPureWhite,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          // Pickup & Destination pin summary pill on map
          Surface(
            color = TotoPureWhite.copy(alpha = 0.92f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, TotoCardBorder),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(TotoDeepEmerald),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                  text = pickupLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = TotoDeepEmerald,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (!destLabel.isNullOrBlank()) {
                Text("→", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .size(9.dp)
                      .clip(CircleShape)
                      .background(TotoRedError),
                  )
                  Spacer(Modifier.width(5.dp))
                  Text(
                    text = destLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TotoRedError,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          if (onLandmarkClicked != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              ChopraLocations.landmarks.take(3).forEach { hub ->
                Surface(
                  color = TotoPureWhite.copy(alpha = 0.94f),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, TotoCardBorder),
                  modifier = Modifier.clickable { onLandmarkClicked(hub) },
                ) {
                  Text(
                    text = hub.displayName(isBengali),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    Text(
      text = strings.mapsSdkMissingNotice,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun PhotoPreviewDialog(
  title: String,
  base64: String?,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val bitmap = remember(base64) { ImageCompression.decodeBase64ToImageBitmap(base64) }
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(TotoLightMintSurface)
          .border(1.dp, TotoCardBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
          )
        } else {
          Text(
            text = "No image uploaded",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
