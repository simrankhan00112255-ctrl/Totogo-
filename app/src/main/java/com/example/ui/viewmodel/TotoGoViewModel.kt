package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ChopraLocations
import com.example.core.NotificationHelper
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.AppNotification
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Complaint
import com.example.data.model.DriverProfile
import com.example.data.model.DriverPublicProfile
import com.example.data.model.DriverStatus
import com.example.data.model.FareSettings
import com.example.data.model.FixedRoute
import com.example.data.model.RideRating
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VehicleRecord
import com.example.data.repository.TotoGoRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TotoGoViewModel(
  private val repository: TotoGoRepository,
  val currentUserId: String,
  val userEmail: String,
  val userDisplayName: String,
  val isEmailVerified: Boolean,
  private val appContext: Context,
) : ViewModel() {

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _dismissedBookingIds = MutableStateFlow<Set<String>>(emptySet())
  val dismissedBookingIds: StateFlow<Set<String>> = _dismissedBookingIds.asStateFlow()

  private var seenNotificationIds = mutableSetOf<String>()

  init {
    viewModelScope.launch {
      _isAdmin.value = repository.checkIsAdmin(currentUserId, userEmail, isEmailVerified)
    }
  }

  private fun <T> Flow<T>.unwrapCatch(fallback: T): Flow<T> = catch { error ->
    val fsErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (fsErr == null && error is CancellationException) throw error
    Log.w(TAG, "Stream error: ${(fsErr ?: error).message}")
    emit(fallback)
  }

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        val fsErr = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        emit(UiState.Error((fsErr ?: error).message ?: "Failed to load profile"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val driverProfile: StateFlow<DriverProfile?> =
    repository.observeDriverProfile(currentUserId)
      .unwrapCatch(null)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val vehicleRecord: StateFlow<VehicleRecord?> =
    repository.observeVehicle(currentUserId)
      .unwrapCatch(null)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val myDriverPublic: StateFlow<DriverPublicProfile?> =
    repository.observeDriverPublic(currentUserId)
      .unwrapCatch(null)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val verifiedOnlineDrivers: StateFlow<List<DriverPublicProfile>> =
    repository.observeVerifiedOnlineDrivers()
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val passengerBookings: StateFlow<List<Booking>> =
    repository.observePassengerBookings(currentUserId)
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val searchingBookings: StateFlow<List<Booking>> =
    repository.observeSearchingBookings()
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val driverBookings: StateFlow<List<Booking>> =
    repository.observeDriverBookings(currentUserId)
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val driverRatings: StateFlow<List<RideRating>> =
    repository.observeDriverRatings(currentUserId)
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val fareSettings: StateFlow<FareSettings> =
    repository.observeFareSettings()
      .unwrapCatch(FareSettings())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FareSettings())

  val fixedRoutes: StateFlow<List<FixedRoute>> =
    repository.observeFixedRoutes()
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val notifications: StateFlow<List<AppNotification>> =
    repository.observeMyNotifications(currentUserId)
      .onEach { list ->
        list.filter { !it.isRead && seenNotificationIds.add(it.id) }
          .take(2)
          .forEach { notif ->
            NotificationHelper.showLocalNotification(
              appContext,
              notif.id.hashCode(),
              notif.title,
              notif.body,
            )
          }
      }
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val myComplaints: StateFlow<List<Complaint>> =
    repository.observeMyComplaints(currentUserId)
      .unwrapCatch(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val activeBookingPublicDriver: StateFlow<DriverPublicProfile?> =
    passengerBookings.flatMapLatest { bookings ->
      val active = bookings.firstOrNull {
        it.status in listOf(
          BookingStatus.ASSIGNED,
          BookingStatus.ARRIVING,
          BookingStatus.STARTED,
          BookingStatus.COMPLETED,
        ) && !it.driverId.isNullOrBlank()
      }
      val dId = active?.driverId
      if (dId.isNullOrBlank()) flowOf(null)
      else repository.observeDriverPublic(dId).unwrapCatch(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  // Admin-gated streams (only subscribed when isAdmin == true)
  val adminUsers: StateFlow<List<UserProfile>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllUsersAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminDrivers: StateFlow<List<DriverProfile>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllDriversAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminVehicles: StateFlow<List<VehicleRecord>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllVehiclesAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminBookings: StateFlow<List<Booking>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllBookingsAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminComplaints: StateFlow<List<Complaint>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllComplaintsAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminRatings: StateFlow<List<RideRating>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllRatingsAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminAuditLogs: StateFlow<List<AdminAuditLog>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAuditLogsAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminRegistry: StateFlow<List<AdminRecord>> =
    isAdmin.flatMapLatest { admin ->
      if (!admin) flowOf(emptyList()) else repository.observeAllAdminsAdmin().unwrapCatch(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun postMessage(message: String) {
    _actionMessage.value = message
  }

  fun dismissIncomingBooking(bookingId: String) {
    _dismissedBookingIds.value = _dismissedBookingIds.value + bookingId
  }

  fun completeUserRegistration(
    fullName: String,
    mobileNumber: String,
    mobileVerified: Boolean,
    preferredLanguage: String,
    role: String,
    photoBase64: String?,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.createOrUpdateUserProfile(
        fullName = fullName,
        email = userEmail,
        mobileNumber = mobileNumber,
        mobileVerified = mobileVerified,
        preferredLanguage = preferredLanguage,
        role = if (role == UserRole.DRIVER) UserRole.DRIVER else UserRole.PASSENGER,
        photoBase64 = photoBase64,
      )
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Registration failed" }
    }
  }

  fun switchRole(role: String) {
    viewModelScope.launch {
      val target = when (role) {
        UserRole.ADMIN -> if (_isAdmin.value) UserRole.ADMIN else UserRole.PASSENGER
        UserRole.DRIVER -> UserRole.DRIVER
        else -> UserRole.PASSENGER
      }
      if (target == UserRole.ADMIN) {
        // Admin panel is accessed directly via verified isAdmin flag; keep Firestore role safe
        repository.switchUserRole(UserRole.PASSENGER)
      } else {
        repository.switchUserRole(target)
      }
    }
  }

  fun updatePreferredLanguage(langCode: String) {
    viewModelScope.launch {
      repository.updateUserLanguage(langCode)
    }
  }

  fun submitDriverApplication(
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
    onSuccessMsg: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.submitDriverRegistration(
        fullName = fullName,
        mobileNumber = mobileNumber,
        mobileVerified = mobileVerified,
        registrationNumber = registrationNumber,
        vehicleType = vehicleType,
        vehicleModel = vehicleModel,
        serviceArea = serviceArea,
        licenceNumber = licenceNumber,
        permitNumber = permitNumber,
        driverPhotoBase64 = driverPhotoBase64,
        frontPhotoBase64 = frontPhotoBase64,
        rearPhotoBase64 = rearPhotoBase64,
        sidePhotoBase64 = sidePhotoBase64,
        licenceDocBase64 = licenceDocBase64,
        permitDocBase64 = permitDocBase64,
      )
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = onSuccessMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to submit driver application" }
    }
  }

  fun setDriverOnline(isOnline: Boolean, lat: Double?, lng: Double?, hasGpsFix: Boolean) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.setDriverOnlineStatus(isOnline, lat, lng, hasGpsFix)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update online status" }
    }
  }

  fun updateDriverLiveGps(lat: Double, lng: Double) {
    viewModelScope.launch {
      repository.updateDriverLiveGps(lat, lng)
    }
  }

  fun bookToto(
    passengerName: String,
    passengerMobile: String?,
    pickupName: String,
    pickupLat: Double,
    pickupLng: Double,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double,
    onBooked: () -> Unit = {},
  ) {
    val calc = ChopraLocations.calculateFare(
      pickupName = pickupName,
      pickupLat = pickupLat,
      pickupLng = pickupLng,
      destinationName = destinationName,
      destinationLat = destinationLat,
      destinationLng = destinationLng,
      settings = fareSettings.value,
      fixedRoutes = fixedRoutes.value,
    )
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.createBooking(
        Booking(
          passengerId = currentUserId,
          passengerName = passengerName,
          passengerMobile = passengerMobile?.takeIf { it.isNotBlank() },
          pickupName = pickupName,
          pickupLat = pickupLat,
          pickupLng = pickupLng,
          destinationName = destinationName,
          destinationLat = destinationLat,
          destinationLng = destinationLng,
          distanceKm = calc.distanceKm,
          estimatedFare = calc.estimatedFare,
          paymentMethod = "cash",
        )
      )
      _isBusy.value = false
      res.onSuccess { onBooked() }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Booking failed" }
    }
  }

  fun updateAccountProfile(
    fullName: String,
    mobileNumber: String,
    mobileVerified: Boolean,
    preferredLanguage: String,
    currentRole: String,
    photoBase64: String?,
    successMsg: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.createOrUpdateUserProfile(
        fullName = fullName,
        email = userEmail,
        mobileNumber = mobileNumber,
        mobileVerified = mobileVerified,
        preferredLanguage = preferredLanguage,
        role = if (currentRole == UserRole.DRIVER) UserRole.DRIVER else UserRole.PASSENGER,
        photoBase64 = photoBase64,
      )
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Profile update failed" }
    }
  }

  fun cancelBookingByPassenger(booking: Booking, reason: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.cancelBookingByPassenger(booking, reason)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Could not cancel booking" }
    }
  }

  fun acceptBooking(booking: Booking, driver: DriverProfile) {
    val fullVehicleLabel = if (driver.vehicleModel.isNotBlank()) {
      "${driver.vehicleType} (${driver.vehicleModel})"
    } else {
      driver.vehicleType
    }
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.acceptBookingTransaction(
        bookingId = booking.id,
        driverName = driver.fullName,
        driverPhone = driver.mobileNumber,
        vehicleRegNumber = driver.registrationNumber,
        vehicleType = fullVehicleLabel.take(80),
        passengerId = booking.passengerId,
      )
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Booking already taken or unavailable" }
    }
  }

  fun progressDriverBooking(
    booking: Booking,
    newStatus: String,
    finalFare: Double? = null,
    driverLat: Double? = null,
    driverLng: Double? = null,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateBookingStatusByDriver(
        booking = booking,
        newStatus = newStatus,
        finalFare = finalFare,
        driverLat = driverLat,
        driverLng = driverLng,
      )
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Status update failed" }
    }
  }

  fun pushDriverGpsToActiveBooking(bookingId: String, lat: Double, lng: Double) {
    viewModelScope.launch {
      repository.updateActiveBookingDriverLocation(bookingId, lat, lng)
    }
  }

  fun submitRating(bookingId: String, driverId: String, stars: Int, comment: String?) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.submitRating(bookingId, driverId, stars, comment)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Rating failed" }
    }
  }

  fun submitComplaint(
    reporterRole: String,
    targetUserId: String,
    bookingId: String?,
    category: String,
    description: String,
    successMsg: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.submitComplaint(reporterRole, targetUserId, bookingId, category, description)
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to submit report" }
    }
  }

  fun markNotificationRead(id: String) {
    viewModelScope.launch {
      repository.markNotificationRead(id)
    }
  }

  // --- Admin actions ---
  fun adminApproveDriver(driver: DriverProfile, vehicle: VehicleRecord?) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.approveDriverAdmin(driver, vehicle)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Approval failed" }
    }
  }

  fun adminSetDriverStatus(driverId: String, status: String, reason: String? = null) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateDriverStatusAdmin(driverId, status, reason)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Update failed" }
    }
  }

  fun adminSaveFareSettings(settings: FareSettings, successMsg: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveFareSettings(settings)
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save fare settings" }
    }
  }

  fun adminAddFixedRoute(origin: String, destination: String, fare: Double) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.addFixedRoute(origin, destination, fare)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to add fixed route" }
    }
  }

  fun adminDeleteFixedRoute(routeId: String) {
    viewModelScope.launch {
      repository.deleteFixedRoute(routeId)
    }
  }

  fun adminToggleUserSuspension(userId: String, suspend: Boolean) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.toggleUserSuspensionAdmin(userId, suspend)
      _isBusy.value = false
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update user status" }
    }
  }

  fun adminUpdateComplaint(
    complaintId: String,
    status: String,
    notes: String?,
    reporterId: String? = null,
    successMsg: String? = null,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateComplaintAdmin(complaintId, status, notes, reporterId)
      _isBusy.value = false
      res.onSuccess { if (!successMsg.isNullOrBlank()) _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to update complaint" }
    }
  }

  fun adminGrantAccess(targetUid: String, targetEmail: String, successMsg: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.grantAdminAccess(targetUid, targetEmail)
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to authorize admin" }
    }
  }

  fun adminRevokeAccess(targetUid: String, successMsg: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.revokeAdminAccess(targetUid)
      _isBusy.value = false
      res.onSuccess { _actionMessage.value = successMsg }
      res.onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to revoke admin" }
    }
  }

  private companion object {
    const val TAG = "TotoGoVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
