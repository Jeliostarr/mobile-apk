package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoTextMuted
import com.example.util.SharePlatform
import com.example.util.shareViaChat

private val WhatsAppGreen = Color(0xFF25D366)
private val TelegramBlue = Color(0xFF229ED9)

/**
 * Two big platform buttons. No API/network call to our own backend happens
 * here — this only copies text and opens a chat app/link.
 */
@Composable
fun SharePlatformPicker(
    enabled: Boolean,
    buildMessage: () -> String,
    onSent: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlatformButton(
                label = "WhatsApp",
                color = WhatsAppGreen,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                shareViaChat(context, SharePlatform.WHATSAPP, buildMessage())
                onSent()
            }
            PlatformButton(
                label = "Telegram",
                color = TelegramBlue,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                shareViaChat(context, SharePlatform.TELEGRAM, buildMessage())
                onSent()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = YoTextMuted,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(14.dp)
            )
            Text(
                text = "Opens our community chat — paste your message there and send it.",
                fontSize = 11.sp,
                color = YoTextMuted
            )
        }
    }
}

@Composable
private fun PlatformButton(
    label: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (enabled) 0.14f else 0.06f))
            .border(1.dp, color.copy(alpha = if (enabled) 0.35f else 0.12f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else YoTextMuted
        )
    }
}
