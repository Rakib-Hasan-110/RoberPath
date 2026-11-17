package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String, val isStaffRole: Boolean) {
  CUSTOMER("Customer", false),
  SUPER_ADMIN("Super Admin", true),
  ADMIN("Admin", true),
  MANAGER("Manager", true),
  ACCOUNTANT("Accountant", true),
  VISA_OFFICER("Visa Officer", true),
  SALES_EXECUTIVE("Sales Executive", true),
  OPERATIONS_STAFF("Operations Staff", true);

  companion object {
    fun fromRaw(raw: String?): UserRole =
      entries.firstOrNull { it.name == raw } ?: CUSTOMER
  }
}

enum class CurrencyCode(val symbol: String, val label: String) {
  BDT("৳", "BDT"),
  SAR("SAR", "SAR");

  companion object {
    fun fromRaw(raw: String?): CurrencyCode =
      entries.firstOrNull { it.name == raw } ?: BDT
  }
}

enum class ServiceType(val label: String) {
  UMRAH("Umrah"),
  HAJJ("Hajj");

  companion object {
    fun fromRaw(raw: String?): ServiceType =
      entries.firstOrNull { it.name == raw } ?: UMRAH
  }
}

enum class PackageTier(val label: String) {
  TRANSIT_ECONOMY("Transit Economy"),
  DIRECT_ECONOMY("Direct Economy"),
  STANDARD("Standard"),
  PREMIUM("Premium"),
  VIP("VIP"),
  CUSTOM_FAMILY("Family Custom");

  companion object {
    fun fromRaw(raw: String?): PackageTier =
      entries.firstOrNull { it.name == raw } ?: STANDARD
  }
}

enum class FlightType(val label: String) {
  TRANSIT("Transit Flight"),
  DIRECT("Direct Flight");

  companion object {
    fun fromRaw(raw: String?): FlightType =
      entries.firstOrNull { it.name == raw } ?: DIRECT
  }
}

enum class BookingType(val label: String) {
  INDIVIDUAL("Individual"),
  FAMILY("Family"),
  GROUP("Group / Kafela");

  companion object {
    fun fromRaw(raw: String?): BookingType =
      entries.firstOrNull { it.name == raw } ?: INDIVIDUAL
  }
}

enum class BookingStatus(val label: String) {
  INQUIRY("Inquiry"),
  CONFIRMED("Confirmed"),
  VISA_PROCESSING("Visa Processing"),
  TICKETED("Ticketed"),
  DEPARTED("Departed"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled");

  companion object {
    fun fromRaw(raw: String?): BookingStatus =
      entries.firstOrNull { it.name == raw } ?: INQUIRY
  }
}

enum class Gender(val label: String) {
  MALE("Male"),
  FEMALE("Female");

  companion object {
    fun fromRaw(raw: String?): Gender =
      entries.firstOrNull { it.name == raw } ?: MALE
  }
}

enum class DocumentStatus(val label: String) {
  PENDING("Pending Upload"),
  UPLOADED("Uploaded"),
  VERIFIED("Verified"),
  REJECTED("Needs Re-upload");

