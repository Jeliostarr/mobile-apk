package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width   // ✅ ADDED this import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DASHBOARD_URL
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

private const val TUTORIAL_YOUTUBE_VIDEO_ID = ""
private val TUTORIAL_YOUTUBE_URL =
    if (TUTORIAL_YOUTUBE_VIDEO_ID.isNotBlank())
        "https://www.youtube.com/watch?v=$LCX5Ropft-8"
        
    else
        "https://www.youtube.com/results?search_query=yocinema+api+key+tutorial"

@Composable
fun LoginScreen(
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKeyInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0C),
                        Color(0xFF1A1A20)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.yocinema_logo_1786014644709),
                    contentDescription = "YOCINEMA Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .shadow(10.dp, CircleShape, clip = false)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "YOCINEMA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your API key to continue",
                fontSize = 13.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(26.dp), clip = false)
                    .clip(RoundedCornerShape(26.dp))
                    .background(YoSurface)
                    .border(1.dp, YoBorder, RoundedCornerShape(26.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("Paste your API key", color = YoTextMuted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = YoPrimaryViolet
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text
                                        if (!text.isNullOrBlank()) {
                                            apiKeyInput = text.toString().trim()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = YoPrimaryViolet
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YoPrimaryViolet,
                            unfocusedBorderColor = YoBorder,
                            focusedContainerColor = YoBaseBackground,
                            unfocusedContainerColor = YoBaseBackground,
                            focusedTextColor = YoTextPrimary,
                            unfocusedTextColor = YoTextPrimary
                        )
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = YoDestructive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (apiKeyInput.trim().isBlank()) {
                                errorMessage = "Please enter an API key"
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val result = repository.loginWithKey(apiKeyInput.trim())
                                isLoading = false
                                if (result.isSuccess) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryViolet,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        if (isLoading) {
                            ModernLoader(size = 26.dp, strokeWidth = 3.dp)
                        } else {
                            Text(
                                text = "Sign In & Continue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DASHBOARD_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YoTextPrimary)
                    ) {
                        Text("Get a key at the Dashboard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = YoTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TutorialCard(
                videoId = TUTORIAL_YOUTUBE_VIDEO_ID,
                onOpenInYouTube = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TUTORIAL_YOUTUBE_URL))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        Text(
            text = "v1.0.0",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = YoTextMuted.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

// ✅ kept this extension – now used correctly
private fun String?.isNullOrBlank(): Boolean = this == null || this.trim().isEmpty()

@Composable
private fun TutorialCard(videoId: String, onOpenInYouTube: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "New here? Watch the tutorial",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary
        )
        Text(
            text = "How to create an account and get your API key",
            fontSize = 11.sp,
            color = YoTextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (videoId.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF15151A))
                    .clickable(onClick = onOpenInYouTube),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Open YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.cueVideo(videoId, 0f)
                                }
                            })
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenInYouTube),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open in YouTube", fontSize = 11.sp, color = YoTextMuted)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = YoTextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}