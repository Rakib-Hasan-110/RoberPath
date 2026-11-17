package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Booking
import com.example.data.model.PaymentRecord
import com.example.data.model.RoberPathCatalog
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

  fun formatBdt(amount: Long): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    val formatted = if (digits.length <= 3) {
      digits
    } else {
      val lastThree = digits.takeLast(3)
      val rest = digits.dropLast(3)
      val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
      "$groupedRest,$lastThree"
    }
    return if (negative) "-৳$formatted" else "৳$formatted"
  }

  fun formatAmount(amountBdt: Long, currency: String, sarRate: Double): String {
    return if (currency == "SAR") {
      val sar = (amountBdt / sarRate.coerceAtLeast(1.0)).toLong()
      "${NumberFormat.getNumberInstance(Locale.US).format(sar)} SAR"
    } else {
      formatBdt(amountBdt)
    }
  }

  fun exportBookingInvoicePdf(
    context: Context,
    booking: Booking,
    payments: List<PaymentRecord>,
    sarRate: Double,
  ): Result<Unit> = runCatching {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    val headerPaint = Paint().apply {
      color = Color.parseColor("#0F5C38")
      style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, 595f, 118f, headerPaint)

    val goldPaint = Paint().apply {
      color = Color.parseColor("#C89B3C")
      strokeWidth = 4f
    }
    canvas.drawLine(0f, 118f, 595f, 118f, goldPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE
      textSize = 24f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    canvas.drawText("ROBER PATH", 36f, 44f, titlePaint)

    val subPaint = Paint().apply {
      color = Color.parseColor("#F9E4B7")
      textSize = 11f
    }
    canvas.drawText("Hajj & Umrah Services • ${RoberPathCatalog.LEGAL_PARENT}", 36f, 64f, subPaint)
    canvas.drawText("Agent No: ${RoberPathCatalog.AGENT_NUMBER}  |  License No: ${RoberPathCatalog.LICENSE_NUMBER}", 36f, 82f, subPaint)
    canvas.drawText(RoberPathCatalog.ADDRESS, 36f, 100f, subPaint)

    val bodyTitlePaint = Paint().apply {
      color = Color.parseColor("#18201B")
      textSize = 16f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val textPaint = Paint().apply {
      color = Color.parseColor("#2A332D")
      textSize = 11.5f
    }
    val mutedPaint = Paint().apply {
      color = Color.parseColor("#5A655E")
      textSize = 10f
    }

    var y = 152f
    canvas.drawText("OFFICIAL BOOKING INVOICE & PAYMENT RECEIPT", 36f, y, bodyTitlePaint)
    y += 22f
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    canvas.drawText("Generated: $dateStr    |    Booking Code: ${booking.bookingCode}", 36f, y, mutedPaint)
    y += 26f

    canvas.drawText("Pilgrim / Customer: ${booking.customerName}", 36f, y, textPaint)
    y += 18f
    canvas.drawText("Phone: ${booking.customerPhone}    |    Type: ${booking.bookingType} (${booking.pilgrimCount} Pilgrim(s))", 36f, y, textPaint)
    y += 18f
    canvas.drawText("Package: ${booking.packageTitle}", 36f, y, textPaint)
    y += 18f
    canvas.drawText("Status: ${booking.status}    |    Batch: ${booking.travelBatchDate.ifBlank { "Upcoming Kafela" }}", 36f, y, textPaint)
    y += 28f

    val dueBdt = (booking.totalAmountBdt - booking.paidAmountBdt).coerceAtLeast(0L)
    canvas.drawText("FINANCIAL SUMMARY", 36f, y, bodyTitlePaint)
    y += 20f
    canvas.drawText("Total Payable: ${formatBdt(booking.totalAmountBdt)} (${formatAmount(booking.totalAmountBdt, "SAR", sarRate)})", 36f, y, textPaint)
    y += 18f
    canvas.drawText("Total Verified Paid: ${formatBdt(booking.paidAmountBdt)}", 36f, y, textPaint)
    y += 18f
    canvas.drawText("Outstanding Balance: ${formatBdt(dueBdt)}", 36f, y, bodyTitlePaint)
    y += 28f

    canvas.drawText("PAYMENT TRANSACTIONS", 36f, y, bodyTitlePaint)
    y += 20f
    if (payments.isEmpty()) {
      canvas.drawText("No payments recorded yet.", 36f, y, mutedPaint)
      y += 18f
    } else {
      payments.take(14).forEach { pay ->
        val line = "${pay.receiptNumber}  •  ${pay.method}  •  ${formatBdt(pay.amountBdt)}  •  ${pay.verificationStatus}  (${pay.referenceNo.ifBlank { "Cash" }})"
        canvas.drawText(line, 36f, y, textPaint)
        y += 18f
      }
    }

    y += 24f
    canvas.drawLine(36f, y, 559f, y, goldPaint)
    y += 20f
    canvas.drawText("Authorized Contact: ${RoberPathCatalog.DIRECTOR_NAME} (${RoberPathCatalog.WHATSAPP_NUMBER})", 36f, y, mutedPaint)
    y += 15f
    canvas.drawText("Email: ${RoberPathCatalog.OFFICIAL_EMAIL}  |  Web: ${RoberPathCatalog.WEBSITE}", 36f, y, mutedPaint)

    pdf.finishPage(page)

    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, "RoberPath_Invoice_${booking.bookingCode}.pdf")
    FileOutputStream(outFile).use { pdf.writeTo(it) }
    pdf.close()

    sharePdfFile(context, outFile, "Rober Path Invoice ${booking.bookingCode}")
  }

  fun exportAdminFinancialSummaryPdf(
    context: Context,
    totalBookings: Int,
    totalCollectedBdt: Long,
    totalExpensesBdt: Long,
    totalRefundsBdt: Long,
    pendingDuesBdt: Long,
  ): Result<Unit> = runCatching {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    val headerPaint = Paint().apply { color = Color.parseColor("#0F5C38") }
    canvas.drawRect(0f, 0f, 595f, 104f, headerPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE
      textSize = 22f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    canvas.drawText("ROBER PATH • FINANCIAL REPORT", 36f, 46f, titlePaint)
    val subPaint = Paint().apply { color = Color.parseColor("#F9E4B7"); textSize = 11f }
    canvas.drawText("Dhaka Bangladesh Agency Ledger  |  License #${RoberPathCatalog.LICENSE_NUMBER}", 36f, 72f, subPaint)

    val labelPaint = Paint().apply {
      color = Color.parseColor("#18201B")
      textSize = 13f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val valPaint = Paint().apply { color = Color.parseColor("#2A332D"); textSize = 12f }

    var y = 148f
    val netBdt = totalCollectedBdt - totalExpensesBdt - totalRefundsBdt
    val rows = listOf(
      "Report Generated" to SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date()),
      "Active Bookings" to "$totalBookings reservations",
      "Verified Collections" to formatBdt(totalCollectedBdt),
      "Total Operational Expenses" to formatBdt(totalExpensesBdt),
      "Total Refunds Issued" to formatBdt(totalRefundsBdt),
      "Net Cash Position" to formatBdt(netBdt),
      "Outstanding Customer Dues" to formatBdt(pendingDuesBdt),
    )
    rows.forEach { (k, v) ->
      canvas.drawText(k, 36f, y, labelPaint)
      canvas.drawText(v, 280f, y, valPaint)
      y += 28f
    }

    pdf.finishPage(page)
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, "RoberPath_Financial_Report.pdf")
    FileOutputStream(outFile).use { pdf.writeTo(it) }
    pdf.close()

    sharePdfFile(context, outFile, "Rober Path Financial Report")
  }

  private fun sharePdfFile(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, title)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }
}
