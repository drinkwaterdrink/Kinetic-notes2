package com.example.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChecklistItemEntity
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary

@Composable
fun ChecklistEditor(
    items: List<ChecklistItemEntity>,
    onAddItem: (String) -> Unit,
    onToggleItem: (ChecklistItemEntity) -> Unit,
    onDeleteItem: (ChecklistItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var newItemText by remember { mutableStateOf("") }

    val completedCount = items.count { it.isChecked }
    val progress = if (items.isNotEmpty()) completedCount.toFloat() / items.size else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("checklist_editor")
    ) {
        // Progress Card
        Surface(
            color = KineticDarkSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TASK PROGRESS",
                        color = KineticTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$completedCount of ${items.size} completed (${(progress * 100).toInt()}%)",
                        color = KineticSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF10B981),
                    trackColor = KineticDarkBorder,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // Add New Item Input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text("Add next checklist task...", color = KineticTextMuted, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = KineticDarkSurface,
                    unfocusedContainerColor = KineticDarkSurface,
                    focusedBorderColor = KineticPrimary,
                    unfocusedBorderColor = KineticDarkBorder,
                    focusedTextColor = KineticTextPrimary,
                    unfocusedTextColor = KineticTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_new_task")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(KineticPrimary, RoundedCornerShape(12.dp))
                    .testTag("btn_add_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = Color.White
                )
            }
        }

        // Checklist Items List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(items, key = { it.id }) { item ->
                Surface(
                    color = KineticDarkSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleItem(item) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { onToggleItem(item) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF10B981),
                                uncheckedColor = KineticTextMuted,
                                checkmarkColor = Color.Black
                            )
                        )

                        Text(
                            text = item.text,
                            color = if (item.isChecked) KineticTextMuted else KineticTextPrimary,
                            fontSize = 14.sp,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { onDeleteItem(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Item",
                                tint = KineticTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = "No checklist items yet. Add tasks above!",
                        color = KineticTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
