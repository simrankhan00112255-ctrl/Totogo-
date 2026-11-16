const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PASSENGER_UID = "passenger_01";
const OTHER_UID = "passenger_02";
const DRIVER_UID = "driver_01";
const ADMIN_UID = "admin_01";
const ADMIN_EMAIL = "aliaktar919@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function adminCtx() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

test("Unauthenticated user cannot read or list users or bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(PASSENGER_UID).get());
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("drivers_public").get());
});

test("User cannot self-assign admin role or read another user PII", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  await assertFails(
    passengerDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      fullName: "Rahim Mondal",
      role: "admin",
      preferredLanguage: "bn",
      mobileVerified: true,
      isSuspended: false,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    passengerDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      fullName: "Rahim Mondal",
      role: "passenger",
      preferredLanguage: "bn",
      mobileVerified: true,
      isSuspended: false,
      mobileNumber: "+919800000001",
      createdAt: new Date(),
    })
  );

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(otherDb.collection("users").doc(PASSENGER_UID).get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("fare_settings").doc("default").set({
      minFare: 20,
      perKmFare: 12,
      minDistanceKm: 1.5,
      additionalDistanceFarePerKm: 10,
      waitingChargePerMin: 2,
      updatedAt: new Date(),
    })
  );
});

test("Driver cannot self-approve, and private vehicle docs are isolated from other drivers", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertFails(
    driverDb.collection("drivers").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "Karim Ali",
      mobileNumber: "+919800000002",
      mobileVerified: true,
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      serviceArea: "Chopra Bazar",
      status: "approved",
      totalTrips: 0,
      totalEarnings: 0,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    driverDb.collection("drivers").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "Karim Ali",
      mobileNumber: "+919800000002",
      mobileVerified: true,
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      vehicleModel: "Mahindra Treo",
      serviceArea: "Chopra Bazar, Daspara Haat",
      status: "pending_verification",
      totalTrips: 0,
      totalEarnings: 0,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    driverDb.collection("vehicles").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      vehicleModel: "Mahindra Treo",
      photosVerified: false,
      docsVerified: false,
      frontPhotoBase64: "front_img",
      rearPhotoBase64: "rear_img",
      sidePhotoBase64: "side_img",
      licenceDocBase64: "sample_doc",
      permitDocBase64: "permit_doc",
      createdAt: new Date(),
    })
  );

  // Private vehicle docs and driver records are isolated from other drivers/passengers
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(otherDb.collection("drivers").doc(DRIVER_UID).get());
  await assertFails(otherDb.collection("vehicles").doc(DRIVER_UID).get());

  // Unapproved driver cannot create or toggle drivers_public online
  await assertFails(
    driverDb.collection("drivers_public").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "Karim Ali",
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      serviceArea: "Chopra Bazar",
      isVerified: true,
      isOnline: true,
      currentLat: 26.4042,
      currentLng: 88.2485,
      hasGpsFix: false,
      ratingAvg: 5.0,
      ratingCount: 0,
      updatedAt: new Date(),
    })
  );

  // Admin rejects driver; driver can resubmit to pending_verification, but NEVER self-approve
  const adminDb = adminCtx().firestore();
  await assertSucceeds(
    adminDb.collection("drivers").doc(DRIVER_UID).update({
      status: "rejected",
      rejectionReason: "Blurry permit photo",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    driverDb.collection("drivers").doc(DRIVER_UID).update({
      status: "approved",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    driverDb.collection("drivers").doc(DRIVER_UID).update({
      status: "pending_verification",
      updatedAt: new Date(),
    })
  );
});

test("Booking workflow: passenger creates, approved online driver accepts once, unapproved driver fails, duplicate accept fails", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const adminDb = adminCtx().firestore();

  // Create booking as passenger
  await assertSucceeds(
    passengerDb.collection("bookings").doc("bk_001").set({
      passengerId: PASSENGER_UID,
      passengerName: "Rahim Mondal",
      passengerMobile: "+919800000001",
      pickupName: "Chopra Bazar",
      pickupLat: 26.4042,
      pickupLng: 88.2485,
      destinationName: "Sonapur More",
      destinationLat: 26.4250,
      destinationLng: 88.2610,
      distanceKm: 2.6,
      estimatedFare: 35,
      paymentMethod: "cash",
      status: "searching",
      hasRated: false,
      createdAt: new Date(),
    })
  );

  // Unapproved driver cannot accept
  await assertFails(
    otherDb.collection("bookings").doc("bk_001").update({
      driverId: OTHER_UID,
      driverName: "Fake Driver",
      driverPhone: "+919999999999",
      vehicleRegNumber: "WB-59-X-9999",
      vehicleType: "Electric TOTO",
      status: "assigned",
      updatedAt: new Date(),
    })
  );

  // Admin approves driver and creates drivers_public with isOnline = true
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("drivers").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "Karim Ali",
      mobileNumber: "+919800000002",
      mobileVerified: true,
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      serviceArea: "Chopra Bazar",
      status: "approved",
      totalTrips: 0,
      totalEarnings: 0,
      createdAt: new Date(),
    });
  });

  await assertSucceeds(
    adminDb.collection("drivers_public").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "Karim Ali",
      registrationNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      serviceArea: "Chopra Bazar",
      isVerified: true,
      isOnline: true,
      currentLat: 26.405,
      currentLng: 88.249,
      hasGpsFix: false,
      ratingAvg: 5.0,
      ratingCount: 0,
      updatedAt: new Date(),
    })
  );

  // Approved online driver accepts booking
  await assertSucceeds(
    driverDb.collection("bookings").doc("bk_001").update({
      driverId: DRIVER_UID,
      driverName: "Karim Ali",
      driverPhone: "+919800000002",
      vehicleRegNumber: "WB-59-T-1024",
      vehicleType: "Electric TOTO",
      status: "assigned",
      updatedAt: new Date(),
    })
  );

  // Duplicate accept fails (even if second driver is approved)
  await assertFails(
    otherDb.collection("bookings").doc("bk_001").update({
      driverId: OTHER_UID,
      driverName: "Other Driver",
      driverPhone: "+919800000003",
      vehicleRegNumber: "WB-59-T-2222",
      vehicleType: "Electric TOTO",
      status: "assigned",
      updatedAt: new Date(),
    })
  );

  // Driver progresses: arriving -> started -> completed
  await assertSucceeds(
    driverDb.collection("bookings").doc("bk_001").update({
      status: "arriving",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    driverDb.collection("bookings").doc("bk_001").update({
      status: "started",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    driverDb.collection("bookings").doc("bk_001").update({
      status: "completed",
      finalFare: 35,
      updatedAt: new Date(),
    })
  );

  // Terminal lock: driver cannot revert or alter completed booking
  await assertFails(
    driverDb.collection("bookings").doc("bk_001").update({
      status: "cancelled",
      updatedAt: new Date(),
    })
  );
});

test("Shadow update test: ghost field injection is rejected", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  await assertFails(
    passengerDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      fullName: "Rahim Mondal",
      role: "passenger",
      preferredLanguage: "bn",
      mobileVerified: true,
      isSuspended: false,
      ghostField: "hack",
      createdAt: new Date(),
    })
  );
});

