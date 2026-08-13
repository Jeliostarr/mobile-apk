package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadEntity
import com.example.repository.YocinemaRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    repository: YocinemaRepository,
    onPlayOfflineFile: (movieId: String, localFilePath: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Completed
    val activeDownloads by repository.activeDownloads.collectAsState(initial = emptyList())
    val completedDownloads by repository.completedDownloads.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

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
                        val context = androidx.compose.ui.platform.LocalContext.current
                        ActiveDownloadRow(
                            download = item,
                            onPause = {
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                scope.launch {
                                    repository.downloadDao.updateProgress(item.downloadId, item.downloadedBytes, item.totalBytes, 0, "PAUSED")
                                }
                            },
                            onResume = {
                                scope.launch {
                                    val m = repository.getMovieDetail(item.movieId) ?: com.example.data.model.Movie(id = item.movieId, title = item.title)
                                    var sNum: Int? = null
                                    var eNum: Int? = null
                                    if (item.episodeId != null && item.episodeId.startsWith("S")) {
                                        val parts = item.episodeId.split("E")
                                        if (parts.size == 2) {
                                            sNum = parts[0].removePrefix("S").toIntOrNull()
                                            eNum = parts[1].toIntOrNull()
                                        }
                                    }
                                    startDownloadWorker(context, m, sNum, eNum)
                                }
                            },
                            onDelete = {
                                androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(item.downloadId)
                                scope.launch { repository.deleteDownload(item.downloadId) }
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
                            onPlay = { onPlayOfflineFile(item.movieId, item.localFilePath) },
                            onDelete = {
                                scope.launch { repository.deleteDownload(item.downloadId) }
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
    onDelete: () -> Unit
) {
    val progress = if (download.totalBytes > 0) {
        (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val formattedDownloaded = formatBytes(download.downloadedBytes)
    val formattedTotal = if (download.totalBytes > 0) formatBytes(download.totalBytes) else "..."
    val formattedSpeed = formatSpeed(download.speedBytesPerSec)
    val isDownloading = download.status == "DOWNLOADING"

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$formattedDownloaded / $formattedTotal",
                    fontSize = 11.sp,
                    color = YoTextMuted
                )

                Text(
                    text = if (isDownloading) formattedSpeed else download.status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YoPrimaryAmber
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row {
            if (isDownloading) {
                IconButton(onClick = onPause) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = YoPrimaryAmber
                    )
                }
            } else {
                IconButton(onClick = onResume) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = YoPrimaryAmber
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancel",
                    tint = YoDestructive
                )
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
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(YoPrimaryAmber),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = YoBaseBackground,
                modifier = Modifier.size(28.dp)
            )
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatBytes(download.downloadedBytes)} · Offline Ready",
                fontSize = 12.sp,
                color = YoTextMuted
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = YoTextMuted
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
            Text(
                text = message,
                fontSize = 14.sp,
                color = YoTextMuted
            )
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
