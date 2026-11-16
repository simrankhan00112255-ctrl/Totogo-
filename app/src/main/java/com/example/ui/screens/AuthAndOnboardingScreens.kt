package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.LocalAppStrings
import com.example.data.model.UserRole
import com.example.ui.components.MobileOtpVerificationSection
import com.example.ui.components.PhotoUploadField
import com.example.ui.components.TotoLogoHeader
import com.example.ui.theme.TotoCardBorder
import com.example.ui.theme.TotoDeepEmerald
import com.example.ui.theme.TotoEmeraldPrimary
import com.example.ui.theme.TotoLightMintSurface
import com.example.ui.theme.TotoPureWhite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun resolveDefaultWebClientId(context: Context): String? {
  val resId = context.resources.getIdentifier(
    "default_web_client_id",
    "string",
    context.packageName,
  ).takeIf { it != 0 }
    ?: runCatching { R.string::class.java.getField("default_web_client_id").getInt(null) }
      .getOrDefault(0)
  if (resId == 0) return null
  return runCatching { context.getString(resId) }
    .getOrNull()
    ?.takeIf { it.isNotBlank() }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = resolveDefaultWebClientId(context) ?: run {
    onUnauthenticated()
    return
  }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = resolveDefaultWebClientId(context) ?: run {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found. Ensure app/google-services.json includes a Web OAuth client.")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

@Composable
fun SplashScreenView() {
  val strings = LocalAppStrings.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(TotoDeepEmerald, Color(0xFF073E29)),
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.padding(24.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_toto_launcher),
        contentDescription = strings.appName,
        modifier = Modifier
          .size(104.dp)
          .clip(RoundedCornerShape(28.dp)),
        contentScale = ContentScale.Crop,
      )
      Text(
        text = strings.appName,
        style = MaterialTheme.typography.displaySmall,
        color = TotoPureWhite,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = strings.tagline,
        style = MaterialTheme.typography.titleMedium,
        color = TotoLightMintSurface,
      )
      Text(
        text = strings.locationSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TotoPureWhite.copy(alpha = 0.78f),
      )
      Spacer(Modifier.height(12.dp))
      CircularProgressIndicator(
        color = TotoPureWhite,
        modifier = Modifier.size(28.dp),
        strokeWidth = 2.5.dp,
      )
    }
  }
}

@Composable
fun AuthScreen(
  onToggleLanguage: () -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  val strings = LocalAppStrings.current
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)
        .widthIn(max = 560.dp)
        .align(Alignment.Center),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      TotoLogoHeader(onToggleLanguage = onToggleLanguage)

      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TotoCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column {
          Image(
            painter = painterResource(id = R.drawable.img_toto_hero),
            contentDescription = strings.tagline,
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp),
            contentScale = ContentScale.Crop,
          )
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = strings.locationSubtitle,
              style = MaterialTheme.typography.labelLarge,
              color = TotoEmeraldPrimary,
            )
            Text(
              text = strings.signInSubtitle,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Button(
              onClick = {
                isLoading = true
                errorMsg = null
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isLoading = false
                    errorMsg = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isLoading = false },
                )
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_sign_in_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = TotoPureWhite,
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.5.dp,
                )
              } else {
                Text(
                  text = strings.signInWithGoogle,
                  style = MaterialTheme.typography.titleMedium,
                  color = TotoPureWhite,
                )
              }
            }
            if (errorMsg != null) {
              Text(
                text = errorMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun OnboardingRegistrationScreen(
  defaultName: String,
  isBusy: Boolean,
  onToggleLanguage: () -> Unit,
  onSubmit: (
    fullName: String,
    mobileNumber: String,
    mobileVerified: Boolean,
    preferredLanguage: String,
    role: String,
    photoBase64: String?,
  ) -> Unit,
) {
  val strings = LocalAppStrings.current
  val currentLang = LocalAppLanguage.current
  var selectedRole by remember { mutableStateOf(UserRole.PASSENGER) }
  var fullName by remember { mutableStateOf(defaultName) }
  var mobileNumber by remember { mutableStateOf("") }
  var mobileVerified by remember { mutableStateOf(false) }
  var photoBase64 by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    TotoLogoHeader(onToggleLanguage = onToggleLanguage)

    Text(
      text = strings.completeRegistration,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )

    Text(
      text = strings.selectRoleTitle,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      RoleChoiceCard(
        title = strings.passengerRole,
        desc = strings.passengerRoleDesc,
        selected = selectedRole == UserRole.PASSENGER,
        icon = Icons.Filled.Person,
        onClick = { selectedRole = UserRole.PASSENGER },
        modifier = Modifier
          .weight(1f)
          .testTag("role_passenger_card"),
      )
      RoleChoiceCard(
        title = strings.driverRole,
        desc = strings.driverRoleDesc,
        selected = selectedRole == UserRole.DRIVER,
        icon = Icons.Filled.ElectricRickshaw,
        onClick = { selectedRole = UserRole.DRIVER },
        modifier = Modifier
          .weight(1f)
          .testTag("role_driver_card"),
      )
    }

    OutlinedTextField(
      value = fullName,
      onValueChange = { fullName = it },
      label = { Text(strings.fullName) },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("registration_name_input"),
    )

    MobileOtpVerificationSection(
      mobileNumber = mobileNumber,
      onMobileChange = {
        mobileNumber = it
        mobileVerified = false
      },
      isVerified = mobileVerified,
      onVerified = { mobileVerified = true },
    )

    PhotoUploadField(
      label = strings.optionalPhoto,
      base64 = photoBase64,
      onPhotoEncoded = { photoBase64 = it },
    )

    if (validationError != null) {
      Text(
        text = validationError!!,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }

    Button(
      onClick = {
        when {
          fullName.trim().length < 2 -> validationError = "Please enter your full name"
          mobileNumber.filter { it.isDigit() }.length < 10 ->
            validationError = "Please enter a valid mobile number"
          !mobileVerified -> validationError = "Please verify mobile OTP first"
          else -> {
            validationError = null
            onSubmit(
              fullName.trim(),
              mobileNumber.trim(),
              mobileVerified,
              currentLang.code,
              selectedRole,
              photoBase64,
            )
          }
        }
      },
      enabled = !isBusy,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("save_registration_button"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = TotoDeepEmerald),
    ) {
      if (isBusy) {
        CircularProgressIndicator(color = TotoPureWhite, modifier = Modifier.size(22.dp))
      } else {
        Text(strings.saveAndContinue, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@Composable
private fun RoleChoiceCard(
  title: String,
  desc: String,
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = if (selected) TotoLightMintSurface else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      width = if (selected) 2.dp else 1.dp,
      color = if (selected) TotoDeepEmerald else TotoCardBorder,
    ),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (selected) TotoDeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(26.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
