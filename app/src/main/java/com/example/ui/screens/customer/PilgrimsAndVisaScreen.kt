package com.example.ui.screens.customer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AgencySettings
import com.example.data.model.Booking
import com.example.data.model.Gender
import com.example.data.model.Pilgrim
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PassportExpiryWarningBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.ui.viewmodel.AgencyViewModel

@Composable
fun PilgrimsAndVisaScreen(
  pilgrims: List<Pilgrim>,
  bookings: List<Booking>,
  settings: AgencySettings,
  onSavePilgrim: (
    bookingId: String,
    fullName: String,
    passportNumber: String,
    passportExpiry: String,
    dateOfBirth: String,
    gender: String,
    relation: String,
    nidNumber: String,
    passportPhotoUri: String,
  ) -> Unit,
  onDeletePilgrim: (String) -> Unit,
) {
  var showAddDialog by remember { mutableStateOf(false) }

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
          Text("Pilgrims, Passports & Visa", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "Keep pilgrim records & passport scans (min ${settings.minPassportMonths} months validity)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = { showAddDialog = true },
          modifier = Modifier.testTag("add_pilgrim_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Pilgrim", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Pilgrim")
        }
      }
    }

    if (pilgrims.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.Badge,
          title = "No Pilgrims Added Yet",
          description = "Add passport details and upload your passport photo page via Android Photo Picker.",
        )
      }
    } else {
      items(pilgrims, key = { it.id.ifBlank { it.passportNumber } }) { pilgrim ->
        val expiringSoon = AgencyViewModel.isPassportExpiringSoon(
          pilgrim.passportExpiry,
          settings.minPassportMonths,
        )
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(pilgrim.fullName, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Passport: ${pilgrim.passportNumber} • Exp: ${pilgrim.passportExpiry}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
              IconButton(onClick = { onDeletePilgrim(pilgrim.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Pilgrim")
              }
            }
            if (expiringSoon) {
              PassportExpiryWarningBadge(pilgrim.passportExpiry)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              StatusPill("Docs: ${pilgrim.documentStatus}", statusToneFor(pilgrim.documentStatus))
              StatusPill("Visa: ${pilgrim.visaStatus}", statusToneFor(pilgrim.visaStatus))
            }
            if (pilgrim.groupName.isNotBlank() || pilgrim.roomAllocation.isNotBlank()) {
              Text(
                text = "Group: ${pilgrim.groupName.ifBlank { "Pending" }} • Room: ${pilgrim.roomAllocation.ifBlank { "Pending" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (pilgrim.passportPhotoUri.isNotBlank()) {
              AsyncImage(
                model = Uri.parse(pilgrim.passportPhotoUri),
                contentDescription = "Passport attachment for ${pilgrim.fullName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(110.dp)
                  .clip(RoundedCornerShape(10.dp)),
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddPilgrimDialog(
      defaultBookingId = bookings.firstOrNull()?.id ?: "",
      onDismiss = { showAddDialog = false },
      onSave = { bookingId, fullName, passportNo, expiry, dob, gender, relation, nid, photoUri ->
        onSavePilgrim(bookingId, fullName, passportNo, expiry, dob, gender, relation, nid, photoUri)
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun AddPilgrimDialog(
  defaultBookingId: String,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, String, String, String, String) -> Unit,
) {
  var fullName by remember { mutableStateOf("") }
  var passportNo by remember { mutableStateOf("") }
  var expiry by remember { mutableStateOf("2028-10-15") }
  var dob by remember { mutableStateOf("1985-04-12") }
  var gender by remember { mutableStateOf(Gender.MALE.name) }
  var relation by remember { mutableStateOf("Self") }
  var nid by remember { mutableStateOf("") }
  var photoUri by remember { mutableStateOf("") }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri -> if (uri != null) photoUri = uri.toString() },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Pilgrim & Passport") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name (as in Passport)") }, singleLine = true)
        OutlinedTextField(value = passportNo, onValueChange = { passportNo = it }, label = { Text("Passport Number") }, singleLine = true)
        OutlinedTextField(value = expiry, onValueChange = { expiry = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, singleLine = true)
        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Gender.entries.forEach { g ->
            FilterChip(
              selected = gender == g.name,
              onClick = { gender = g.name },
              label = { Text(g.label) },
            )
          }
        }
        OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation (Self / Spouse / Parent)") }, singleLine = true)
        OutlinedTextField(value = nid, onValueChange = { nid = it }, label = { Text("NID / Birth Reg No.") }, singleLine = true)
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_passport_photo_button"),
        ) {
          Icon(Icons.Default.UploadFile, contentDescription = "Pick Passport Photo", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (photoUri.isBlank()) "Attach Passport Scan (Photo Picker)" else "Passport Image Attached ✓")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(defaultBookingId, fullName, passportNo, expiry, dob, gender, relation, nid, photoUri) },
        enabled = fullName.isNotBlank() && passportNo.length >= 4 && expiry.length >= 4,
      ) {
        Text("Save Pilgrim")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