test("Admin security: non-admin blocked from audit logs, admin registry, fare settings & complaint replies; audit logs are immutable", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const secondAdminUid = "admin_02";
  const secondAdminDb = testEnv.authenticatedContext(secondAdminUid, {
    email: "ops@totogo.in",
    email_verified: true,
  }).firestore();
  const adminDb = adminCtx().firestore();

  // Seed a complaint from passenger
  await assertSucceeds(
    passengerDb.collection("complaints").doc("cmp_01").set({
      reporterId: PASSENGER_UID,
      reporterRole: "passenger",
      targetUserId: DRIVER_UID,
      category: "Fare issue",
      description: "Charged extra waiting fee",
      status: "open",
      createdAt: new Date(),
    })
  );

  // Non-admin cannot create audit logs, register as admin, edit fares, or resolve complaints
  await assertFails(
    passengerDb.collection("admins").doc(PASSENGER_UID).set({
      uid: PASSENGER_UID,
      email: "attacker@test.com",
      createdAt: new Date(),
    })
  );
  await assertFails(
    passengerDb.collection("admin_audit_logs").doc("log_bad").set({
      adminId: PASSENGER_UID,
      adminEmail: "attacker@test.com",
      action: "FAKE_ACTION",
      targetType: "user",
      targetId: PASSENGER_UID,
      details: "Attempted log forge",
      createdAt: new Date(),
    })
  );
  await assertFails(passengerDb.collection("admin_audit_logs").get());
  await assertFails(
    passengerDb.collection("complaints").doc("cmp_01").update({
      status: "resolved",
      adminNotes: "Self-resolved",
      updatedAt: new Date(),
    })
  );

  // Bootstrapped admin authorizes second admin UID & creates immutable audit log
  await assertSucceeds(
    adminDb.collection("admins").doc(secondAdminUid).set({
      uid: secondAdminUid,
      email: "ops@totogo.in",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    adminDb.collection("admin_audit_logs").doc("log_01").set({
      adminId: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "GRANT_ADMIN",
      targetType: "admin",
      targetId: secondAdminUid,
      details: "Authorized ops@totogo.in",
      createdAt: new Date(),
    })
  );

  // Even admin CANNOT update or delete an existing audit log (immutable trail)
  await assertFails(
    adminDb.collection("admin_audit_logs").doc("log_01").update({
      details: "Tampered details",
    })
  );
  await assertFails(adminDb.collection("admin_audit_logs").doc("log_01").delete());

  // Second authorized UID admin can list collections, reply to complaint (only status/adminNotes/updatedAt), and cannot alter reporter description
  await assertSucceeds(secondAdminDb.collection("admin_audit_logs").get());
  await assertSucceeds(secondAdminDb.collection("complaints").get());
  await assertFails(
    secondAdminDb.collection("complaints").doc("cmp_01").update({
      description: "Altered passenger complaint",
      status: "resolved",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    secondAdminDb.collection("complaints").doc("cmp_01").update({
      status: "in_review",
      adminNotes: "Reviewing fare breakdown with driver.",
      updatedAt: new Date(),
    })
  );
});

