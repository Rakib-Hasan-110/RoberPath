package com.example.ui.screens.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencySettings
import com.example.data.model.Booking
import com.example.data.model.CurrencyCode
import com.example.data.model.ExpenseRecord
import com.example.data.model.InquiryLead
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentType
import com.example.data.model.Pilgrim
import com.example.data.model.TourGroup
import com.example.data.model.VerificationStatus
import com.example.data.model.VisaStatus
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RoberPathBrandBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.viewmodel.AgencyViewModel
import com.example.util.PdfReportGenerator

@Composable
fun AdminDashboardScreen(
  bookings: List<Booking>,
  pilgrims: List<Pilgrim>,
  payments: List<PaymentRecord>,
  expenses: List<ExpenseRecord>,
  inquiries: List<InquiryLead>,
  groups: List<TourGroup>,
  currency: CurrencyCode,
  settings: AgencySettings,
  onNavigateTab: (Int) -> Unit,
) {
  val totalCollected = payments
    .filter { it.verificationStatus == VerificationStatus.VERIFIED.name && it.paymentType != PaymentType.REFUND.name }
    .sumOf { it.amountBdt }
  val totalExpenses = expenses.sumOf { it.amountBdt }
  val totalRefunds = payments
    .filter { it.paymentType == PaymentType.REFUND.name || it.verificationStatus == VerificationStatus.REFUNDED.name }
    .sumOf { it.amountBdt }
  val totalPayable = bookings.sumOf { it.totalAmountBdt }
  val totalPaid = bookings.sumOf { it.paidAmountBdt }
  val duesBdt = (totalPayable - totalPaid).coerceAtLeast(0L)
  val netProfitBdt = totalCollected - totalExpenses - totalRefunds

  val pendingVerifyCount = payments.count { it.verificationStatus == VerificationStatus.PENDING.name }
  val expiringPassports = pilgrims.count {
    AgencyViewModel.isPassportExpiringSoon(it.passportExpiry, settings.minPassportMonths)
  }
  val issuedVisas = pilgrims.count { it.visaStatus == VisaStatus.ISSUED.name }
  val openLeads = inquiries.count { it.leadStatus in listOf("NEW", "FOLLOW_UP", "QUOTED") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      RoberPathBrandBanner(
        headline = "Agency Command Center",
        subheadline = "Super Admin • Dhaka HQ • 1 SAR = ৳${settings.sarToBdtRate}",
      )
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiMetricCard(
          title = "Bookings & Leads",
          value = "${bookings.size}",
          subtitle = "$openLeads open CRM leads",
          icon = Icons.Default.BookOnline,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Collections",
          value = PdfReportGenerator.formatAmount(totalCollected, currency.name, settings.sarToBdtRate),
          subtitle = "$pendingVerifyCount pending approval",
          icon = Icons.Default.AccountBalanceWallet,
          accentColor = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiMetricCard(
          title = "Net Balance",
          value = PdfReportGenerator.formatAmount(netProfitBdt, currency.name, settings.sarToBdtRate),
          subtitle = "Dues: ${PdfReportGenerator.formatAmount(duesBdt, currency.name, settings.sarToBdtRate)}",
          icon = Icons.Default.TrendingUp,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Pilgrims & Visas",
          value = "${pilgrims.size}",
          subtitle = "$issuedVisas issued • $expiringPassports expiry alert",
          icon = Icons.Default.Badge,
          accentColor = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      FinancialBarChartCard(
        collectedBdt = totalCollected,
        expensesBdt = totalExpenses,
        refundsBdt = totalRefunds,
        duesBdt = duesBdt,
        currency = currency,
        sarRate = settings.sarToBdtRate,
      )
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notifications & Action Alerts", style = MaterialTheme.typography.titleMedium)
          }
          AlertRow("Pending Payment Verifications", "$pendingVerifyCount", if (pendingVerifyCount > 0) StatusTone.PENDING else StatusTone.VERIFIED)
          AlertRow("Passport < ${settings.minPassportMonths} Months Validity", "$expiringPassports", if (expiringPassports > 0) StatusTone.ALERT else StatusTone.VERIFIED)
          AlertRow("Active Tour Groups (Kafela)", "${groups.size}", StatusTone.INFO)
          Spacer(modifier = Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onNavigateTab(1) }, modifier = Modifier.weight(1f)) {
              Text("CRM & Bookings")
            }
            FilledTonalButton(onClick = { onNavigateTab(3) }, modifier = Modifier.weight(1f)) {
              Text("Verify Payments")
            }
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Groups, contentDescription = "Upcoming Departures", tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upcoming Kafela Departures", style = MaterialTheme.typography.titleMedium)
          }
          if (groups.isEmpty()) {
            Text("Create Kafela groups in CRM & Groups tab.", style = MaterialTheme.typography.bodySmall)
          } else {
            groups.take(3).forEach { grp ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${grp.groupCode} • ${grp.name}", style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${grp.departureDate} • ${grp.airline} ${grp.flightNumber} • ${grp.assignedCount}/${grp.capacity} seats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                StatusPill(grp.groupStatus, StatusTone.INFO)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AlertRow(label: String, count: String, tone: StatusTone) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    StatusPill(count, tone)
  }
}
