package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary

@Composable
fun AskGeminiBottomBar(
    isExpanded: Boolean,
    isLoading: Boolean,
    aiOutput: String?,
    onToggleExpand: () -> Unit,
    onAutoSort: () -> Unit,
    onSynthesizeSpace: () -> Unit,
    onBeautifyCurrentNote: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("ask_gemini_container")
    ) {
        // Expandable Action Sheet & Output
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = KineticDarkSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = KineticSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Spatial Assistant",
                            color = KineticTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = KineticSecondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            IconButton(
                                onClick = onToggleExpand,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = KineticTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick AI Feature Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = KineticDarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAutoSort() }
                                .testTag("btn_ai_autosort")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = KineticSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Auto-Sort", color = KineticTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("Quadrants", color = KineticTextMuted, fontSize = 9.sp)
                            }
                        }

                        Surface(
                            color = KineticDarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSynthesizeSpace() }
                                .testTag("btn_ai_synthesize")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Synthesize", tint = KineticPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Synthesize", color = KineticTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("Active Space", color = KineticTextMuted, fontSize = 9.sp)
                            }
                        }

                        Surface(
                            color = KineticDarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onBeautifyCurrentNote() }
                                .testTag("btn_ai_tidy")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Icon(Icons.Outlined.Checklist, contentDescription = "Tidy", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tidy Note", color = KineticTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("Neat Format", color = KineticTextMuted, fontSize = 9.sp)
                            }
                        }
                    }

                    // AI Result Text
                    aiOutput?.let { output ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = KineticDarkSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output,
                                color = KineticTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Capsule Bar (exact match to video recording)
        Surface(
            color = Color(0xFF151926).copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x28FFFFFF)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .height(52.dp)
                .testTag("ask_gemini_capsule")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Left '+' Action Button
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Actions",
                        tint = KineticTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Input / Trigger
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = "Ask Gemini",
                            color = KineticTextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (queryText.isNotBlank()) {
                            onSubmitQuery(queryText)
                            queryText = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = KineticTextPrimary,
                        unfocusedTextColor = KineticTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_ask_gemini")
                )

                // Voice / Mic Icon
                IconButton(
                    onClick = {
                        onSubmitQuery("Summarize and format notes in space")
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = KineticTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Send / Sparkle Submit Button
                IconButton(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            onSubmitQuery(queryText)
                            queryText = ""
                        } else {
                            onToggleExpand()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Send",
                        tint = KineticSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
