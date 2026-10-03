package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("targetId")]
)
data class NoteLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val colorHex: String = "#6366F1",
    val label: String? = null
)
