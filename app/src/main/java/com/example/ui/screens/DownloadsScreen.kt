package com.example.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.download.startDownloadWorker
import com.example.repository.YocinemaRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "DownloadsScreen"

@Composable
fun DownloadsScreen(
    repository: YocinemaRepository,
    onPlayOfflineFile: (movieId: String, localFilePath: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val activeDownloads by repository.activeDownloads.collectAsState(initial = emptyList())
    val completedDownloads by repository.completedDownloads.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Downloads",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = YoSurface,
            contentColor = YoPrimaryViolet,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = YoPrimaryViolet
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active (${activeDownloads.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Completed (${completedDownloads.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        if (selectedTab == 0) {
            if (activeDownloads.isEmpty()) {
                EmptyState("No active downloads")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeDownloads, key = { it.downloadId }) { item ->
                        ActiveDownloadRow(
                            download = item,
                            onPause = {
                                Log.d(TAG, "Pausing download: ${item.downloadId}")
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                scope.launch {
                                    repository.downloadDao.updateStatus(item.downloadId, DownloadEntity.STATUS_PAUSED)
                                    // Verify status after update
                                    val updated = repository.downloadDao.getDownloadById(item.downloadId)
                                    Log.d(TAG, "Status after pause: ${updated?.status}")
                                    // Optionally force a refresh by calling getActiveDownloads again (not needed, but for debugging)
                                }
                            },
                            onResume = {
                                Log.d(TAG, "Resuming download: ${item.downloadId}")
                                scope.launch {
                                    val movie = repository.getMovieDetail(item.movieId) ?: return@launch
                                    startDownloadWorker(
                                        context,
                                        movie,
                                        item.seasonNumber,
                                        item.episodeNumber
                                    )
                                }
                            },
                            onCancel = {
                                Log.d(TAG, "Cancelling download: ${item.downloadId}")
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                item.tempFilePath?.let { path ->
                                    try { java.io.File(path).delete() } catch (_: Exception) {}
                                }
                                scope.launch { repository.downloadDao.deleteDownload(item.downloadId) }
                            }
                        )
                    }
                }
            }
        } else {
            if (completedDownloads.isEmpty()) {
                EmptyState("No completed downloads")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(completedDownloads, key = { it.downloadId }) { item ->
                        CompletedDownloadRow(
                            download = item,
                            onPlay = {
                                item.localFilePath?.let { path ->
                                    onPlayOfflineFile(item.movieId, path)
                                }
                            },
                            onDelete = {
                                item.localFilePath?.let { uriStr ->
                                    try {
                                        val uri = Uri.parse(uriStr)
                                        if (uri.scheme == "content") {
                                            context.contentResolver.delete(uri, null, null)
                                        } else {
                                            java.io.File(uriStr).delete()
                                        }
                                    } catch (_: Exception) {}
                                }
                                scope.launch { repository.downloadDao.deleteDownload(item.downloadId) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadRow(
    download: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = if (download.totalBytes > 0) {
        (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val formattedDownloaded = formatBytes(download.downloadedBytes)
    val formattedTotal = if (download.totalBytes > 0) formatBytes(download.totalBytes) else "..."
    val isDownloading = download.status == DownloadEntity.STATUS_DOWNLOADING
    val isPaused = download.status == DownloadEntity.STATUS_PAUSED
    val isFailed = download.status == DownloadEntity.STATUS_FAILED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(YoBorder)
        ) {
            if (!download.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = download.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = YoTextMuted,
                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = buildString {
                download.vjName?.let { append("by $it") }
                if (download.seasonNumber != null && download.episodeNumber != null) {
                    if (isNotEmpty()) append(" · ")
                    append("S${download.seasonNumber} E${download.episodeNumber}")
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = YoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = YoPrimaryViolet,
                trackColor = YoBorder
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedDownloaded / $formattedTotal",
                    fontSize = 11.sp,
                    color = YoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val statusText = when {
                    isDownloading -> formatSpeed(download.speedBytesPerSec)
                    isPaused -> "Paused"
                    isFailed -> "Failed — tap retry"
                    else -> download.status
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isDownloading -> YoPrimaryViolet
                        isFailed -> YoDestructive
                        else -> YoTextMuted
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDownloading) {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = YoPrimaryViolet)
                }
            } else if (isPaused || isFailed) {
                IconButton(onClick = onResume) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = if (isFailed) "Retry" else "Resume",
                        tint = if (isFailed) YoDestructive else YoPrimaryViolet
                    )
                }
            }

            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = YoTextMuted)
            }
        }
    }
}

@Composable
fun CompletedDownloadRow(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
            .clickable { onPlay() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(YoBorder)
        ) {
            if (!download.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = download.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(YoPrimaryViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = YoPrimaryViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = buildString {
                download.vjName?.let { append("by $it") }
                if (download.seasonNumber != null && download.episodeNumber != null) {
                    if (isNotEmpty()) append(" · ")
                    append("S${download.seasonNumber} E${download.episodeNumber}")
                }
                if (download.downloadedAt != null) {
                    if (isNotEmpty()) append(" · ")
                    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(download.downloadedAt))
                    append(date)
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = YoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatBytes(download.downloadedBytes)} · Ready",
                fontSize = 11.sp,
                color = YoTextMuted
            )
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = YoTextMuted)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(YoSurface)
                    .border(1.dp, YoBorder, RoundedCornerShape(44.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = YoTextMuted,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, fontSize = 14.sp, color = YoTextMuted)
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return "${formatBytes(bytesPerSec)}/s"
}