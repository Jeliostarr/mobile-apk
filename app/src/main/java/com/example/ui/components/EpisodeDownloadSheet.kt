package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadEntity
import com.example.data.model.Episode
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

/**
 * Lets the user pick one or more episodes to download instead of the download
 * button always grabbing whichever episode the backend resolves for a bare
 * movie-level request. Selection is keyed as "S{season}E{episode}" so it
 * survives switching seasons within the sheet.
 *
 * Deliberately does NOT show an upfront total size for the selection: the
 * backend doesn't expose a per-episode file-size field or a cheap way to get
 * one, and firing a HEAD request per episode just to estimate a number would
 * burn through the API's rate limits for a figure that's often wrong anyway
 * (range requests, variable bitrate). Real, accurate sizes already appear per
 * item on the Downloads screen once each one starts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDownloadSheet(
    sheetState: SheetState,
    episodes: List<Episode>,
    downloadStatusByEpisodeKey: Map<String, String>,
    onDismiss: () -> Unit,
    onDownloadSelected: (List<Episode>) -> Unit
) {
    fun episodeKey(ep: Episode) = "S${ep.sNum}E${ep.eNum}"

    val seasons = remember(episodes) { episodes.mapNotNull { it.sNum }.distinct().sorted() }
    var selectedSeason by remember { mutableStateOf(seasons.firstOrNull() ?: 1) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    val visibleEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.sNum == selectedSeason }.ifEmpty { episodes }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YoSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Download Episodes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select one or more episodes to download",
                fontSize = 13.sp,
                color = YoTextMuted
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (seasons.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons) { s ->
                        val isSelected = s == selectedSeason
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) YoPrimaryAmber else YoBaseBackground)
                                .border(1.dp, if (isSelected) YoPrimaryAmber else YoBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedSeason = s }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Season $s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) YoBaseBackground else YoTextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val selectableKeysThisSeason = remember(visibleEpisodes, downloadStatusByEpisodeKey) {
                visibleEpisodes.map { episodeKey(it) }.filter { it !in downloadStatusByEpisodeKey }.toSet()
            }
            val allSelected = selectableKeysThisSeason.isNotEmpty() && selectedKeys.containsAll(selectableKeysThisSeason)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedKeys.isEmpty()) "No episodes selected" else "${selectedKeys.size} selected",
                    fontSize = 13.sp,
                    color = YoTextMuted
                )
                if (selectableKeysThisSeason.isNotEmpty()) {
                    Text(
                        text = if (allSelected) "Deselect all" else "Select all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoPrimaryAmber,
                        modifier = Modifier.clickable {
                            selectedKeys = if (allSelected) selectedKeys - selectableKeysThisSeason
                                           else selectedKeys + selectableKeysThisSeason
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(visibleEpisodes, key = { episodeKey(it) }) { ep ->
                    val key = episodeKey(ep)
                    val existingStatus = downloadStatusByEpisodeKey[key]
                    val isLocked = existingStatus != null
                    val isChecked = key in selectedKeys

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(YoBaseBackground)
                            .then(
                                if (isLocked) Modifier
                                else Modifier.clickable {
                                    selectedKeys = if (isChecked) selectedKeys - key else selectedKeys + key
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked || isLocked,
                            onCheckedChange = { checked ->
                                if (!isLocked) {
                                    selectedKeys = if (checked) selectedKeys + key else selectedKeys - key
                                }
                            },
                            enabled = !isLocked,
                            colors = CheckboxDefaults.colors(
                                checkedColor = YoPrimaryAmber,
                                uncheckedColor = YoTextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "E${ep.eNum} · ${ep.title ?: "Episode ${ep.eNum}"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (existingStatus != null) {
                            Text(
                                text = statusLabel(existingStatus),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = YoTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onDownloadSelected(episodes.filter { episodeKey(it) in selectedKeys }) },
                enabled = selectedKeys.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YoPrimaryAmber,
                    contentColor = YoBaseBackground
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedKeys.isEmpty()) "Select episodes"
                           else "Download ${selectedKeys.size} episode${if (selectedKeys.size > 1) "s" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    DownloadEntity.STATUS_COMPLETED -> "Downloaded"
    DownloadEntity.STATUS_DOWNLOADING -> "Downloading"
    DownloadEntity.STATUS_PAUSED -> "Paused"
    DownloadEntity.STATUS_QUEUED -> "Queued"
    else -> status
}
