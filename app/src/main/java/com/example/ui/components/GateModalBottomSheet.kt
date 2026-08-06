package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateModalBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onEnterKeyClicked: () -> Unit,
    onSaveKey: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YoSurface,
        scrimColor = YoBaseBackground.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = YoPrimaryAmber,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "API Key Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter or paste your YOCINEMA API key to unlock full video playback, high-speed downloads, search & media details.",
                fontSize = 13.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                placeholder = { Text("Paste your API key here...", fontSize = 13.sp, color = YoTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YoPrimaryAmber,
                    unfocusedBorderColor = YoBorder,
                    focusedContainerColor = YoSurfaceVariant,
                    unfocusedContainerColor = YoSurfaceVariant,
                    focusedTextColor = YoTextPrimary,
                    unfocusedTextColor = YoTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (inputKey.isNotBlank() && onSaveKey != null) {
                Button(
                    onClick = {
                        onSaveKey(inputKey.trim())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoPrimaryAmber,
                        contentColor = YoBaseBackground
                    )
                ) {
                    Text(
                        text = "Save Key & Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onEnterKeyClicked()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoPrimaryAmber,
                        contentColor = YoBaseBackground
                    )
                ) {
                    Text(
                        text = "Go to Key / Login Screen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.yocinema.dpdns.org"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = YoTextPrimary
                )
            ) {
                Text(
                    text = "Get a Key Online",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = YoTextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
