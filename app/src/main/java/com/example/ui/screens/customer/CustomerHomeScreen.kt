package com.example.ui.screens.customer

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.R
import com.example.data.model.AgencySettings
import com.example.data.model.Booking
import com.example.data.model.CurrencyCode
import com.example.data.model.Pilgrim
import com.example.data.model.RoberPathCatalog
import com.example.data.model.VisaStatus
import com.example.ui.components.BrochurePreviewDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RoberPathBrandBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.openWhatsAppOrDial
import com.example.ui.components.statusToneFor
import com.example.util.PdfReportGenerator

@Composable
fun CustomerHomeScreen(
  displayName: String,
  bookings: List<Booking>,
  pilgrims: List<Pilgrim>,
  currency: CurrencyCode,
  settings: AgencySettings,
  onNavigateTab: (Int) -> Unit,
) {
  val context = LocalContext.current
  var showBrochure by remember { mutableStateOf(false) }

  val totalPayable = bookings.sumOf { it.totalAmountBdt }
  val totalPaid = bookings.sumOf { it.paidAmountBdt }
  val outstanding = (totalPayable - totalPaid).coerceAtLeast(0L)
  val visasIssued = pilgrims.count { it.visaStatus == VisaStatus.ISSUED.name }
  val activeBooking = bookings.firstOrNull()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(modifier = Modifier.widthIn(max = 720.dp)) {
        Text(
          text = "Assalamu Alaikum, ${displayName.ifBlank { "Respected Pilgrim" }}",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "সুন্নাহর পথে আপনার বিশ্বস্ত সাথী • 2026–27 Umrah & Hajj",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      RoberPathBrandBanner(
        headline = "Umrah & Hajj 2026–27",
        subheadline = "Packages from ৳1,39,500 • Tap emblem to view official brochure",
        onOpenBrochure = { showBrochure = true },
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        KpiMetricCard(
          title = "My Bookings",
          value = "${bookings.size}",
          subtitle = "${pilgrims.size} pilgrim(s) • $visasIssued visa issued",
          icon = Icons.Default.Mosque,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Due Balance",
          value = PdfReportGenerator.formatAmount(outstanding, currency.name, settings.sarToBdtRate),
          subtitle = "Paid: ${PdfReportGenerator.formatAmount(totalPaid, currency.name, settings.sarToBdtRate)}",
          icon = Icons.Default.Payments,
          accentColor = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      if (activeBooking == null) {
        EmptyStateCard(
          icon = Icons.Default.FlightTakeoff,
          title = "Start Your Haramain Journey",
          description = "Browse our 5 regular 2026–27 Umrah tiers or request a custom Family Package.",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { onNavigateTab(1) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("explore_packages_cta"),
        ) {
          Text("Explore 2026–27 Packages")
        }
      } else {
        val progress = when (activeBooking.status) {
          "INQUIRY" -> 0.2f
          "CONFIRMED" -> 0.4f
          "VISA_PROCESSING" -> 0.65f
          "TICKETED" -> 0.85f
          "DEPARTED", "COMPLETED" -> 1.0f
          else -> 0.2f
        }
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Active Booking • ${activeBooking.bookingCode}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(activeBooking.packageTitle, style = MaterialTheme.typography.titleMedium)
              }
              StatusPill(activeBooking.status, statusToneFor(activeBooking.status))
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Batch: ${activeBooking.travelBatchDate.ifBlank { "Flexible 2026–27" }} • ${activeBooking.pilgrimCount} Pilgrim(s)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Moallem: ${activeBooking.moallemName.ifBlank { "Assigned before departure" }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilledTonalButton(
                onClick = { onNavigateTab(3) },
                modifier = Modifier.weight(1f),
              ) {
                Text("Pilgrims & Visa")
              }
              Button(
                onClick = { onNavigateTab(4) },
                modifier = Modifier.weight(1f),
              ) {
                Text("Pay / Receipt")
              }
            }
          }
        }
      }
    }

    item {
      Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickTileButton(
          icon = Icons.Default.EventNote,
          label = "14-Day Safar",
          onClick = { onNavigateTab(2) },
          modifier = Modifier.weight(1f),
        )
        QuickTileButton(
          icon = Icons.Default.Badge,
          label = "Passport Upload",
          onClick = { onNavigateTab(3) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("24/7 Dhaka Agency Support", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${RoberPathCatalog.DIRECTOR_NAME} • ${RoberPathCatalog.DIRECTOR_TITLE}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = RoberPathCatalog.ADDRESS,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { openWhatsAppOrDial(context, RoberPathCatalog.MOBILE_NUMBER) },
              modifier = Modifier
                .weight(1f)
                .testTag("call_office_button"),
            ) {
              Icon(Icons.Default.Call, contentDescription = "Call Office", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Call Office")
            }
            Button(
              onClick = {
                openWhatsAppOrDial(
                  context,
                  RoberPathCatalog.WHATSAPP_NUMBER,
                  "Assalamu Alaikum, I would like information about Rober Path 2026-27 Umrah packages.",
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("whatsapp_support_button"),
            ) {
              Icon(Icons.Default.Chat, contentDescription = "WhatsApp Support", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("WhatsApp")
            }
          }
        }
      }
    }
  }

  if (showBrochure) {
    BrochurePreviewDialog(
      drawableResId = R.drawable.rober_path_brochure,
      title = "Rober Path 2026–27 Official Packages",
      onDismiss = { showBrochure = false },
    )
  }
}

@Composable
private fun QuickTileButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier.height(52.dp),
  ) {
    Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(label)
  }
}
