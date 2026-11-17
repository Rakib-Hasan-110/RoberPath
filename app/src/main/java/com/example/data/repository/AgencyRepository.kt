package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AgencySettings
import com.example.data.model.AttendanceStatus
import com.example.data.model.AuditLog
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CurrencyCode
import com.example.data.model.DocumentStatus
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.GroupStatus
import com.example.data.model.InquiryLead
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentType
import com.example.data.model.Pilgrim
import com.example.data.model.RoberPathCatalog
import com.example.data.model.StaffMember
import com.example.data.model.SupplierCategory
import com.example.data.model.SupplierRecord
import com.example.data.model.TourGroup
import com.example.data.model.TravelPackage
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VisaStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AgencyRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun isBootstrapAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.isEmailVerified && user.email.equals(RoberPathCatalog.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
  }

  private fun <T> safeSnapshotFlow(
    path: String,
    query: Query,
    mapper: (List<com.google.firebase.firestore.DocumentSnapshot>) -> List<T>,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    emitAll(
      query.snapshots()
        .map { snapshot -> mapper(snapshot.documents) }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          } else null
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observePackages(): Flow<List<TravelPackage>> =
    safeSnapshotFlow("packages", db.collection("packages")) { docs ->
      docs.mapNotNull { it.toObject(TravelPackage::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  fun observeAgencySettings(): Flow<AgencySettings> = flow {
    val uid = requireUserId()
    val path = "agency_settings/default_config"
    emitAll(
      db.collection("agency_settings").document("default_config")
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(AgencySettings::class.java, ServerTimestampBehavior.ESTIMATE)
              ?: AgencySettings()
          } else {
            AgencySettings()
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeBookings(userId: String, isAdminView: Boolean): Flow<List<Booking>> {
    val query =
      if (isAdminView) db.collection("bookings")
      else db.collection("bookings").whereEqualTo("userId", userId)
    return safeSnapshotFlow("bookings", query) { docs ->
      docs.mapNotNull { it.toObject(Booking::class.java, ServerTimestampBehavior.ESTIMATE) }
    }
  }

  fun observePilgrims(userId: String, isAdminView: Boolean): Flow<List<Pilgrim>> {
    val query =
      if (isAdminView) db.collection("pilgrims")
      else db.collection("pilgrims").whereEqualTo("userId", userId)
    return safeSnapshotFlow("pilgrims", query) { docs ->
      docs.mapNotNull { it.toObject(Pilgrim::class.java, ServerTimestampBehavior.ESTIMATE) }
    }
  }

  fun observePayments(userId: String, isAdminView: Boolean): Flow<List<PaymentRecord>> {
    val query =
      if (isAdminView) db.collection("payments")
      else db.collection("payments").whereEqualTo("userId", userId)
    return safeSnapshotFlow("payments", query) { docs ->
      docs.mapNotNull { it.toObject(PaymentRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
    }
  }

  fun observeInquiries(userId: String, isAdminView: Boolean): Flow<List<InquiryLead>> {
    val query =
      if (isAdminView) db.collection("inquiries")
      else db.collection("inquiries").whereEqualTo("userId", userId)
    return safeSnapshotFlow("inquiries", query) { docs ->
      docs.mapNotNull { it.toObject(InquiryLead::class.java, ServerTimestampBehavior.ESTIMATE) }
    }
  }

  fun observeGroups(): Flow<List<TourGroup>> =
    safeSnapshotFlow("groups", db.collection("groups")) { docs ->
      docs.mapNotNull { it.toObject(TourGroup::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  fun observeExpenses(): Flow<List<ExpenseRecord>> =
    safeSnapshotFlow("expenses", db.collection("expenses")) { docs ->
      docs.mapNotNull { it.toObject(ExpenseRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  fun observeSuppliers(): Flow<List<SupplierRecord>> =
    safeSnapshotFlow("suppliers", db.collection("suppliers")) { docs ->
      docs.mapNotNull { it.toObject(SupplierRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  fun observeStaffMembers(): Flow<List<StaffMember>> =
    safeSnapshotFlow("staff_members", db.collection("staff_members")) { docs ->
      docs.mapNotNull { it.toObject(StaffMember::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  fun observeAuditLogs(): Flow<List<AuditLog>> =
    safeSnapshotFlow("audit_logs", db.collection("audit_logs")) { docs ->
      docs.mapNotNull { it.toObject(AuditLog::class.java, ServerTimestampBehavior.ESTIMATE) }
    }

  suspend fun ensureProfileAndSeedIfAdmin(): Result<Unit> =
    try {
      val uid = requireUserId()
      val user = auth.currentUser!!
      val userDocRef = db.collection("users").document(uid)
      val existing = userDocRef.get().await()
      val isAdmin = isBootstrapAdmin()
      if (!existing.exists()) {
        val role = if (isAdmin) UserRole.SUPER_ADMIN.name else UserRole.CUSTOMER.name
        val payload = mapOf(
          "userId" to uid,
          "displayName" to (user.displayName?.take(80)?.ifBlank { "Rober Path Pilgrim" } ?: "Rober Path Pilgrim"),
          "email" to (user.email?.take(120) ?: "user@roberpath.com"),
          "phone" to (user.phoneNumber?.take(30) ?: ""),
          "role" to role,
          "department" to (if (isAdmin) "Executive Management" else ""),
          "preferredCurrency" to CurrencyCode.BDT.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        userDocRef.set(payload).await()
      }

      if (isAdmin) {
        seedInitialAgencyCatalogAndOps()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid ?: ""}")
      Result.failure(e)
    }

  private suspend fun seedInitialAgencyCatalogAndOps() {
    val pkgSnap = db.collection("packages").limit(1).get().await()
    if (pkgSnap.isEmpty) {
      RoberPathCatalog.defaultPackages.forEach { pkg ->
        val payload = mapOf(
          "title" to pkg.title,
          "subtitle" to pkg.subtitle,
          "serviceType" to pkg.serviceType,
          "tier" to pkg.tier,
          "flightType" to pkg.flightType,
          "priceBdt" to pkg.priceBdt,
          "priceSar" to pkg.priceSar,
          "durationDays" to pkg.durationDays,
          "makkahNights" to pkg.makkahNights,
          "madinahNights" to pkg.madinahNights,
          "makkahHotelDistance" to pkg.makkahHotelDistance,
          "madinahHotelDistance" to pkg.madinahHotelDistance,
          "hotelRating" to pkg.hotelRating,
          "inclusions" to pkg.inclusions,
          "active" to pkg.active,
          "season" to pkg.season,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection("packages").document(pkg.id).set(payload).await()
      }
    }

    val settingsDoc = db.collection("agency_settings").document("default_config").get().await()
    if (!settingsDoc.exists()) {
      val defaults = AgencySettings()
      db.collection("agency_settings").document("default_config").set(
        mapOf(
          "agencyName" to defaults.agencyName,
          "agentNumber" to defaults.agentNumber,
          "licenseNumber" to defaults.licenseNumber,
          "sarToBdtRate" to defaults.sarToBdtRate,
          "minPassportMonths" to defaults.minPassportMonths,
          "bkashMerchant" to defaults.bkashMerchant,
          "nagadMerchant" to defaults.nagadMerchant,
          "bankAccountSummary" to defaults.bankAccountSummary,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

    val groupsSnap = db.collection("groups").limit(1).get().await()
    if (groupsSnap.isEmpty) {
      val sampleGroups = listOf(
        mapOf(
          "groupCode" to "KAF-DEC-2026",
          "name" to "Dhaka Kafela Batch 01 (Dec)",
          "packageTitle" to "Direct Flight (Standard)",
          "departureDate" to "2026-12-12",
          "returnDate" to "2026-12-26",
          "airline" to "Biman Bangladesh Airlines",
          "flightNumber" to "BG-335 (DAC-JED)",
          "makkahHotel" to "Al Kiswah Towers (500m)",
          "madinahHotel" to "Saja Al Madinah (600m)",
          "transportCoach" to "SAPTCO VIP AC Coach #14",
          "moallemName" to "Maulana Tariqul Islam",
          "capacity" to 45,
          "assignedCount" to 28,
          "groupStatus" to GroupStatus.READY.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "groupCode" to "KAF-JAN-2027",
          "name" to "Rajab Premium Kafela (Jan)",
          "packageTitle" to "Direct Flight (Premium)",
          "departureDate" to "2027-01-10",
          "returnDate" to "2027-01-24",
          "airline" to "Saudia Airlines",
          "flightNumber" to "SV-803 (DAC-JED)",
          "makkahHotel" to "Swissotel Makkah (300m)",
          "madinahHotel" to "Pullman Zamzam Madinah (250m)",
          "transportCoach" to "Haramain Express + Private Coach",
          "moallemName" to "Mufti Habibur Rahman",
          "capacity" to 35,
          "assignedCount" to 19,
          "groupStatus" to GroupStatus.PLANNING.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      sampleGroups.forEachIndexed { idx, data ->
        db.collection("groups").document("grp_seed_${idx + 1}").set(data).await()
      }
    }

    val staffSnap = db.collection("staff_members").limit(1).get().await()
    if (staffSnap.isEmpty) {
      val seedStaff = listOf(
        mapOf(
          "fullName" to "Md. Rofiqul Islam",
          "email" to "rofiqul@roberpath.com",
          "phone" to "+8801999445576",
          "role" to UserRole.ADMIN.name,
          "department" to "Customer Service & Marketing",
          "attendanceStatus" to AttendanceStatus.PRESENT.name,
          "salaryBdt" to 65000L,
          "leaveDaysRemaining" to 14,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "fullName" to "Farhana Yasmin",
          "email" to "visa@roberpath.com",
          "phone" to "+8801711223344",
          "role" to UserRole.VISA_OFFICER.name,
          "department" to "Visa & Nusuk Desk",
          "attendanceStatus" to AttendanceStatus.PRESENT.name,
          "salaryBdt" to 42000L,
          "leaveDaysRemaining" to 12,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "fullName" to "Kamrul Hasan",
          "email" to "accounts@roberpath.com",
          "phone" to "+8801811334455",
          "role" to UserRole.ACCOUNTANT.name,
          "department" to "Finance & Audit",
          "attendanceStatus" to AttendanceStatus.PRESENT.name,
          "salaryBdt" to 48000L,
          "leaveDaysRemaining" to 10,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      seedStaff.forEachIndexed { idx, data ->
        db.collection("staff_members").document("staff_seed_${idx + 1}").set(data).await()
      }
    }

    val suppliersSnap = db.collection("suppliers").limit(1).get().await()
    if (suppliersSnap.isEmpty) {
      val seedSuppliers = listOf(
        mapOf(
          "name" to "Biman & Saudia GSA Dhaka",
          "category" to SupplierCategory.AIRLINE.name,
          "contactPerson" to "Tariq Mahmud",
          "phone" to "+8801715001122",
          "city" to "Dhaka",
          "payableBdt" to 1450000L,
          "paidBdt" to 1100000L,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "name" to "Al Haramain Hospitality Group",
          "category" to SupplierCategory.HOTEL_MAKKAH.name,
          "contactPerson" to "Sheikh Khalid",
          "phone" to "+966501122334",
          "city" to "Makkah",
          "payableBdt" to 960000L,
          "paidBdt" to 720000L,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      seedSuppliers.forEachIndexed { idx, data ->
        db.collection("suppliers").document("sup_seed_${idx + 1}").set(data).await()
      }
    }
  }

  suspend fun createBooking(
    customerName: String,
    customerPhone: String,
    pkg: TravelPackage,
    bookingType: String,
    pilgrimCount: Int,
    currency: String,
    travelBatchDate: String,
    notes: String,
  ): Result<String> =
    try {
      val uid = requireUserId()
      val cleanCount = pilgrimCount.coerceIn(1, 100)
      val totalBdt = pkg.priceBdt * cleanCount
      val bookingCode = "RP-${(1000..9999).random()}"
      val docId = "bk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val payload = mapOf(
        "userId" to uid,
        "bookingCode" to bookingCode,
        "customerName" to customerName.trim().take(100).ifBlank { "Valued Pilgrim" },
        "customerPhone" to customerPhone.trim().take(30).ifBlank { "+8801609900193" },
        "packageId" to pkg.id.ifBlank { "pkg_custom" },
        "packageTitle" to pkg.title.take(120),
        "bookingType" to bookingType,
        "groupName" to "",
        "pilgrimCount" to cleanCount,
        "totalAmountBdt" to totalBdt,
        "paidAmountBdt" to 0L,
        "currency" to currency,
        "status" to BookingStatus.INQUIRY.name,
        "travelBatchDate" to travelBatchDate.trim().take(40),
        "flightInfo" to (if (pkg.flightType == "DIRECT") "Direct DAC–JED (Pending PNR)" else "Transit Flight (Pending PNR)"),
        "hotelMakkah" to "${pkg.hotelRating} (${pkg.makkahHotelDistance})",
        "hotelMadinah" to "${pkg.hotelRating} (${pkg.madinahHotelDistance})",
        "transportInfo" to "AC Haramain Coach (Airport-Makkah-Madinah)",
        "moallemName" to "Assigned before departure",
        "notes" to notes.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("bookings").document(docId).set(payload).await()
      Result.success(bookingCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "bookings")
      Result.failure(e)
    }

  suspend fun updateBookingAdmin(
    bookingId: String,
    status: String,
    groupName: String,
    flightInfo: String,
    hotelMakkah: String,
    hotelMadinah: String,
    transportInfo: String,
    moallemName: String,
  ): Result<Unit> =
    try {
      requireUserId()
      val path = "bookings/$bookingId"
      db.document(path).update(
        mapOf(
          "status" to status,
          "groupName" to groupName.trim().take(80),
          "flightInfo" to flightInfo.trim().take(160),
          "hotelMakkah" to hotelMakkah.trim().take(160),
          "hotelMadinah" to hotelMadinah.trim().take(160),
          "transportInfo" to transportInfo.trim().take(160),
          "moallemName" to moallemName.trim().take(100),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("UPDATE_BOOKING", "BOOKINGS", "Updated $bookingId -> status=$status, group=$groupName")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId")
      Result.failure(e)
    }

  suspend fun savePilgrim(
    bookingId: String,
    fullName: String,
    passportNumber: String,
    passportExpiry: String,
    dateOfBirth: String,
    gender: String,
    relation: String,
    nidNumber: String,
    passportPhotoUri: String,
  ): Result<Unit> =
    try {
      val uid = requireUserId()
      val docId = "pil_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val docStatus = if (passportPhotoUri.isNotBlank()) DocumentStatus.UPLOADED.name else DocumentStatus.PENDING.name
      val payload = mapOf(
        "userId" to uid,
        "bookingId" to bookingId.take(128),
        "fullName" to fullName.trim().take(100),
        "passportNumber" to passportNumber.trim().uppercase().take(30),
        "passportExpiry" to passportExpiry.trim().take(30),
        "dateOfBirth" to dateOfBirth.trim().take(30),
        "gender" to gender,
        "relation" to relation.trim().take(40),
        "nidNumber" to nidNumber.trim().take(40),
        "documentStatus" to docStatus,
        "visaStatus" to VisaStatus.NOT_STARTED.name,
        "passportPhotoUri" to passportPhotoUri.take(300),
        "groupName" to "",
        "roomAllocation" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("pilgrims").document(docId).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "pilgrims")
      Result.failure(e)
    }

  suspend fun updatePilgrimOps(
    pilgrimId: String,
    documentStatus: String,
    visaStatus: String,
    groupName: String,
    roomAllocation: String,
  ): Result<Unit> =
    try {
      requireUserId()
      val path = "pilgrims/$pilgrimId"
      db.document(path).update(
        mapOf(
          "documentStatus" to documentStatus,
          "visaStatus" to visaStatus,
          "groupName" to groupName.trim().take(80),
          "roomAllocation" to roomAllocation.trim().take(80),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("UPDATE_VISA_DOC", "VISA_DOCS", "Pilgrim $pilgrimId -> doc=$documentStatus, visa=$visaStatus")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "pilgrims/$pilgrimId")
      Result.failure(e)
    }

  suspend fun deletePilgrim(pilgrimId: String): Result<Unit> =
    try {
      requireUserId()
      db.collection("pilgrims").document(pilgrimId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "pilgrims/$pilgrimId")
      Result.failure(e)
    }

  suspend fun submitPayment(
    booking: Booking,
    amountBdt: Long,
    currency: String,
    method: String,
    referenceNo: String,
    paymentType: String,
    notes: String,
  ): Result<String> =
    try {
      val uid = requireUserId()
      val docId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val receiptNo = "RCP-${(10000..99999).random()}"
      val payload = mapOf(
        "userId" to uid,
        "bookingId" to booking.id,
        "bookingCode" to booking.bookingCode,
        "customerName" to booking.customerName,
        "amountBdt" to amountBdt.coerceAtLeast(1L),
        "currency" to currency,
        "method" to method,
        "referenceNo" to referenceNo.trim().take(80),
        "paymentType" to paymentType,
        "verificationStatus" to VerificationStatus.PENDING.name,
        "receiptNumber" to receiptNo,
        "notes" to notes.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      db.collection("payments").document(docId).set(payload).await()
      Result.success(receiptNo)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "payments")
      Result.failure(e)
    }

  suspend fun verifyPaymentAndSyncBooking(
    payment: PaymentRecord,
    newStatus: String,
    booking: Booking?,
    allBookingPayments: List<PaymentRecord>,
  ): Result<Unit> =
    try {
      requireUserId()
      db.collection("payments").document(payment.id).update(
        mapOf(
          "verificationStatus" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      if (booking != null && booking.id.isNotBlank()) {
        val verifiedTotal = allBookingPayments
          .map { if (it.id == payment.id) it.copy(verificationStatus = newStatus) else it }
          .filter { it.verificationStatus == VerificationStatus.VERIFIED.name && it.paymentType != PaymentType.REFUND.name }
          .sumOf { it.amountBdt }
        val updatedBookingStatus =
          if (verifiedTotal > 0 && booking.status == BookingStatus.INQUIRY.name) {
            BookingStatus.CONFIRMED.name
          } else booking.status

        db.collection("bookings").document(booking.id).update(
          mapOf(
            "paidAmountBdt" to verifiedTotal,
            "status" to updatedBookingStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      logAudit("PAYMENT_$newStatus", "FINANCE", "${payment.receiptNumber} (${payment.method}) ৳${payment.amountBdt}")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "payments/${payment.id}")
      Result.failure(e)
    }

  suspend fun processRefund(
    booking: Booking,
    amountBdt: Long,
    method: String,
    referenceNo: String,
    notes: String,
  ): Result<Unit> =
    try {
      requireUserId()
      val docId = "ref_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val receiptNo = "RFD-${(1000..9999).random()}"
      db.collection("payments").document(docId).set(
        mapOf(
          "userId" to booking.userId,
          "bookingId" to booking.id,
          "bookingCode" to booking.bookingCode,
          "customerName" to booking.customerName,
          "amountBdt" to amountBdt.coerceAtLeast(1L),
          "currency" to CurrencyCode.BDT.name,
          "method" to method,
          "referenceNo" to referenceNo.trim().take(80),
          "paymentType" to PaymentType.REFUND.name,
          "verificationStatus" to VerificationStatus.REFUNDED.name,
          "receiptNumber" to receiptNo,
          "notes" to notes.trim().take(300),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      val newPaid = (booking.paidAmountBdt - amountBdt).coerceAtLeast(0L)
      db.collection("bookings").document(booking.id).update(
        mapOf(
          "paidAmountBdt" to newPaid,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("PROCESS_REFUND", "REFUNDS", "Refunded ৳$amountBdt for ${booking.bookingCode}")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "payments")
      Result.failure(e)
    }

  suspend fun submitInquiry(
    leadName: String,
    phone: String,
    packageInterest: String,
    pilgrimCount: Int,
    source: String,
    notes: String,
  ): Result<Unit> =
    try {
      val uid = requireUserId()
      val docId = "inq_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      db.collection("inquiries").document(docId).set(
        mapOf(
          "userId" to uid,
          "leadName" to leadName.trim().take(100),
          "phone" to phone.trim().take(30),
          "packageInterest" to packageInterest.trim().take(120),
          "pilgrimCount" to pilgrimCount.coerceIn(1, 100),
          "source" to source,
          "leadStatus" to LeadStatus.NEW.name,
          "followUpDate" to "",
          "notes" to notes.trim().take(500),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "inquiries")
      Result.failure(e)
    }

  suspend fun updateInquiryStatus(inquiryId: String, status: String, followUpDate: String): Result<Unit> =
    try {
      requireUserId()
      db.collection("inquiries").document(inquiryId).update(
        mapOf(
          "leadStatus" to status,
          "followUpDate" to followUpDate.trim().take(30),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("UPDATE_LEAD", "CRM", "Lead $inquiryId -> $status")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "inquiries/$inquiryId")
      Result.failure(e)
    }

  suspend fun savePackage(pkg: TravelPackage): Result<Unit> =
    try {
      requireUserId()
      val docId = pkg.id.ifBlank { "pkg_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
      val docRef = db.collection("packages").document(docId)
      val exists = docRef.get().await().exists()
      if (exists) {
        docRef.update(
          mapOf(
            "title" to pkg.title.trim().take(120),
            "subtitle" to pkg.subtitle.trim().take(160),
            "priceBdt" to pkg.priceBdt,
            "priceSar" to pkg.priceSar,
            "makkahHotelDistance" to pkg.makkahHotelDistance.take(60),
            "madinahHotelDistance" to pkg.madinahHotelDistance.take(60),
            "active" to pkg.active,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "title" to pkg.title.trim().take(120),
            "subtitle" to pkg.subtitle.trim().take(160),
            "serviceType" to pkg.serviceType,
            "tier" to pkg.tier,
            "flightType" to pkg.flightType,
            "priceBdt" to pkg.priceBdt,
            "priceSar" to pkg.priceSar,
            "durationDays" to pkg.durationDays,
            "makkahNights" to pkg.makkahNights,
            "madinahNights" to pkg.madinahNights,
            "makkahHotelDistance" to pkg.makkahHotelDistance.take(60),
            "madinahHotelDistance" to pkg.madinahHotelDistance.take(60),
            "hotelRating" to pkg.hotelRating.take(40),
            "inclusions" to pkg.inclusions.take(15),
            "active" to pkg.active,
            "season" to pkg.season.take(40),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      logAudit("SAVE_PACKAGE", "PACKAGES", "${pkg.title} (৳${pkg.priceBdt})")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "packages")
      Result.failure(e)
    }

  suspend fun saveTourGroup(group: TourGroup): Result<Unit> =
    try {
      requireUserId()
      val docId = group.id.ifBlank { "grp_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
      db.collection("groups").document(docId).set(
        mapOf(
          "groupCode" to group.groupCode.trim().take(40),
          "name" to group.name.trim().take(100),
          "packageTitle" to group.packageTitle.trim().take(120),
          "departureDate" to group.departureDate.trim().take(40),
          "returnDate" to group.returnDate.trim().take(40),
          "airline" to group.airline.trim().take(80),
          "flightNumber" to group.flightNumber.trim().take(40),
          "makkahHotel" to group.makkahHotel.trim().take(120),
          "madinahHotel" to group.madinahHotel.trim().take(120),
          "transportCoach" to group.transportCoach.trim().take(120),
          "moallemName" to group.moallemName.trim().take(100),
          "capacity" to group.capacity.coerceIn(1, 500),
          "assignedCount" to group.assignedCount.coerceIn(0, 500),
          "groupStatus" to group.groupStatus,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("SAVE_GROUP", "GROUPS_OPS", "${group.groupCode} (${group.name})")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "groups")
      Result.failure(e)
    }

  suspend fun addExpense(
    title: String,
    category: String,
    amountBdt: Long,
    supplierName: String,
    paymentMethod: String,
    expenseDate: String,
    notes: String,
  ): Result<Unit> =
    try {
      requireUserId()
      val docId = "exp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("expenses").document(docId).set(
        mapOf(
          "title" to title.trim().take(120),
          "category" to category,
          "amountBdt" to amountBdt.coerceAtLeast(1L),
          "supplierName" to supplierName.trim().take(100),
          "paymentMethod" to paymentMethod,
          "expenseDate" to expenseDate.trim().take(30),
          "notes" to notes.trim().take(300),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("ADD_EXPENSE", "ACCOUNTS", "$title • ৳$amountBdt")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "expenses")
      Result.failure(e)
    }

  suspend fun saveSupplier(
    name: String,
    category: String,
    contactPerson: String,
    phone: String,
    city: String,
    payableBdt: Long,
    paidBdt: Long,
  ): Result<Unit> =
    try {
      requireUserId()
      val docId = "sup_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("suppliers").document(docId).set(
        mapOf(
          "name" to name.trim().take(120),
          "category" to category,
          "contactPerson" to contactPerson.trim().take(80),
          "phone" to phone.trim().take(30),
          "city" to city.trim().take(60),
          "payableBdt" to payableBdt.coerceAtLeast(0L),
          "paidBdt" to paidBdt.coerceAtLeast(0L),
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("ADD_SUPPLIER", "SUPPLIERS", "$name ($city)")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "suppliers")
      Result.failure(e)
    }

  suspend fun addStaffMember(
    fullName: String,
    email: String,
    phone: String,
    role: String,
    department: String,
    salaryBdt: Long,
  ): Result<Unit> =
    try {
      requireUserId()
      val docId = "stf_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("staff_members").document(docId).set(
        mapOf(
          "fullName" to fullName.trim().take(100),
          "email" to email.trim().take(120),
          "phone" to phone.trim().take(30),
          "role" to role,
          "department" to department.trim().take(60),
          "attendanceStatus" to AttendanceStatus.PRESENT.name,
          "salaryBdt" to salaryBdt.coerceAtLeast(0L),
          "leaveDaysRemaining" to 14,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("ADD_STAFF", "HR_STAFF", "$fullName ($role)")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "staff_members")
      Result.failure(e)
    }

  suspend fun updateStaffAttendance(staffId: String, attendanceStatus: String): Result<Unit> =
    try {
      requireUserId()
      db.collection("staff_members").document(staffId).update(
        mapOf(
          "attendanceStatus" to attendanceStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "staff_members/$staffId")
      Result.failure(e)
    }

  suspend fun updateAgencySettings(settings: AgencySettings): Result<Unit> =
    try {
      requireUserId()
      db.collection("agency_settings").document("default_config").set(
        mapOf(
          "agencyName" to settings.agencyName.trim().take(120),
          "agentNumber" to settings.agentNumber.trim().take(40),
          "licenseNumber" to settings.licenseNumber.trim().take(40),
          "sarToBdtRate" to settings.sarToBdtRate.coerceIn(1.0, 500.0),
          "minPassportMonths" to settings.minPassportMonths.coerceIn(1, 24),
          "bkashMerchant" to settings.bkashMerchant.trim().take(40),
          "nagadMerchant" to settings.nagadMerchant.trim().take(40),
          "bankAccountSummary" to settings.bankAccountSummary.trim().take(200),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAudit("UPDATE_SETTINGS", "SETTINGS", "Updated SAR rate=${settings.sarToBdtRate}, minPassport=${settings.minPassportMonths}m")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "agency_settings/default_config")
      Result.failure(e)
    }

  private suspend fun logAudit(action: String, module: String, details: String) {
    try {
      val user = auth.currentUser ?: return
      val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
      db.collection("audit_logs").document(logId).set(
        mapOf(
          "actorEmail" to (user.email ?: "admin@roberpath.com").take(120),
          "actorRole" to (if (isBootstrapAdmin()) "SUPER_ADMIN" else "STAFF"),
          "action" to action.take(80),
          "targetModule" to module.take(60),
          "details" to details.take(400),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (_: Exception) {
      // Audit logging should not crash the primary operation
    }
  }
}
