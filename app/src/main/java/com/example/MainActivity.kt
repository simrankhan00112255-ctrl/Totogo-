package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AppLanguage
import com.example.core.BengaliStrings
import com.example.core.EnglishStrings
import com.example.core.LanguagePreferences
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.TotoGoRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DriverDashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingRegistrationScreen
import com.example.ui.screens.PassengerHomeScreen
import com.example.ui.screens.PassengerRidesScreen
import com.example.ui.screens.ProfileAndHelpScreen
import com.example.ui.screens.SplashScreenView
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.theme.TotoGoTheme
import com.example.ui.viewmodel.TotoGoViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TotoRoutes {
  const val HOME = "home"
  const val RIDES = "rides"
  const val DRIVER = "driver"
  const val ALERTS = "alerts"
  const val PROFILE = "profile"
  const val ADMIN = "admin"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TotoGoAppRoot()
    }
  }
}

@Composable
fun TotoGoAppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val langPrefs = remember { LanguagePreferences(context) }
  val language by langPrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.BENGALI)
  val isBengali = language == AppLanguage.BENGALI
  val strings = if (isBengali) BengaliStrings else EnglishStrings

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var showSplash by remember { mutableStateOf(true) }
  val credentialManager = remember { CredentialManager.create(context) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
      onUnauthenticated = {},
      scope = scope,
    )
    delay(900L)
    showSplash = false
  }

  val toggleLanguage: () -> Unit = {
    val next = if (language == AppLanguage.BENGALI) AppLanguage.ENGLISH else AppLanguage.BENGALI
    scope.launch { langPrefs.setLanguage(next) }
  }

  CompositionLocalProvider(
    LocalAppLanguage provides language,
    LocalAppStrings provides strings,
  ) {
    TotoGoTheme(isBengali = isBengali) {
      if (showSplash) {
        SplashScreenView()
      } else {
        val user = currentUser
        if (user == null) {
          AuthScreen(
            onToggleLanguage = toggleLanguage,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
          )
        } else {
          key(user.uid) {
            AuthenticatedTotoGoShell(
              user = user,
              onToggleLanguage = toggleLanguage,
              onSignedOut = { currentUser = null },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedTotoGoShell(
  user: FirebaseUser,
  onToggleLanguage: () -> Unit,
  onSignedOut: () -> Unit,
) {
  val viewModel: TotoGoViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val dbId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        TotoGoViewModel(
          repository = TotoGoRepository(db, Firebase.auth),
          currentUserId = user.uid,
          userEmail = user.email ?: "",
          userDisplayName = user.displayName ?: "",
          isEmailVerified = user.isEmailVerified,
          appContext = app.applicationContext,
        )
      }
    },
  )

  val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  when (val state = profileState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }

    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(state.message)
      }
    }

    is UiState.Success -> {
      val profile = state.data
      if (profile == null) {
        OnboardingRegistrationScreen(
          defaultName = user.displayName ?: "",
          isBusy = isBusy,
          onToggleLanguage = onToggleLanguage,
          onSubmit = { name, mobile, verified, langCode, role, photo ->
            viewModel.completeUserRegistration(name, mobile, verified, langCode, role, photo)
          },
        )
      } else {
        MainRoleNavigationHost(
          profile = profile,
          isAdmin = isAdmin,
          viewModel = viewModel,
          snackbarHostState = snackbarHostState,
          onToggleLanguage = {
            onToggleLanguage()
            val nextCode = if (profile.preferredLanguage == "bn") "en" else "bn"
            viewModel.updatePreferredLanguage(nextCode)
          },
          onSignedOut = onSignedOut,
        )
      }
    }
  }
}

