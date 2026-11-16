package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DriverStatus
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class TotoGoRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private suspend fun signInVerifiedAdmin(): String = withContext(Dispatchers.IO) {
    val uid = signInTestUser(TotoGoRepository.BOOTSTRAPPED_ADMIN_EMAIL)
    val user = checkNotNull(auth.currentUser)
    if (!user.isEmailVerified) {
      user.sendEmailVerification().await()
      val oobUrl = URL("http://127.0.0.1:9099/emulator/v1/projects/demo-no-project/oobCodes")
      val conn = (oobUrl.openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
      val body = conn.inputStream.bufferedReader().readText()
      conn.disconnect()
      val array = JSONObject(body).getJSONArray("oobCodes")
      var code: String? = null
      for (i in array.length() - 1 downTo 0) {
        val item = array.getJSONObject(i)
        if (item.optString("email").equals(TotoGoRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) &&
          item.optString("requestType") == "VERIFY_EMAIL"
        ) {
          code = item.getString("oobCode")
          break
        }
      }
      checkNotNull(code) { "Auth emulator did not emit verification code for admin" }
      auth.applyActionCode(code).await()
      user.reload().await()
      user.getIdToken(true).await()
    }
    uid
  }

  @Test
  fun createUserProfile_authenticated_persistsAndCrossUserReadDenied() = runBlocking {
    val passengerUid = signInTestUser("passenger_${UUID.randomUUID()}@test.com")
    val repo = TotoGoRepository(firestore, auth)

    val saveResult = withTimeout(TIMEOUT_MS) {
      repo.createOrUpdateUserProfile(
        fullName = "Rahim Mondal",
        email = "passenger@test.com",
        mobileNumber = "+919876543210",
        mobileVerified = true,
        preferredLanguage = "bn",
        role = UserRole.PASSENGER,
        photoBase64 = null,
      )
    }
    assertTrue(saveResult.isSuccess)

    val loaded = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile(passengerUid).first { it != null }
    }
    assertEquals("Rahim Mondal", loaded?.fullName)
    assertEquals("+919876543210", loaded?.mobileNumber)

    // Cross-user PII read must fail with PERMISSION_DENIED
    signInTestUser("other_${UUID.randomUUID()}@test.com")
    val crossReadError = runCatching {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("users").document(passengerUid).get().await()
      }
    }.exceptionOrNull()
    val fsErr = crossReadError?.unwrapFirestoreException()
    assertNotNull(fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun observeBookings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = TotoGoRepository(firestore, auth)
    val error = runCatching {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSearchingBookings().first()
      }
    }.exceptionOrNull()
    assertNotNull(error)
    assertTrue(
      error is IllegalStateException ||
        error?.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    )
  }

  @Test
  fun unapprovedDriverCannotAcceptBooking_andUnauthenticatedReadDenied() = runBlocking {
    val driverEmail = "driver_${UUID.randomUUID()}@test.com"
    val driverUid = signInTestUser(driverEmail)
    val driverRepo = TotoGoRepository(firestore, auth)

    // Driver registers as pending_verification
    val regResult = withTimeout(TIMEOUT_MS) {
      driverRepo.submitDriverRegistration(
        fullName = "Karim Ali",
        mobileNumber = "+919800112233",
        mobileVerified = true,
        registrationNumber = "WB-59-T-4040",
        vehicleType = "Electric TOTO",
        vehicleModel = "Mahindra Treo",
        serviceArea = "Chopra Bazar, Daspara Haat",
        licenceNumber = "WB59-2024001",
        permitNumber = "PRM-101",
        driverPhotoBase64 = null,
        frontPhotoBase64 = null,
        rearPhotoBase64 = null,
        sidePhotoBase64 = null,
        licenceDocBase64 = null,
        permitDocBase64 = null,
      )
    }
    assertTrue(regResult.isSuccess)

    val driverProfile = withTimeout(FLOW_TIMEOUT_MS) {
      driverRepo.observeDriverProfile(driverUid).first { it != null }
    }
    assertEquals(DriverStatus.PENDING_VERIFICATION, driverProfile?.status)

    // Passenger creates a booking
    val passengerUid = signInTestUser("p_${UUID.randomUUID()}@test.com")
    val passengerRepo = TotoGoRepository(firestore, auth)
    val bookingId = withTimeout(TIMEOUT_MS) {
      passengerRepo.createBooking(
        Booking(
          passengerId = passengerUid,
          passengerName = "Suman Das",
          passengerMobile = "+919800001111",
          pickupName = "Chopra Bazar",
          pickupLat = 26.4042,
          pickupLng = 88.2485,
          destinationName = "Sonapur More",
          destinationLat = 26.4254,
          destinationLng = 88.2620,
          distanceKm = 3.1,
          estimatedFare = 40.0,
          paymentMethod = "cash",
        )
      ).getOrThrow()
    }

    // Unapproved driver cannot accept
    signInTestUser(driverEmail)
    val unapprovedAccept = withTimeout(TIMEOUT_MS) {
      driverRepo.acceptBookingTransaction(
        bookingId = bookingId,
        driverName = "Karim Ali",
        driverPhone = "+919800112233",
        vehicleRegNumber = "WB-59-T-4040",
        vehicleType = "Electric TOTO",
        passengerId = passengerUid,
      )
    }
    assertTrue(unapprovedAccept.isFailure)

    // Unauthenticated direct firestore query fails
    auth.signOut()
    val directFail = runCatching {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("bookings").document(bookingId).get().await()
      }
    }.exceptionOrNull()
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      directFail?.unwrapFirestoreException()?.code,
    )
  }

  @Test
  fun endToEndPassengerBookingFlow_preventsDuplicateDriverAccept_showsRealPhotosAndPhone_completesAndRates() = runBlocking {
    val repo = TotoGoRepository(firestore, auth)
    val d1Email = "d1_${UUID.randomUUID()}@test.com"
    val d2Email = "d2_${UUID.randomUUID()}@test.com"
    val passengerEmail = "p_e2e_${UUID.randomUUID()}@test.com"

    // 1. Register Driver 1 with real portrait + front TOTO photo strings
    val d1Uid = signInTestUser(d1Email)
    assertTrue(
      repo.submitDriverRegistration(
        fullName = "Bikash Roy",
        mobileNumber = "+919811112222",
        mobileVerified = true,
        registrationNumber = "WB-59-E-1101",
        vehicleType = "Electric TOTO",
        vehicleModel = "Mahindra Treo Zor",
        serviceArea = "Chopra Bazar",
        licenceNumber = "LIC-1101",
        permitNumber = "PRM-1101",
        driverPhotoBase64 = "ZmFrZV9wb3J0cmFpdF9kMQ==",
        frontPhotoBase64 = "ZmFrZV9mcm9udF90b3RvX2Qx",
        rearPhotoBase64 = "ZmFrZV9yZWFyX3RvdG9fZDE=",
        sidePhotoBase64 = "ZmFrZV9zaWRlX3RvdG9fZDE=",
        licenceDocBase64 = "ZmFrZV9saWNlbmNlX2Qx",
        permitDocBase64 = "ZmFrZV9wZXJtaXRfZDE=",
      ).isSuccess
    )
    val d1Profile = repo.observeDriverProfile(d1Uid).first { it != null }!!
    val d1Vehicle = repo.observeVehicle(d1Uid).first { it != null }

    // 2. Register Driver 2
    val d2Uid = signInTestUser(d2Email)
    assertTrue(
      repo.submitDriverRegistration(
        fullName = "Raju Mia",
        mobileNumber = "+919822223333",
        mobileVerified = true,
        registrationNumber = "WB-59-E-2202",
        vehicleType = "Electric TOTO",
        vehicleModel = "Mayuri Pro",
        serviceArea = "Sonapur More",
        licenceNumber = null,
        permitNumber = null,
        driverPhotoBase64 = "ZmFrZV9wb3J0cmFpdF9kMg==",
        frontPhotoBase64 = "ZmFrZV9mcm9udF90b3RvX2Qy",
        rearPhotoBase64 = null,
        sidePhotoBase64 = null,
        licenceDocBase64 = null,
        permitDocBase64 = null,
      ).isSuccess
    )
    val d2Profile = repo.observeDriverProfile(d2Uid).first { it != null }!!
    val d2Vehicle = repo.observeVehicle(d2Uid).first { it != null }

    // 3. Verified Admin approves both drivers & publishes their public vehicle profiles
    signInVerifiedAdmin()
    assertTrue(repo.approveDriverAdmin(d1Profile, d1Vehicle).isSuccess)
    assertTrue(repo.approveDriverAdmin(d2Profile, d2Vehicle).isSuccess)

    // 4. Both approved drivers go online
    signInTestUser(d1Email)
    assertTrue(repo.setDriverOnlineStatus(true, 26.4045, 88.2489, true).isSuccess)
    signInTestUser(d2Email)
    assertTrue(repo.setDriverOnlineStatus(true, 26.4250, 88.2618, true).isSuccess)

    // 5. Passenger signs in, sees online approved TOTOs, and creates booking
    val passengerUid = signInTestUser(passengerEmail)
    assertTrue(
      repo.createOrUpdateUserProfile(
        fullName = "Ananya Sen",
        email = passengerEmail,
        mobileNumber = "+919833334444",
        mobileVerified = true,
        preferredLanguage = "bn",
        role = UserRole.PASSENGER,
        photoBase64 = null,
      ).isSuccess
    )
    val nearby = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeVerifiedOnlineDrivers().first { list -> list.any { it.driverId == d1Uid } && list.any { it.driverId == d2Uid } }
    }
    assertTrue(nearby.size >= 2)

    val bookingId = repo.createBooking(
      Booking(
        passengerId = passengerUid,
        passengerName = "Ananya Sen",
        passengerMobile = "+919833334444",
        pickupName = "Chopra Bazar",
        pickupLat = 26.4042,
        pickupLng = 88.2485,
        destinationName = "Sonapur More",
        destinationLat = 26.4254,
        destinationLng = 88.2620,
        distanceKm = 3.2,
        estimatedFare = 40.0,
        paymentMethod = "cash",
      )
    ).getOrThrow()

    // 6. Driver 1 accepts booking atomically
    signInTestUser(d1Email)
    val accept1 = repo.acceptBookingTransaction(
      bookingId = bookingId,
      driverName = d1Profile.fullName,
      driverPhone = d1Profile.mobileNumber,
      vehicleRegNumber = d1Profile.registrationNumber,
      vehicleType = d1Profile.vehicleType,
      passengerId = passengerUid,
    )
    assertTrue(accept1.isSuccess)

    // 7. Driver 2 (also approved & online!) MUST NOT accept the same booking
    signInTestUser(d2Email)
    val accept2 = repo.acceptBookingTransaction(
      bookingId = bookingId,
      driverName = d2Profile.fullName,
      driverPhone = d2Profile.mobileNumber,
      vehicleRegNumber = d2Profile.registrationNumber,
      vehicleType = d2Profile.vehicleType,
      passengerId = passengerUid,
    )
    assertTrue(accept2.isFailure)

    // 8. Passenger sees Driver 1 phone, name, plate, vehicle type, and real photos
    signInTestUser(passengerEmail)
    val assignedBooking = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePassengerBookings(passengerUid).first { list ->
        list.firstOrNull { it.id == bookingId }?.status == BookingStatus.ASSIGNED
      }.first { it.id == bookingId }
    }
    assertEquals(d1Uid, assignedBooking.driverId)
    assertEquals("+919811112222", assignedBooking.driverPhone)
    assertEquals("WB-59-E-1101", assignedBooking.vehicleRegNumber)
    assertEquals("Electric TOTO", assignedBooking.vehicleType)

    val publicIdentity = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDriverPublic(d1Uid).first { it != null }
    }!!
    assertEquals("Mahindra Treo Zor", publicIdentity.vehicleModel)
    assertEquals("ZmFrZV9wb3J0cmFpdF9kMQ==", publicIdentity.profilePhotoBase64)
    assertEquals("ZmFrZV9mcm9udF90b3RvX2Qx", publicIdentity.frontPhotoBase64)

    // 9. Driver 1 progresses booking: arriving -> started -> completed
    signInTestUser(d1Email)
    assertTrue(repo.updateBookingStatusByDriver(assignedBooking, BookingStatus.ARRIVING).isSuccess)
    assertTrue(repo.updateBookingStatusByDriver(assignedBooking, BookingStatus.STARTED).isSuccess)
    assertTrue(repo.updateBookingStatusByDriver(assignedBooking, BookingStatus.COMPLETED, finalFare = 40.0).isSuccess)

    // 10. Passenger rates completed ride & can also cancel a separate searching ride
    signInTestUser(passengerEmail)
    assertTrue(repo.submitRating(bookingId, d1Uid, 5, "Great local ride").isSuccess)

    val cancelBookingId = repo.createBooking(
      Booking(
        passengerId = passengerUid,
        passengerName = "Ananya Sen",
        passengerMobile = "+919833334444",
        pickupName = "Chopra Bazar",
        pickupLat = 26.4042,
        pickupLng = 88.2485,
        destinationName = "Daspara Haat",
        destinationLat = 26.4390,
        destinationLng = 88.2940,
        distanceKm = 7.2,
        estimatedFare = 85.0,
        paymentMethod = "cash",
      )
    ).getOrThrow()

    val toCancel = repo.observePassengerBookings(passengerUid).first { list ->
      list.any { it.id == cancelBookingId }
    }.first { it.id == cancelBookingId }
    assertTrue(repo.cancelBookingByPassenger(toCancel, "Changed plans").isSuccess)

    val cancelled = repo.observePassengerBookings(passengerUid).first { list ->
      list.firstOrNull { it.id == cancelBookingId }?.status == BookingStatus.CANCELLED
    }.first { it.id == cancelBookingId }
    assertEquals("Changed plans", cancelled.cancelReason)
    assertFalse(cancelled.hasRated)
  }

  @Test
  fun driverRegistration_verificationGating_privateDocProtection_andLiveGpsBookingFlow() = runBlocking {
    val repo = TotoGoRepository(firestore, auth)
    val driverEmail = "drv_full_${UUID.randomUUID()}@test.com"
    val passengerEmail = "pax_full_${UUID.randomUUID()}@test.com"

    // 1. Driver registers with full portrait, 3 TOTO photos, licence + permit docs, model & multi-area
    val driverUid = signInTestUser(driverEmail)
    assertTrue(
      repo.submitDriverRegistration(
        fullName = "Safiul Islam",
        mobileNumber = "+919844445555",
        mobileVerified = true,
        registrationNumber = "WB-59-D-7788",
        vehicleType = "Passenger E-Rickshaw",
        vehicleModel = "Mayuri Pro",
        serviceArea = "Chopra Bazar, Daspara Haat",
        licenceNumber = "WB59-LIC-7788",
        permitNumber = "WB59-PRM-7788",
        driverPhotoBase64 = "cG9ydHJhaXRfZDc=",
        frontPhotoBase64 = "ZnJvbnRfZDc=",
        rearPhotoBase64 = "cmVhcl9kNw==",
        sidePhotoBase64 = "c2lkZV9kNw==",
        licenceDocBase64 = "cHJpdmF0ZV9saWNlbmNl",
        permitDocBase64 = "cHJpdmF0ZV9wZXJtaXQ=",
      ).isSuccess
    )

    val pendingDriver = repo.observeDriverProfile(driverUid).first { it != null }!!
    assertEquals(DriverStatus.PENDING_VERIFICATION, pendingDriver.status)
    assertEquals("Mayuri Pro", pendingDriver.vehicleModel)
    assertEquals("Chopra Bazar, Daspara Haat", pendingDriver.serviceArea)

    // Unapproved driver CANNOT go online on drivers_public
    val prematureOnline = repo.setDriverOnlineStatus(true, 26.4042, 88.2485, true)
    assertTrue(prematureOnline.isFailure)

    // 2. Private documents (vehicles & drivers) MUST NOT be readable by a passenger
    val paxUid = signInTestUser(passengerEmail)
    val docLeakError = runCatching {
      firestore.collection("vehicles").document(driverUid).get().await()
    }.exceptionOrNull()
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      docLeakError?.unwrapFirestoreException()?.code,
    )

    // 3. Admin rejects first, driver re-submits updated photos -> status returns to pending_verification
    signInVerifiedAdmin()
    assertTrue(repo.updateDriverStatusAdmin(driverUid, DriverStatus.REJECTED, "Retake rear TOTO photo").isSuccess)
    signInTestUser(driverEmail)
    assertTrue(
      repo.submitDriverRegistration(
        fullName = "Safiul Islam",
        mobileNumber = "+919844445555",
        mobileVerified = true,
        registrationNumber = "WB-59-D-7788",
        vehicleType = "Passenger E-Rickshaw",
        vehicleModel = "Mayuri Pro",
        serviceArea = "Chopra Bazar, Daspara Haat",
        licenceNumber = "WB59-LIC-7788",
        permitNumber = "WB59-PRM-7788",
        driverPhotoBase64 = "cG9ydHJhaXRfZDc=",
        frontPhotoBase64 = "ZnJvbnRfZDc=",
        rearPhotoBase64 = "Y2xlYXJfcmVhcl9kNw==",
        sidePhotoBase64 = "c2lkZV9kNw==",
        licenceDocBase64 = "cHJpdmF0ZV9saWNlbmNl",
        permitDocBase64 = "cHJpdmF0ZV9wZXJtaXQ=",
      ).isSuccess
    )
    val resubmitted = repo.observeDriverProfile(driverUid).first { it?.status == DriverStatus.PENDING_VERIFICATION }!!
    assertEquals(DriverStatus.PENDING_VERIFICATION, resubmitted.status)
    val updatedVehicle = repo.observeVehicle(driverUid).first { it?.rearPhotoBase64 == "Y2xlYXJfcmVhcl9kNw==" }!!

    // 4. Admin approves driver -> public profile contains verified portrait, all 3 TOTO photos, and model
    signInVerifiedAdmin()
    assertTrue(repo.approveDriverAdmin(resubmitted, updatedVehicle).isSuccess)

    // 5. Driver goes online and pushes real GPS location updates
    signInTestUser(driverEmail)
    assertTrue(repo.setDriverOnlineStatus(true, 26.4048, 88.2491, true).isSuccess)
    assertTrue(repo.updateDriverLiveGps(26.4090, 88.2520).isSuccess)
    val publicAfterGps = repo.observeDriverPublic(driverUid).first { it?.currentLat == 26.4090 }!!
    assertTrue(publicAfterGps.isVerified)
    assertTrue(publicAfterGps.isOnline)
    assertTrue(publicAfterGps.hasGpsFix)
    assertEquals("Mayuri Pro", publicAfterGps.vehicleModel)
    assertEquals("ZnJvbnRfZDc=", publicAfterGps.frontPhotoBase64)
    assertEquals("Y2xlYXJfcmVhcl9kNw==", publicAfterGps.rearPhotoBase64)
    assertEquals("c2lkZV9kNw==", publicAfterGps.sidePhotoBase64)

    // 6. Passenger creates booking -> driver sees it, accepts, completes -> earnings increment accurately
    signInTestUser(passengerEmail)
    val bookingId = repo.createBooking(
      Booking(
        passengerId = paxUid,
        passengerName = "Mita Paul",
        passengerMobile = "+919855556666",
        pickupName = "Chopra Bazar",
        pickupLat = 26.4042,
        pickupLng = 88.2485,
        destinationName = "Daspara Haat",
        destinationLat = 26.4390,
        destinationLng = 88.2940,
        distanceKm = 7.2,
        estimatedFare = 85.0,
        paymentMethod = "cash",
      )
    ).getOrThrow()

    signInTestUser(driverEmail)
    val incoming = repo.observeSearchingBookings().first { list -> list.any { it.id == bookingId } }
      .first { it.id == bookingId }
    assertTrue(
      repo.acceptBookingTransaction(
        bookingId = incoming.id,
        driverName = resubmitted.fullName,
        driverPhone = resubmitted.mobileNumber,
        vehicleRegNumber = resubmitted.registrationNumber,
        vehicleType = "Passenger E-Rickshaw (Mayuri Pro)",
        passengerId = incoming.passengerId,
      ).isSuccess
    )
    assertTrue(repo.updateBookingStatusByDriver(incoming, BookingStatus.ARRIVING).isSuccess)
    assertTrue(repo.updateBookingStatusByDriver(incoming, BookingStatus.STARTED).isSuccess)
    assertTrue(repo.updateBookingStatusByDriver(incoming, BookingStatus.COMPLETED, finalFare = 85.0).isSuccess)

    val completedDriver = repo.observeDriverProfile(driverUid).first { (it?.totalTrips ?: 0L) >= 1L }!!
    assertEquals(1L, completedDriver.totalTrips)
    assertEquals(85.0, completedDriver.totalEarnings, 0.01)

    // 7. Suspended driver is immediately taken offline & blocked from going online
    signInVerifiedAdmin()
    assertTrue(repo.updateDriverStatusAdmin(driverUid, DriverStatus.SUSPENDED, "Complaint review").isSuccess)
    signInTestUser(driverEmail)
    val suspendedOnlineAttempt = repo.setDriverOnlineStatus(true, 26.4042, 88.2485, true)
    assertTrue(suspendedOnlineAttempt.isFailure)
  }

  @Test
  fun adminDashboard_fareRules_complaintReplies_userSuspension_adminRegistry_andAuditLogImmutability() = runBlocking {
    withTimeout(30_000L) {
      val repo = TotoGoRepository(firestore, auth)
      val passengerEmail = "pax_adm_${UUID.randomUUID()}@test.com"
      val opsAdminEmail = "ops_adm_${UUID.randomUUID()}@test.com"

      // 1. Passenger registers & submits a complaint
      val paxUid = signInTestUser(passengerEmail)
      assertTrue(
        repo.createOrUpdateUserProfile(
          fullName = "Bapi Sarkar",
          email = passengerEmail,
          mobileNumber = "+919866667777",
          mobileVerified = true,
          preferredLanguage = "bn",
          role = UserRole.PASSENGER,
          photoBase64 = null,
        ).isSuccess
      )
      assertTrue(
        repo.submitComplaint(
          reporterRole = UserRole.PASSENGER,
          targetUserId = "driver_unknown",
          bookingId = null,
          category = "Fare overcharge",
          description = "Requested ₹10 extra above fixed rate",
        ).isSuccess
      )
      val submittedComplaint = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMyComplaints(paxUid).first { it.isNotEmpty() }.first()
      }

      // 2. Non-admin MUST NOT alter fare rules, suspend users, update complaints, or grant admin
      assertTrue(repo.saveFareSettings(com.example.data.model.FareSettings(minFare = 999.0)).isFailure)
      assertTrue(repo.updateComplaintAdmin(submittedComplaint.id, "resolved", "Hacked", paxUid).isFailure)
      assertTrue(repo.grantAdminAccess(paxUid, passengerEmail).isFailure)

      // Create secondary account UID to test authorized admin delegation
      val opsUid = signInTestUser(opsAdminEmail)

      // 3. Verified Admin logs in, updates fare rules, suspends & restores passenger, replies to complaint, and authorizes secondary admin
      val adminUid = signInVerifiedAdmin()
      assertTrue(repo.checkIsAdmin(adminUid, TotoGoRepository.BOOTSTRAPPED_ADMIN_EMAIL, true))

      assertTrue(
        repo.saveFareSettings(
          com.example.data.model.FareSettings(
            minFare = 25.0,
            perKmFare = 14.0,
            minDistanceKm = 1.5,
            additionalDistanceFarePerKm = 11.0,
            waitingChargePerMin = 3.0,
          )
        ).isSuccess
      )
      assertTrue(repo.addFixedRoute("Chopra Bazar", "Haptiagachh", 35.0).isSuccess)
      assertTrue(repo.toggleUserSuspensionAdmin(paxUid, true).isSuccess)
      val suspendedPax = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserProfile(paxUid).first { it?.isSuspended == true }!!
      }
      assertTrue(suspendedPax.isSuspended)
      assertTrue(repo.toggleUserSuspensionAdmin(paxUid, false).isSuccess)

      assertTrue(
        repo.updateComplaintAdmin(
          complaintId = submittedComplaint.id,
          status = com.example.data.model.ComplaintStatus.RESOLVED,
          notes = "Issued fare warning to driver.",
          reporterId = paxUid,
        ).isSuccess
      )
      val resolvedComplaint = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAllComplaintsAdmin().first { list ->
          list.firstOrNull { it.id == submittedComplaint.id }?.status == com.example.data.model.ComplaintStatus.RESOLVED
        }.first { it.id == submittedComplaint.id }
      }
      assertEquals("Issued fare warning to driver.", resolvedComplaint.adminNotes)

      // Grant secondary admin access & verify immutable audit trail
      assertTrue(repo.grantAdminAccess(opsUid, opsAdminEmail).isSuccess)
      val auditLogs = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAuditLogsAdmin().first { logs ->
          logs.any { it.action == "UPDATE_FARE_SETTINGS" } &&
            logs.any { it.action == "UPDATE_COMPLAINT" } &&
            logs.any { it.action == "GRANT_ADMIN" }
        }
      }
      assertTrue(auditLogs.size >= 4)

      val firstLogId = auditLogs.first().id
      val tamperErr = runCatching {
        withTimeout(TIMEOUT_MS) {
          firestore.collection("admin_audit_logs").document(firstLogId).delete().await()
        }
      }.exceptionOrNull()
      assertEquals(
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        tamperErr?.unwrapFirestoreException()?.code,
      )

      // Secondary authorized UID admin passes checkIsAdmin() & can list bookings/users
      signInTestUser(opsAdminEmail)
      assertTrue(repo.checkIsAdmin(opsUid, opsAdminEmail, false))
      val allUsersFromOps = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAllUsersAdmin().first { list -> list.any { it.userId == paxUid } }
      }
      assertTrue(allUsersFromOps.isNotEmpty())
    }
  }

  private companion object {
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
