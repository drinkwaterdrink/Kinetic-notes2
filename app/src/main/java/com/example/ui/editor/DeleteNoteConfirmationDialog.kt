package com.example.ui.editor

import androidx.compose.ui.platform.testTag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.theme.KineticTextSecondary

/**
 * Shared destructive-note confirmation used by editor surfaces.
 *
 * The dialog owns a one-shot guard so a fast repeated tap cannot dispatch the same delete
 * operation twice. The caller closes the dialog before starting its asynchronous repository
 * operation; a repository failure therefore leaves the editor mounted and able to show its
 * error notice.
 */
@Composable
fun DeleteNoteConfirmationDialog(
    noteTitle: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    var actionStarted by remember(noteTitle) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!actionStarted) onDismissRequest() },
        title = { Text("Delete this note?") },
        text = {
            Text(
                "${noteTitle.ifBlank { "This note" }} will be removed from the canvas and library. This cannot be undone.",
                color = KineticTextSecondary
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("confirm_delete_note"),
                enabled = !actionStarted,
                onClick = {
                    if (!actionStarted) {
                        actionStarted = true
                        onConfirm()
                    }
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("cancel_delete_note"),
                enabled = !actionStarted,
                onClick = onDismissRequest
            ) {
                Text("Cancel", color = KineticTextSecondary)
            }
        }
    )
}
