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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencySettings
import com.example.data.model.Booking
import com.example.data.model.CurrencyCode
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.util.PdfReportGenerator

@Composable
fun CustomerPaymentsScreen(
  bookings: List<Booking>,
  payments: List<PaymentRecord>,
  currency: CurrencyCode,
  settings: AgencySettings,
  onSubmitPayment: (
    booking: Booking,
    amountBdt: Long,
    method: String,
    referenceNo: String,
    paymentType: String,
    notes: String,
  ) -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  var showPayDialog by remember { mutableStateOf(false) }
  val activeBooking = bookings.firstOrNull()

  val totalPayable = bookings.sumOf { it.totalAmountBdt }
  val totalPaid = bookings.sumOf { it.paidAmountBdt }
  val dueBdt = (totalPayable - totalPaid).coerceAtLeast(0L)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Payments & Installment Ledger (${currency.name})", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
              Text("Total Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(PdfReportGenerator.formatAmount(totalPayable, currency.name, settings.sarToBdtRate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column {
              Text("Verified Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(PdfReportGenerator.formatAmount(totalPaid, currency.name, settings.sarToBdtRate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Balance Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(PdfReportGenerator.formatAmount(dueBdt, currency.name, settings.sarToBdtRate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "bKash: ${settings.bkashMerchant}  •  Nagad: ${settings.nagadMerchant}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = settings.bankAccountSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showPayDialog = true },
              enabled = activeBooking != null,
              modifier = Modifier
                .weight(1f)
                .testTag("submit_payment_button"),
            ) {
              Icon(Icons.Default.Payments, contentDescription = "Submit Payment", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record Payment")
            }
            OutlinedButton(
              onClick = {
                if (activeBooking != null) {
                  val bookingPays = payments.filter { it.bookingId == activeBooking.id }
                  PdfReportGenerator.exportBookingInvoicePdf(context, activeBooking, bookingPays, settings.sarToBdtRate)
                    .onFailure { onFeedback("Unable to share PDF: ${it.message}") }
                }
              },
              enabled = activeBooking != null,
              modifier = Modifier
                .weight(1f)
                .testTag("download_invoice_pdf_button"),
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Invoice PDF", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("PDF Receipt")
            }
          }
        }
      }
    }

    item {
      Text("Payment & Receipt History", style = MaterialTheme.typography.titleMedium)
    }

    if (payments.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.ReceiptLong,
          title = "No Payments Submitted",
          description = "After requesting a booking, submit Cash, Bank Transfer, bKash, or Nagad reference details here.",
        )
      }
    } else {
      items(payments, key = { it.id.ifBlank { it.receiptNumber } }) { pay ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${pay.receiptNumber} • ${pay.method}", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "${pay.paymentType} • Ref: ${pay.referenceNo.ifBlank { "Cash Counter" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = PdfReportGenerator.formatAmount(pay.amountBdt, currency.name, settings.sarToBdtRate),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(4.dp))
              StatusPill(pay.verificationStatus, statusToneFor(pay.verificationStatus))
            }
          }
        }
      }
    }
  }

  if (showPayDialog && activeBooking != null) {
    SubmitPaymentDialog(
      booking = activeBooking,
      onDismiss = { showPayDialog = false },
      onSubmit = { amountBdt, method, refNo, payType, notes ->
        onSubmitPayment(activeBooking, amountBdt, method, refNo, payType, notes)
        showPayDialog = false
      },
    )
  }
}

@Composable
private fun SubmitPaymentDialog(
  booking: Booking,
  onDismiss: () -> Unit,
  onSubmit: (Long, String, String, String, String) -> Unit,
) {
  var amountText by remember { mutableStateOf("50000") }
  var method by remember { mutableStateOf(PaymentMethod.BKASH.name) }
  var paymentType by remember { mutableStateOf(PaymentType.ADVANCE.name) }
  var referenceNo by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Submit Payment (${booking.bookingCode})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Amount in BDT (৳)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text("Payment Method", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PaymentMethod.entries) { m ->
            FilterChip(
              selected = method == m.name,
              onClick = { method = m.name },
              label = { Text(m.label) },
            )
          }
        }
        Text("Installment Type", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf(PaymentType.ADVANCE, PaymentType.INSTALLMENT, PaymentType.FINAL)) { pt ->
            FilterChip(
              selected = paymentType == pt.name,
              onClick = { paymentType = pt.name },
              label = { Text(pt.label) },
            )
          }
        }
        OutlinedTextField(
          value = referenceNo,
          onValueChange = { referenceNo = it },
          label = { Text("TrxID / Bank Slip / Cash Receipt Ref") },
          singleLine = true,
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Optional Note") },
          maxLines = 2,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toLongOrNull() ?: 0L
          onSubmit(amt, method, referenceNo, paymentType, notes)
        },
        enabled = (amountText.toLongOrNull() ?: 0L) > 0L,
      ) {
        Text("Submit")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