@Composable
private fun MainRoleNavigationHost(
  profile: UserProfile,
  isAdmin: Boolean,
  viewModel: TotoGoViewModel,
  snackbarHostState: SnackbarHostState,
  onToggleLanguage: () -> Unit,
  onSignedOut: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val navController = rememberNavController()
  val navBackStack by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStack?.destination?.route

  val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()
  val vehicleRecord by viewModel.vehicleRecord.collectAsStateWithLifecycle()
  val myDriverPublic by viewModel.myDriverPublic.collectAsStateWithLifecycle()
  val onlineDrivers by viewModel.verifiedOnlineDrivers.collectAsStateWithLifecycle()
  val passengerBookings by viewModel.passengerBookings.collectAsStateWithLifecycle()
  val activeDriverPublic by viewModel.activeBookingPublicDriver.collectAsStateWithLifecycle()
  val searchingBookings by viewModel.searchingBookings.collectAsStateWithLifecycle()
  val driverBookings by viewModel.driverBookings.collectAsStateWithLifecycle()
  val driverRatings by viewModel.driverRatings.collectAsStateWithLifecycle()
  val dismissedIds by viewModel.dismissedBookingIds.collectAsStateWithLifecycle()
  val fareSettings by viewModel.fareSettings.collectAsStateWithLifecycle()
  val fixedRoutes by viewModel.fixedRoutes.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val adminDrivers by viewModel.adminDrivers.collectAsStateWithLifecycle()
  val adminVehicles by viewModel.adminVehicles.collectAsStateWithLifecycle()
  val adminBookings by viewModel.adminBookings.collectAsStateWithLifecycle()
  val adminComplaints by viewModel.adminComplaints.collectAsStateWithLifecycle()
  val adminRatings by viewModel.adminRatings.collectAsStateWithLifecycle()
  val adminAuditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()
  val adminRegistry by viewModel.adminRegistry.collectAsStateWithLifecycle()
  val myComplaints by viewModel.myComplaints.collectAsStateWithLifecycle()

  val unreadCount = notifications.count { !it.isRead }
  val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

  val tabs = buildList {
    if (profile.role == UserRole.DRIVER) {
      add(Triple(TotoRoutes.DRIVER, strings.tabDriverDesk, Icons.Filled.ElectricRickshaw))
      add(Triple(TotoRoutes.HOME, strings.tabHome, Icons.Filled.Home))
    } else {
      add(Triple(TotoRoutes.HOME, strings.tabHome, Icons.Filled.Home))
      add(Triple(TotoRoutes.RIDES, strings.tabRides, Icons.Filled.History))
    }
    add(Triple(TotoRoutes.ALERTS, strings.tabAlerts, Icons.Filled.Notifications))
    if (isAdmin) {
      add(Triple(TotoRoutes.ADMIN, strings.tabAdmin, Icons.Filled.AdminPanelSettings))
    }
    add(Triple(TotoRoutes.PROFILE, strings.tabProfile, Icons.Filled.Person))
  }

  val startDestination = if (profile.role == UserRole.DRIVER) TotoRoutes.DRIVER else TotoRoutes.HOME

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (!isWideScreen) {
        NavigationBar {
          tabs.forEach { (route, label, icon) ->
            NavigationBarItem(
              selected = currentRoute == route,
              onClick = {
                navController.navigate(route) {
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                if (route == TotoRoutes.ALERTS && unreadCount > 0) {
                  BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                    Icon(icon, contentDescription = label)
                  }
                } else {
                  Icon(icon, contentDescription = label)
                }
              },
              label = { Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (isWideScreen) {
        NavigationRail {
          tabs.forEach { (route, label, icon) ->
            NavigationRailItem(
              selected = currentRoute == route,
              onClick = {
                navController.navigate(route) {
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(icon, contentDescription = label) },
              label = { Text(label) },
            )
          }
        }
      }

      NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.weight(1f),
      ) {
        composable(TotoRoutes.HOME) {
          PassengerHomeScreen(
            userProfile = profile,
            onlineDrivers = onlineDrivers,
            bookings = passengerBookings,
            activeDriverPublic = activeDriverPublic,
            fareSettings = fareSettings,
            fixedRoutes = fixedRoutes,
            isBusy = isBusy,
            onToggleLanguage = onToggleLanguage,
            onBookToto = { pName, pLat, pLng, dName, dLat, dLng, onBooked ->
              viewModel.bookToto(
                passengerName = profile.fullName,
                passengerMobile = profile.mobileNumber,
                pickupName = pName,
                pickupLat = pLat,
                pickupLng = pLng,
                destinationName = dName,
                destinationLat = dLat,
                destinationLng = dLng,
                onBooked = onBooked,
              )
            },
            onCancelBooking = { b, reason -> viewModel.cancelBookingByPassenger(b, reason) },
            onSubmitRating = { bId, dId, stars, comment ->
              viewModel.submitRating(bId, dId, stars, comment)
            },
            onReportDriver = { targetId, bId, cat, desc ->
              viewModel.submitComplaint(UserRole.PASSENGER, targetId, bId, cat, desc, "Safety report submitted")
            },
          )
        }
        composable(TotoRoutes.RIDES) {
          PassengerRidesScreen(
            bookings = passengerBookings,
            isBusy = isBusy,
            onCancelBooking = { b, reason -> viewModel.cancelBookingByPassenger(b, reason) },
            onReportDriver = { targetId, bId, cat, desc ->
              viewModel.submitComplaint(UserRole.PASSENGER, targetId, bId, cat, desc, "Safety report submitted")
            },
          )
        }
        composable(TotoRoutes.DRIVER) {
          DriverDashboardScreen(
            userProfile = profile,
            driverProfile = driverProfile,
            vehicleRecord = vehicleRecord,
            driverPublic = myDriverPublic,
            searchingBookings = searchingBookings,
            driverBookings = driverBookings,
            driverRatings = driverRatings,
            dismissedBookingIds = dismissedIds,
            isBusy = isBusy,
            onToggleLanguage = onToggleLanguage,
            onSubmitRegistration = { name, mob, verified, reg, vType, vModel, area, lic, perm, dP, fP, rP, sP, lD, pD ->
              viewModel.submitDriverApplication(
                fullName = name,
                mobileNumber = mob,
                mobileVerified = verified,
                registrationNumber = reg,
                vehicleType = vType,
                vehicleModel = vModel,
                serviceArea = area,
                licenceNumber = lic,
                permitNumber = perm,
                driverPhotoBase64 = dP,
                frontPhotoBase64 = fP,
                rearPhotoBase64 = rP,
                sidePhotoBase64 = sP,
                licenceDocBase64 = lD,
                permitDocBase64 = pD,
                onSuccessMsg = "Application submitted for admin verification",
              )
            },
            onToggleOnline = { online, lat, lng, hasFix ->
              viewModel.setDriverOnline(online, lat, lng, hasFix)
            },
            onPushLiveGps = { lat, lng ->
              viewModel.updateDriverLiveGps(lat, lng)
            },
            onPushActiveTripGps = { bId, lat, lng ->
              viewModel.pushDriverGpsToActiveBooking(bId, lat, lng)
            },
            onAcceptBooking = { booking, dProfile -> viewModel.acceptBooking(booking, dProfile) },
            onDeclineBooking = { bId -> viewModel.dismissIncomingBooking(bId) },
            onProgressBooking = { booking, status, fare, lat, lng ->
              viewModel.progressDriverBooking(booking, status, fare, lat, lng)
            },
            onReportPassenger = { pId, bId, cat, desc ->
              viewModel.submitComplaint(UserRole.DRIVER, pId, bId, cat, desc, "Safety report submitted")
            },
          )
        }
        composable(TotoRoutes.ALERTS) {
          NotificationsScreen(
            notifications = notifications,
            onMarkRead = { id -> viewModel.markNotificationRead(id) },
          )
        }
        composable(TotoRoutes.ADMIN) {
          if (isAdmin) {
            AdminDashboardScreen(
              currentAdminUid = viewModel.currentUserId,
              users = adminUsers,
              drivers = adminDrivers,
              vehicles = adminVehicles,
              onlineDrivers = onlineDrivers,
              bookings = adminBookings,
              complaints = adminComplaints,
              ratings = adminRatings,
              auditLogs = adminAuditLogs,
              admins = adminRegistry,
              fareSettings = fareSettings,
              fixedRoutes = fixedRoutes,
              isBusy = isBusy,
              onToggleLanguage = onToggleLanguage,
              onApproveDriver = { d, v -> viewModel.adminApproveDriver(d, v) },
              onSetDriverStatus = { id, status, reason -> viewModel.adminSetDriverStatus(id, status, reason) },
              onToggleUserSuspend = { uid, suspend -> viewModel.adminToggleUserSuspension(uid, suspend) },
              onSaveFareSettings = { s -> viewModel.adminSaveFareSettings(s, "Fare rules saved") },
              onAddFixedRoute = { o, d, f -> viewModel.adminAddFixedRoute(o, d, f) },
              onDeleteFixedRoute = { id -> viewModel.adminDeleteFixedRoute(id) },
              onUpdateComplaint = { id, status, notes, reporterId ->
                viewModel.adminUpdateComplaint(id, status, notes, reporterId, "Complaint updated")
              },
              onGrantAdmin = { uid, email ->
                viewModel.adminGrantAccess(uid, email, "Admin authorized")
              },
              onRevokeAdmin = { uid ->
                viewModel.adminRevokeAccess(uid, "Admin access revoked")
              },
              onNotifyMessage = { msg -> viewModel.postMessage(msg) },
            )
          }
        }
        composable(TotoRoutes.PROFILE) {
          ProfileAndHelpScreen(
            userProfile = profile,
            isAdmin = isAdmin,
            myComplaints = myComplaints,
            isBusy = isBusy,
            onToggleLanguage = onToggleLanguage,
            onUpdateProfile = { name, mob, verified, photo ->
              viewModel.updateAccountProfile(
                fullName = name,
                mobileNumber = mob,
                mobileVerified = verified,
                preferredLanguage = profile.preferredLanguage,
                currentRole = profile.role,
                photoBase64 = photo,
                successMsg = strings.profileUpdatedMsg,
              )
            },
            onSwitchRole = { newRole ->
              viewModel.switchRole(newRole)
              val dest = if (newRole == UserRole.DRIVER) TotoRoutes.DRIVER else TotoRoutes.HOME
              navController.navigate(dest) { launchSingleTop = true }
            },
            onOpenAdminPanel = {
              if (isAdmin) {
                navController.navigate(TotoRoutes.ADMIN) { launchSingleTop = true }
              }
            },
            onSignedOut = onSignedOut,
          )
        }
      }
    }
  }
}
