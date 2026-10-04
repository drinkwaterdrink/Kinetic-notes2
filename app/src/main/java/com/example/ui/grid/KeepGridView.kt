package com.example.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import com.example.data.local.NoteEntity
import com.example.data.local.NoteType
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary

@Composable
fun KeepGridView(
    notes: List<NoteEntity>,
    searchQuery: String,
    recentSearches: List<String> = emptyList(),
    selectedFolder: String,
    folders: List<com.example.ui.viewmodel.FolderItem>,
    tags: List<String> = emptyList(),
    selectedTag: String? = null,
    unlockedNoteIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit = {},
    /** Increment this token to open the search field and summon the keyboard. */
    requestSearchFocusToken: Int = 0,
    onFolderSelect: (String) -> Unit,
    onTagSelect: (String?) -> Unit = {},
    onNoteClick: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedNotes = notes.filter { it.isPinned }
    val otherNotes = notes.filter { !it.isPinned }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestSearchFocusToken) {
        if (requestSearchFocusToken > 0) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KineticDarkBackground)
            .testTag("keep_grid_view")
    ) {
        // Search Bar (Samsung / Keep style)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text("Search notes, groups, or links...", color = KineticTextMuted, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = KineticTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = KineticTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmitted(searchQuery) }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KineticDarkSurface,
                unfocusedContainerColor = KineticDarkSurface,
                focusedBorderColor = KineticPrimary,
                unfocusedBorderColor = KineticDarkBorder,
                focusedTextColor = KineticTextPrimary,
                unfocusedTextColor = KineticTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(searchFocusRequester)
                .testTag("input_search_notes")
        )

        if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
            ) {
                Text("RECENT", color = KineticTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recentSearches) { recent ->
                    FilterChip(
                        selected = false,
                        onClick = { onSearchChange(recent) },
                        label = { Text(recent, fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = KineticDarkSurface,
                            labelColor = KineticTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = KineticDarkBorder,
                            enabled = true,
                            selected = false
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Group / Category Filter Chips Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(folders) { folder ->
                val isSelected = selectedFolder.equals(folder.name, ignoreCase = true)
                val folderCol = try {
                    Color(android.graphics.Color.parseColor(folder.colorHex))
                } catch (e: Exception) {
                    KineticSecondary
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onFolderSelect(folder.name) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (folder.name != "All Notes") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(folderCol)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(folder.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = KineticDarkSurface,
                        labelColor = KineticTextSecondary,
                        selectedContainerColor = folderCol.copy(alpha = 0.2f),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) folderCol else KineticDarkBorder,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        if (tags.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
            ) {
                Text("TAGS", color = KineticTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag?.equals(tag, ignoreCase = true) == true,
                        onClick = { onTagSelect(tag) },
                        label = { Text("#${tag}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = KineticDarkSurface,
                            labelColor = KineticTextSecondary,
                            selectedContainerColor = KineticSecondary.copy(alpha = 0.2f),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedTag?.equals(tag, ignoreCase = true) == true) KineticSecondary else KineticDarkBorder,
                            enabled = true,
selected = selectedTag?.equals(tag, ignoreCase = true) == true
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Staggered Note Grid
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            if (pinnedNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "PINNED (${pinnedNotes.size})",
                        color = KineticTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                    KeepNoteCard(
                        note = note,
                        isUnlocked = unlockedNoteIds.contains(note.id),
                        onNoteClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) }
                    )
                }
            }

            if (otherNotes.isNotEmpty()) {
                if (pinnedNotes.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = "OTHERS (${otherNotes.size})",
                            color = KineticTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                }

                items(otherNotes, key = { "other_${it.id}" }) { note ->
                    KeepNoteCard(
                        note = note,
                        isUnlocked = unlockedNoteIds.contains(note.id),
                        onNoteClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) }
                    )
                }
            }

            if (notes.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Text(
                            text = "No notes found",
                            color = KineticTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query or tag filter",
                            color = KineticTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeepNoteCard(
    note: NoteEntity,
    isUnlocked: Boolean,
    onNoteClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spineColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        KineticPrimary
    }

    Surface(
        color = KineticDarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onNoteClick() }
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tactile ColorNote Top Ribbon Spine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(spineColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header: Tag & Note Type Badge / Pin Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = spineColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${note.tag}",
                            color = spineColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Type indicator icons
                    when (note.type) {
                        NoteType.SKETCH -> Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = "Sketch",
                            tint = KineticSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        NoteType.AUDIO -> Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Audio",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(15.dp)
                        )
                        NoteType.CHECKLIST -> Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Checklist",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Toggle Pin",
                            tint = if (note.isPinned) spineColor else KineticTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = note.title,
                    color = KineticTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Locked Privacy Mask or Preview Content
                if (note.isLocked && !isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KineticDarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Note",
                            tint = KineticTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private Note (Tap to view)",
                            color = KineticTextMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    when (note.type) {
                        NoteType.SKETCH -> {
                            Surface(
                                color = KineticDarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✏️ Stylus Inking Vector Canvas",
                                        color = KineticTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        NoteType.AUDIO -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(KineticDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice Memo • ${note.audioDurationMs / 1000}s",
                                    color = KineticTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        NoteType.CHECKLIST -> {
                            Text(
                                text = "Tasks: Tap to review dynamic checklist",
                                color = KineticTextMuted,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                        NoteType.CODE -> {
                            Text(
                                text = note.codeSnippet ?: note.content.ifBlank { "// Code snippet" },
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        NoteType.DOC -> {
                            Text(
                                text = note.content.ifBlank { "No content" },
                                color = KineticTextMuted,
                                fontSize = 12.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
