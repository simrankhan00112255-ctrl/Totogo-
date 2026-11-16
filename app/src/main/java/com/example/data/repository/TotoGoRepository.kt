package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.AppNotification
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Complaint
import com.example.data.model.ComplaintStatus
import com.example.data.model.DriverProfile
import com.example.data.model.DriverPublicProfile
import com.example.data.model.DriverStatus
import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import com.example.data.model.RideRating
import com.example.data.model.UserProfile
import com.example.data.model.VehicleRecord
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
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

class TotoGoRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> Flow<T>.handleFlowErrors(op: OperationType, path: String, expectedUid: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
        handleFirestoreError(exceptionToLog, op, path, auth)
      }
      throw error
    }

  // --- Admin Authorization Check ---
  suspend fun checkIsAdmin(uid: String, email: String?, isEmailVerified: Boolean): Boolean {
    if (email == BOOTSTRAPPED_ADMIN_EMAIL && isEmailVerified) return true
    val docRef = db.collection("admins").document(uid)
    return try {
      docRef.get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  // --- Users ---
  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val activeUid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .handleFlowErrors(OperationType.GET, docRef.path, activeUid)
    )
  }

  suspend fun createOrUpdateUserProfile(
    fullName: String,
    email: String,
    mobileNumber: String,
    mobileVerified: Boolean,
    preferredLanguage: String,
    role: String,
    photoBase64: String?,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val profile = UserProfile(
          userId = uid,
          fullName = fullName.trim(),
          email = email.trim(),
          mobileNumber = mobileNumber.trim(),
          mobileVerified = mobileVerified,
          preferredLanguage = preferredLanguage,
          role = role,
          photoBase64 = photoBase64,
          isSuspended = false,
        )
        docRef.set(profile.toCreateMap()).await()
      } else {
        val updates = mutableMapOf<String, Any>(
          "fullName" to fullName.trim(),
          "preferredLanguage" to preferredLanguage,
          "role" to role,
          "mobileVerified" to mobileVerified,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (mobileNumber.isNotBlank()) updates["mobileNumber"] = mobileNumber.trim()
        if (email.isNotBlank()) updates["email"] = email.trim()
        if (!photoBase64.isNullOrBlank()) updates["photoBase64"] = photoBase64
        docRef.update(updates).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateUserLanguage(preferredLanguage: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef.update(
        mapOf(
          "preferredLanguage" to preferredLanguage,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun switchUserRole(role: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef.update(
        mapOf(
          "role" to role,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  // --- Driver Registration & Profile ---
  fun observeDriverProfile(driverId: String): Flow<DriverProfile?> = flow {
    val activeUid = requireUserId()
    val docRef = db.collection("drivers").document(driverId)
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else null
        }
        .handleFlowErrors(OperationType.GET, docRef.path, activeUid)
    )
  }

  fun observeVehicle(driverId: String): Flow<VehicleRecord?> = flow {
    val activeUid = requireUserId()
    val docRef = db.collection("vehicles").document(driverId)
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(VehicleRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else null
        }
        .handleFlowErrors(OperationType.GET, docRef.path, activeUid)
    )
  }

  suspend fun submitDriverRegistration(
    fullName: String,
    mobileNumber: String,
    mobileVerified: Boolean,
    registrationNumber: String,
    vehicleType: String,
    vehicleModel: String,
    serviceArea: String,
    licenceNumber: String?,
    permitNumber: String?,
    driverPhotoBase64: String?,
    frontPhotoBase64: String?,
    rearPhotoBase64: String?,
    sidePhotoBase64: String?,
    licenceDocBase64: String?,
    permitDocBase64: String?,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val driverRef = db.collection("drivers").document(uid)
    val vehicleRef = db.collection("vehicles").document(uid)
    val cleanReg = registrationNumber.trim().uppercase()
    val cleanType = vehicleType.trim()
    val cleanModel = vehicleModel.trim()
    try {
      val existingDriver = driverRef.get().await()
      if (!existingDriver.exists()) {
        val driver = DriverProfile(
          driverId = uid,
          fullName = fullName.trim(),
          mobileNumber = mobileNumber.trim(),
          mobileVerified = mobileVerified,
          registrationNumber = cleanReg,
          vehicleType = cleanType,
          vehicleModel = cleanModel,
          serviceArea = serviceArea.trim(),
          licenceNumber = licenceNumber?.trim()?.takeIf { it.isNotBlank() },
          permitNumber = permitNumber?.trim()?.takeIf { it.isNotBlank() },
          profilePhotoBase64 = driverPhotoBase64,
        )
        driverRef.set(driver.toCreateMap()).await()
      } else {
        val updates = mutableMapOf<String, Any>(
          "fullName" to fullName.trim(),
          "mobileNumber" to mobileNumber.trim(),
          "mobileVerified" to mobileVerified,
          "registrationNumber" to cleanReg,
          "vehicleType" to cleanType,
          "serviceArea" to serviceArea.trim(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (cleanModel.isNotBlank()) updates["vehicleModel"] = cleanModel
        if (existingDriver.getString("status") == DriverStatus.REJECTED) {
          updates["status"] = DriverStatus.PENDING_VERIFICATION
        }
        if (!licenceNumber.isNullOrBlank()) updates["licenceNumber"] = licenceNumber.trim()
        if (!permitNumber.isNullOrBlank()) updates["permitNumber"] = permitNumber.trim()
        if (!driverPhotoBase64.isNullOrBlank()) updates["profilePhotoBase64"] = driverPhotoBase64
        driverRef.update(updates).await()
      }

      val existingVehicle = vehicleRef.get().await()
      if (!existingVehicle.exists()) {
        val vehicle = VehicleRecord(
          driverId = uid,
          registrationNumber = cleanReg,
          vehicleType = cleanType,
          vehicleModel = cleanModel,
          frontPhotoBase64 = frontPhotoBase64,
          rearPhotoBase64 = rearPhotoBase64,
          sidePhotoBase64 = sidePhotoBase64,
          licenceDocBase64 = licenceDocBase64,
          permitDocBase64 = permitDocBase64,
        )
        vehicleRef.set(vehicle.toCreateMap()).await()
      } else {
        val vUpdates = mutableMapOf<String, Any>(
          "registrationNumber" to cleanReg,
          "vehicleType" to cleanType,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (cleanModel.isNotBlank()) vUpdates["vehicleModel"] = cleanModel
        if (!frontPhotoBase64.isNullOrBlank()) vUpdates["frontPhotoBase64"] = frontPhotoBase64
        if (!rearPhotoBase64.isNullOrBlank()) vUpdates["rearPhotoBase64"] = rearPhotoBase64
        if (!sidePhotoBase64.isNullOrBlank()) vUpdates["sidePhotoBase64"] = sidePhotoBase64
        if (!licenceDocBase64.isNullOrBlank()) vUpdates["licenceDocBase64"] = licenceDocBase64
        if (!permitDocBase64.isNullOrBlank()) vUpdates["permitDocBase64"] = permitDocBase64
        vehicleRef.update(vUpdates).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, driverRef.path, auth)
      throw e
    }
  }

  // --- Approved Public Drivers ---
  fun observeVerifiedOnlineDrivers(): Flow<List<DriverPublicProfile>> = flow {
    val activeUid = requireUserId()
    val path = "drivers_public"
    emitAll(
      db.collection(path)
        .whereEqualTo("isVerified", true)
        .whereEqualTo("isOnline", true)
        .snapshots()
        .map { snap -> snap.toObjects(DriverPublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeDriverPublic(driverId: String): Flow<DriverPublicProfile?> = flow {
    val activeUid = requireUserId()
    val docRef = db.collection("drivers_public").document(driverId)
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(DriverPublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else null
        }
        .handleFlowErrors(OperationType.GET, docRef.path, activeUid)
    )
  }

  suspend fun setDriverOnlineStatus(
    isOnline: Boolean,
    lat: Double?,
    lng: Double?,
    hasGpsFix: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("drivers_public").document(uid)
    val updates = mutableMapOf<String, Any>(
      "isOnline" to isOnline,
      "hasGpsFix" to hasGpsFix,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (lat != null && lng != null) {
      updates["currentLat"] = lat
      updates["currentLng"] = lng
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateDriverLiveGps(lat: Double, lng: Double): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("drivers_public").document(uid)
    try {
      docRef.update(
        mapOf(
          "currentLat" to lat,
          "currentLng" to lng,
          "hasGpsFix" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  // --- Bookings ---
  suspend fun createBooking(booking: Booking): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("bookings").document()
    val payload = booking.copy(passengerId = uid).toCreateMap()
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  fun observePassengerBookings(passengerId: String): Flow<List<Booking>> = flow {
    val activeUid = requireUserId()
    val path = "bookings"
    emitAll(
      db.collection(path)
        .whereEqualTo("passengerId", passengerId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeSearchingBookings(): Flow<List<Booking>> = flow {
    val activeUid = requireUserId()
    val path = "bookings"
    emitAll(
      db.collection(path)
        .whereEqualTo("status", BookingStatus.SEARCHING)
        .snapshots()
        .map { snap ->
          snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeDriverBookings(driverId: String): Flow<List<Booking>> = flow {
    val activeUid = requireUserId()
    val path = "bookings"
    emitAll(
      db.collection(path)
        .whereEqualTo("driverId", driverId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  // Atomic transaction prevents duplicate booking acceptance by multiple drivers
  suspend fun acceptBookingTransaction(
    bookingId: String,
    driverName: String,
    driverPhone: String,
    vehicleRegNumber: String,
    vehicleType: String,
    passengerId: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val bookingRef = db.collection("bookings").document(bookingId)
    try {
      db.runTransaction { transaction ->
        val snap = transaction.get(bookingRef)
        if (!snap.exists()) throw IllegalStateException("Booking not found")
        val currentStatus = snap.getString("status")
        val currentDriver = snap.getString("driverId")
        if (currentStatus != BookingStatus.SEARCHING || currentDriver != null) {
          throw IllegalStateException("Booking already accepted by another driver")
        }
        val updates = mutableMapOf<String, Any>(
          "driverId" to uid,
          "driverName" to driverName,
          "vehicleRegNumber" to vehicleRegNumber,
          "vehicleType" to vehicleType,
          "status" to BookingStatus.ASSIGNED,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (driverPhone.isNotBlank()) {
          updates["driverPhone"] = driverPhone.trim().take(20)
        }
        transaction.update(bookingRef, updates)
      }.await()

      sendNotification(
        recipientId = passengerId,
        bookingId = bookingId,
        title = "TOTO Driver Assigned",
        body = "$driverName ($vehicleRegNumber) accepted your ride request.",
        type = "booking_accepted",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
      throw e
    }
  }

  suspend fun updateBookingStatusByDriver(
    booking: Booking,
    newStatus: String,
    finalFare: Double? = null,
    cancelReason: String? = null,
    driverLat: Double? = null,
    driverLng: Double? = null,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val bookingRef = db.collection("bookings").document(booking.id)
    val updates = mutableMapOf<String, Any>(
      "status" to newStatus,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (finalFare != null) updates["finalFare"] = finalFare
    if (!cancelReason.isNullOrBlank()) updates["cancelReason"] = cancelReason
    if (driverLat != null && driverLng != null) {
      updates["driverLat"] = driverLat
      updates["driverLng"] = driverLng
    }
    try {
      bookingRef.update(updates).await()

      if (newStatus == BookingStatus.COMPLETED) {
        val earned = finalFare ?: booking.estimatedFare
        db.collection("drivers").document(uid).update(
          mapOf(
            "totalTrips" to FieldValue.increment(1L),
            "totalEarnings" to FieldValue.increment(earned),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      val notifTitle = when (newStatus) {
        BookingStatus.ARRIVING -> "Driver Arriving"
        BookingStatus.STARTED -> "Trip Started"
        BookingStatus.COMPLETED -> "Trip Completed"
        BookingStatus.CANCELLED -> "Booking Cancelled"
        else -> "Ride Update"
      }
      val notifBody = when (newStatus) {
        BookingStatus.ARRIVING -> "${booking.driverName ?: "Driver"} is arriving at ${booking.pickupName}."
        BookingStatus.STARTED -> "Your ride to ${booking.destinationName} has started."
        BookingStatus.COMPLETED -> "Trip finished. Fare: ₹${(finalFare ?: booking.estimatedFare).toInt()} (Cash)."
        BookingStatus.CANCELLED -> "Your booking was cancelled."
        else -> "Status updated to $newStatus"
      }
      sendNotification(
        recipientId = booking.passengerId,
        bookingId = booking.id,
        title = notifTitle,
        body = notifBody,
        type = "booking_$newStatus",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
      throw e
    }
  }

  suspend fun updateActiveBookingDriverLocation(bookingId: String, lat: Double, lng: Double): Result<Unit> =
    runCatching {
      requireUserId()
      val bookingRef = db.collection("bookings").document(bookingId)
      try {
        bookingRef.update(
          mapOf(
            "driverLat" to lat,
            "driverLng" to lng,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
        throw e
      }
    }

  suspend fun cancelBookingByPassenger(booking: Booking, reason: String): Result<Unit> = runCatching {
    requireUserId()
    val bookingRef = db.collection("bookings").document(booking.id)
    try {
      bookingRef.update(
        mapOf(
          "status" to BookingStatus.CANCELLED,
          "cancelReason" to reason.take(200),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      if (!booking.driverId.isNullOrBlank()) {
        sendNotification(
          recipientId = booking.driverId,
          bookingId = booking.id,
          title = "Ride Cancelled",
          body = "${booking.passengerName} cancelled the booking.",
          type = "booking_cancelled",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
      throw e
    }
  }

  // --- Fare Settings & Fixed Routes ---
  fun observeFareSettings(): Flow<FareSettings> = flow {
    val activeUid = requireUserId()
    val docRef = db.collection("fare_settings").document("default")
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(FareSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?: FareSettings()
          } else {
            FareSettings()
          }
        }
        .handleFlowErrors(OperationType.GET, docRef.path, activeUid)
    )
  }

  fun observeFixedRoutes(): Flow<List<FixedRoute>> = flow {
    val activeUid = requireUserId()
    val path = "fare_settings/default/fixed_routes"
    emitAll(
      db.collection("fare_settings").document("default").collection("fixed_routes")
        .snapshots()
        .map { snap -> snap.toObjects(FixedRoute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  suspend fun saveFareSettings(settings: FareSettings): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("fare_settings").document("default")
    try {
      docRef.set(settings.toMap()).await()
      logAdminAction(
        action = "UPDATE_FARE_SETTINGS",
        targetType = "fare_settings",
        targetId = "default",
        details = "Min ₹${settings.minFare.toInt()}, ₹${settings.perKmFare.toInt()}/km, min ${settings.minDistanceKm}km",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }

  suspend fun addFixedRoute(origin: String, destination: String, fixedFare: Double): Result<Unit> =
    runCatching {
      requireUserId()
      val docRef = db.collection("fare_settings").document("default").collection("fixed_routes").document()
      try {
        docRef.set(
          FixedRoute(
            originName = origin.trim(),
            destinationName = destination.trim(),
            fixedFare = fixedFare,
          ).toMap()
        ).await()
        logAdminAction(
          action = "ADD_FIXED_ROUTE",
          targetType = "fixed_route",
          targetId = docRef.id,
          details = "${origin.trim()} <-> ${destination.trim()} @ ₹${fixedFare.toInt()}",
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
        throw e
      }
    }

  suspend fun deleteFixedRoute(routeId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("fare_settings").document("default").collection("fixed_routes").document(routeId)
    try {
      docRef.delete().await()
      logAdminAction(
        action = "DELETE_FIXED_ROUTE",
        targetType = "fixed_route",
        targetId = routeId,
        details = "Removed fixed route $routeId",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  // --- Ratings & Complaints ---
  suspend fun submitRating(
    bookingId: String,
    driverId: String,
    stars: Int,
    comment: String?,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ratingRef = db.collection("ratings").document(bookingId)
    val bookingRef = db.collection("bookings").document(bookingId)
    val publicRef = db.collection("drivers_public").document(driverId)
    try {
      db.runTransaction { transaction ->
        val publicSnap = transaction.get(publicRef)
        val oldAvg = publicSnap.getDouble("ratingAvg") ?: 5.0
        val oldCount = publicSnap.getLong("ratingCount") ?: 0L
        val newCount = oldCount + 1L
        val newAvg = ((oldAvg * oldCount) + stars) / newCount.toDouble()

        val ratingMap = mutableMapOf<String, Any>(
          "bookingId" to bookingId,
          "passengerId" to uid,
          "driverId" to driverId,
          "stars" to stars,
          "createdAt" to FieldValue.serverTimestamp(),
        )
        if (!comment.isNullOrBlank()) ratingMap["comment"] = comment.trim().take(300)

        transaction.set(ratingRef, ratingMap)
        transaction.update(
          bookingRef,
          mapOf("hasRated" to true, "updatedAt" to FieldValue.serverTimestamp()),
        )
        if (publicSnap.exists()) {
          transaction.update(
            publicRef,
            mapOf(
              "ratingAvg" to newAvg,
              "ratingCount" to newCount,
              "lastRatedBookingId" to bookingId,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ratingRef.path, auth)
      throw e
    }
  }

  fun observeDriverRatings(driverId: String): Flow<List<RideRating>> = flow {
    val activeUid = requireUserId()
    val path = "ratings"
    emitAll(
      db.collection(path)
        .whereEqualTo("driverId", driverId)
        .snapshots()
        .map { snap -> snap.toObjects(RideRating::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  suspend fun submitComplaint(
    reporterRole: String,
    targetUserId: String,
    bookingId: String?,
    category: String,
    description: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("complaints").document()
    val payload = mutableMapOf<String, Any>(
      "reporterId" to uid,
      "reporterRole" to reporterRole,
      "targetUserId" to targetUserId,
      "category" to category.trim(),
      "description" to description.trim(),
      "status" to ComplaintStatus.OPEN,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (!bookingId.isNullOrBlank()) payload["bookingId"] = bookingId
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  fun observeMyComplaints(uid: String): Flow<List<Complaint>> = flow {
    val activeUid = requireUserId()
    val path = "complaints"
    emitAll(
      db.collection(path)
        .whereEqualTo("reporterId", uid)
        .snapshots()
        .map { snap -> snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  // --- Notifications ---
  suspend fun sendNotification(
    recipientId: String,
    bookingId: String?,
    title: String,
    body: String,
    type: String,
  ) {
    val senderId = auth.currentUser?.uid ?: return
    val docRef = db.collection("notifications").document()
    val payload = mutableMapOf<String, Any>(
      "userId" to recipientId,
      "senderId" to senderId,
      "title" to title.take(100),
      "body" to body.take(300),
      "type" to type.take(50),
      "isRead" to false,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (!bookingId.isNullOrBlank()) payload["bookingId"] = bookingId
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
    }
  }

  fun observeMyNotifications(uid: String): Flow<List<AppNotification>> = flow {
    val activeUid = requireUserId()
    val path = "notifications"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  suspend fun markNotificationRead(notificationId: String) {
    val docRef = db.collection("notifications").document(notificationId)
    try {
      docRef.update("isRead", true).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
    }
  }

  // --- Admin Operations ---
  fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
    val activeUid = requireUserId()
    val path = "users"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllDriversAdmin(): Flow<List<DriverProfile>> = flow {
    val activeUid = requireUserId()
    val path = "drivers"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllVehiclesAdmin(): Flow<List<VehicleRecord>> = flow {
    val activeUid = requireUserId()
    val path = "vehicles"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(VehicleRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllBookingsAdmin(): Flow<List<Booking>> = flow {
    val activeUid = requireUserId()
    val path = "bookings"
    emitAll(
      db.collection(path)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snap -> snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllComplaintsAdmin(): Flow<List<Complaint>> = flow {
    val activeUid = requireUserId()
    val path = "complaints"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllRatingsAdmin(): Flow<List<RideRating>> = flow {
    val activeUid = requireUserId()
    val path = "ratings"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(RideRating::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAuditLogsAdmin(): Flow<List<AdminAuditLog>> = flow {
    val activeUid = requireUserId()
    val path = "admin_audit_logs"
    emitAll(
      db.collection(path)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(100)
        .snapshots()
        .map { snap -> snap.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  fun observeAllAdminsAdmin(): Flow<List<AdminRecord>> = flow {
    val activeUid = requireUserId()
    val path = "admins"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleFlowErrors(OperationType.LIST, path, activeUid)
    )
  }

  private suspend fun logAdminAction(
    action: String,
    targetType: String,
    targetId: String,
    details: String,
  ) {
    val adminUser = auth.currentUser ?: return
    val docRef = db.collection("admin_audit_logs").document()
    val payload = mapOf(
      "adminId" to adminUser.uid,
      "adminEmail" to (adminUser.email?.takeIf { it.isNotBlank() } ?: "admin@totogo"),
      "action" to action.take(80),
      "targetType" to targetType.take(50),
      "targetId" to targetId.take(128),
      "details" to details.take(300),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
    }
  }

  suspend fun approveDriverAdmin(driver: DriverProfile, vehicle: VehicleRecord?): Result<Unit> =
    runCatching {
      requireUserId()
      val driverRef = db.collection("drivers").document(driver.driverId)
      val vehicleRef = db.collection("vehicles").document(driver.driverId)
      val publicRef = db.collection("drivers_public").document(driver.driverId)
      try {
        driverRef.update(
          mapOf(
            "status" to DriverStatus.APPROVED,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()

        if (vehicle != null) {
          vehicleRef.update(
            mapOf(
              "photosVerified" to true,
              "docsVerified" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }

        val existingPublic = publicRef.get().await()
        val publicPayload = mutableMapOf<String, Any>(
          "driverId" to driver.driverId,
          "fullName" to driver.fullName,
          "registrationNumber" to driver.registrationNumber,
          "vehicleType" to driver.vehicleType,
          "serviceArea" to driver.serviceArea,
          "isVerified" to true,
          "isOnline" to (existingPublic.getBoolean("isOnline") ?: false),
          "currentLat" to (existingPublic.getDouble("currentLat") ?: 26.4042),
          "currentLng" to (existingPublic.getDouble("currentLng") ?: 88.2485),
          "hasGpsFix" to (existingPublic.getBoolean("hasGpsFix") ?: false),
          "ratingAvg" to (existingPublic.getDouble("ratingAvg") ?: 5.0),
          "ratingCount" to (existingPublic.getLong("ratingCount") ?: 0L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        driver.vehicleModel.takeIf { it.isNotBlank() }?.let {
          publicPayload["vehicleModel"] = it
        }
        driver.profilePhotoBase64?.takeIf { it.isNotBlank() }?.let {
          publicPayload["profilePhotoBase64"] = it
        }
        vehicle?.frontPhotoBase64?.takeIf { it.isNotBlank() }?.let {
          publicPayload["frontPhotoBase64"] = it
        }
        vehicle?.rearPhotoBase64?.takeIf { it.isNotBlank() }?.let {
          publicPayload["rearPhotoBase64"] = it
        }
        vehicle?.sidePhotoBase64?.takeIf { it.isNotBlank() }?.let {
          publicPayload["sidePhotoBase64"] = it
        }

        publicRef.set(publicPayload).await()

        sendNotification(
          recipientId = driver.driverId,
          bookingId = null,
          title = "Driver Account Approved",
          body = "Your TOTO (${driver.registrationNumber}) is verified. You can now go online!",
          type = "driver_approved",
        )
        logAdminAction(
          action = "APPROVE_DRIVER",
          targetType = "driver",
          targetId = driver.driverId,
          details = "Approved ${driver.fullName} (${driver.registrationNumber})",
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, driverRef.path, auth)
        throw e
      }
    }

  suspend fun updateDriverStatusAdmin(
    driverId: String,
    newStatus: String,
    reason: String?,
  ): Result<Unit> = runCatching {
    requireUserId()
    val driverRef = db.collection("drivers").document(driverId)
    val publicRef = db.collection("drivers_public").document(driverId)
    try {
      val updates = mutableMapOf<String, Any>(
        "status" to newStatus,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (!reason.isNullOrBlank()) updates["rejectionReason"] = reason.trim()
      driverRef.update(updates).await()

      val publicSnap = publicRef.get().await()
      if (publicSnap.exists() && newStatus != DriverStatus.APPROVED) {
        publicRef.update(
          mapOf(
            "isVerified" to false,
            "isOnline" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      sendNotification(
        recipientId = driverId,
        bookingId = null,
        title = "Driver Status Updated",
        body = "Your driver verification status is now: $newStatus",
        type = "driver_status",
      )
      logAdminAction(
        action = "UPDATE_DRIVER_STATUS",
        targetType = "driver",
        targetId = driverId,
        details = "Set status=$newStatus (${reason?.trim().orEmpty().ifBlank { "Admin action" }})",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, driverRef.path, auth)
      throw e
    }
  }

  suspend fun toggleUserSuspensionAdmin(userId: String, suspend: Boolean): Result<Unit> = runCatching {
    requireUserId()
    val userRef = db.collection("users").document(userId)
    try {
      userRef.update(
        mapOf(
          "isSuspended" to suspend,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAdminAction(
        action = if (suspend) "SUSPEND_USER" else "UNSUSPEND_USER",
        targetType = "user",
        targetId = userId,
        details = if (suspend) "Suspended user account" else "Restored user account",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
      throw e
    }
  }

  suspend fun updateComplaintAdmin(
    complaintId: String,
    status: String,
    notes: String?,
    reporterId: String? = null,
  ): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("complaints").document(complaintId)
    val cleanNotes = notes?.trim()?.take(300)
    val updates = mutableMapOf<String, Any>(
      "status" to status,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!cleanNotes.isNullOrBlank()) updates["adminNotes"] = cleanNotes
    try {
      ref.update(updates).await()
      if (!reporterId.isNullOrBlank()) {
        sendNotification(
          recipientId = reporterId,
          bookingId = null,
          title = "Support Ticket Update ($status)",
          body = cleanNotes?.ifBlank { "Your support ticket status is now $status." }
            ?: "Your support ticket status is now $status.",
          type = "complaint_reply",
        )
      }
      logAdminAction(
        action = "UPDATE_COMPLAINT",
        targetType = "complaint",
        targetId = complaintId,
        details = "Status=$status${if (!cleanNotes.isNullOrBlank()) ", Reply: $cleanNotes" else ""}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path, auth)
      throw e
    }
  }

  suspend fun grantAdminAccess(targetUid: String, targetEmail: String): Result<Unit> = runCatching {
    requireUserId()
    val cleanUid = targetUid.trim()
    val cleanEmail = targetEmail.trim().lowercase()
    require(cleanUid.isNotEmpty() && cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
      "Invalid Firebase UID format"
    }
    require(cleanEmail.contains("@") && cleanEmail.length >= 5) {
      "Valid Google email address is required"
    }
    val ref = db.collection("admins").document(cleanUid)
    try {
      ref.set(
        mapOf(
          "uid" to cleanUid,
          "email" to cleanEmail,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      logAdminAction(
        action = "GRANT_ADMIN",
        targetType = "admin",
        targetId = cleanUid,
        details = "Authorized admin access for $cleanEmail",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
      throw e
    }
  }

  suspend fun revokeAdminAccess(targetUid: String): Result<Unit> = runCatching {
    val activeUid = requireUserId()
    val cleanUid = targetUid.trim()
    require(cleanUid != activeUid) { "Cannot revoke your own active admin access" }
    val ref = db.collection("admins").document(cleanUid)
    try {
      ref.delete().await()
      logAdminAction(
        action = "REVOKE_ADMIN",
        targetType = "admin",
        targetId = cleanUid,
        details = "Revoked admin access for UID $cleanUid",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
      throw e
    }
  }

  companion object {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "aliaktar919@gmail.com"
  }
}
