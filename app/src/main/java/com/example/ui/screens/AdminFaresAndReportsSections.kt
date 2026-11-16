package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AdminAnalytics
import com.example.core.AdminBookingStats
import com.example.core.DriverPerformanceRow
import com.example.core.LocalAppStrings
import com.example.data.model.Booking
import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import com.example.ui.theme.TotoAmber
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoRedError
import java.util.Locale

@Composable
fun AdminFareEditorCard(
  fareSettings: FareSettings,
  fixedRoutes: List<FixedRoute>,
  isBusy: Boolean,
  onSaveSettings: (FareSettings) -> Unit,
  onAddFixedRoute: (String, String, Double) -> Unit,
  onDeleteFixedRoute: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  var minFare by remember(fareSettings) { mutableStateOf(fareSettings.minFare.toString()) }
  var perKm by remember(fareSettings) { mutableStateOf(fareSettings.perKmFare.toString()) }
  var minDist by remember(fareSettings) { mutableStateOf(fareSettings.minDistanceKm.toString()) }
  var extraKm by remember(fareSettings) { mutableStateOf(fareSettings.additionalDistanceFarePerKm.toString()) }
  var waitCharge by remember(fareSettings) { mutableStateOf(fareSettings.waitingChargePerMin.toString()) }

  var routeOrigin by remember { mutableStateOf("Chopra Bazar") }
  var routeDest by remember { mutableStateOf("Sonapur More") }
  var routeFare by remember { mutableStateOf("25") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, TotoCardBorder),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(strings.fareSettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TotoDeepEmerald)
      OutlinedTextField(minFare, { minFare = it }, label = { Text(strings.minFareLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(perKm, { perKm = it }, label = { Text(strings.perKmFareLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(minDist, { minDist = it }, label = { Text(strings.minDistanceLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(extraKm, { extraKm = it }, label = { Text(strings.extraKmFareLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(waitCharge, { waitCharge = it }, label = { Text(strings.waitingChargeLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())

      Button(
        onClick = {
          onSaveSettings(
            FareSettings(
              minFare = minFare.toDoubleOrNull() ?: 20.0,
              perKmFare = perKm.toDoubleOrNull() ?: 12.0,
              minDistanceKm = minDist.toDoubleOrNull() ?: 1.5,
              additionalDistanceFarePerKm = extraKm.toDoubleOrNull() ?: 10.0,
              waitingChargePerMin = waitCharge.toDoubleOrNull() ?: 2.0,
            )
          )
        },
        enabled = !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("save_fare_rules_button"),
        colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
      ) {
        Text(strings.saveFareRules)
      }

      Spacer(Modifier.height(8.dp))
      Text(strings.addFixedRoute, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(routeOrigin, { routeOrigin = it }, label = { Text(strings.fixedRouteOriginLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(routeDest, { routeDest = it }, label = { Text(strings.fixedRouteDestLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(routeFare, { routeFare = it }, label = { Text(strings.fixedRouteFareLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedButton(
        onClick = {
          val f = routeFare.toDoubleOrNull() ?: return@OutlinedButton
          if (routeOrigin.isNotBlank() && routeDest.isNotBlank()) {
            onAddFixedRoute(routeOrigin, routeDest, f)
          }
        },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(strings.addFixedRoute)
      }

      fixedRoutes.forEach { route ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("${route.originName} ↔ ${route.destinationName}: ₹${route.fixedFare.toInt()}")
          IconButton(onClick = { onDeleteFixedRoute(route.id) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete route", tint = TotoRedError)
          }
        }
      }
    }
  }
}

@Composable
fun AdminReportsAndAnalyticsCard(
  stats: AdminBookingStats,
  bookings: List<Booking>,
  driverPerformance: List<DriverPerformanceRow>,
  onNotifyMessage: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  var pendingCsvContent by remember { mutableStateOf("") }

  val createCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    if (uri != null && pendingCsvContent.isNotBlank()) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(pendingCsvContent.toByteArray(Charsets.UTF_8))
        }
      }.onSuccess {
        onNotifyMessage(strings.csvSavedMsg)
      }.onFailure { err ->
        onNotifyMessage(err.localizedMessage ?: "Export failed")
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = strings.reportsAnalyticsTitle,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = TotoDeepEmerald,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      AdminKpiCard(strings.dailyBookingStatsTitle, "₹${stats.todayRevenue.toInt()} (${stats.todayCompletedBookings})", Modifier.weight(1f))
      AdminKpiCard(strings.monthlyBookingStatsTitle, "₹${stats.monthRevenue.toInt()} (${stats.monthCompletedBookings})", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      AdminKpiCard(strings.completedBookingsCount, "${stats.completedBookings}", Modifier.weight(1f))
      AdminKpiCard(
        strings.cancellationRateLabel,
        String.format(Locale.US, "%.1f%% (%d)", stats.cancellationRatePercent, stats.cancelledBookings),
        Modifier.weight(1f),
      )
    }

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = TotoLightMintSurface),
      border = BorderStroke(1.dp, TotoCardBorder),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = {
              pendingCsvContent = AdminAnalytics.buildBookingsCsv(bookings)
              createCsvLauncher.launch("totogo_bookings_report.csv")
            },
            colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
            modifier = Modifier
              .weight(1f)
              .testTag("download_bookings_csv_button"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.downloadBookingsCsv, style = MaterialTheme.typography.labelSmall)
          }
          Button(
            onClick = {
              pendingCsvContent = AdminAnalytics.buildDriverPerformanceCsv(driverPerformance)
              createCsvLauncher.launch("totogo_driver_performance.csv")
            },
            colors = ButtonDefaults.buttonColors(containerColor = TotoEmeraldPrimary),
            modifier = Modifier
              .weight(1f)
              .testTag("download_drivers_csv_button"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.downloadDriversCsv, style = MaterialTheme.typography.labelSmall)
          }
        }
        OutlinedButton(
          onClick = {
            val csv = AdminAnalytics.buildBookingsCsv(bookings)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/csv"
              putExtra(Intent.EXTRA_SUBJECT, "TOTO GO Bookings Report")
              putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(Intent.createChooser(shareIntent, strings.shareCsvReport))
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.shareCsvReport)
        }
      }
    }

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, TotoCardBorder),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.cancellationReportTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (stats.cancellationReasons.isEmpty()) {
          Text("No cancelled bookings recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          stats.cancellationReasons.forEach { (reason, count) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(reason, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Text("$count", style = MaterialTheme.typography.labelMedium, color = TotoRedError, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, TotoCardBorder),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.driverPerformanceTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (driverPerformance.isEmpty()) {
          Text("No drivers registered yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          driverPerformance.forEach { row ->
            Surface(
              color = TotoLightMintSurface.copy(alpha = 0.6f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${row.fullName} (${row.registrationNumber})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                  Text("${row.serviceArea} • ${row.status}", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("${row.totalTrips} trips • ₹${row.totalEarnings.toInt()}", fontWeight = FontWeight.Bold, color = TotoDeepEmerald, style = MaterialTheme.typography.labelMedium)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = TotoAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                      text = row.avgRating?.let { String.format(Locale.US, "%.1f (%d)", it, row.ratingCount) } ?: "—",
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
