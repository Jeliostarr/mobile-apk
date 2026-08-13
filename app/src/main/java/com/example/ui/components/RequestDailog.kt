package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.util.buildRequestMessage

/**
 * "Request a movie or series" — collects what the user wants, then hands
 * off to WhatsApp/Telegram. No backend/API call is made.
 */
@Composable
fun RequestDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(YoBaseBackground)
                .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = YoPrimaryAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request a movie or series", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
                }
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text("Tell us what you're looking for — we'll pick it up in the chat.", fontSize = 12.sp, color = YoTextMuted)

                Spacer(modifier = Modifier.padding(top = 14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Movie or series title", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = YoTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YoPrimaryAmber,
                        unfocusedBorderColor = YoBorder,
                        cursorColor = YoPrimaryAmber
                    )
                )

                Spacer(modifier = Modifier.padding(top = 10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Any details (optional) — year, VJ, language…", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    textStyle = TextStyle(fontSize = 13.sp, color = YoTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YoPrimaryAmber,
                        unfocusedBorderColor = YoBorder,
                        cursorColor = YoPrimaryAmber
                    )
                )

                Spacer(modifier = Modifier.padding(top = 14.dp))

                SharePlatformPicker(
                    enabled = title.isNotBlank(),
                    buildMessage = { buildRequestMessage(title.trim(), note) },
                    onSent = onDismiss
                )
            }
        }
    }
}
