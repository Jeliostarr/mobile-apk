package com.example.ui.screens

import android.net.Uri
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
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    repository: YocinemaRepository,
    onPlayOfflineFile: (movieId: String, localFilePath: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Completed
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = YoSurface,
            contentColor = YoPrimaryAmber,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = YoPrimaryAmber
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
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                scope.launch {
                                    repository.downloadDao.updateStatus(item.downloadId, DownloadEntity.STATUS_PAUSED)
                                }
                            },
                            onResume = {
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
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                // Delete temp file if exists
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
                                // Delete the actual file
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle: VJ / Season & Episode
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
                    fontSize = 12.sp,
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
                    .clip(RoundedCornerShape(3.dp)),
                color = YoPrimaryAmber,
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
                    else -> download.status
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDownloading) YoPrimaryAmber else YoTextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDownloading) {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = YoPrimaryAmber)
                }
            } else if (isPaused) {
                IconButton(onClick = onResume) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resume", tint = YoPrimaryAmber)
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
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable { onPlay() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster thumbnail
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.fillMaxSize().background(YoPrimaryAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = YoPrimaryAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle and episode info
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
                    fontSize = 12.sp,
                    color = YoTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatBytes(download.downloadedBytes)} · Ready",
                fontSize = 12.sp,
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