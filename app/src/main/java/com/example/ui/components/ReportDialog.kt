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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String) -> Unit
) {
    val reasons = listOf(
        "Broken stream link",
        "No audio / Audio out of sync",
        "Wrong episode or movie",
        "Subtitle errors",
        "Poor video quality",
        "Other issue"
    )

    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YoSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = YoPrimaryAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.padding(start = 10.dp))
                Text(
                    text = "Report Stream Issue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Select what problem you are experiencing with this video:",
                    fontSize = 13.sp,
                    color = YoTextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                reasons.forEach { reason ->
                    val isSelected = (reason == selectedReason)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) YoSurfaceVariant else YoSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) YoPrimaryAmber else YoBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = reason,
                            fontSize = 13.sp,
                            color = if (isSelected) YoTextPrimary else YoTextMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) YoPrimaryAmber else YoBorder,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (selectedReason == "Other issue") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = { Text("Describe the issue...", color = YoTextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YoPrimaryAmber,
                            unfocusedBorderColor = YoBorder,
                            focusedTextColor = YoTextPrimary,
                            unfocusedTextColor = YoTextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Other issue" && customNotes.isNotBlank()) {
                        "Other: $customNotes"
                    } else selectedReason
                    onSubmitReport(finalReason)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YoPrimaryAmber,
                    contentColor = YoBaseBackground
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = YoTextMuted, fontSize = 13.sp)
            }
        }
    )
}
