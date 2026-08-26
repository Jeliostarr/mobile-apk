package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.ApkInstaller
import kotlinx.coroutines.launch

private sealed class DownloadUiState {
    object Idle : DownloadUiState()
    data class Downloading(val progress: Int) : DownloadUiState()
    object ReadyToInstall : DownloadUiState()
    data class Failed(val message: String) : DownloadUiState()
}

/**
 * [isForced] = true (below minSupportedVersionCode, or the admin's
 * force-update toggle) makes this non-dismissible — no back button, no
 * outside-tap, and no "Later" button. Otherwise it's a normal dismissible
 * nudge (used for AppUpdateState.Optional).
 */
@Composable
fun UpdateDialog(
    versionName: String,
    releaseNotes: String,
    apkUrl: String,
    isForced: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<DownloadUiState>(DownloadUiState.Idle) }

    fun startDownload() {
        if (apkUrl.isBlank()) return
        val downloadId = ApkInstaller.startDownload(context, apkUrl)
        downloadState = DownloadUiState.Downloading(0)
        scope.launch {
            ApkInstaller.awaitDownload(
                context = context,
                downloadId = downloadId,
                onProgress = { percent -> downloadState = DownloadUiState.Downloading(percent) },
                onSuccess = {
                    downloadState = DownloadUiState.ReadyToInstall
                    // Auto-triggers the install prompt the moment the
                    // download finishes, rather than making the user tap
                    // a second button — this is the "auto in-app install"
                    // part; the OS still owns the actual install
                    // confirmation screen from here.
                    ApkInstaller.installApk(context)
                },
                onFailure = { message -> downloadState = DownloadUiState.Failed(message) }
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!isForced) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isForced,
            dismissOnClickOutside = !isForced
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(YoSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(YoPrimaryViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = YoPrimaryViolet,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isForced) "Update Required" else "Update Available",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (versionName.isNotBlank()) "Version $versionName is ready to install." else "A new version is ready to install.",
                fontSize = 13.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center
            )

            if (releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(YoBaseBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = releaseNotes,
                        fontSize = 12.sp,
                        color = YoTextMuted,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (val state = downloadState) {
                is DownloadUiState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = YoPrimaryViolet
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Downloading… ${state.progress}%", fontSize = 12.sp, color = YoTextMuted)
                }

                is DownloadUiState.ReadyToInstall -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = YoPrimaryViolet)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Downloaded — opening installer…", fontSize = 12.sp, color = YoTextMuted, textAlign = TextAlign.Center)
                }

                is DownloadUiState.Failed -> {
                    Text(state.message, fontSize = 12.sp, color = YoDestructive, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { startDownload() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                    ) {
                        Text("Retry Download", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                DownloadUiState.Idle -> {
                    if (apkUrl.isNotBlank()) {
                        Button(
                            onClick = { startDownload() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Install", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                        },
                        enabled = apkUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open in Browser", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = YoTextPrimary)
                    }
                }
            }

            if (!isForced) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onDismiss) {
                    Text("Later", color = YoTextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
