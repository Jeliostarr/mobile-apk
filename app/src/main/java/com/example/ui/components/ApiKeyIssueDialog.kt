package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SpeedOutlined
import androidx.compose.material.icons.filled.VpnKeyOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.ApiKeyIssue
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

private val WarningAmber = Color(0xFFFFA726)

@Composable
fun ApiKeyIssueDialog(
    issue: ApiKeyIssue,
    onSwitchKey: () -> Unit,
    onPurchase: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, tint) = iconFor(issue)
    val title = titleFor(issue)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(YoSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = issue.message,
                fontSize = 13.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (issue.canSwitchKey) {
                Button(
                    onClick = onSwitchKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoPrimaryViolet,
                        contentColor = YoBaseBackground
                    )
                ) {
                    Text("Switch API Key", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (issue.canPurchase) {
                OutlinedButton(
                    onClick = onPurchase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (issue is ApiKeyIssue.RateLimited) "Upgrade Plan" else "Get a New Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = YoTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            TextButton(onClick = onDismiss) {
                Text("Not now", color = YoTextMuted, fontSize = 13.sp)
            }
        }
    }
}

private fun titleFor(issue: ApiKeyIssue): String = when (issue) {
    is ApiKeyIssue.Missing -> "Sign in required"
    is ApiKeyIssue.Invalid -> "Key not recognized"
    is ApiKeyIssue.Revoked -> "Key revoked"
    is ApiKeyIssue.Deleted -> "Key no longer exists"
    is ApiKeyIssue.Paused -> "Key paused"
    is ApiKeyIssue.Suspended -> "Key suspended"
    is ApiKeyIssue.Expired -> "Key expired"
    is ApiKeyIssue.AccountInactive -> "Account inactive"
    is ApiKeyIssue.RateLimited -> "Daily limit reached"
    is ApiKeyIssue.Other -> "Something's wrong with this key"
}

private fun iconFor(issue: ApiKeyIssue): Pair<ImageVector, Color> =
    when (issue) {
        is ApiKeyIssue.Expired -> Icons.Default.HourglassBottom to WarningAmber
        is ApiKeyIssue.RateLimited -> Icons.Default.SpeedOutlined to WarningAmber
        is ApiKeyIssue.Paused -> Icons.Default.PauseCircle to WarningAmber
        is ApiKeyIssue.Revoked, is ApiKeyIssue.Deleted, is ApiKeyIssue.Suspended -> Icons.Default.Block to YoDestructive
        is ApiKeyIssue.AccountInactive -> Icons.Default.PersonOff to YoDestructive
        is ApiKeyIssue.Missing, is ApiKeyIssue.Invalid -> Icons.Default.VpnKeyOff to YoDestructive
        is ApiKeyIssue.Other -> Icons.Default.Key to WarningAmber
    }