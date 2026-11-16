package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object UserRole {
  const val PASSENGER = "passenger"
  const val DRIVER = "driver"
  const val ADMIN = "admin"
}

object DriverStatus {
  const val PENDING_VERIFICATION = "pending_verification"
  const val APPROVED = "approved"
  const val REJECTED = "rejected"
  const val SUSPENDED = "suspended"
}

object BookingStatus {
  const val SEARCHING = "searching"
  const val ASSIGNED = "assigned"
  const val ARRIVING = "arriving"
  const val STARTED = "started"
  const val COMPLETED = "completed"
  const val CANCELLED = "cancelled"
}

object ComplaintStatus {
  const val OPEN = "open"
  const val IN_REVIEW = "in_review"
  const val RESOLVED = "resolved"
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val mobileNumber: String = "",
  val mobileVerified: Boolean = false,
  val preferredLanguage: String = "bn",
  val role: String = UserRole.PASSENGER,
  val photoBase64: String? = null,
  @get:com.google.firebase.firestore.PropertyName("isSuspended")
  @set:com.google.firebase.firestore.PropertyName("isSuspended")
  var isSuspended: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "userId" to userId,
      "fullName" to fullName,
      "role" to role,
      "preferredLanguage" to preferredLanguage,
      "mobileVerified" to mobileVerified,
      "isSuspended" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (email.isNotBlank()) map["email"] = email
    if (mobileNumber.isNotBlank()) map["mobileNumber"] = mobileNumber
    if (!photoBase64.isNullOrBlank()) map["photoBase64"] = photoBase64
    return map
  }
}

@IgnoreExtraProperties
data class DriverProfile(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val fullName: String = "",
  val mobileNumber: String = "",
  val mobileVerified: Boolean = false,
  val registrationNumber: String = "",
  val vehicleType: String = "Electric TOTO",
  val vehicleModel: String = "",
  val serviceArea: String = "Chopra Bazar",
  val licenceNumber: String? = null,
  val permitNumber: String? = null,
  val profilePhotoBase64: String? = null,
  val status: String = DriverStatus.PENDING_VERIFICATION,
  val rejectionReason: String? = null,
  val totalTrips: Long = 0L,
  val totalEarnings: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "driverId" to driverId,
      "fullName" to fullName,
      "mobileNumber" to mobileNumber,
      "mobileVerified" to mobileVerified,
      "registrationNumber" to registrationNumber,
      "vehicleType" to vehicleType,
      "serviceArea" to serviceArea,
      "status" to DriverStatus.PENDING_VERIFICATION,
      "totalTrips" to 0L,
      "totalEarnings" to 0.0,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (vehicleModel.isNotBlank()) map["vehicleModel"] = vehicleModel
    if (!licenceNumber.isNullOrBlank()) map["licenceNumber"] = licenceNumber
    if (!permitNumber.isNullOrBlank()) map["permitNumber"] = permitNumber
    if (!profilePhotoBase64.isNullOrBlank()) map["profilePhotoBase64"] = profilePhotoBase64
    return map
  }
}

@IgnoreExtraProperties
data class VehicleRecord(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val registrationNumber: String = "",
  val vehicleType: String = "Electric TOTO",
  val vehicleModel: String = "",
  val frontPhotoBase64: String? = null,
  val rearPhotoBase64: String? = null,
  val sidePhotoBase64: String? = null,
  val licenceDocBase64: String? = null,
  val permitDocBase64: String? = null,
  val photosVerified: Boolean = false,
  val docsVerified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "driverId" to driverId,
      "registrationNumber" to registrationNumber,
      "vehicleType" to vehicleType,
      "photosVerified" to false,
      "docsVerified" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (vehicleModel.isNotBlank()) map["vehicleModel"] = vehicleModel
    if (!frontPhotoBase64.isNullOrBlank()) map["frontPhotoBase64"] = frontPhotoBase64
    if (!rearPhotoBase64.isNullOrBlank()) map["rearPhotoBase64"] = rearPhotoBase64
    if (!sidePhotoBase64.isNullOrBlank()) map["sidePhotoBase64"] = sidePhotoBase64
    if (!licenceDocBase64.isNullOrBlank()) map["licenceDocBase64"] = licenceDocBase64
    if (!permitDocBase64.isNullOrBlank()) map["permitDocBase64"] = permitDocBase64
    return map
  }
}

