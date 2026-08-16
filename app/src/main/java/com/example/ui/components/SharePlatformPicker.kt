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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlatformButton(
                label = "WhatsApp",
                icon = Icons.Default.Send,
                color = WhatsAppGreen,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                shareViaChat(context, SharePlatform.WHATSAPP, buildMessage())
                onSent()
            }
            PlatformButton(
                label = "Telegram",
                icon = Icons.Default.Send,
                color = TelegramBlue,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                shareViaChat(context, SharePlatform.TELEGRAM, buildMessage())
                onSent()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = YoTextMuted,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(16.dp)
            )
            Text(
                text = "Paste your message and send it in the chat.",
                fontSize = 12.sp,
                color = YoTextMuted
            )
        }
    }
}

@Composable
private fun PlatformButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                1.5.dp,
                if (enabled) color.copy(alpha = 0.5f) else YoTextMuted.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else YoTextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else YoTextMuted
        )
    }
}