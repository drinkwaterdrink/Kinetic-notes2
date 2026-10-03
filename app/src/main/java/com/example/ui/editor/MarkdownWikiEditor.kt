package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun MarkdownWikiEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Regex for finding [[WikiLinks]] in the note markdown
    val wikiLinks by remember(content) {
        derivedStateOf {
            val regex = Regex("\\[\\[([^\\]]+)\\]\\]")
            regex.findAll(content).map { it.groupValues[1] }.distinct().toList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_wiki_editor")
    ) {
        // Markdown Quick Formatting Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(KineticDarkSurfaceVariant, RoundedCornerShape(10.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = { onContentChange("$content**bold text**") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { onContentChange("$content*italic text*") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { onContentChange("$content\n# Heading 1\n") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Title, contentDescription = "Heading", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { onContentChange("$content\n- [ ] Task item\n") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Task", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { onContentChange("$content\n> Quote block\n") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { onContentChange("$content\n```typescript\n// Code snippet\nconst result = 42;\n```\n") },
                modifier = Modifier.size(36.dp)
            ) {
                Text("</>", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            IconButton(
                onClick = { onContentChange("$content[[Linked Note]]") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = "WikiLink", tint = KineticSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // Detected [[WikiLinks]] Pill Row
        if (wikiLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "LINKS:",
                    color = KineticTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )

                wikiLinks.forEach { linkTitle ->
                    Surface(
                        color = KineticPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KineticPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { onWikiLinkClick(linkTitle) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Link",
                                tint = KineticPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "[[$linkTitle]]",
                                color = KineticSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Markdown Text Field
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = {
                Text(
                    text = "Write your thoughts using Markdown and [[WikiLinks]] to connect notes...",
                    color = KineticTextMuted,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KineticDarkSurface,
                unfocusedContainerColor = KineticDarkSurface,
                focusedBorderColor = KineticPrimary.copy(alpha = 0.5f),
                unfocusedBorderColor = KineticDarkBorder,
                focusedTextColor = KineticTextPrimary,
                unfocusedTextColor = KineticTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("input_markdown_content")
        )
    }
}
