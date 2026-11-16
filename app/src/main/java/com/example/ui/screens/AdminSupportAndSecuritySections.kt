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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AdminAnalytics
import com.example.core.AppStrings
import com.example.core.LocalAppStrings
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.ui.theme.TotoAmberContainer
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoRedContainer
import com.example.ui.theme.TotoRedError
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LazyListScope.adminComplaintsSection(
  strings: AppStrings,
  complaints: List<Complaint>,
  statusFilter: Int,
  onSelectFilter: (Int) -> Unit,
  onOpenReplyDialog: (Complaint) -> Unit,
) {
  item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(strings.complaintsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          "${strings.filterAll} (${complaints.size})",
          "${strings.complaintStatusOpen} (${complaints.count { it.status == ComplaintStatus.OPEN }})",
          "${strings.complaintStatusInReview} (${complaints.count { it.status == ComplaintStatus.IN_REVIEW }})",
          "${strings.complaintStatusResolved} (${complaints.count { it.status == ComplaintStatus.RESOLVED }})",
        ).forEachIndexed { idx, label ->
          FilterChip(
            selected = statusFilter == idx,
            onClick = { onSelectFilter(idx) },
            label = { Text(label) },
          )
        }
      }
    }
  }

  val filtered = complaints.filter { c ->
    when (statusFilter) {
      1 -> c.status == ComplaintStatus.OPEN
      2 -> c.status == ComplaintStatus.IN_REVIEW
      3 -> c.status == ComplaintStatus.RESOLVED
      else -> true
    }
  }

  if (filtered.isEmpty()) {
    item { Text("No complaints in this view.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
  } else {
    items(filtered, key = { it.id }) { c ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${c.category} • ${c.reporterRole}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Surface(
              color = when (c.status) {
                ComplaintStatus.RESOLVED -> TotoLightMintSurface
                ComplaintStatus.IN_REVIEW -> TotoAmberContainer
                else -> TotoRedContainer
              },
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = c.status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          Text(c.description, style = MaterialTheme.typography.bodySmall)
          if (!c.bookingId.isNullOrBlank()) {
            Text("Booking: ${c.bookingId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (!c.adminNotes.isNullOrBlank()) {
            Surface(color = TotoLightMintSurface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "${strings.adminResponsePrefix} ${c.adminNotes}",
                style = MaterialTheme.typography.labelMedium,
                color = TotoDeepEmerald,
                modifier = Modifier.padding(8.dp),
              )
            }
          }
          OutlinedButton(
            onClick = { onOpenReplyDialog(c) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_reply_complaint_${c.id}"),
          ) {
            Text(strings.replyToComplaint)
          }
        }
      }
    }
  }
}

@Composable
fun AdminComplaintReplyDialog(
  complaint: Complaint,
  isBusy: Boolean,
  onSubmit: (status: String, notes: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var selectedStatus by remember(complaint) { mutableStateOf(complaint.status) }
  var replyText by remember(complaint) { mutableStateOf(complaint.adminNotes.orEmpty()) }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    title = { Text(strings.replyToComplaint, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${complaint.category}: ${complaint.description}", style = MaterialTheme.typography.bodySmall)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            ComplaintStatus.OPEN to strings.complaintStatusOpen,
            ComplaintStatus.IN_REVIEW to strings.complaintStatusInReview,
            ComplaintStatus.RESOLVED to strings.complaintStatusResolved,
          ).forEach { (statusValue, label) ->
            FilterChip(
              selected = selectedStatus == statusValue,
              onClick = { selectedStatus = statusValue },
              label = { Text(label) },
            )
          }
        }
        OutlinedTextField(
          value = replyText,
          onValueChange = { replyText = it },
          label = { Text(strings.adminReplyLabel) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(selectedStatus, replyText.trim()) },
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
      ) {
        Text(strings.sendReplyAndUpdate)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isBusy) {
        Text(strings.closeLabel)
      }
    },
  )
}

fun LazyListScope.adminSecurityAndAuditSection(
  strings: AppStrings,
  currentAdminUid: String,
  admins: List<AdminRecord>,
  auditLogs: List<AdminAuditLog>,
  isBusy: Boolean,
  onGrantAdmin: (uid: String, email: String) -> Unit,
  onRevokeAdmin: (uid: String) -> Unit,
) {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.US)

  item {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = TotoLightMintSurface),
      border = BorderStroke(1.dp, TotoCardBorder),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Security, contentDescription = null, tint = TotoDeepEmerald)
          Spacer(Modifier.width(8.dp))
          Text(strings.adminSecurityTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
        }
        Text(
          text = "• Public signup NEVER allows selecting or escalating to Admin.\n" +
            "• Server-side firestore.rules verify verified bootstrap admin email or authorized /admins/{uid} records.\n" +
            "• Enterprise tip: Add a Firebase Cloud Function to mirror /admins/{uid} onto Firebase Auth Custom Claims for multi-service backend scaling.",
          style = MaterialTheme.typography.bodySmall,
          color = TotoDeepEmerald,
        )
      }
    }
  }

  item {
    AdminGrantAccessCard(
      strings = strings,
      currentAdminUid = currentAdminUid,
      admins = admins,
      isBusy = isBusy,
      onGrantAdmin = onGrantAdmin,
      onRevokeAdmin = onRevokeAdmin,
    )
  }

  item {
    Text(
      text = "${strings.auditLogTitle} (${auditLogs.size})",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = TotoDeepEmerald,
    )
  }

  if (auditLogs.isEmpty()) {
    item {
      Text("No admin audit actions recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  } else {
    items(auditLogs, key = { it.id }) { log ->
      val timeText = log.createdAt?.seconds?.let { sec ->
        Instant.ofEpochSecond(sec).atZone(AdminAnalytics.IST_ZONE).format(dateFormatter)
      } ?: ""
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TotoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(log.action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
            Text(timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(log.details, style = MaterialTheme.typography.bodySmall)
          Text("${log.adminEmail} • ${log.targetType}:${log.targetId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun AdminGrantAccessCard(
  strings: AppStrings,
  currentAdminUid: String,
  admins: List<AdminRecord>,
  isBusy: Boolean,
  onGrantAdmin: (uid: String, email: String) -> Unit,
  onRevokeAdmin: (uid: String) -> Unit,
) {
  var uidInput by remember { mutableStateOf("") }
  var emailInput by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = TotoDeepEmerald)
        Spacer(Modifier.width(6.dp))
        Text(strings.authorizedAdminsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }
      admins.forEach { admin ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(admin.email, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("UID: ${admin.uid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (admin.uid != currentAdminUid) {
            TextButton(onClick = { onRevokeAdmin(admin.uid) }, enabled = !isBusy) {
              Text(strings.revokeAdminBtn, color = TotoRedError)
            }
          }
        }
      }

      Spacer(Modifier.height(4.dp))
      Text(strings.grantAdminTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = uidInput,
        onValueChange = { uidInput = it },
        label = { Text(strings.targetUidLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = emailInput,
        onValueChange = { emailInput = it },
        label = { Text(strings.targetEmailLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {
          if (uidInput.isNotBlank() && emailInput.isNotBlank()) {
            onGrantAdmin(uidInput.trim(), emailInput.trim())
            uidInput = ""
            emailInput = ""
          }
        },
        enabled = !isBusy && uidInput.isNotBlank() && emailInput.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(strings.authorizeAdminBtn)
      }
    }
  }
}