@IgnoreExtraProperties
data class DriverPublicProfile(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val fullName: String = "",
  val registrationNumber: String = "",
  val vehicleType: String = "Electric TOTO",
  val vehicleModel: String = "",
  val serviceArea: String = "Chopra Bazar",
  @get:com.google.firebase.firestore.PropertyName("isVerified")
  @set:com.google.firebase.firestore.PropertyName("isVerified")
  var isVerified: Boolean = false,
  @get:com.google.firebase.firestore.PropertyName("isOnline")
  @set:com.google.firebase.firestore.PropertyName("isOnline")
  var isOnline: Boolean = false,
  val currentLat: Double = 26.4042,
  val currentLng: Double = 88.2485,
  val hasGpsFix: Boolean = false,
  val profilePhotoBase64: String? = null,
  val frontPhotoBase64: String? = null,
  val rearPhotoBase64: String? = null,
  val sidePhotoBase64: String? = null,
  val ratingAvg: Double = 5.0,
  val ratingCount: Long = 0L,
  val lastRatedBookingId: String? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Booking(
  @DocumentId val id: String = "",
  val passengerId: String = "",
  val passengerName: String = "",
  val passengerMobile: String? = null,
  val driverId: String? = null,
  val driverName: String? = null,
  val driverPhone: String? = null,
  val vehicleRegNumber: String? = null,
  val vehicleType: String? = null,
  val pickupName: String = "",
  val pickupLat: Double = 0.0,
  val pickupLng: Double = 0.0,
  val destinationName: String = "",
  val destinationLat: Double = 0.0,
  val destinationLng: Double = 0.0,
  val distanceKm: Double = 0.0,
  val estimatedFare: Double = 0.0,
  val finalFare: Double? = null,
  val paymentMethod: String = "cash",
  val status: String = BookingStatus.SEARCHING,
  val cancelReason: String? = null,
  val driverLat: Double? = null,
  val driverLng: Double? = null,
  val hasRated: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "passengerId" to passengerId,
      "passengerName" to passengerName,
      "pickupName" to pickupName,
      "pickupLat" to pickupLat,
      "pickupLng" to pickupLng,
      "destinationName" to destinationName,
      "destinationLat" to destinationLat,
      "destinationLng" to destinationLng,
      "distanceKm" to distanceKm,
      "estimatedFare" to estimatedFare,
      "paymentMethod" to paymentMethod,
      "status" to BookingStatus.SEARCHING,
      "hasRated" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!passengerMobile.isNullOrBlank()) {
      map["passengerMobile"] = passengerMobile.trim().take(20)
    }
    return map
  }
}

@IgnoreExtraProperties
data class FareSettings(
  @DocumentId val id: String = "default",
  val minFare: Double = 20.0,
  val perKmFare: Double = 12.0,
  val minDistanceKm: Double = 1.5,
  val additionalDistanceFarePerKm: Double = 10.0,
  val waitingChargePerMin: Double = 2.0,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toMap(): Map<String, Any> =
    mapOf(
      "minFare" to minFare,
      "perKmFare" to perKmFare,
      "minDistanceKm" to minDistanceKm,
      "additionalDistanceFarePerKm" to additionalDistanceFarePerKm,
      "waitingChargePerMin" to waitingChargePerMin,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class FixedRoute(
  @DocumentId val id: String = "",
  val originName: String = "",
  val destinationName: String = "",
  val fixedFare: Double = 0.0,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toMap(): Map<String, Any> =
    mapOf(
      "originName" to originName,
      "destinationName" to destinationName,
      "fixedFare" to fixedFare,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class RideRating(
  @DocumentId val id: String = "",
  val bookingId: String = "",
  val passengerId: String = "",
  val driverId: String = "",
  val stars: Long = 5L,
  val comment: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Complaint(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val reporterRole: String = UserRole.PASSENGER,
  val targetUserId: String = "",
  val bookingId: String? = null,
  val category: String = "",
  val description: String = "",
  val status: String = ComplaintStatus.OPEN,
  val adminNotes: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val senderId: String = "",
  val bookingId: String? = null,
  val title: String = "",
  val body: String = "",
  val type: String = "booking",
  @get:com.google.firebase.firestore.PropertyName("isRead")
  @set:com.google.firebase.firestore.PropertyName("isRead")
  var isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminRecord(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminAuditLog(
  @DocumentId val id: String = "",
  val adminId: String = "",
  val adminEmail: String = "",
  val action: String = "",
  val targetType: String = "",
  val targetId: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