  companion object {
    fun fromRaw(raw: String?): DocumentStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

enum class VisaStatus(val label: String) {
  NOT_STARTED("Not Started"),
  DOCS_READY("Docs Ready"),
  SUBMITTED("Submitted (Enjaz/Mofa)"),
  ISSUED("Visa Issued"),
  REJECTED("Rejected");

  companion object {
    fun fromRaw(raw: String?): VisaStatus =
      entries.firstOrNull { it.name == raw } ?: NOT_STARTED
  }
}

enum class PaymentMethod(val label: String) {
  CASH("Cash"),
  BANK_TRANSFER("Bank Transfer"),
  BKASH("bKash"),
  NAGAD("Nagad");

  companion object {
    fun fromRaw(raw: String?): PaymentMethod =
      entries.firstOrNull { it.name == raw } ?: BKASH
  }
}

enum class PaymentType(val label: String) {
  ADVANCE("Advance"),
  INSTALLMENT("Installment"),
  FINAL("Final Settlement"),
  REFUND("Refund");

  companion object {
    fun fromRaw(raw: String?): PaymentType =
      entries.firstOrNull { it.name == raw } ?: ADVANCE
  }
}

enum class VerificationStatus(val label: String) {
  PENDING("Pending Verification"),
  VERIFIED("Verified"),
  REJECTED("Rejected"),
  REFUNDED("Refunded");

  companion object {
    fun fromRaw(raw: String?): VerificationStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

enum class LeadSource(val label: String) {
  APP("Mobile App"),
  WHATSAPP("WhatsApp"),
  PHONE("Phone Call"),
  FACEBOOK("Facebook Page"),
  WALK_IN("Dhaka Office Walk-In");

  companion object {
    fun fromRaw(raw: String?): LeadSource =
      entries.firstOrNull { it.name == raw } ?: APP
  }
}

enum class LeadStatus(val label: String) {
  NEW("New Lead"),
  FOLLOW_UP("Follow Up"),
  QUOTED("Quoted"),
  CONVERTED("Converted"),
  CLOSED("Closed");

  companion object {
    fun fromRaw(raw: String?): LeadStatus =
      entries.firstOrNull { it.name == raw } ?: NEW
  }
}

enum class GroupStatus(val label: String) {
  PLANNING("Planning"),
  READY("Ready to Depart"),
  DEPARTED("In Haramain"),
  COMPLETED("Returned");

  companion object {
    fun fromRaw(raw: String?): GroupStatus =
      entries.firstOrNull { it.name == raw } ?: PLANNING
  }
}

enum class ExpenseCategory(val label: String) {
  AIRLINE("Airline Tickets"),
  HOTEL("Makkah/Madinah Hotels"),
  VISA("Visa & Mofa"),
  TRANSPORT("Saudi Transport"),
  MOALLEM("Moallem & Guide"),
  OFFICE("Dhaka Office"),
  MARKETING("Marketing");

  companion object {
    fun fromRaw(raw: String?): ExpenseCategory =
      entries.firstOrNull { it.name == raw } ?: AIRLINE
  }
}

enum class SupplierCategory(val label: String) {
  AIRLINE("Airline"),
  HOTEL_MAKKAH("Makkah Hotel"),
  HOTEL_MADINAH("Madinah Hotel"),
  TRANSPORT("Transport Coach"),
  VISA_AGENT("Visa Agent"),
  CATERING("Bangladeshi Catering");

  companion object {
    fun fromRaw(raw: String?): SupplierCategory =
      entries.firstOrNull { it.name == raw } ?: AIRLINE
  }
}

enum class AttendanceStatus(val label: String) {
  PRESENT("Present"),
  ON_LEAVE("On Leave"),
  REMOTE("Remote"),
  FIELD_DUTY("Airport/Field Duty");

  companion object {
    fun fromRaw(raw: String?): AttendanceStatus =
      entries.firstOrNull { it.name == raw } ?: PRESENT
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val role: String = UserRole.CUSTOMER.name,
  val department: String = "",
  val preferredCurrency: String = CurrencyCode.BDT.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TravelPackage(
  @DocumentId val id: String = "",
  val title: String = "",
  val subtitle: String = "",
  val serviceType: String = ServiceType.UMRAH.name,
  val tier: String = PackageTier.STANDARD.name,
  val flightType: String = FlightType.DIRECT.name,
  val priceBdt: Long = 0L,
  val priceSar: Long = 0L,
  val durationDays: Int = 14,
  val makkahNights: Int = 7,
  val madinahNights: Int = 6,
  val makkahHotelDistance: String = "",
  val madinahHotelDistance: String = "",
  val hotelRating: String = "4/5 Star Hotel",
  val inclusions: List<String> = emptyList(),
  val active: Boolean = true,
  val season: String = "2026-27",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Booking(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingCode: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val packageId: String = "",
  val packageTitle: String = "",
  val bookingType: String = BookingType.INDIVIDUAL.name,
  val groupName: String = "",
  val pilgrimCount: Int = 1,
  val totalAmountBdt: Long = 0L,
  val paidAmountBdt: Long = 0L,
  val currency: String = CurrencyCode.BDT.name,
  val status: String = BookingStatus.INQUIRY.name,
  val travelBatchDate: String = "",
  val flightInfo: String = "",
  val hotelMakkah: String = "",
  val hotelMadinah: String = "",
  val transportInfo: String = "",
  val moallemName: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Pilgrim(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingId: String = "",
  val fullName: String = "",
  val passportNumber: String = "",
  val passportExpiry: String = "",
  val dateOfBirth: String = "",
  val gender: String = Gender.MALE.name,
  val relation: String = "Self",
  val nidNumber: String = "",
  val documentStatus: String = DocumentStatus.PENDING.name,
  val visaStatus: String = VisaStatus.NOT_STARTED.name,
  val passportPhotoUri: String = "",
  val groupName: String = "",
  val roomAllocation: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bookingId: String = "",
  val bookingCode: String = "",
  val customerName: String = "",
  val amountBdt: Long = 0L,
  val currency: String = CurrencyCode.BDT.name,
  val method: String = PaymentMethod.BKASH.name,
  val referenceNo: String = "",
  val paymentType: String = PaymentType.ADVANCE.name,
  val verificationStatus: String = VerificationStatus.PENDING.name,
  val receiptNumber: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class InquiryLead(
  @DocumentId val id: String = "",
  val userId: String = "",
  val leadName: String = "",
  val phone: String = "",
  val packageInterest: String = "",
  val pilgrimCount: Int = 1,
  val source: String = LeadSource.APP.name,
  val leadStatus: String = LeadStatus.NEW.name,
  val followUpDate: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TourGroup(
  @DocumentId val id: String = "",
  val groupCode: String = "",
  val name: String = "",
  val packageTitle: String = "",
  val departureDate: String = "",
  val returnDate: String = "",
  val airline: String = "",
  val flightNumber: String = "",
  val makkahHotel: String = "",
  val madinahHotel: String = "",
  val transportCoach: String = "",
  val moallemName: String = "",
  val capacity: Int = 40,
  val assignedCount: Int = 0,
  val groupStatus: String = GroupStatus.PLANNING.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ExpenseRecord(
  @DocumentId val id: String = "",
  val title: String = "",
  val category: String = ExpenseCategory.AIRLINE.name,
  val amountBdt: Long = 0L,
  val supplierName: String = "",
  val paymentMethod: String = PaymentMethod.BANK_TRANSFER.name,
  val expenseDate: String = "",
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SupplierRecord(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = SupplierCategory.AIRLINE.name,
  val contactPerson: String = "",
  val phone: String = "",
  val city: String = "",
  val payableBdt: Long = 0L,
  val paidBdt: Long = 0L,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val fullName: String = "",
  val email: String = "",
  val phone: String = "",
  val role: String = UserRole.SALES_EXECUTIVE.name,
  val department: String = "",
  val attendanceStatus: String = AttendanceStatus.PRESENT.name,
  val salaryBdt: Long = 0L,
  val leaveDaysRemaining: Int = 14,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
  @DocumentId val id: String = "",
  val actorEmail: String = "",
  val actorRole: String = "",
  val action: String = "",
  val targetModule: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AgencySettings(
  @DocumentId val id: String = "",
  val agencyName: String = "Rober Path Hajj & Umrah Services",
  val agentNumber: String = "1006525",
  val licenseNumber: String = "001468",
  val sarToBdtRate: Double = 32.0,
  val minPassportMonths: Int = 6,
  val bkashMerchant: String = "+8801609900193",
  val nagadMerchant: String = "+8801999445576",
  val bankAccountSummary: String = "Islami Bank Bangladesh PLC • Nikunja Branch • A/C 2050-1006525",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class ItineraryDay(
  val dayNumber: Int,
  val titleEn: String,
  val titleBn: String,
  val city: String,
  val description: String,
)

data class RolePermissionSpec(
  val role: UserRole,
  val capabilities: List<String>,
)

object RoberPathCatalog {
  const val BOOTSTRAP_ADMIN_EMAIL = "alhams20216@gmail.com"
  const val AGENCY_BANGLA_NAME = "রবের পথ (Rober Path)"
  const val LEGAL_PARENT = "Government Approved Agency • Bosphorus International Ltd., Bangladesh"
  const val AGENT_NUMBER = "1006525"
  const val LICENSE_NUMBER = "001468"
  const val ADDRESS = "House # 14, Road # 15, Nikunja-02, Khilkhet, Dhaka-1229, Bangladesh"
  const val DIRECTOR_NAME = "Md. Rofiqul Islam"
  const val DIRECTOR_TITLE = "Director, Customer Service & Marketing"
  const val WHATSAPP_NUMBER = "+8801999445576"
  const val MOBILE_NUMBER = "+8801609900193"
  const val OFFICIAL_EMAIL = "info.ceo@roberpath.com"
  const val WEBSITE = "www.roberpath.com"

  val standardInclusions = listOf(
    "Umrah Visa Processing & Insurance",
    "Up-Down International Air Ticket",
    "Makkah & Madinah 4/5 Star Hotel Reservation",
    "Airport - Makkah - Madinah - Airport AC Coach",
    "Historical & Religious Ziyarah (Makkah & Madinah)",
    "Riyazul Jannah Permit Assistance",
    "Daily 3 Healthy Bangladeshi Meals",
    "Experienced Alem, Moallem & 24/7 Support",
  )

  val travelTips = listOf(
    "Avoid packing unnecessary items to stay light during Ihram transit.",
    "Organize essentials in small pouches inside your cabin bag for quick access.",
    "Always keep your passport, BDT/SAR cash, and Nusuk/permit documents on your person.",
    "Ensure passport validity is at least 6 months from your scheduled departure date.",
  )

  val defaultPackages: List<TravelPackage> = listOf(
    TravelPackage(
      id = "pkg_transit_economy",
      title = "Transit Flight (Economy)",
      subtitle = "ট্রানজিট ফ্লাইট (ইকোনমি) • Value 14-Day Umrah",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.TRANSIT_ECONOMY.name,
      flightType = FlightType.TRANSIT.name,
      priceBdt = 139500L,
      priceSar = 4360L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "800–900m from Haram",
      madinahHotelDistance = "800–900m from Masjid an-Nabawi",
      hotelRating = "4 Star Quality Hotel",
      inclusions = standardInclusions,
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_direct_economy",
      title = "Direct Flight (Economy)",
      subtitle = "ডিরেক্ট ফ্লাইট (ইকোনমি) • Non-Stop Dhaka–Jeddah/Madinah",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.DIRECT_ECONOMY.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 149500L,
      priceSar = 4670L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "800–900m from Haram",
      madinahHotelDistance = "800–900m from Masjid an-Nabawi",
      hotelRating = "4 Star Quality Hotel",
      inclusions = standardInclusions,
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_direct_standard",
      title = "Direct Flight (Standard)",
      subtitle = "ডিরেক্ট ফ্লাইট (স্ট্যান্ডার্ড) • Closer Walk to Both Harams",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.STANDARD.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 165000L,
      priceSar = 5155L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "500–600m from Haram",
      madinahHotelDistance = "600–700m from Masjid an-Nabawi",
      hotelRating = "4/5 Star Hotel",
      inclusions = standardInclusions,
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_direct_premium",
      title = "Direct Flight (Premium)",
      subtitle = "ডিরেক্ট ফ্লাইট (প্রিমিয়াম) • Short Walk & Priority Support",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.PREMIUM.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 175000L,
      priceSar = 5470L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "300–400m from Haram",
      madinahHotelDistance = "200–300m from Masjid an-Nabawi",
      hotelRating = "5 Star Hotel",
      inclusions = standardInclusions,
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_direct_vip",
      title = "Direct Flight (VIP)",
      subtitle = "ডিরেক্ট ফ্লাইট (ভিআইপি) • Courtyard Step Hotels",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.VIP.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 260000L,
      priceSar = 8125L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "100–200m (Haram Courtyard)",
      madinahHotelDistance = "100–200m (Markazia)",
      hotelRating = "5 Star Luxury Haram View",
      inclusions = standardInclusions + "Private Haramain High-Speed Rail / VIP GMC Option",
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_family_custom",
      title = "Family Custom Umrah Package",
      subtitle = "বিশেষ সুবিধায় ফ্যামিলি কাস্টমাইজ প্যাকেজ",
      serviceType = ServiceType.UMRAH.name,
      tier = PackageTier.CUSTOM_FAMILY.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 170000L,
      priceSar = 5310L,
      durationDays = 14,
      makkahNights = 7,
      madinahNights = 6,
      makkahHotelDistance = "Flexible 150m–600m",
      madinahHotelDistance = "Flexible 150m–600m",
      hotelRating = "Private Family Suite (4/5 Star)",
      inclusions = standardInclusions + "Dedicated Family Room & Flexible Travel Dates",
      active = true,
      season = "2026-27",
    ),
    TravelPackage(
      id = "pkg_hajj_regular",
      title = "Regular Hajj Package 2026–27",
      subtitle = "হজ্জ নিয়মিত প্যাকেজ • Full Mina, Arafah & Azizia Support",
      serviceType = ServiceType.HAJJ.name,
      tier = PackageTier.STANDARD.name,
      flightType = FlightType.DIRECT.name,
      priceBdt = 575000L,
      priceSar = 17970L,
      durationDays = 35,
      makkahNights = 22,
      madinahNights = 10,
      makkahHotelDistance = "400–600m from Haram",
      madinahHotelDistance = "300–500m from Masjid an-Nabawi",
      hotelRating = "Gov. Approved 4/5 Star & Mina Tent",
      inclusions = standardInclusions + "Mina-Arafah-Muzdalifah Full Logistics & Qurbani Guidance",
      active = true,
      season = "2026-27",
    ),
  )

  val fourteenDayItinerary: List<ItineraryDay> = listOf(
    ItineraryDay(1, "First Umrah", "প্রথম ওমরাহ", "Makkah", "Arrival in Jeddah, transfer to Makkah hotel, and perform first Umrah with Moallem guidance."),
    ItineraryDay(2, "Rest & Ibadah", "বিশ্রাম ও ইবাদত", "Makkah", "Recovery, Tawaf, and prayers at Masjid al-Haram."),
    ItineraryDay(3, "Makkah Ziyarah", "মক্কা জিয়ারাহ", "Makkah", "Guided tour of Jabal al-Noor, Jabal Thawr, Mina, Arafat, and Muzdalifah."),
    ItineraryDay(4, "Taif Ziyarah & 2nd Umrah", "তায়েফ জিয়ারাহ ও দ্বিতীয় ওমরাহ", "Taif / Makkah", "Day excursion to Taif historical mosques and Miqat Qarn al-Manazil for optional 2nd Umrah."),
    ItineraryDay(5, "Jeddah Tour", "জেদ্দা সফর", "Jeddah", "Guided visit to Jeddah Corniche, Al-Rahmah Floating Mosque, and Red Sea coast."),
    ItineraryDay(6, "Umrah Niyyah at Masjid Ayesha", "আয়েশা মসজিদে ওমরাহ নিয়ত", "Makkah", "Ihram intention from Masjid Aisha (Taneem) with Kafela director."),
    ItineraryDay(7, "Ibadah & Shopping", "ইবাদত ও কেনাকাটা", "Makkah", "Farewell Tawaf preparation and time for dates/gifts in Makkah."),
    ItineraryDay(8, "Makkah to Madinah", "মক্কা থেকে মদিনা", "Transit", "Check out from Makkah and travel by comfortable AC coach to Madinah al-Munawwarah."),
    ItineraryDay(9, "Ibadah & Prophet's Rawdah Ziyarah", "ইবাদত ও নবীজির রওজা জিয়ারত", "Madinah", "Prayers in Masjid an-Nabawi and Salaam at the Rawdah of Prophet Muhammad (SAW)."),
    ItineraryDay(10, "Madinah Ziyarah", "মদিনা জিয়ারাহ", "Madinah", "Guided tour to Masjid Quba, Masjid Qiblatain, Uhud Mountain, and Seven Mosques."),
    ItineraryDay(11, "Riyazul Jannah", "রিয়াজুল জান্নাহ", "Madinah", "Scheduled entry to Riyazul Jannah via official permit coordinated by Rober Path."),
    ItineraryDay(12, "Jinn Hill & Badr Ziyarah", "জিন পাহাড় ও বদর জিয়ারত", "Badr / Madinah", "Historical trip to Wadi al-Jinn and the Martyrs of Badr site."),
    ItineraryDay(13, "Rest & Ibadah", "বিশ্রাম ও ইবাদত", "Madinah", "Final Jummah/prayers and farewell Salaam at Masjid an-Nabawi."),
    ItineraryDay(14, "Madinah to Dhaka", "মদিনা থেকে ঢাকা", "Return", "Airport coach transfer from Madinah and return flight to Dhaka."),
  )

  val rolePermissions: List<RolePermissionSpec> = listOf(
    RolePermissionSpec(UserRole.SUPER_ADMIN, listOf("All 24 Modules", "RBAC & Settings", "Audit Logs", "Finance Approvals")),
    RolePermissionSpec(UserRole.ADMIN, listOf("Packages", "Bookings", "CRM", "Operations", "Finance View", "Staff")),
    RolePermissionSpec(UserRole.MANAGER, listOf("Bookings", "Groups/Kafela", "Operations", "CRM Follow-ups", "Reports")),
    RolePermissionSpec(UserRole.ACCOUNTANT, listOf("Payments & Verification", "Invoices/PDF", "Expenses", "Refunds", "Financial Reports")),
    RolePermissionSpec(UserRole.VISA_OFFICER, listOf("Passport Verification", "Expiry Alerts", "Visa Processing", "Riyazul Jannah Permits")),
    RolePermissionSpec(UserRole.SALES_EXECUTIVE, listOf("Leads & CRM", "Package Quotes", "Booking Intake", "Customer Follow-ups")),
    RolePermissionSpec(UserRole.OPERATIONS_STAFF, listOf("Flights & Tickets", "Hotels & Rooms", "Transport Coaches", "Itineraries")),
    RolePermissionSpec(UserRole.CUSTOMER, listOf("Browse Packages", "Request Bookings", "Manage Pilgrims & Docs", "Track Visa & Pay")),
  )
}
