package com.example

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.push.YoFirebaseMessagingService
import com.example.ui.MainAppNav
import com.example.ui.theme.YocinemaTheme

/**
 * Holds a movieId from a tapped push notification until MainAppNav's
 * effect picks it up and navigates, then clears it back to null — a plain
 * top-level object works here (no ViewModel needed) since there's only
 * ever one Activity/nav host in this app.
 */
object PendingDeepLink {
    var movieId by mutableStateOf<String?>(null)
}

// Changed from ComponentActivity to FragmentActivity: androidx.biometric's
// BiometricPrompt (used to gate copying the API key — see BiometricCopyHelper)
// requires a FragmentActivity host. FragmentActivity already extends
// ComponentActivity, so setContent {} and everything else below is unaffected.
class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        consumeDeepLink(intent)

        setContent {
            YocinemaTheme {
                MainAppNav()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App was already running (in the background) when the
        // notification was tapped — CLEAR_TOP brings this same Activity
        // instance back to front and delivers the new intent here instead
        // of a fresh onCreate().
        consumeDeepLink(intent)
    }

    private fun consumeDeepLink(intent: Intent?) {
        val movieId = intent?.getStringExtra(YoFirebaseMessagingService.EXTRA_MOVIE_ID)
        if (!movieId.isNullOrBlank()) {
            PendingDeepLink.movieId = movieId
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // We keep the activity alive – orientation changes are handled by the composable
    }
}
