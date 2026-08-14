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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

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
        containerColor = YoSurface,
        scrimColor = YoBaseBackground.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Download Episodes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select one or more episodes to download",
                fontSize = 14.sp,
                color = YoTextMuted
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (seasons.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(seasons) { s ->
                        val isSelected = s == selectedSeason
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) YoPrimaryAmber else YoSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) YoPrimaryAmber else YoBorder,
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable { selectedSeason = s }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Season $s",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) YoBaseBackground else YoTextPrimary
                            )
                        }
                    }
                }
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
                    fontSize = 14.sp,
                    color = YoTextMuted
                )
                if (selectableKeysThisSeason.isNotEmpty()) {
                    Text(
                        text = if (allSelected) "Deselect all" else "Select all",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoPrimaryAmber,
                        modifier = Modifier.clickable {
                            selectedKeys = if (allSelected) selectedKeys - selectableKeysThisSeason
                                           else selectedKeys + selectableKeysThisSeason
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(YoSurfaceVariant)
                            .then(
                                if (isLocked) Modifier
                                else Modifier.clickable {
                                    selectedKeys = if (isChecked) selectedKeys - key else selectedKeys + key
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "E${ep.eNum} · ${ep.title ?: "Episode ${ep.eNum}"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLocked) YoTextMuted else YoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (existingStatus != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(YoPrimaryAmber.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusLabel(existingStatus),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = YoPrimaryAmber
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onDownloadSelected(episodes.filter { episodeKey(it) in selectedKeys }) },
                enabled = selectedKeys.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 24.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), clip = false),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YoPrimaryAmber,
                    contentColor = YoBaseBackground,
                    disabledContainerColor = YoSurfaceVariant,
                    disabledContentColor = YoTextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedKeys.isEmpty()) "Select episodes"
                           else "Download ${selectedKeys.size} episode${if (selectedKeys.size > 1) "s" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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