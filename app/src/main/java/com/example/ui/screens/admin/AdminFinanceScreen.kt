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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentType
import com.example.data.model.VerificationStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.util.PdfReportGenerator

@Composable
fun AdminFinanceScreen(
  bookings: List<Booking>,
  payments: List<PaymentRecord>,
  expenses: List<ExpenseRecord>,
  currency: CurrencyCode,
  settings: AgencySettings,
  onVerifyPayment: (PaymentRecord, String, List<Booking>, List<PaymentRecord>) -> Unit,
  onAddExpense: (String, String, Long, String, String, String, String) -> Unit,
  onProcessRefund: (Booking, Long, String, String, String) -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  var financeTab by remember { mutableStateOf("PAYMENTS") }
  var showExpenseDialog by remember { mutableStateOf(false) }
  var showRefundDialog by remember { mutableStateOf(false) }

  val totalCollected = payments
    .filter { it.verificationStatus == VerificationStatus.VERIFIED.name && it.paymentType != PaymentType.REFUND.name }
    .sumOf { it.amountBdt }
  val totalExpenses = expenses.sumOf { it.amountBdt }
  val totalRefunds = payments
    .filter { it.paymentType == PaymentType.REFUND.name || it.verificationStatus == VerificationStatus.REFUNDED.name }
    .sumOf { it.amountBdt }
  val pendingDues = (bookings.sumOf { it.totalAmountBdt } - bookings.sumOf { it.paidAmountBdt }).coerceAtLeast(0L)

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val tabs = listOf(
        "PAYMENTS" to "Payments (${payments.size})",
        "EXPENSES" to "Expenses (${expenses.size})",
        "REFUNDS" to "Refunds",
        "REPORTS" to "Reports & PDF",
      )
      items(tabs) { (key, label) ->
        FilterChip(selected = financeTab == key, onClick = { financeTab = key }, label = { Text(label) })
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (financeTab) {
        "PAYMENTS" -> {
          if (payments.isEmpty()) {
            item { EmptyStateCard(Icons.Default.ReceiptLong, "No Payments Yet", "Customer bKash, Nagad, Bank, and Cash records appear here.") }
          } else {
            items(payments, key = { it.id.ifBlank { it.receiptNumber } }) { pay ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${pay.receiptNumber} • ${pay.customerName}", style = MaterialTheme.typography.titleSmall)
                    StatusPill(pay.verificationStatus, statusToneFor(pay.verificationStatus))
                  }
                  Text(
                    "${pay.bookingCode} • ${pay.method} (${pay.paymentType}) • Ref: ${pay.referenceNo.ifBlank { "Counter" }}",
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                    PdfReportGenerator.formatAmount(pay.amountBdt, currency.name, settings.sarToBdtRate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  if (pay.verificationStatus == VerificationStatus.PENDING.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedButton(
                        onClick = { onVerifyPayment(pay, VerificationStatus.REJECTED.name, bookings, payments) },
                        modifier = Modifier.weight(1f),
                      ) { Text("Reject") }
                      Button(
                        onClick = { onVerifyPayment(pay, VerificationStatus.VERIFIED.name, bookings, payments) },
                        modifier = Modifier.weight(1f),
                      ) { Text("Verify Payment") }
                    }
                  }
                }
              }
            }
          }
        }

        "EXPENSES" -> {
          item {
            Button(onClick = { showExpenseDialog = true }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.Add, contentDescription = "Add Expense")
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record Agency Expense")
            }
          }
          if (expenses.isEmpty()) {
            item { EmptyStateCard(Icons.Default.ReceiptLong, "No Expenses Recorded", "Track airline tickets, Saudi hotel payments, visa fees, and office costs.") }
          } else {
            items(expenses, key = { it.id.ifBlank { it.title } }) { exp ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(exp.title, style = MaterialTheme.typography.titleSmall)
                    Text("${exp.category} • ${exp.supplierName} • ${exp.expenseDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(
                    PdfReportGenerator.formatAmount(exp.amountBdt, currency.name, settings.sarToBdtRate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }

        "REFUNDS" -> {
          item {
            Button(
              onClick = { showRefundDialog = true },
              enabled = bookings.isNotEmpty(),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Issue Customer Refund")
            }
          }
          val refundRecords = payments.filter { it.paymentType == PaymentType.REFUND.name }
          if (refundRecords.isEmpty()) {
            item { EmptyStateCard(Icons.Default.ReceiptLong, "No Refunds Issued", "Processed customer refunds appear here with audit trail.") }
          } else {
            items(refundRecords, key = { it.id.ifBlank { it.receiptNumber } }) { ref ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("${ref.receiptNumber} • ${ref.customerName}", style = MaterialTheme.typography.titleSmall)
                    Text("${ref.bookingCode} • ${ref.method} • ${ref.notes}", style = MaterialTheme.typography.bodySmall)
                  }
                  Text(PdfReportGenerator.formatAmount(ref.amountBdt, currency.name, settings.sarToBdtRate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        "REPORTS" -> {
          item {
            FinancialBarChartCard(
              collectedBdt = totalCollected,
              expensesBdt = totalExpenses,
              refundsBdt = totalRefunds,
              duesBdt = pendingDues,
              currency = currency,
              sarRate = settings.sarToBdtRate,
            )
          }
          item {
            Button(
              onClick = {
                PdfReportGenerator.exportAdminFinancialSummaryPdf(
                  context = context,
                  totalBookings = bookings.size,
                  totalCollectedBdt = totalCollected,
                  totalExpensesBdt = totalExpenses,
                  totalRefundsBdt = totalRefunds,
                  pendingDuesBdt = pendingDues,
                ).onFailure { onFeedback("Failed to export PDF: ${it.message}") }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("export_admin_pdf_button"),
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Financial PDF", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export Official PDF Financial Report")
            }
          }
        }
      }
    }
  }

  if (showExpenseDialog) {
    AddExpenseDialog(
      onDismiss = { showExpenseDialog = false },
      onSave = { title, cat, amt, supplier, method, date, notes ->
        onAddExpense(title, cat, amt, supplier, method, date, notes)
        showExpenseDialog = false
      },
    )
  }

  if (showRefundDialog && bookings.isNotEmpty()) {
    IssueRefundDialog(
      bookings = bookings,
      onDismiss = { showRefundDialog = false },
      onRefund = { bk, amt, method, ref, notes ->
        onProcessRefund(bk, amt, method, ref, notes)
        showRefundDialog = false
      },
    )
  }
}

@Composable
private fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, Long, String, String, String, String) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ExpenseCategory.AIRLINE.name) }
  var amountText by remember { mutableStateOf("") }
  var supplier by remember { mutableStateOf("") }
  var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER.name) }
  var date by remember { mutableStateOf("2026-11-17") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Operational Expense") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Description") }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(ExpenseCategory.entries) { cat ->
            FilterChip(selected = category == cat.name, onClick = { category = cat.name }, label = { Text(cat.label) })
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Amount (BDT ৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
        OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier / Payee") }, singleLine = true)
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Voucher / Notes") })
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, category, amountText.toLongOrNull() ?: 0L, supplier, method, date, notes) },
        enabled = title.isNotBlank() && (amountText.toLongOrNull() ?: 0L) > 0L,
      ) { Text("Save Expense") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun IssueRefundDialog(
  bookings: List<Booking>,
  onDismiss: () -> Unit,
  onRefund: (Booking, Long, String, String, String) -> Unit,
) {
  var selectedBooking by remember { mutableStateOf(bookings.first()) }
  var amountText by remember { mutableStateOf("10000") }
  var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER.name) }
  var refNo by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("Cancellation adjustment") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Process Customer Refund") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(bookings.take(6)) { bk ->
            FilterChip(
              selected = selectedBooking.id == bk.id,
              onClick = { selectedBooking = bk },
              label = { Text(bk.bookingCode) },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { c -> c.isDigit() } },
          label = { Text("Refund Amount (BDT ৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PaymentMethod.entries) { m ->
            FilterChip(selected = method == m.name, onClick = { method = m.name }, label = { Text(m.label) })
          }
        }
        OutlinedTextField(value = refNo, onValueChange = { refNo = it }, label = { Text("Refund Reference / Slip") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Reason") })
      }
    },
    confirmButton = {
      Button(
        onClick = { onRefund(selectedBooking, amountText.toLongOrNull() ?: 0L, method, refNo, notes) },
        enabled = (amountText.toLongOrNull() ?: 0L) > 0L,
      ) { Text("Issue Refund") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
