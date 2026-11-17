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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.model.AttendanceStatus
import com.example.data.model.AuditLog
import com.example.data.model.CurrencyCode
import com.example.data.model.FlightType
import com.example.data.model.PackageTier
import com.example.data.model.RoberPathCatalog
import com.example.data.model.ServiceType
import com.example.data.model.StaffMember
import com.example.data.model.SupplierCategory
import com.example.data.model.SupplierRecord
import com.example.data.model.TravelPackage
import com.example.data.model.UserRole
import com.example.ui.components.StatusPill
import com.example.ui.components.statusToneFor
import com.example.util.PdfReportGenerator

@Composable
fun AdminAgencyHubScreen(
  packages: List<TravelPackage>,
  staffMembers: List<StaffMember>,
  suppliers: List<SupplierRecord>,
  auditLogs: List<AuditLog>,
  settings: AgencySettings,
  currency: CurrencyCode,
  onSavePackage: (TravelPackage) -> Unit,
  onAddStaff: (String, String, String, String, String, Long) -> Unit,
  onUpdateAttendance: (String, String) -> Unit,
  onAddSupplier: (String, String, String, String, String, Long, Long) -> Unit,
  onSaveSettings: (AgencySettings) -> Unit,
) {
  var hubTab by remember { mutableStateOf("PACKAGES") }
  var editingPackage by remember { mutableStateOf<TravelPackage?>(null) }
  var showAddStaffDialog by remember { mutableStateOf(false) }
  var showAddSupplierDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val tabs = listOf(
        "PACKAGES" to "Packages (${packages.size})",
        "STAFF" to "Staff & RBAC (${staffMembers.size})",
        "SUPPLIERS" to "Suppliers (${suppliers.size})",
        "SETTINGS" to "Settings & Audit",
      )
      items(tabs) { (key, label) ->
        FilterChip(selected = hubTab == key, onClick = { hubTab = key }, label = { Text(label) })
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (hubTab) {
        "PACKAGES" -> {
          item {
            Button(
              onClick = {
                editingPackage = TravelPackage(
                  title = "New Ramadan Umrah Package",
                  subtitle = "Special 2026-27 Kafela",
                  serviceType = ServiceType.UMRAH.name,
                  tier = PackageTier.STANDARD.name,
                  flightType = FlightType.DIRECT.name,
                  priceBdt = 185000L,
                  priceSar = 5780L,
                  makkahHotelDistance = "400–500m from Haram",
                  madinahHotelDistance = "300–400m from Masjid an-Nabawi",
                  inclusions = RoberPathCatalog.standardInclusions,
                )
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Default.Add, contentDescription = "New Package")
              Text("Create Umrah / Hajj Package")
            }
          }
          items(packages, key = { it.id.ifBlank { it.title } }) { pkg ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text(pkg.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                  Text(
                    PdfReportGenerator.formatAmount(pkg.priceBdt, currency.name, settings.sarToBdtRate),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Text("Makkah: ${pkg.makkahHotelDistance} • Madinah: ${pkg.madinahHotelDistance}", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  StatusPill(if (pkg.active) "ACTIVE" else "PAUSED", if (pkg.active) statusToneFor("VERIFIED") else statusToneFor("PENDING"))
                  OutlinedButton(onClick = { editingPackage = pkg }) { Text("Edit Price / Info") }
                }
              }
            }
          }
        }

        "STAFF" -> {
          item {
            Button(onClick = { showAddStaffDialog = true }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.Add, contentDescription = "Add Staff")
              Text("Add Agency Staff Member")
            }
          }
          items(staffMembers, key = { it.id.ifBlank { it.email } }) { stf ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Column {
                    Text(stf.fullName, style = MaterialTheme.typography.titleSmall)
                    Text("${stf.role} • ${stf.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  StatusPill(stf.attendanceStatus, statusToneFor(stf.attendanceStatus))
                }
                Text("Salary: ${PdfReportGenerator.formatBdt(stf.salaryBdt)} • Leave Balance: ${stf.leaveDaysRemaining} days", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(AttendanceStatus.entries) { att ->
                    FilterChip(
                      selected = stf.attendanceStatus == att.name,
                      onClick = { onUpdateAttendance(stf.id, att.name) },
                      label = { Text(att.label, style = MaterialTheme.typography.labelSmall) },
                    )
                  }
                }
              }
            }
          }
          item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("8-Role Permission Matrix (Backend Enforced)", style = MaterialTheme.typography.titleMedium)
          }
          items(RoberPathCatalog.rolePermissions, key = { it.role.name }) { spec ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(spec.role.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(spec.capabilities.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }

        "SUPPLIERS" -> {
          item {
            Button(onClick = { showAddSupplierDialog = true }, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.Add, contentDescription = "Add Supplier")
              Text("Add Airline / Hotel / Visa Supplier")
            }
          }
          items(suppliers, key = { it.id.ifBlank { it.name } }) { sup ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${sup.name} (${sup.city})", style = MaterialTheme.typography.titleSmall)
                Text("${sup.category} • Contact: ${sup.contactPerson} (${sup.phone})", style = MaterialTheme.typography.bodySmall)
                val due = (sup.payableBdt - sup.paidBdt).coerceAtLeast(0L)
                Text(
                  "Payable: ${PdfReportGenerator.formatBdt(sup.payableBdt)} • Paid: ${PdfReportGenerator.formatBdt(sup.paidBdt)} • Due: ${PdfReportGenerator.formatBdt(due)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        "SETTINGS" -> {
          item {
            AgencySettingsCard(settings = settings, onSave = onSaveSettings)
          }
          item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Immutable Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
          }
          items(auditLogs.take(20), key = { it.id.ifBlank { it.action + it.details } }) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("${log.action} • ${log.targetModule}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(log.details, style = MaterialTheme.typography.bodySmall)
                Text("Actor: ${log.actorEmail} (${log.actorRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }

  editingPackage?.let { pkg ->
    EditPackageDialog(
      pkg = pkg,
      onDismiss = { editingPackage = null },
      onSave = { updated ->
        onSavePackage(updated)
        editingPackage = null
      },
    )
  }

  if (showAddStaffDialog) {
    AddStaffDialog(
      onDismiss = { showAddStaffDialog = false },
      onSave = { name, email, phone, role, dept, salary ->
        onAddStaff(name, email, phone, role, dept, salary)
        showAddStaffDialog = false
      },
    )
  }

  if (showAddSupplierDialog) {
    AddSupplierDialog(
      onDismiss = { showAddSupplierDialog = false },
      onSave = { name, cat, contact, phone, city, payable, paid ->
        onAddSupplier(name, cat, contact, phone, city, payable, paid)
        showAddSupplierDialog = false
      },
    )
  }
}

@Composable
private fun AgencySettingsCard(
  settings: AgencySettings,
  onSave: (AgencySettings) -> Unit,
) {
  var sarRate by remember(settings.sarToBdtRate) { mutableStateOf(settings.sarToBdtRate.toString()) }
  var minMonths by remember(settings.minPassportMonths) { mutableStateOf(settings.minPassportMonths.toString()) }
  var bkash by remember(settings.bkashMerchant) { mutableStateOf(settings.bkashMerchant) }
  var nagad by remember(settings.nagadMerchant) { mutableStateOf(settings.nagadMerchant) }
  var bank by remember(settings.bankAccountSummary) { mutableStateOf(settings.bankAccountSummary) }

  Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Configurable Agency Rules & Accounts", style = MaterialTheme.typography.titleMedium)
      Text("Agent #${settings.agentNumber} • License #${settings.licenseNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = sarRate,
          onValueChange = { sarRate = it },
          label = { Text("1 SAR in BDT") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = minMonths,
          onValueChange = { minMonths = it.filter { c -> c.isDigit() } },
          label = { Text("Min Passport (Mo)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }
      OutlinedTextField(value = bkash, onValueChange = { bkash = it }, label = { Text("bKash Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = nagad, onValueChange = { nagad = it }, label = { Text("Nagad Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Bank Account Summary") }, modifier = Modifier.fillMaxWidth())
      Button(
        onClick = {
          onSave(
            settings.copy(
              sarToBdtRate = sarRate.toDoubleOrNull()?.coerceIn(1.0, 500.0) ?: 32.0,
              minPassportMonths = minMonths.toIntOrNull()?.coerceIn(1, 24) ?: 6,
              bkashMerchant = bkash,
              nagadMerchant = nagad,
              bankAccountSummary = bank,
            )
          )
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Save Agency Settings")
      }
    }
  }
}

@Composable
private fun EditPackageDialog(
  pkg: TravelPackage,
  onDismiss: () -> Unit,
  onSave: (TravelPackage) -> Unit,
) {
  var title by remember { mutableStateOf(pkg.title) }
  var priceBdt by remember { mutableStateOf(pkg.priceBdt.toString()) }
  var priceSar by remember { mutableStateOf(pkg.priceSar.toString()) }
  var makkahDist by remember { mutableStateOf(pkg.makkahHotelDistance) }
  var madinahDist by remember { mutableStateOf(pkg.madinahHotelDistance) }
  var active by remember { mutableStateOf(pkg.active) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Configure Package") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Package Title") }, singleLine = true)
        OutlinedTextField(value = priceBdt, onValueChange = { priceBdt = it.filter { c -> c.isDigit() } }, label = { Text("Price BDT (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(value = priceSar, onValueChange = { priceSar = it.filter { c -> c.isDigit() } }, label = { Text("Price SAR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(value = makkahDist, onValueChange = { makkahDist = it }, label = { Text("Makkah Distance") }, singleLine = true)
        OutlinedTextField(value = madinahDist, onValueChange = { madinahDist = it }, label = { Text("Madinah Distance") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("Package Active for Customers")
          Switch(checked = active, onCheckedChange = { active = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            pkg.copy(
              title = title,
              priceBdt = priceBdt.toLongOrNull() ?: pkg.priceBdt,
              priceSar = priceSar.toLongOrNull() ?: pkg.priceSar,
              makkahHotelDistance = makkahDist,
              madinahHotelDistance = madinahDist,
              active = active,
            )
          )
        },
        enabled = title.isNotBlank(),
      ) { Text("Save Package") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddStaffDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, Long) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("+8801") }
  var role by remember { mutableStateOf(UserRole.SALES_EXECUTIVE.name) }
  var dept by remember { mutableStateOf("Sales & CRM") }
  var salary by remember { mutableStateOf("35000") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Agency Staff") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(UserRole.entries.filter { it.isStaffRole }) { r ->
            FilterChip(selected = role == r.name, onClick = { role = r.name }, label = { Text(r.label) })
          }
        }
        OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("Department") }, singleLine = true)
        OutlinedTextField(value = salary, onValueChange = { salary = it.filter { c -> c.isDigit() } }, label = { Text("Monthly Salary (BDT)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, email, phone, role, dept, salary.toLongOrNull() ?: 0L) },
        enabled = name.isNotBlank() && email.contains("@"),
      ) { Text("Add Staff") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddSupplierDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, Long, Long) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var cat by remember { mutableStateOf(SupplierCategory.HOTEL_MAKKAH.name) }
  var contact by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("Makkah") }
  var payable by remember { mutableStateOf("500000") }
  var paid by remember { mutableStateOf("250000") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Supplier / Agent") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name") }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(SupplierCategory.entries) { c ->
            FilterChip(selected = cat == c.name, onClick = { cat = c.name }, label = { Text(c.label) })
          }
        }
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Person") }, singleLine = true)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
        OutlinedTextField(value = payable, onValueChange = { payable = it.filter { c -> c.isDigit() } }, label = { Text("Total Payable (BDT)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(value = paid, onValueChange = { paid = it.filter { c -> c.isDigit() } }, label = { Text("Paid So Far (BDT)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, cat, contact, phone, city, payable.toLongOrNull() ?: 0L, paid.toLongOrNull() ?: 0L) },
        enabled = name.isNotBlank(),
      ) { Text("Save Supplier") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
