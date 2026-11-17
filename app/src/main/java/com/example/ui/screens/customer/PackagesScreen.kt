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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AgencySettings
import com.example.data.model.BookingType
import com.example.data.model.CurrencyCode
import com.example.data.model.LeadSource
import com.example.data.model.TravelPackage
import com.example.ui.components.BrochurePreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.util.PdfReportGenerator

@Composable
fun PackagesScreen(
  packages: List<TravelPackage>,
  currency: CurrencyCode,
  settings: AgencySettings,
  defaultCustomerName: String,
  onCreateBooking: (
    customerName: String,
    customerPhone: String,
    pkg: TravelPackage,
    bookingType: String,
    pilgrimCount: Int,
    travelBatchDate: String,
    notes: String,
  ) -> Unit,
  onSubmitInquiry: (
    leadName: String,
    phone: String,
    packageInterest: String,
    pilgrimCount: Int,
    source: String,
    notes: String,
  ) -> Unit,
) {
  var selectedFilter by remember { mutableStateOf("ALL") }
  var selectedPackageForBooking by remember { mutableStateOf<TravelPackage?>(null) }
  var selectedPackageForInquiry by remember { mutableStateOf<TravelPackage?>(null) }
  var showBrochure by remember { mutableStateOf(false) }

  val filteredPackages = remember(packages, selectedFilter) {
    packages.filter { pkg ->
      pkg.active && when (selectedFilter) {
        "UMRAH" -> pkg.serviceType == "UMRAH"
        "HAJJ" -> pkg.serviceType == "HAJJ"
        "DIRECT" -> pkg.flightType == "DIRECT"
        else -> true
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("2026–27 Regular & Custom Packages", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "All packages include visa, ticket, 4/5★ hotels, transport & 3 Bangladeshi meals",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedButton(
          onClick = { showBrochure = true },
          modifier = Modifier.testTag("view_brochure_button"),
        ) {
          Icon(Icons.Default.Image, contentDescription = "View Brochure", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Brochure")
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val filters = listOf("ALL" to "All Tiers", "UMRAH" to "Umrah", "HAJJ" to "Hajj", "DIRECT" to "Direct Flight")
        items(filters) { (key, label) ->
          FilterChip(
            selected = selectedFilter == key,
            onClick = { selectedFilter = key },
            label = { Text(label) },
          )
        }
      }
    }

    items(filteredPackages, key = { it.id.ifBlank { it.title } }) { pkg ->
      PackageCard(
        pkg = pkg,
        currency = currency,
        sarRate = settings.sarToBdtRate,
        onBookClick = { selectedPackageForBooking = pkg },
        onInquireClick = { selectedPackageForInquiry = pkg },
      )
    }
  }

  if (showBrochure) {
    BrochurePreviewDialog(
      drawableResId = R.drawable.rober_path_brochure,
      title = "Rober Path 2026–27 Price & Services Brochure",
      onDismiss = { showBrochure = false },
    )
  }

  selectedPackageForBooking?.let { pkg ->
    BookingRequestDialog(
      pkg = pkg,
      defaultName = defaultCustomerName,
      currency = currency,
      sarRate = settings.sarToBdtRate,
      onDismiss = { selectedPackageForBooking = null },
      onConfirm = { name, phone, bookingType, count, batchDate, notes ->
        onCreateBooking(name, phone, pkg, bookingType, count, batchDate, notes)
        selectedPackageForBooking = null
      },
    )
  }

  selectedPackageForInquiry?.let { pkg ->
    InquiryDialog(
      pkg = pkg,
      defaultName = defaultCustomerName,
      onDismiss = { selectedPackageForInquiry = null },
      onSubmit = { name, phone, count, notes ->
        onSubmitInquiry(name, phone, pkg.title, count, LeadSource.APP.name, notes)
        selectedPackageForInquiry = null
      },
    )
  }
}

@Composable
private fun PackageCard(
  pkg: TravelPackage,
  currency: CurrencyCode,
  sarRate: Double,
  onBookClick: () -> Unit,
  onInquireClick: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusPill(pkg.serviceType, StatusTone.VERIFIED)
            StatusPill(if (pkg.flightType == "DIRECT") "Direct Flight" else "Transit", StatusTone.INFO)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(pkg.title, style = MaterialTheme.typography.titleLarge)
          if (pkg.subtitle.isNotBlank()) {
            Text(
              text = pkg.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = PdfReportGenerator.formatAmount(pkg.priceBdt, currency.name, sarRate),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text("per person", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Hotel, contentDescription = "Hotels", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Makkah: ${pkg.makkahHotelDistance} • Madinah: ${pkg.madinahHotelDistance}",
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flight, contentDescription = "Duration", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${pkg.durationDays} Days (${pkg.makkahNights}N Makkah + ${pkg.madinahNights}N Madinah) • ${pkg.hotelRating}",
          style = MaterialTheme.typography.bodySmall,
        )
      }

      if (expanded) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Included in Package:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        pkg.inclusions.forEach { inc ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp),
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(inc, style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = { expanded = !expanded }) {
          Text(if (expanded) "Hide Details" else "Inclusions")
        }
        OutlinedButton(
          onClick = onInquireClick,
          modifier = Modifier.weight(1f),
        ) {
          Text("Inquire")
        }
        Button(
          onClick = onBookClick,
          modifier = Modifier
            .weight(1f)
            .testTag("book_package_${pkg.id}"),
        ) {
          Text("Book Now")
        }
      }
    }
  }
}

@Composable
private fun BookingRequestDialog(
  pkg: TravelPackage,
  defaultName: String,
  currency: CurrencyCode,
  sarRate: Double,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Int, String, String) -> Unit,
) {
  var name by remember { mutableStateOf(defaultName) }
  var phone by remember { mutableStateOf("+8801") }
  var bookingType by remember { mutableStateOf(BookingType.INDIVIDUAL.name) }
  var pilgrimCountText by remember { mutableStateOf("1") }
  var batchDate by remember { mutableStateOf("2026-12-12") }
  var notes by remember { mutableStateOf("") }

  val count = pilgrimCountText.toIntOrNull()?.coerceIn(1, 100) ?: 1
  val estimatedTotal = pkg.priceBdt * count

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Book ${pkg.title}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Estimated Total: ${PdfReportGenerator.formatAmount(estimatedTotal, currency.name, sarRate)} ($count pilgrim(s))",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Customer Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Mobile / WhatsApp (+880...)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          BookingType.entries.forEach { type ->
            FilterChip(
              selected = bookingType == type.name,
              onClick = { bookingType = type.name },
              label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = pilgrimCountText,
            onValueChange = { pilgrimCountText = it.filter { c -> c.isDigit() } },
            label = { Text("Pilgrims") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = batchDate,
            onValueChange = { batchDate = it },
            label = { Text("Batch Date") },
            singleLine = true,
            modifier = Modifier.weight(1.5f),
          )
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Room / Wheelchair / Family Notes") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name, phone, bookingType, count, batchDate, notes) },
        enabled = name.isNotBlank() && phone.length >= 6,
      ) {
        Text("Submit Booking")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun InquiryDialog(
  pkg: TravelPackage,
  defaultName: String,
  onDismiss: () -> Unit,
  onSubmit: (String, String, Int, String) -> Unit,
) {
  var name by remember { mutableStateOf(defaultName) }
  var phone by remember { mutableStateOf("+8801") }
  var countText by remember { mutableStateOf("2") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Inquire: ${pkg.title}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your Name") }, singleLine = true)
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone / WhatsApp") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        OutlinedTextField(
          value = countText,
          onValueChange = { countText = it.filter { c -> c.isDigit() } },
          label = { Text("Approx. Pilgrims") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Questions / Custom Needs") }, maxLines = 3)
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(name, phone, countText.toIntOrNull() ?: 1, notes) },
        enabled = name.isNotBlank() && phone.length >= 6,
      ) {
        Text("Send Inquiry")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
