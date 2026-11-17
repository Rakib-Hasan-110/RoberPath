const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_rober";
const ADMIN_EMAIL = "alhams20216@gmail.com";
const ALICE_UID = "customer_alice";
const BOB_UID = "customer_bob";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("packages").get());
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(
    unauthDb.collection("bookings").doc("bk_1").set({
      userId: ALICE_UID,
      bookingCode: "RP-2026-001",
      customerName: "Alice",
      customerPhone: "+8801711111111",
      packageId: "pkg_1",
      packageTitle: "Direct Flight (Standard)",
      bookingType: "INDIVIDUAL",
      groupName: "",
      pilgrimCount: 1,
      totalAmountBdt: 165000,
      paidAmountBdt: 0,
      currency: "BDT",
      status: "INQUIRY",
      travelBatchDate: "2026-12-10",
      flightInfo: "",
      hotelMakkah: "",
      hotelMadinah: "",
      transportInfo: "",
      moallemName: "",
      notes: "",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Customer cannot self-escalate role to SUPER_ADMIN or ADMIN", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      phone: "+8801700000000",
      role: "SUPER_ADMIN",
      department: "",
      preferredCurrency: "BDT",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      phone: "+8801700000000",
      role: "CUSTOMER",
      department: "",
      preferredCurrency: "BDT",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Customer booking lifecycle, cross-user isolation, and query alignment", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const created = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_alice_1").set({
      userId: ALICE_UID,
      bookingCode: "RP-2026-101",
      customerName: "Alice Rahman",
      customerPhone: "+8801712345678",
      packageId: "pkg_standard",
      packageTitle: "Direct Flight (Standard)",
      bookingType: "FAMILY",
      groupName: "",
      pilgrimCount: 2,
      totalAmountBdt: 330000,
      paidAmountBdt: 0,
      currency: "BDT",
      status: "INQUIRY",
      travelBatchDate: "2026-12-15",
      flightInfo: "",
      hotelMakkah: "",
      hotelMadinah: "",
      transportInfo: "",
      moallemName: "",
      notes: "Family room preferred",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Scoped query works; unfiltered customer query fails
  await assertSucceeds(
    aliceDb.collection("bookings").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("bookings").get());

  // Bob cannot read or mutate Alice's booking
  await assertFails(bobDb.collection("bookings").doc("bk_alice_1").get());
  await assertFails(
    bobDb.collection("bookings").doc("bk_alice_1").update({ notes: "Hijack" })
  );

  // Customer cannot self-mark paidAmountBdt or status=CONFIRMED via update
  await assertFails(
    aliceDb.collection("bookings").doc("bk_alice_1").update({
      paidAmountBdt: 330000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can query all bookings and update booking status
  await assertSucceeds(adminDb.collection("bookings").get());
  await assertSucceeds(
    adminDb.collection("bookings").doc("bk_alice_1").update({
      status: "CONFIRMED",
      paidAmountBdt: 50000,
      groupName: "Kafela-Dhaka-Dec",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  await assertFails(spoofAdminDb.collection("bookings").get());
  await assertFails(spoofAdminDb.collection("expenses").get());
});

test("Pilgrim, Payment, and AuditLog strict validation and immutability", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = new Date(Date.now() - 3000);

  // Reject pilgrim payload containing forbidden @DocumentId `id` field
  await assertFails(
    aliceDb.collection("pilgrims").doc("pil_1").set({
      id: "pil_1",
      userId: ALICE_UID,
      bookingId: "bk_alice_1",
      fullName: "Haji Karim",
      passportNumber: "A01234567",
      passportExpiry: "2028-05-01",
      dateOfBirth: "1980-01-15",
      gender: "MALE",
      relation: "Father",
      nidNumber: "1980123456789",
      documentStatus: "UPLOADED",
      visaStatus: "NOT_STARTED",
      passportPhotoUri: "content://picker/1",
      groupName: "",
      roomAllocation: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("pilgrims").doc("pil_1").set({
      userId: ALICE_UID,
      bookingId: "bk_alice_1",
      fullName: "Haji Karim",
      passportNumber: "A01234567",
      passportExpiry: "2028-05-01",
      dateOfBirth: "1980-01-15",
      gender: "MALE",
      relation: "Father",
      nidNumber: "1980123456789",
      documentStatus: "UPLOADED",
      visaStatus: "NOT_STARTED",
      passportPhotoUri: "content://picker/1",
      groupName: "",
      roomAllocation: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot verify their own payment
  await assertFails(
    aliceDb.collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      bookingId: "bk_alice_1",
      bookingCode: "RP-2026-101",
      customerName: "Alice Rahman",
      amountBdt: 50000,
      currency: "BDT",
      method: "BKASH",
      referenceNo: "TRX998877",
      paymentType: "ADVANCE",
      verificationStatus: "VERIFIED",
      receiptNumber: "RCP-101",
      notes: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      bookingId: "bk_alice_1",
      bookingCode: "RP-2026-101",
      customerName: "Alice Rahman",
      amountBdt: 50000,
      currency: "BDT",
      method: "BKASH",
      referenceNo: "TRX998877",
      paymentType: "ADVANCE",
      verificationStatus: "PENDING",
      receiptNumber: "RCP-101",
      notes: "Advance via bKash",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Audit log creation by admin succeeds, updates/deletes are strictly immutable
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      actorEmail: ADMIN_EMAIL,
      actorRole: "SUPER_ADMIN",
      action: "VERIFY_PAYMENT",
      targetModule: "FINANCE",
      details: "Verified bKash TRX998877",
      createdAt: now,
    })
  );
  await assertFails(
    adminDb.collection("audit_logs").doc("log_1").update({ details: "Tampered" })
  );
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
});
