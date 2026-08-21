package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.example.ui.MainAppNav
import com.example.ui.theme.YocinemaTheme

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

        setContent {
            YocinemaTheme {
                MainAppNav()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // We keep the activity alive – orientation changes are handled by the composable
    }
}
