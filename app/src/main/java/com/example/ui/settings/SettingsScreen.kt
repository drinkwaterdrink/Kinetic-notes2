package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDanger
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import com.example.ui.viewmodel.FolderItem

private val GroupColors = listOf("#6366F1", "#38BDF8", "#10B981", "#EC4899", "#8B5CF6", "#F59E0B")

/**
 * Conventional, reachable Settings surface. Group management lives here rather than in a
 * dead dialog: groups are durable Room records and all destructive actions are explicit.
 */
@Composable
fun SettingsScreen(
    groups: List<FolderItem>,
    onBack: () -> Unit,
    onCreateGroup: (name: String, colorHex: String) -> Unit,
    onUpdateGroup: (id: String, name: String, colorHex: String) -> Unit,
    onDeleteGroupKeepNotes: (id: String) -> Unit,
    onDeleteGroupAndNotes: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorGroup by remember { mutableStateOf<FolderItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Surface(
        color = KineticDarkBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101420))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KineticTextPrimary
                    )
                }
                Icon(Icons.Default.Settings, contentDescription = null, tint = KineticSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Settings", color = KineticTextPrimary, fontSize = 18.sp)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                item {
                    Text("Workspace", color = KineticTextPrimary, fontSize = 20.sp)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        "Manage the durable groups used to organize your notes.",
                        color = KineticTextSecondary,
                        fontSize = 13.sp
                    )
                }
                item {
                    Surface(
                        color = KineticDarkSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x20FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(KineticPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = KineticPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("New group", color = KineticTextPrimary, fontSize = 14.sp)
                                Text("Create a durable group", color = KineticTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item {
                    Text("Groups", color = KineticTextSecondary, fontSize = 12.sp)
                }
                items(groups.filter { it.id != null }, key = { it.id!! }) { group ->
                    Surface(
                        color = KineticDarkSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseGroupColor(group.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, color = KineticTextPrimary, fontSize = 14.sp)
                                Text("Notes stay safe when the group is deleted", color = KineticTextMuted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { editorGroup = group }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit ${group.name}", tint = KineticTextSecondary)
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Delete group keeps its notes in All Notes. The separate Delete group and notes action is irreversible.",
                        color = KineticTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        GroupEditorDialog(
            title = "New group",
            initialName = "",
            initialColor = GroupColors.first(),
            showDeleteActions = false,
            onDismiss = { showCreateDialog = false },
            onSave = { name, color ->
                onCreateGroup(name, color)
                showCreateDialog = false
            },
            onDeleteKeepNotes = {},
            onDeleteAndNotes = {}
        )
    }

    editorGroup?.let { group ->
        GroupEditorDialog(
            title = "Edit group",
            initialName = group.name,
            initialColor = group.colorHex,
            showDeleteActions = true,
            onDismiss = { editorGroup = null },
            onSave = { name, color ->
                onUpdateGroup(group.id!!, name, color)
                editorGroup = null
            },
            onDeleteKeepNotes = {
                onDeleteGroupKeepNotes(group.id!!)
                editorGroup = null
            },
            onDeleteAndNotes = {
                onDeleteGroupAndNotes(group.id!!)
                editorGroup = null
            }
        )
    }
}

@Composable
private fun GroupEditorDialog(
    title: String,
    initialName: String,
    initialColor: String,
    showDeleteActions: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit,
    onDeleteKeepNotes: () -> Unit,
    onDeleteAndNotes: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var color by remember(initialColor) { mutableStateOf(initialColor) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteNotes by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = KineticTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = KineticPrimary,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedLabelColor = KineticPrimary,
                        unfocusedLabelColor = KineticTextMuted
                    )
                )
                Text("Color", color = KineticTextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GroupColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(parseGroupColor(hex))
                                .border(
                                    width = if (color.equals(hex, true)) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { color = hex }
                        )
                    }
                }
                if (showDeleteActions) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = KineticDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Delete group, keep notes", color = KineticDanger)
                    }
                    TextButton(onClick = { confirmDeleteNotes = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = KineticDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Delete group and notes", color = KineticDanger)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.trim().isNotBlank()) onSave(name.trim(), color) },
                colors = ButtonDefaults.buttonColors(containerColor = KineticPrimary)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = KineticTextSecondary) } },
        containerColor = Color(0xFF141824)
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete group?", color = KineticTextPrimary) },
            text = { Text("The group will be removed, but all notes inside will stay in All Notes.", color = KineticTextSecondary) },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDeleteKeepNotes() }, colors = ButtonDefaults.buttonColors(containerColor = KineticPrimary)) {
                    Text("Delete group")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = KineticTextSecondary) } },
            containerColor = Color(0xFF141824)
        )
    }

    if (confirmDeleteNotes) {
        AlertDialog(
            onDismissRequest = { confirmDeleteNotes = false },
            title = { Text("Delete group and notes?", color = KineticDanger) },
            text = { Text("Every note in this group will be permanently deleted. This cannot be undone.", color = KineticTextSecondary) },
            confirmButton = {
                Button(onClick = { confirmDeleteNotes = false; onDeleteAndNotes() }, colors = ButtonDefaults.buttonColors(containerColor = KineticDanger)) {
                    Text("Delete everything")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteNotes = false }) { Text("Cancel", color = KineticTextSecondary) } },
            containerColor = Color(0xFF141824)
        )
    }
}

private fun parseGroupColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    KineticSecondary
}
