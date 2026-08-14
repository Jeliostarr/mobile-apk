package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.util.buildReportMessage

private val REASONS = listOf(
    "Video won't play",
    "Wrong movie or episode",
    "No sound / bad audio",
    "Poor video quality",
    "Wrong VJ translation",
    "Download link broken",
    "Subtitles out of sync"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    movieId: String,
    movieTitle: String,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    val hasContent = reason != null || details.isNotBlank()

    val sheetState = remember { androidx.compose.material3.rememberModalBottomSheetState() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YoSurface,
        scrimColor = YoBaseBackground.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = YoPrimaryAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report a problem", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(movieTitle, fontSize = 13.sp, color = YoTextMuted, maxLines = 1)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                REASONS.forEach { r ->
                    val selected = reason == r
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) YoPrimaryAmber.copy(alpha = 0.18f) else YoSurface)
                            .border(
                                1.dp,
                                if (selected) YoPrimaryAmber else YoBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { reason = if (selected) null else r }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = r,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) YoPrimaryAmber else YoTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                placeholder = { Text("Add any details (optional) — e.g. which episode, at what time…", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                textStyle = TextStyle(fontSize = 14.sp, color = YoTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YoPrimaryAmber,
                    unfocusedBorderColor = YoBorder,
                    cursorColor = YoPrimaryAmber
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SharePlatformPicker(
                enabled = hasContent,
                buildMessage = { buildReportMessage(movieTitle, movieId, reason, details) },
                onSent = onDismiss
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}