package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val text: String,
    val isChecked: Boolean = false,
    val orderIndex: Int = 0
)
