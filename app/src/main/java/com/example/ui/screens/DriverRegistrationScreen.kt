package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.ChopraLocations
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.data.model.DriverProfile
import com.example.data.model.DriverStatus
import com.example.data.model.VehicleRecord
import com.example.ui.components.MobileOtpVerificationSection
import com.example.ui.components.PhotoUploadField
import com.example.ui.components.TotoLogoHeader
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoPureWhite
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError

private val TOTO_VEHICLE_TYPES = listOf(
  "Electric TOTO",
  "Passenger E-Rickshaw",
  "Deluxe E-Rickshaw",
)

private val POPULAR_TOTO_MODELS = listOf(
  "Mahindra Treo",
  "Mayuri Pro",
  "YC Electric Yatri",
  "Mini Metro",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverRegistrationForm(
  defaultName: String,
  defaultMobile: String,
  existingDriver: DriverProfile?,
  existingVehicle: VehicleRecord?,
  isBusy: Boolean,
  onToggleLanguage: (() -> Unit)? = null,
  onCancelEdit: (() -> Unit)?,
  onSubmit: (
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
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val isBengali = LocalAppLanguage.current == AppLanguage.BENGALI

  if (onCancelEdit != null) {
    BackHandler { onCancelEdit() }
  }

  var fullName by remember { mutableStateOf(defaultName) }
  var mobile by remember { mutableStateOf(defaultMobile) }
  var mobileVerified by remember {
    mutableStateOf(
      existingDriver?.mobileVerified ?: (defaultMobile.filter { it.isDigit() }.length >= 10),
    )
  }
  var regNo by remember { mutableStateOf(existingDriver?.registrationNumber ?: "WB-59-") }
  var vehicleType by remember { mutableStateOf(existingDriver?.vehicleType?.ifBlank { "Electric TOTO" } ?: "Electric TOTO") }
  var vehicleModel by remember { mutableStateOf(existingDriver?.vehicleModel ?: existingVehicle?.vehicleModel ?: "") }

  // Service area selection initialized from existing area or default Chopra Bazar + Daspara option
  val initialAreas = remember(existingDriver?.serviceArea) {
    existingDriver?.serviceArea
      ?.split(",")
      ?.map { it.trim() }
      ?.filter { it.isNotBlank() }
      ?.toSet()
      ?: setOf("Chopra Bazar")
  }
  val landmarkNamesEn = remember { ChopraLocations.landmarks.map { it.nameEn }.toSet() }
  var selectedAreas by remember {
    mutableStateOf(initialAreas.filter { it in landmarkNamesEn }.toSet().ifEmpty { setOf("Chopra Bazar") })
  }
  var customAreaText by remember {
    mutableStateOf(initialAreas.filter { it !in landmarkNamesEn }.joinToString(", "))
  }

  var licenceNo by remember { mutableStateOf(existingDriver?.licenceNumber ?: "") }
  var permitNo by remember { mutableStateOf(existingDriver?.permitNumber ?: "") }

  var driverPhoto by remember { mutableStateOf(existingDriver?.profilePhotoBase64) }
  var frontPhoto by remember { mutableStateOf(existingVehicle?.frontPhotoBase64) }
  var rearPhoto by remember { mutableStateOf(existingVehicle?.rearPhotoBase64) }
  var sidePhoto by remember { mutableStateOf(existingVehicle?.sidePhotoBase64) }
  var licenceDoc by remember { mutableStateOf(existingVehicle?.licenceDocBase64) }
  var permitDoc by remember { mutableStateOf(existingVehicle?.permitDocBase64) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val combinedServiceArea = buildList {
    selectedAreas.forEach { add(it) }
    customAreaText.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
  }.distinct().joinToString(", ")

  val checksCompleted = listOf(
    fullName.trim().length >= 2 && mobileVerified,
    driverPhoto != null,
    regNo.trim().length >= 5 && vehicleType.isNotBlank() && vehicleModel.isNotBlank(),
    combinedServiceArea.isNotBlank(),
    frontPhoto != null && rearPhoto != null && sidePhoto != null,
    permitDoc != null || licenceDoc != null,
  ).count { it }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (onToggleLanguage != null) {
      item {
        TotoLogoHeader(onToggleLanguage = onToggleLanguage)
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.driverRegistrationTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TotoDeepEmerald,
          modifier = Modifier.weight(1f),
        )
        if (onCancelEdit != null) {
          OutlinedButton(
            onClick = onCancelEdit,
            modifier = Modifier.testTag("driver_reg_back_button"),
          ) {
            Text(strings.closeLabel)
          }
        }
      }
    }

    if (existingDriver?.status == DriverStatus.REJECTED) {
      item {
        Surface(
          color = TotoRedContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = strings.statusRejected,
              style = MaterialTheme.typography.labelLarge,
              color = TotoRedError,
              fontWeight = FontWeight.Bold,
            )
            if (!existingDriver.rejectionReason.isNullOrBlank()) {
              Text(
                text = existingDriver.rejectionReason,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }

    // Verification checklist progress card
    item {
      Surface(
        color = TotoLightMintSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TotoDeepEmerald, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = strings.photoUploadChecklistLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TotoDeepEmerald,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = "$checksCompleted / 6",
              style = MaterialTheme.typography.labelLarge,
              color = TotoDeepEmerald,
              fontWeight = FontWeight.ExtraBold,
            )
          }
          LinearProgressIndicator(
            progress = { checksCompleted / 6f },
            color = TotoEmeraldPrimary,
            trackColor = TotoPureWhite,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp),
          )
          Text(
            text = strings.pendingBannerDesc,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Section 1: Personal identity & Mobile OTP
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(strings.fullName) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("driver_reg_name_input"),
          )

          MobileOtpVerificationSection(
            mobileNumber = mobile,
            onMobileChange = {
              mobile = it
              mobileVerified = false
            },
            isVerified = mobileVerified,
            onVerified = { mobileVerified = true },
          )

          PhotoUploadField(
            label = "${strings.driverPhoto} *",
            base64 = driverPhoto,
            onPhotoEncoded = { driverPhoto = it },
            modifier = Modifier.testTag("upload_driver_portrait"),
          )
        }
      }
    }

    // Section 2: Vehicle Registration, Type, Model & Service Areas
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it },
            label = { Text("${strings.totoRegNumber} *") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("driver_reg_plate_input"),
          )

          Text(strings.vehicleType, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            TOTO_VEHICLE_TYPES.forEach { t ->
              FilterChip(
                selected = vehicleType == t,
                onClick = { vehicleType = t },
                label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TotoLightMintSurface,
                  selectedLabelColor = TotoDeepEmerald,
                ),
              )
            }
          }

          OutlinedTextField(
            value = vehicleModel,
            onValueChange = { vehicleModel = it },
            label = { Text("${strings.vehicleModelLabel} *") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("driver_reg_model_input"),
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            POPULAR_TOTO_MODELS.forEach { mdl ->
              FilterChip(
                selected = vehicleModel.equals(mdl, ignoreCase = true),
                onClick = { vehicleModel = mdl },
                label = { Text(mdl, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          Spacer(Modifier.height(2.dp))
          Text(
            text = "${strings.serviceArea} (${strings.serviceAreaHint})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = TotoDeepEmerald,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ChopraLocations.landmarks.forEach { hub ->
              val selected = hub.nameEn in selectedAreas
              FilterChip(
                selected = selected,
                onClick = {
                  selectedAreas = if (selected) selectedAreas - hub.nameEn else selectedAreas + hub.nameEn
                },
                label = { Text(hub.displayName(isBengali), style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TotoDeepEmerald,
                  selectedLabelColor = TotoPureWhite,
                ),
              )
            }
          }
          OutlinedTextField(
            value = customAreaText,
            onValueChange = { customAreaText = it },
            label = { Text(strings.enterCustomAddress) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    // Section 3: Actual TOTO Front, Rear & Side Photos
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = strings.totoPhotosSectionTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TotoDeepEmerald,
          )
          Surface(color = TotoAmberContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              text = strings.totoPhotosSafetyNote,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(10.dp),
            )
          }
          PhotoUploadField("${strings.totoFrontPhoto} *", frontPhoto, { frontPhoto = it })
          PhotoUploadField("${strings.totoRearPhoto} *", rearPhoto, { rearPhoto = it })
          PhotoUploadField("${strings.totoSidePhoto} *", sidePhoto, { sidePhoto = it })
        }
      }
    }

    // Section 4: Driving Licence & Private Vehicle Documents
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = TotoDeepEmerald, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = strings.privateDocsSectionTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = TotoDeepEmerald,
            )
          }
          Surface(color = TotoLightMintSurface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              text = strings.privateDocsPrivacyNote,
              style = MaterialTheme.typography.labelSmall,
              color = TotoDeepEmerald,
              modifier = Modifier.padding(10.dp),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = licenceNo,
              onValueChange = { licenceNo = it },
              label = { Text(strings.licenceNumber) },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = permitNo,
              onValueChange = { permitNo = it },
              label = { Text(strings.permitNumber) },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          PhotoUploadField(strings.licenceDocPhoto, licenceDoc, { licenceDoc = it })
          PhotoUploadField("${strings.permitDocPhoto} *", permitDoc, { permitDoc = it })
        }
      }
    }

    if (errorMsg != null) {
      item {
        Surface(color = TotoRedContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
          Text(
            text = errorMsg!!,
            color = TotoRedError,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    item {
      Button(
        onClick = {
          when {
            fullName.trim().length < 2 ->
              errorMsg = if (isBengali) "চালকের পূর্ণ নাম লিখুন" else "Enter driver full name"
            mobile.filter { it.isDigit() }.length < 10 ->
              errorMsg = if (isBengali) "সঠিক ১০ সংখ্যার মোবাইল নম্বর দিন" else "Enter a valid 10-digit mobile number"
            !mobileVerified ->
              errorMsg = if (isBengali) "আগে মোবাইল OTP যাচাই করুন" else "Verify mobile OTP first"
            regNo.trim().length < 5 ->
              errorMsg = if (isBengali) "সঠিক টোটো রেজিস্ট্রেশন নম্বর লিখুন" else "Enter a valid TOTO registration number"
            vehicleModel.trim().length < 2 ->
              errorMsg = if (isBengali) "টোটোর মডেল লিখুন (যেমন Mahindra Treo / Mayuri)" else "Enter TOTO vehicle model"
            combinedServiceArea.isBlank() ->
              errorMsg = if (isBengali) "কমপক্ষে একটি সার্ভিস এলাকা নির্বাচন করুন" else "Select at least one service area"
            driverPhoto == null ->
              errorMsg = if (isBengali) "চালকের স্পষ্ট প্রোফাইল ছবি আপলোড করুন" else "Upload a clear driver portrait photo"
            frontPhoto == null || rearPhoto == null || sidePhoto == null ->
              errorMsg = if (isBengali) {
                "টোটোর সামনে, পিছনে ও পাশের তিনটি আসল ছবি আপলোড করুন"
              } else {
                "Upload actual TOTO front, rear, and side photos"
              }
            permitDoc == null && licenceDoc == null ->
              errorMsg = if (isBengali) {
                "কমপক্ষে টোটো পারমিট/নথি বা ড্রাইভিং লাইসেন্সের ছবি আপলোড করুন"
              } else {
                "Upload at least the vehicle permit or driving licence document photo"
              }
            else -> {
              errorMsg = null
              onSubmit(
                fullName.trim(),
                mobile.trim(),
                mobileVerified,
                regNo.trim().uppercase(),
                vehicleType.trim(),
                vehicleModel.trim(),
                combinedServiceArea,
                licenceNo.takeIf { it.isNotBlank() }?.trim(),
                permitNo.takeIf { it.isNotBlank() }?.trim(),
                driverPhoto,
                frontPhoto,
                rearPhoto,
                sidePhoto,
                licenceDoc,
                permitDoc,
              )
            }
          }
        },
        enabled = !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("submit_driver_application_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
      ) {
        if (isBusy) {
          CircularProgressIndicator(color = TotoPureWhite, modifier = Modifier.size(22.dp))
        } else {
          Icon(Icons.Filled.CheckCircle, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          val submitText = if (existingDriver?.status == DriverStatus.REJECTED) {
            strings.resubmitForVerification
          } else {
            strings.submitApplication
          }
          Text(submitText, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
