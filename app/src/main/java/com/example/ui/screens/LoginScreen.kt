package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import kotlinx.coroutines.launch

private const val TUTORIAL_YOUTUBE_VIDEO_ID = "LCX5Ropft-8"
private val TUTORIAL_YOUTUBE_URL =
    if (TUTORIAL_YOUTUBE_VIDEO_ID.isNotBlank())
        "https://www.youtube.com/watch?v=$TUTORIAL_YOUTUBE_VIDEO_ID"
    else
        "https://www.youtube.com/results?search_query=yocinema+api+key+tutorial"

private val TUTORIAL_THUMBNAIL_URL =
    if (TUTORIAL_YOUTUBE_VIDEO_ID.isNotBlank())
        "https://img.youtube.com/vi/$TUTORIAL_YOUTUBE_VIDEO_ID/hqdefault.jpg"
    else null

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
                    colors = listOf(Color(0xFF0A0A0C), Color(0xFF16141C), Color(0xFF0A0A0C))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ---- Logo (small) ----
            Image(
                painter = painterResource(id = R.drawable.yocinema_logo_1786014644709),
                contentDescription = "YOCINEMA Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "YOCINEMA",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = YoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Enter your API key to continue",
                fontSize = 12.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ---- Input card ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(YoSurface)
                    .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("Paste your API key", color = YoTextMuted, fontSize = 13.sp) },
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
                        shape = RoundedCornerShape(14.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = YoDestructive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ---- Sign In Button ----
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
                            .height(44.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp), clip = false),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryViolet,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        if (isLoading) {
                            ModernLoader(size = 20.dp, strokeWidth = 3.dp)
                        } else {
                            Text(
                                text = "Sign In & Continue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---- "Get a key" OutlinedButton (restored) ----
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DASHBOARD_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YoBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YoTextPrimary)
                    ) {
                        Text("Get a key at the Dashboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.size(6.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = YoTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ---- Tutorial card (compact thumbnail) ----
            TutorialCard(
                onOpenInYouTube = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TUTORIAL_YOUTUBE_URL))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ---- Version ----
            Text(
                text = "v1.0.0",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = YoTextMuted.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun TutorialCard(onOpenInYouTube: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "New here? Watch the tutorial",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary
        )
        Text(
            text = "How to create an account and get your API key",
            fontSize = 10.sp,
            color = YoTextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Compact thumbnail (80dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF15151A))
                .clickable(onClick = onOpenInYouTube)
        ) {
            if (TUTORIAL_THUMBNAIL_URL != null) {
                AsyncImage(
                    model = TUTORIAL_THUMBNAIL_URL,
                    contentDescription = "Tutorial preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                )
            }

            // Play button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Watch on YouTube",
                    tint = YoPrimaryViolet,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "Open in YouTube" link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenInYouTube),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Open in YouTube", fontSize = 10.sp, color = YoTextMuted)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = YoTextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}