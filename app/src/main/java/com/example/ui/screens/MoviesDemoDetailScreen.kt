package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.MdCastMember
import com.example.data.model.MdDetails
import com.example.data.model.MdQuality
import com.example.data.model.moviesDemoDownloadFilename
import com.example.data.model.moviesDemoDownloadUrl
import com.example.data.model.moviesDemoStreamUrl
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoGlowGradient
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoRatingGold
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.MoviesDemoDownloader
import kotlinx.coroutines.launch

@OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MoviesDemoDetailScreen(
    detailPath: String,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onPlayClick: (detailPath: String, title: String, seasonNum: Int?, epNum: Int?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var details by remember { mutableStateOf<MdDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var season by remember { mutableIntStateOf(1) }
    var episode by remember { mutableIntStateOf(1) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var isPlayingTrailer by remember { mutableStateOf(false) }
    var downloadQualities by remember { mutableStateOf<List<MdQuality>>(emptyList()) }
    var loadingQualities by remember { mutableStateOf(false) }
    var seasonsInfo by remember { mutableStateOf<com.example.data.model.MdSeasonsResponse?>(null) }

    LaunchedEffect(detailPath) {
        isLoading = true
        val response = repository.moviesDemoApi.details(detailPath)
        val d = if (response.isSuccessful) response.body() else null
        details = d
        isLoading = false
        if (d?.type == "tv") {
            val seasonsRes = repository.moviesDemoApi.seasons(detailPath)
            seasonsInfo = if (seasonsRes.isSuccessful) seasonsRes.body() else null
        }
    }

    fun openDownloadSheet() {
        val d = details ?: return
        scope.launch {
            loadingQualities = true
            showDownloadSheet = true
            val response = if (d.type == "tv") {
                repository.moviesDemoApi.tvStream(detailPath, season, episode)
            } else {
                repository.moviesDemoApi.movieStream(detailPath)
            }
            downloadQualities = if (response.isSuccessful) response.body()?.freeQualities ?: emptyList() else emptyList()
            loadingQualities = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(YoBaseBackground)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ModernLoader()
            }
        } else if (details == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Couldn't load this title", color = YoTextMuted, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBackClick) { Text("Go back") }
            }
        } else {
            val d = details!!
            val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.Companion.statusBars
                .asPaddingValues().calculateTopPadding()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // The Box below defines the layout height everything else in
                    // this LazyColumn flows around (a normal 3:4 cover). The
                    // image inside is drawn taller than that — extended upward
                    // by exactly the status bar's height and offset to match —
                    // so it visually reaches the true top of the screen instead
                    // of stopping at the Scaffold's inset padding, without
                    // pushing every other section down by that same amount.
                    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
                        val imageHeight = maxWidth * (4f / 3f) + statusBarHeight
                        SubcomposeAsyncImage(
                            model = d.cover,
                            contentDescription = d.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .offset(y = -statusBarHeight),
                            contentScale = ContentScale.Crop,
                            loading = { YoCinemaLogoPlaceholder() },
                            error = { YoCinemaLogoPlaceholder() }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, YoBaseBackground)))
                        )
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(top = statusBarHeight + 8.dp, start = 12.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(
                            text = d.title ?: "Untitled",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val metaParts = listOfNotNull(
                            d.imdbRatingValue?.takeIf { it.isNotBlank() },
                            d.releaseDate?.take(4)?.takeIf { it.length == 4 },
                            d.countryName?.takeIf { it.isNotBlank() },
                            d.durationText ?: (d.duration?.let { "${it / 60}h ${it % 60}m" })
                        )
                        if (metaParts.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!d.imdbRatingValue.isNullOrBlank()) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = YoRatingGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = metaParts.joinToString("  •  "),
                                    fontSize = 12.sp,
                                    color = YoTextMuted
                                )
                            }
                        }

                        if (!d.genre.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row {
                                d.genre.split(",").take(4).forEach { g ->
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(YoSurfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = g.trim(), fontSize = 10.sp, color = YoTextMuted)
                                    }
                                }
                            }
                        }

                        if (!d.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = d.description,
                                fontSize = 13.sp,
                                color = YoTextMuted,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // ── Trailer: tap to play, never autoplay — plays inline right
                // here rather than rotating to full-screen, since it's a
                // short clip the person is previewing, not committing to
                // watch like the movie itself. ──
                if (!d.trailer?.url.isNullOrBlank()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Trailer", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black)
                            ) {
                                if (isPlayingTrailer) {
                                    InlineTrailerPlayer(
                                        trailerUrl = d.trailer?.url ?: "",
                                        apiKey = repository.tokenManager.getApiKey()
                                    )
                                } else {
                                    SubcomposeAsyncImage(
                                        model = d.trailer?.cover ?: d.cover,
                                        contentDescription = "Trailer",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { isPlayingTrailer = true },
                                        contentScale = ContentScale.Crop,
                                        loading = { YoCinemaLogoPlaceholder() },
                                        error = { YoCinemaLogoPlaceholder() }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f))
                                            .clickable { isPlayingTrailer = true }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Play trailer",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(56.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!d.cast.isNullOrEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Cast",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(d.cast) { member -> MdCastChip(member) }
                            }
                        }
                    }
                }

                if (d.type == "tv") {
                    item {
                        val info = seasonsInfo
                        val seasonCount = info?.seasonCount ?: info?.seasons?.size ?: 1
                        val currentSeasonData = info?.seasons?.firstOrNull { it.season == season }
                        val episodeCount = currentSeasonData?.maxEp ?: 1

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(
                                "Season",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextMuted,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items((1..seasonCount).toList()) { s ->
                                    MdSelectableChip(
                                        label = "S$s",
                                        selected = s == season,
                                        onClick = { season = s; episode = 1 }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Episode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextMuted,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items((1..episodeCount).toList()) { e ->
                                    MdSelectableChip(
                                        label = "$e",
                                        selected = e == episode,
                                        onClick = { episode = e }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }

        if (details != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(YoBaseBackground)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val d = details!!
                        onPlayClick(
                            detailPath,
                            d.title ?: "Untitled",
                            if (d.type == "tv") season else null,
                            if (d.type == "tv") episode else null
                        )
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { openDownloadSheet() },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = YoPrimaryViolet)
                }
            }
        }

        if (showDownloadSheet) {
            ModalBottomSheet(onDismissRequest = { showDownloadSheet = false }, containerColor = YoSurface) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp)) {
                    Text("Choose quality", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (loadingQualities) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            ModernLoader()
                        }
                    } else if (downloadQualities.isEmpty()) {
                        Text("No downloadable quality available", color = YoTextMuted, fontSize = 13.sp)
                    } else {
                        downloadQualities.forEach { q ->
                            val d = details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val url = q.url ?: return@clickable
                                        val filename = moviesDemoDownloadFilename(
                                            title = d?.title ?: "video",
                                            resolution = q.resolution ?: 0,
                                            season = if (d?.type == "tv") season else null,
                                            episode = if (d?.type == "tv") episode else null
                                        )
                                        val apiKey = repository.tokenManager.getApiKey()
                                        MoviesDemoDownloader.startDownload(
                                            context,
                                            moviesDemoDownloadUrl(url, filename),
                                            filename,
                                            apiKey
                                        )
                                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                        showDownloadSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${q.resolution ?: "?"}P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
                                val sizeText = q.size_mb?.let { "%.0f MB".format(it) }
                                if (sizeText != null) {
                                    Text(sizeText, fontSize = 12.sp, color = YoTextMuted)
                                }
                                Icon(Icons.Default.Download, contentDescription = null, tint = YoPrimaryViolet, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MdCastChip(member: MdCastMember) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = member.avatarUrl,
            contentDescription = member.name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(YoSurfaceVariant),
            contentScale = ContentScale.Crop,
            loading = { YoCinemaLogoPlaceholder() },
            error = { YoCinemaLogoPlaceholder() }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name ?: "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!member.character.isNullOrBlank()) {
            Text(
                text = member.character,
                fontSize = 10.sp,
                color = YoTextMuted,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MdSelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brush.linearGradient(YoGlowGradient) else Brush.linearGradient(listOf(YoSurfaceVariant, YoSurfaceVariant)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) YoBaseBackground else YoTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun InlineTrailerPlayer(trailerUrl: String, apiKey: String?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var player by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }

    androidx.compose.runtime.DisposableEffect(trailerUrl) {
        val headers = if (!apiKey.isNullOrBlank()) mapOf("X-API-Key" to apiKey) else emptyMap()
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val exo = androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
        exo.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(moviesDemoStreamUrl(trailerUrl))))
        exo.playWhenReady = true
        exo.prepare()
        player = exo
        onDispose { exo.release() }
    }

    val currentPlayer = player
    if (currentPlayer != null) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    this.player = currentPlayer
                    useController = true
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
