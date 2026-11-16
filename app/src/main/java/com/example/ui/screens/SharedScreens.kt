package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.data.model.AppNotification
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.Base64PhotoThumbnail
import com.example.ui.components.MobileOtpVerificationSection
import com.example.ui.components.PhotoUploadField
import com.example.ui.components.TotoLogoHeader
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoRedError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NotificationsScreen(
  notifications: List<AppNotification>,
  onMarkRead: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) {}

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.tabAlerts,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          OutlinedButton(
            onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Enable Alerts", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    item {
      Surface(
        color = TotoLightMintSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Real-time in-app & local ride alerts are active. For background push delivery when closed, enable Firebase Cloud Messaging (FCM) in Firebase Console.",
          style = MaterialTheme.typography.bodySmall,
          color = TotoDeepEmerald,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    if (notifications.isEmpty()) {
      item {
        Text(
          text = "No notifications yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(notifications, key = { it.id }) { notif ->
        Surface(
          onClick = { if (!notif.isRead) onMarkRead(notif.id) },
          shape = RoundedCornerShape(16.dp),
          color = if (notif.isRead) MaterialTheme.colorScheme.surface else TotoLightMintSurface,
          border = BorderStroke(1.dp, TotoCardBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              if (!notif.isRead) {
                Text("NEW", style = MaterialTheme.typography.labelSmall, color = TotoEmeraldPrimary, fontWeight = FontWeight.Bold)
              }
            }
            Text(notif.body, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

@Composable
fun ProfileAndHelpScreen(
  userProfile: UserProfile,
  isAdmin: Boolean,
  myComplaints: List<Complaint> = emptyList(),
  isBusy: Boolean = false,
  onToggleLanguage: () -> Unit,
  onUpdateProfile: ((fullName: String, mobileNumber: String, mobileVerified: Boolean, photoBase64: String?) -> Unit)? = null,
  onSwitchRole: (String) -> Unit,
  onOpenAdminPanel: () -> Unit,
  onSignedOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val strings = LocalAppStrings.current
  val lang = LocalAppLanguage.current
  var isSigningOut by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      TotoLogoHeader(onToggleLanguage = onToggleLanguage)
    }

    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Base64PhotoThumbnail(
            base64 = userProfile.photoBase64,
            contentDescription = userProfile.fullName,
            size = 64.dp,
            isCircle = true,
            emptyLabel = "User",
          )
          Spacer(Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(userProfile.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(userProfile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = "${userProfile.mobileNumber} ${if (userProfile.mobileVerified) "✓" else ""}",
              style = MaterialTheme.typography.bodySmall,
              color = TotoEmeraldPrimary,
            )
          }
          if (onUpdateProfile != null) {
            IconButton(
              onClick = { showEditDialog = true },
              modifier = Modifier.testTag("edit_profile_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = strings.editProfile,
                tint = TotoDeepEmerald,
              )
            }
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.selectRoleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val isPassenger = userProfile.role == UserRole.PASSENGER
            Button(
              onClick = { onSwitchRole(UserRole.PASSENGER) },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isPassenger) TotoDeepEmerald else TotoLightMintSurface,
                contentColor = if (isPassenger) MaterialTheme.colorScheme.onPrimary else TotoDeepEmerald,
              ),
            ) {
              Text(strings.passengerRole)
            }
            Button(
              onClick = { onSwitchRole(UserRole.DRIVER) },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (!isPassenger) TotoDeepEmerald else TotoLightMintSurface,
                contentColor = if (!isPassenger) MaterialTheme.colorScheme.onPrimary else TotoDeepEmerald,
              ),
            ) {
              Text(strings.driverRole)
            }
          }

          if (isAdmin) {
            OutlinedButton(
              onClick = onOpenAdminPanel,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("open_admin_panel_button"),
            ) {
              Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.adminDashboardTitle)
            }
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = TotoDeepEmerald)
            Spacer(Modifier.width(8.dp))
            Text(strings.helpAndSupport, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Text(
            text = "• Always verify the driver's real photograph & TOTO plate number before boarding.\n" +
              "• Language: ${lang.label} (switch anytime at the top).\n" +
              "• Payments: Cash on arrival (UPI coming soon).\n" +
              "• Configuration Notice: SMS OTP is currently in Testing Mode; Firebase Storage CDN & FCM background push can be enabled in Firebase Console.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (myComplaints.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, TotoCardBorder),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = strings.mySupportReportsTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = TotoDeepEmerald,
            )
            myComplaints.take(10).forEach { c ->
              Surface(
                color = if (c.status == ComplaintStatus.RESOLVED) TotoLightMintSurface else TotoAmberContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TotoCardBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(c.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = TotoDeepEmerald, fontWeight = FontWeight.Bold)
                  }
                  Text(c.description, style = MaterialTheme.typography.bodySmall)
                  if (!c.adminNotes.isNullOrBlank()) {
                    Text(
                      text = "${strings.adminResponsePrefix} ${c.adminNotes}",
                      style = MaterialTheme.typography.labelMedium,
                      color = TotoDeepEmerald,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = {
          isSigningOut = true
          scope.launch {
            withContext(NonCancellable) {
              try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
              } catch (_: Exception) {
              } finally {
                Firebase.auth.signOut()
                isSigningOut = false
                onSignedOut()
              }
            }
          }
        },
        enabled = !isSigningOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TotoRedError),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.signOut, fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showEditDialog && onUpdateProfile != null) {
    var editName by remember { mutableStateOf(userProfile.fullName) }
    var editMobile by remember { mutableStateOf(userProfile.mobileNumber) }
    var editVerified by remember { mutableStateOf(userProfile.mobileVerified) }
    var editPhoto by remember { mutableStateOf(userProfile.photoBase64) }

    AlertDialog(
      onDismissRequest = { if (!isBusy) showEditDialog = false },
      title = { Text(strings.editProfile, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text(strings.fullName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          MobileOtpVerificationSection(
            mobileNumber = editMobile,
            onMobileChange = {
              editMobile = it
              editVerified = (it.trim() == userProfile.mobileNumber.trim() && userProfile.mobileVerified)
            },
            isVerified = editVerified,
            onVerified = { editVerified = true },
          )
          PhotoUploadField(
            label = strings.optionalPhoto,
            base64 = editPhoto,
            onPhotoEncoded = { editPhoto = it },
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editName.trim().length >= 2 && editMobile.filter { it.isDigit() }.length >= 10 && editVerified) {
              onUpdateProfile(editName.trim(), editMobile.trim(), editVerified, editPhoto)
              showEditDialog = false
            }
          },
          enabled = !isBusy && editName.trim().length >= 2 && editVerified,
        ) {
          Text(strings.saveAndContinue)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }, enabled = !isBusy) {
          Text(strings.closeLabel)
        }
      },
    )
  }
}
