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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencySettings
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CurrencyCode
import com.example.data.model.GroupStatus
import com.example.data.model.InquiryLead
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.Pilgrim
import com.example.data.model.TourGroup
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.util.PdfReportGenerator

@Composable
fun AdminCrmBookingsScreen(
  bookings: List<Booking>,
  inquiries: List<InquiryLead>,
  pilgrims: List<Pilgrim>,
  groups: List<TourGroup>,
  currency: CurrencyCode,
  settings: AgencySettings,
  onUpdateBookingAdmin: (String, String, String, String, String, String, String, String) -> Unit,
  onSubmitInquiry: (String, String, String, Int, String, String) -> Unit,
  onUpdateInquiryStatus: (String, String, String) -> Unit,
  onSaveTourGroup: (TourGroup) -> Unit,
) {
  var subTab by remember { mutableStateOf("BOOKINGS") }
  var searchQuery by remember { mutableStateOf("") }
  var editingBooking by remember { mutableStateOf<Booking?>(null) }
  var showNewLeadDialog by remember { mutableStateOf(false) }
  var showNewGroupDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val tabs = listOf(
        "BOOKINGS" to "Bookings (${bookings.size})",
        "LEADS" to "CRM Leads (${inquiries.size})",
        "PILGRIMS" to "Pilgrims (${pilgrims.size})",
        "GROUPS" to "Groups (${groups.size})",
      )
      items(tabs) { (key, label) ->
        FilterChip(selected = subTab == key, onClick = { subTab = key }, label = { Text(label) })
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search name, phone, code, passport...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (subTab) {
        "BOOKINGS" -> {
          val filtered = bookings.filter {
            searchQuery.isBlank() ||
              it.customerName.contains(searchQuery, true) ||
              it.bookingCode.contains(searchQuery, true) ||
              it.packageTitle.contains(searchQuery, true)
          }
          if (filtered.isEmpty()) {
            item {
              EmptyStateCard(Icons.Default.Search, "No Bookings Found", "Customer booking requests will appear here in real time.")
            }
          } else {
            items(filtered, key = { it.id.ifBlank { it.bookingCode } }) { bk ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${bk.bookingCode} • ${bk.customerName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    StatusPill(bk.status, statusToneFor(bk.status))
                  }
                  Text("${bk.packageTitle} (${bk.pilgrimCount} pax • ${bk.bookingType})", style = MaterialTheme.typography.bodySmall)
                  Text(
                    "Total: ${PdfReportGenerator.formatAmount(bk.totalAmountBdt, currency.name, settings.sarToBdtRate)} • Paid: ${PdfReportGenerator.formatAmount(bk.paidAmountBdt, currency.name, settings.sarToBdtRate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text("Group: ${bk.groupName.ifBlank { "Unassigned" }} • Phone: ${bk.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedButton(onClick = { editingBooking = bk }, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage Status & Kafela Assignment")
                  }
                }
              }
            }
          }
        }

        "LEADS" -> {
          item {
            Button(onClick = { showNewLeadDialog = true }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.Add, contentDescription = "Add Lead")
              Text("Add Walk-In / WhatsApp Lead")
            }
          }
          val filtered = inquiries.filter {
            searchQuery.isBlank() || it.leadName.contains(searchQuery, true) || it.phone.contains(searchQuery, true)
          }
          items(filtered, key = { it.id.ifBlank { it.leadName + it.phone } }) { lead ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${lead.leadName} (${lead.pilgrimCount} pax)", style = MaterialTheme.typography.titleSmall)
                  StatusPill(lead.leadStatus, statusToneFor(lead.leadStatus))
                }
                Text("${lead.phone} • ${lead.source} • ${lead.packageInterest}", style = MaterialTheme.typography.bodySmall)
                if (lead.notes.isNotBlank()) {
                  Text(lead.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(LeadStatus.entries) { st ->
                    FilterChip(
                      selected = lead.leadStatus == st.name,
                      onClick = { onUpdateInquiryStatus(lead.id, st.name, "2026-11-25") },
                      label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                    )
                  }
                }
              }
            }
          }
        }

        "PILGRIMS" -> {
          val filtered = pilgrims.filter {
            searchQuery.isBlank() || it.fullName.contains(searchQuery, true) || it.passportNumber.contains(searchQuery, true)
          }
          if (filtered.isEmpty()) {
            item { EmptyStateCard(Icons.Default.Search, "No Pilgrim Records", "Pilgrims added by customers appear here.") }
          } else {
            items(filtered, key = { it.id.ifBlank { it.passportNumber } }) { p ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("${p.fullName} (${p.gender})", style = MaterialTheme.typography.titleSmall)
                  Text("Passport: ${p.passportNumber} • Exp: ${p.passportExpiry} • NID: ${p.nidNumber.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("Doc: ${p.documentStatus}", statusToneFor(p.documentStatus))
                    StatusPill("Visa: ${p.visaStatus}", statusToneFor(p.visaStatus))
                  }
                }
              }
            }
          }
        }

        "GROUPS" -> {
          item {
            Button(onClick = { showNewGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.Add, contentDescription = "Create Group")
              Text("Create Kafela / Tour Group")
            }
          }
          items(groups, key = { it.id.ifBlank { it.groupCode } }) { grp ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${grp.groupCode} • ${grp.name}", style = MaterialTheme.typography.titleSmall)
                  StatusPill(grp.groupStatus, statusToneFor(grp.groupStatus))
                }
                Text("Package: ${grp.packageTitle} • ${grp.departureDate} → ${grp.returnDate}", style = MaterialTheme.typography.bodySmall)
                Text("Flight: ${grp.airline} ${grp.flightNumber} • Moallem: ${grp.moallemName}", style = MaterialTheme.typography.bodySmall)
                Text("Hotels: ${grp.makkahHotel} / ${grp.madinahHotel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }

  editingBooking?.let { bk ->
    EditBookingDialog(
      booking = bk,
      onDismiss = { editingBooking = null },
      onSave = { status, grp, flight, makkah, madinah, transport, moallem ->
        onUpdateBookingAdmin(bk.id, status, grp, flight, makkah, madinah, transport, moallem)
        editingBooking = null
      },
    )
  }

  if (showNewLeadDialog) {
    AddLeadDialog(
      onDismiss = { showNewLeadDialog = false },
      onSave = { name, phone, interest, count, src, notes ->
        onSubmitInquiry(name, phone, interest, count, src, notes)
        showNewLeadDialog = false
      },
    )
  }

  if (showNewGroupDialog) {
    AddTourGroupDialog(
      onDismiss = { showNewGroupDialog = false },
      onSave = { grp ->
        onSaveTourGroup(grp)
        showNewGroupDialog = false
      },
    )
  }
}

@Composable
private fun EditBookingDialog(
  booking: Booking,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, String, String) -> Unit,
) {
  var status by remember { mutableStateOf(booking.status) }
  var groupName by remember { mutableStateOf(booking.groupName) }
  var flight by remember { mutableStateOf(booking.flightInfo) }
  var makkah by remember { mutableStateOf(booking.hotelMakkah) }
  var madinah by remember { mutableStateOf(booking.hotelMadinah) }
  var transport by remember { mutableStateOf(booking.transportInfo) }
  var moallem by remember { mutableStateOf(booking.moallemName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Manage ${booking.bookingCode}") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Status", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(BookingStatus.entries) { st ->
            FilterChip(selected = status == st.name, onClick = { status = st.name }, label = { Text(st.label) })
          }
        }
        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Kafela / Group Name") }, singleLine = true)
        OutlinedTextField(value = flight, onValueChange = { flight = it }, label = { Text("Flight & PNR") }, singleLine = true)
        OutlinedTextField(value = makkah, onValueChange = { makkah = it }, label = { Text("Makkah Hotel & Room") }, singleLine = true)
        OutlinedTextField(value = madinah, onValueChange = { madinah = it }, label = { Text("Madinah Hotel & Room") }, singleLine = true)
        OutlinedTextField(value = transport, onValueChange = { transport = it }, label = { Text("Coach / Transport") }, singleLine = true)
        OutlinedTextField(value = moallem, onValueChange = { moallem = it }, label = { Text("Assigned Moallem") }, singleLine = true)
      }
    },
    confirmButton = {
      Button(onClick = { onSave(status, groupName, flight, makkah, madinah, transport, moallem) }) {
        Text("Save Changes")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddLeadDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, String, Int, String, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("+8801") }
  var interest by remember { mutableStateOf("Direct Flight (Standard)") }
  var countText by remember { mutableStateOf("2") }
  var source by remember { mutableStateOf(LeadSource.WHATSAPP.name) }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New CRM Lead") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Lead Name") }, singleLine = true)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone / WhatsApp") }, singleLine = true)
        OutlinedTextField(value = interest, onValueChange = { interest = it }, label = { Text("Package Interest") }, singleLine = true)
        OutlinedTextField(
          value = countText,
          onValueChange = { countText = it.filter { c -> c.isDigit() } },
          label = { Text("Pilgrim Count") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(LeadSource.entries) { src ->
            FilterChip(selected = source == src.name, onClick = { source = src.name }, label = { Text(src.label) })
          }
        }
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Follow-up Notes") })
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, phone, interest, countText.toIntOrNull() ?: 1, source, notes) },
        enabled = name.isNotBlank() && phone.length >= 6,
      ) { Text("Create Lead") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddTourGroupDialog(
  onDismiss: () -> Unit,
  onSave: (TourGroup) -> Unit,
) {
  var code by remember { mutableStateOf("KAF-FEB-2027") }
  var name by remember { mutableStateOf("Shaban Umrah Kafela") }
  var pkgTitle by remember { mutableStateOf("Direct Flight (Standard)") }
  var dep by remember { mutableStateOf("2027-02-05") }
  var ret by remember { mutableStateOf("2027-02-19") }
  var airline by remember { mutableStateOf("Biman Bangladesh Airlines") }
  var flight by remember { mutableStateOf("BG-335") }
  var makkah by remember { mutableStateOf("Al Kiswah Towers (500m)") }
  var madinah by remember { mutableStateOf("Saja Al Madinah (600m)") }
  var coach by remember { mutableStateOf("SAPTCO VIP Coach") }
  var moallem by remember { mutableStateOf("Maulana Tariqul Islam") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Tour Group (Kafela)") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Group Code") }, singleLine = true)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Kafela Name") }, singleLine = true)
        OutlinedTextField(value = pkgTitle, onValueChange = { pkgTitle = it }, label = { Text("Package") }, singleLine = true)
        OutlinedTextField(value = dep, onValueChange = { dep = it }, label = { Text("Departure (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(value = ret, onValueChange = { ret = it }, label = { Text("Return (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(value = airline, onValueChange = { airline = it }, label = { Text("Airline") }, singleLine = true)
        OutlinedTextField(value = flight, onValueChange = { flight = it }, label = { Text("Flight No") }, singleLine = true)
        OutlinedTextField(value = makkah, onValueChange = { makkah = it }, label = { Text("Makkah Hotel") }, singleLine = true)
        OutlinedTextField(value = madinah, onValueChange = { madinah = it }, label = { Text("Madinah Hotel") }, singleLine = true)
        OutlinedTextField(value = coach, onValueChange = { coach = it }, label = { Text("Transport Coach") }, singleLine = true)
        OutlinedTextField(value = moallem, onValueChange = { moallem = it }, label = { Text("Moallem Name") }, singleLine = true)
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            TourGroup(
              groupCode = code,
              name = name,
              packageTitle = pkgTitle,
              departureDate = dep,
              returnDate = ret,
              airline = airline,
              flightNumber = flight,
              makkahHotel = makkah,
              madinahHotel = madinah,
              transportCoach = coach,
              moallemName = moallem,
              capacity = 45,
              assignedCount = 0,
              groupStatus = GroupStatus.PLANNING.name,
            )
          )
        },
        enabled = code.isNotBlank() && name.isNotBlank(),
      ) { Text("Create Group") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
