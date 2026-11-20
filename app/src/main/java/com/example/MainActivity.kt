package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.configureAppCheck
import com.example.ui.navigation.VitalTrackRoot
import com.example.ui.theme.VitalTrackTheme
import com.example.ui.viewmodel.VitalTrackViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

  private var nfcTriggerNonce by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // CRITICAL: Initialize Firebase App Check BEFORE any Firebase AI or Firestore calls
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    if (intent?.data?.scheme == "vitaltrack") {
      nfcTriggerNonce++
    }

    setContent {
      val viewModel: VitalTrackViewModel = viewModel()
      val preferences by viewModel.preferences.collectAsStateWithLifecycle()

      LaunchedEffect(nfcTriggerNonce) {
        if (nfcTriggerNonce > 0) {
          viewModel.handleNfcTrigger()
          nfcTriggerNonce = 0
        }
      }

      val keyguardManager = remember { getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
      val deviceHasSecurity = remember { keyguardManager?.isDeviceSecure == true }
      var unlockedSession by rememberSaveable { mutableStateOf(false) }

      val requiresLock = preferences.biometricLockEnabled && deviceHasSecurity && !unlockedSession

      val triggerBiometricPrompt = remember {
        {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
              val executor = ContextCompat.getMainExecutor(this@MainActivity)
              val prompt = BiometricPrompt.Builder(this@MainActivity)
                .setTitle("Unlock VITALTRACK")
                .setSubtitle("Verify identity to access your local wellness records")
                .apply {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(
                      android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                  } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                  }
                }
                .build()
              prompt.authenticate(
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                  override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    unlockedSession = true
                  }
                },
              )
            }.onFailure { unlockedSession = true }
          } else {
            unlockedSession = true
          }
        }
      }

      LaunchedEffect(requiresLock) {
        if (requiresLock) {
          triggerBiometricPrompt()
        }
      }

      val baseContext = LocalContext.current
      val localizedContext =
        remember(preferences.languageCode, baseContext) {
          applyLocaleContext(baseContext, preferences.languageCode)
        }

      CompositionLocalProvider(LocalContext provides localizedContext) {
        VitalTrackTheme(
          themePreference = preferences.themePreference,
          accentPalette = preferences.accentPalette,
        ) {
          if (requiresLock) {
            Surface(modifier = Modifier.fillMaxSize()) {
              Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Fingerprint,
                  contentDescription = "Biometric Lock",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("VITALTRACK Protected", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  "Authenticate with biometrics or device credentials to continue.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { triggerBiometricPrompt() }) {
                  Text("Unlock Now")
                }
              }
            }
          } else {
            VitalTrackRoot(viewModel = viewModel)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    configureAppCheck(this, intent)
    if (intent.data?.scheme == "vitaltrack") {
      nfcTriggerNonce++
    }
  }

  private fun applyLocaleContext(context: Context, languageCode: String): Context {
    val locale = Locale.forLanguageTag(languageCode.ifBlank { "en" })
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
  }
}
