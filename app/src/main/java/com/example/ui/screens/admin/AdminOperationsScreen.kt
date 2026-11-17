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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AgencySettings
import com.example.data.model.DocumentStatus
import com.example.data.model.Pilgrim
import com.example.data.model.RoberPathCatalog
import com.example.data.model.TourGroup
import com.example.data.model.VisaStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PassportExpiryWarningBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.ui.viewmodel.AgencyViewModel

@Composable
fun AdminOperationsScreen(
  pilgrims: List<Pilgrim>,
  groups: List<TourGroup>,
  settings: AgencySettings,
  onUpdatePilgrimOps: (String, String, String, String, String) -> Unit,
) {
  var opsTab by remember { mutableStateOf("VISA") }
  var editingPilgrim by remember { mutableStateOf<Pilgrim?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val tabs = listOf(
        "VISA" to "Passports & Visas",
        "FLIGHTS" to "Flights & Tickets",
        "HOTELS" to "Hotels & Rooms",
        "ITINERARY" to "Transport & 14-Day Plan",
      )
      items(tabs) { (key, label) ->
        FilterChip(selected = opsTab == key, onClick = { opsTab = key }, label = { Text(label) })
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (opsTab) {
        "VISA" -> {
          if (pilgrims.isEmpty()) {
            item { EmptyStateCard(Icons.Default.Badge, "No Pilgrim Documents", "Pilgrim passports and visa applications appear here.") }
          } else {
            items(pilgrims, key = { it.id.ifBlank { it.passportNumber } }) { p ->
              val expiring = AgencyViewModel.isPassportExpiringSoon(p.passportExpiry, settings.minPassportMonths)
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("${p.fullName} • PP: ${p.passportNumber}", style = MaterialTheme.typography.titleSmall)
                  Text("Expiry: ${p.passportExpiry} • Group: ${p.groupName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.bodySmall)
                  if (expiring) PassportExpiryWarningBadge(p.passportExpiry)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("Doc: ${p.documentStatus}", statusToneFor(p.documentStatus))
                    StatusPill("Visa: ${p.visaStatus}", statusToneFor(p.visaStatus))
                  }
                  OutlinedButton(onClick = { editingPilgrim = p }, modifier = Modifier.fillMaxWidth()) {
                    Text("Verify Docs / Update Visa & Room")
                  }
                }
              }
            }
          }
        }

        "FLIGHTS" -> {
          if (groups.isEmpty()) {
            item { EmptyStateCard(Icons.Default.FlightTakeoff, "No Flight Schedules", "Create a Kafela group to manage flights and tickets.") }
          } else {
            items(groups, key = { it.id.ifBlank { it.groupCode } }) { grp ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("${grp.airline} • ${grp.flightNumber}", style = MaterialTheme.typography.titleMedium)
                  Text("Kafela: ${grp.name} (${grp.groupCode})", style = MaterialTheme.typography.bodySmall)
                  Text("Departure: ${grp.departureDate} • Return: ${grp.returnDate}", style = MaterialTheme.typography.bodySmall)
                  Text("Seats Allocated: ${grp.assignedCount} / ${grp.capacity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }

        "HOTELS" -> {
          if (groups.isEmpty()) {
            item { EmptyStateCard(Icons.Default.Hotel, "No Hotel Allocations", "Kafela Makkah & Madinah hotels appear here.") }
          } else {
            items(groups, key = { it.id.ifBlank { it.groupCode } }) { grp ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("${grp.groupCode} • ${grp.packageTitle}", style = MaterialTheme.typography.titleSmall)
                  Text("Makkah (7 Nights): ${grp.makkahHotel}", style = MaterialTheme.typography.bodySmall)
                  Text("Madinah (6 Nights): ${grp.madinahHotel}", style = MaterialTheme.typography.bodySmall)
                  val assignedRooms = pilgrims.filter { it.groupName == grp.name && it.roomAllocation.isNotBlank() }
                  Text("Roomed Pilgrims: ${assignedRooms.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
              }
            }
          }
        }

        "ITINERARY" -> {
          items(groups, key = { it.id.ifBlank { it.groupCode } }) { grp ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  StatusPill(grp.groupCode, statusToneFor(grp.groupStatus))
                }
                Text("Transport: ${grp.transportCoach}", style = MaterialTheme.typography.titleSmall)
                Text("Moallem / Guide: ${grp.moallemName}", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Standard 14-Day Rober Path Safar Schedule", style = MaterialTheme.typography.titleMedium)
          }
          items(RoberPathCatalog.fourteenDayItinerary, key = { it.dayNumber }) { day ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Day ${day.dayNumber}: ${day.titleEn} (${day.titleBn})", style = MaterialTheme.typography.titleSmall)
                  Text(day.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(day.city, statusToneFor("READY"))
              }
            }
          }
        }
      }
    }
  }

  editingPilgrim?.let { p ->
    EditPilgrimOpsDialog(
      pilgrim = p,
      onDismiss = { editingPilgrim = null },
      onSave = { doc, visa, grp, room ->
        onUpdatePilgrimOps(p.id, doc, visa, grp, room)
        editingPilgrim = null
      },
    )
  }
}

@Composable
private fun EditPilgrimOpsDialog(
  pilgrim: Pilgrim,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String) -> Unit,
) {
  var docStatus by remember { mutableStateOf(pilgrim.documentStatus) }
  var visaStatus by remember { mutableStateOf(pilgrim.visaStatus) }
  var groupName by remember { mutableStateOf(pilgrim.groupName) }
  var roomAlloc by remember { mutableStateOf(pilgrim.roomAllocation) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Verify: ${pilgrim.fullName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Document Verification", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(DocumentStatus.entries) { st ->
            FilterChip(selected = docStatus == st.name, onClick = { docStatus = st.name }, label = { Text(st.label) })
          }
        }
        Text("Visa Status", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(VisaStatus.entries) { st ->
            FilterChip(selected = visaStatus == st.name, onClick = { visaStatus = st.name }, label = { Text(st.label) })
          }
        }
        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Assign Kafela Group") }, singleLine = true)
        OutlinedTextField(value = roomAlloc, onValueChange = { roomAlloc = it }, label = { Text("Hotel Room (e.g. Makkah Rm 402)") }, singleLine = true)
      }
    },
    confirmButton = {
      Button(onClick = { onSave(docStatus, visaStatus, groupName, roomAlloc) }) {
        Text("Save Status")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
