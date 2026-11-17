package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgencySettings
import com.example.data.model.AuditLog
import com.example.data.model.Booking
import com.example.data.model.CurrencyCode
import com.example.data.model.ExpenseRecord
import com.example.data.model.InquiryLead
import com.example.data.model.PaymentRecord
import com.example.data.model.Pilgrim
import com.example.data.model.RoberPathCatalog
import com.example.data.model.StaffMember
import com.example.data.model.SupplierRecord
import com.example.data.model.TourGroup
import com.example.data.model.TravelPackage
import com.example.data.model.UserProfile
import com.example.data.repository.AgencyRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class PortalMode(val label: String) {
  ADMIN("Admin & Staff"),
  CUSTOMER("Customer Portal"),
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class AgencyViewModel(
  private val repository: AgencyRepository,
  val currentUserId: String,
) : ViewModel() {

  val isAdminUser: Boolean = repository.isBootstrapAdmin()

  private val _portalMode = MutableStateFlow(if (isAdminUser) PortalMode.ADMIN else PortalMode.CUSTOMER)
  val portalMode: StateFlow<PortalMode> = _portalMode.asStateFlow()

  private val _currency = MutableStateFlow(CurrencyCode.BDT)
  val currency: StateFlow<CurrencyCode> = _currency.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureProfileAndSeedIfAdmin()
        .onFailure { err ->
          Log.w(TAG, "Profile init warning: ${err.message}")
        }
    }
  }

  private fun <T> Flow<T>.toUiState(label: String, initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        emit(UiState.Error(rootError.message ?: "Unable to load $label"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = initial,
      )

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId).toUiState("profile")

  val packagesState: StateFlow<UiState<List<TravelPackage>>> =
    repository.observePackages()
      .map { list -> if (list.isEmpty()) RoberPathCatalog.defaultPackages else list.sortedBy { it.priceBdt } }
      .toUiState("packages", UiState.Success(RoberPathCatalog.defaultPackages))

  val agencySettingsState: StateFlow<UiState<AgencySettings>> =
    repository.observeAgencySettings().toUiState("settings", UiState.Success(AgencySettings()))

  val bookingsState: StateFlow<UiState<List<Booking>>> =
    repository.observeBookings(currentUserId, isAdminUser).toUiState("bookings")

  val pilgrimsState: StateFlow<UiState<List<Pilgrim>>> =
    repository.observePilgrims(currentUserId, isAdminUser).toUiState("pilgrims")

  val paymentsState: StateFlow<UiState<List<PaymentRecord>>> =
    repository.observePayments(currentUserId, isAdminUser).toUiState("payments")

  val inquiriesState: StateFlow<UiState<List<InquiryLead>>> =
    repository.observeInquiries(currentUserId, isAdminUser).toUiState("inquiries")

  val groupsState: StateFlow<UiState<List<TourGroup>>> =
    (if (isAdminUser) repository.observeGroups() else flowOf(emptyList()))
      .toUiState("groups", UiState.Success(emptyList()))

  val expensesState: StateFlow<UiState<List<ExpenseRecord>>> =
    (if (isAdminUser) repository.observeExpenses() else flowOf(emptyList()))
      .toUiState("expenses", UiState.Success(emptyList()))

  val suppliersState: StateFlow<UiState<List<SupplierRecord>>> =
    (if (isAdminUser) repository.observeSuppliers() else flowOf(emptyList()))
      .toUiState("suppliers", UiState.Success(emptyList()))

  val staffState: StateFlow<UiState<List<StaffMember>>> =
    (if (isAdminUser) repository.observeStaffMembers() else flowOf(emptyList()))
      .toUiState("staff", UiState.Success(emptyList()))

  val auditLogsState: StateFlow<UiState<List<AuditLog>>> =
    (if (isAdminUser) repository.observeAuditLogs() else flowOf(emptyList()))
      .toUiState("audit logs", UiState.Success(emptyList()))

  fun setPortalMode(mode: PortalMode) {
    if (isAdminUser || mode == PortalMode.CUSTOMER) {
      _portalMode.value = mode
    }
  }

  fun toggleCurrency() {
    _currency.value = if (_currency.value == CurrencyCode.BDT) CurrencyCode.SAR else CurrencyCode.BDT
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun showMessage(msg: String) {
    _feedbackMessage.value = msg
  }

  fun createBooking(
    customerName: String,
    customerPhone: String,
    pkg: TravelPackage,
    bookingType: String,
    pilgrimCount: Int,
    travelBatchDate: String,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.createBooking(
        customerName = customerName,
        customerPhone = customerPhone,
        pkg = pkg,
        bookingType = bookingType,
        pilgrimCount = pilgrimCount,
        currency = _currency.value.name,
        travelBatchDate = travelBatchDate,
        notes = notes,
      ).onSuccess { code ->
        _feedbackMessage.value = "Booking request $code submitted successfully."
      }.onFailure { err ->
        _feedbackMessage.value = err.message ?: "Failed to create booking."
      }
    }
  }

  fun updateBookingAdmin(
    bookingId: String,
    status: String,
    groupName: String,
    flightInfo: String,
    hotelMakkah: String,
    hotelMadinah: String,
    transportInfo: String,
    moallemName: String,
  ) {
    viewModelScope.launch {
      repository.updateBookingAdmin(
        bookingId = bookingId,
        status = status,
        groupName = groupName,
        flightInfo = flightInfo,
        hotelMakkah = hotelMakkah,
        hotelMadinah = hotelMadinah,
        transportInfo = transportInfo,
        moallemName = moallemName,
      ).onSuccess {
        _feedbackMessage.value = "Booking operations updated."
      }.onFailure { err ->
        _feedbackMessage.value = err.message ?: "Failed to update booking."
      }
    }
  }

  fun savePilgrim(
    bookingId: String,
    fullName: String,
    passportNumber: String,
    passportExpiry: String,
    dateOfBirth: String,
    gender: String,
    relation: String,
    nidNumber: String,
    passportPhotoUri: String,
  ) {
    if (fullName.isBlank() || passportNumber.length < 4 || passportExpiry.length < 4) {
      _feedbackMessage.value = "Please enter valid pilgrim name, passport number, and expiry date."
      return
    }
    viewModelScope.launch {
      repository.savePilgrim(
        bookingId = bookingId,
        fullName = fullName,
        passportNumber = passportNumber,
        passportExpiry = passportExpiry,
        dateOfBirth = dateOfBirth,
        gender = gender,
        relation = relation,
        nidNumber = nidNumber,
        passportPhotoUri = passportPhotoUri,
      ).onSuccess {
        _feedbackMessage.value = "Pilgrim profile saved."
      }.onFailure { err ->
        _feedbackMessage.value = err.message ?: "Failed to save pilgrim."
      }
    }
  }

  fun updatePilgrimOps(
    pilgrimId: String,
    documentStatus: String,
    visaStatus: String,
    groupName: String,
    roomAllocation: String,
  ) {
    viewModelScope.launch {
      repository.updatePilgrimOps(pilgrimId, documentStatus, visaStatus, groupName, roomAllocation)
        .onSuccess { _feedbackMessage.value = "Pilgrim visa & operations updated." }
        .onFailure { _feedbackMessage.value = it.message ?: "Update failed." }
    }
  }

  fun deletePilgrim(pilgrimId: String) {
    viewModelScope.launch {
      repository.deletePilgrim(pilgrimId)
        .onSuccess { _feedbackMessage.value = "Pilgrim removed." }
        .onFailure { _feedbackMessage.value = it.message ?: "Delete failed." }
    }
  }

  fun submitPayment(
    booking: Booking,
    amountBdt: Long,
    method: String,
    referenceNo: String,
    paymentType: String,
    notes: String,
  ) {
    if (amountBdt <= 0L) {
      _feedbackMessage.value = "Enter a valid payment amount greater than zero."
      return
    }
    viewModelScope.launch {
      repository.submitPayment(
        booking = booking,
        amountBdt = amountBdt,
        currency = _currency.value.name,
        method = method,
        referenceNo = referenceNo,
        paymentType = paymentType,
        notes = notes,
      ).onSuccess { receipt ->
        _feedbackMessage.value = "Payment $receipt submitted for verification."
      }.onFailure { err ->
        _feedbackMessage.value = err.message ?: "Payment submission failed."
      }
    }
  }

  fun verifyPayment(
    payment: PaymentRecord,
    newStatus: String,
    bookings: List<Booking>,
    allPayments: List<PaymentRecord>,
  ) {
    val targetBooking = bookings.firstOrNull { it.id == payment.bookingId }
    val bookingPayments = allPayments.filter { it.bookingId == payment.bookingId }
    viewModelScope.launch {
      repository.verifyPaymentAndSyncBooking(payment, newStatus, targetBooking, bookingPayments)
        .onSuccess { _feedbackMessage.value = "Payment ${payment.receiptNumber} marked $newStatus." }
        .onFailure { _feedbackMessage.value = it.message ?: "Verification failed." }
    }
  }

  fun processRefund(
    booking: Booking,
    amountBdt: Long,
    method: String,
    referenceNo: String,
    notes: String,
  ) {
    if (amountBdt <= 0L) {
      _feedbackMessage.value = "Refund amount must be positive."
      return
    }
    viewModelScope.launch {
      repository.processRefund(booking, amountBdt, method, referenceNo, notes)
        .onSuccess { _feedbackMessage.value = "Refund recorded for ${booking.bookingCode}." }
        .onFailure { _feedbackMessage.value = it.message ?: "Refund failed." }
    }
  }

  fun submitInquiry(
    leadName: String,
    phone: String,
    packageInterest: String,
    pilgrimCount: Int,
    source: String,
    notes: String,
  ) {
    if (leadName.isBlank() || phone.length < 3) {
      _feedbackMessage.value = "Name and valid phone number are required."
      return
    }
    viewModelScope.launch {
      repository.submitInquiry(leadName, phone, packageInterest, pilgrimCount, source, notes)
        .onSuccess { _feedbackMessage.value = "Inquiry submitted. Our Dhaka team will contact you." }
        .onFailure { _feedbackMessage.value = it.message ?: "Inquiry failed." }
    }
  }

  fun updateInquiryStatus(inquiryId: String, status: String, followUpDate: String) {
    viewModelScope.launch {
      repository.updateInquiryStatus(inquiryId, status, followUpDate)
        .onSuccess { _feedbackMessage.value = "Lead updated to $status." }
        .onFailure { _feedbackMessage.value = it.message ?: "Lead update failed." }
    }
  }

  fun savePackage(pkg: TravelPackage) {
    viewModelScope.launch {
      repository.savePackage(pkg)
        .onSuccess { _feedbackMessage.value = "Package '${pkg.title}' saved." }
        .onFailure { _feedbackMessage.value = it.message ?: "Failed to save package." }
    }
  }

  fun saveTourGroup(group: TourGroup) {
    viewModelScope.launch {
      repository.saveTourGroup(group)
        .onSuccess { _feedbackMessage.value = "Group ${group.groupCode} saved." }
        .onFailure { _feedbackMessage.value = it.message ?: "Failed to save group." }
    }
  }

  fun addExpense(
    title: String,
    category: String,
    amountBdt: Long,
    supplierName: String,
    paymentMethod: String,
    expenseDate: String,
    notes: String,
  ) {
    if (title.isBlank() || amountBdt <= 0L) {
      _feedbackMessage.value = "Provide expense title and valid amount."
      return
    }
    viewModelScope.launch {
      repository.addExpense(title, category, amountBdt, supplierName, paymentMethod, expenseDate, notes)
        .onSuccess { _feedbackMessage.value = "Expense recorded." }
        .onFailure { _feedbackMessage.value = it.message ?: "Failed to add expense." }
    }
  }

  fun saveSupplier(
    name: String,
    category: String,
    contactPerson: String,
    phone: String,
    city: String,
    payableBdt: Long,
    paidBdt: Long,
  ) {
    if (name.isBlank()) {
      _feedbackMessage.value = "Supplier name is required."
      return
    }
    viewModelScope.launch {
      repository.saveSupplier(name, category, contactPerson, phone, city, payableBdt, paidBdt)
        .onSuccess { _feedbackMessage.value = "Supplier saved." }
        .onFailure { _feedbackMessage.value = it.message ?: "Failed to save supplier." }
    }
  }

  fun addStaffMember(
    fullName: String,
    email: String,
    phone: String,
    role: String,
    department: String,
    salaryBdt: Long,
  ) {
    if (fullName.isBlank() || email.isBlank()) {
      _feedbackMessage.value = "Staff name and email are required."
      return
    }
    viewModelScope.launch {
      repository.addStaffMember(fullName, email, phone, role, department, salaryBdt)
        .onSuccess { _feedbackMessage.value = "Staff member added." }
        .onFailure { _feedbackMessage.value = it.message ?: "Failed to add staff." }
    }
  }

  fun updateStaffAttendance(staffId: String, attendanceStatus: String) {
    viewModelScope.launch {
      repository.updateStaffAttendance(staffId, attendanceStatus)
        .onSuccess { _feedbackMessage.value = "Attendance updated." }
        .onFailure { _feedbackMessage.value = it.message ?: "Attendance update failed." }
    }
  }

  fun updateAgencySettings(settings: AgencySettings) {
    viewModelScope.launch {
      repository.updateAgencySettings(settings)
        .onSuccess { _feedbackMessage.value = "Agency settings saved." }
        .onFailure { _feedbackMessage.value = it.message ?: "Settings update failed." }
    }
  }

  companion object {
    private const val TAG = "AgencyVM"

    fun isPassportExpiringSoon(expiryIso: String, minMonths: Int = 6): Boolean {
      return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val expiryDate = sdf.parse(expiryIso.trim()) ?: return true
        val threshold = Calendar.getInstance().apply { add(Calendar.MONTH, minMonths) }.time
        expiryDate.before(threshold)
      } catch (_: Exception) {
        false
      }
    }
  }
}
