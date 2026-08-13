package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.MainAppNav
import com.example.ui.theme.YocinemaTheme

class MainActivity : ComponentActivity() {

  // Required on Android 13+ for the playback notification (lock-screen /
  // media controls, wired up in PlaybackService) to be allowed to show at
  // all. Without requesting this, the notification doesn't error or
  // crash — it just silently never appears, which looks like the feature
  // is broken when it's actually just never been granted permission to post.
  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, playback itself is unaffected */ }

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
}
