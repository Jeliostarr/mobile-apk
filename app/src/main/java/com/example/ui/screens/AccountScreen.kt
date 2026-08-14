package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.HistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.AccountUser
import com.example.data.model.formatDuration
import com.example.repository.YocinemaRepository
import com.example.ui.components.VJBadgeChip
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    repository: YocinemaRepository,
    onLoginClick: () -> Unit,
    onMovieClick: (String) -> Unit,
    onPlayHistoryClick: (movieId: String, seasonNum: Int?, epNum: Int?, posMs: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoggedIn by repository.isLoggedInFlow.collectAsState(initial = repository.isLoggedIn())
    var accountUser by remember { mutableStateOf<AccountUser?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val watchlist by repository.watchlist.collectAsState(initial = emptyList())
    val history by repository.history.collectAsState(initial = emptyList())

    val apiKey = repository.tokenManager.getApiKey()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            accountUser = repository.getAccountMe()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "Account & Library",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = YoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Manage your subscription, watchlist & history",
                fontSize = 13.sp,
                color = YoTextMuted
            )
        }

        // User Info Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(YoSurface)
                .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(YoPrimaryAmber.copy(alpha = 0.15deg.coerceAtLeast(0.15f))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = YoPrimaryAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) (accountUser?.name ?: "YOCINEMA Subscriber") else "Guest Member",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLoggedIn) (accountUser?.email ?: "API Session Active") else "Enter API key for unlimited streaming",
                            fontSize = 12.sp,
                            color = YoTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoggedIn && !apiKey.isNullOrBlank()) {
                    val maskedKey = apiKey.take(6) + "••••••••" + apiKey.takeLast(4)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YoSurfaceVariant)
                            .border(1.dp, YoBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = YoPrimaryAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = maskedKey,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = YoTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("YOCINEMA API Key", apiKey)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "API Key copied securely", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Key",
                                tint = YoPrimaryAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { repository.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YoDestructive.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YoDestructive)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryAmber,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter API Key", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Watchlist / History Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = YoSurface,
            contentColor = YoPrimaryAmber,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = YoPrimaryAmber,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Watchlist (${watchlist.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("History (${history.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        if (selectedTab == 0) {
            if (watchlist.isEmpty()) {
                EmptyAccountState("Your watchlist is currently empty")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watchlist, key = { it.movieId }) { item ->
                        WatchlistRow(
                            item = item,
                            onClick = { onMovieClick(item.movieId) },
                            onDelete = {
                                scope.launch { repository.watchlistDao.deleteWatchlist(item.movieId) }
                            }
                        )
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                EmptyAccountState("No watch history recorded yet")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryRow(
                            item = item,
                            onClick = {
                                onPlayHistoryClick(item.movieId, item.seasonNumber, item.episodeNumber, item.positionMs)
                            },
                            onDelete = {
                                scope.launch { repository.historyDao.deleteHistory(item.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistRow(
    item: WatchlistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(YoSurfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.vjName.isNull_or_Blank()) {
                Spacer(modifier = Modifier.height(6.dp))
                VJBadgeChip(vjName = item.vjName!!)
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = YoTextMuted
            )
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(YoSurfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(YoPrimaryAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = YoBaseBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.seasonNumber != null && item.episodeNumber != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "S${item.seasonNumber} E${item.episodeNumber}${if (!item.episodeTitle.isNull_or_Blank()) " · ${item.episodeTitle}" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YoPrimaryAmber
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val posSec = (item.positionMs / 1000).toInt()
            val durSec = (item.durationMs / 1000).toInt()
            Text(
                text = "Resume at ${formatDuration(posSec)} of ${formatDuration(durSec)}",
                fontSize = 11.sp,
                color = YoTextMuted
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = YoTextMuted
            )
        }
    }
}

@Composable
fun EmptyAccountState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = YoTextMuted
        )
    }
}

private fun String?.isNull_or_Blank(): Boolean = this == null || this.trim().isEmpty()