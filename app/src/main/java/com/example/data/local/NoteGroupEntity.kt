package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A durable, first-class Group (folder) definition.
 *
 * Groups used to live only in ViewModel state keyed by display name, which meant they
 * disappeared on process death and renaming had to rewrite every note. They are now real
 * rows with stable ids: [NoteEntity.groupId] references this id, so renaming a group is a
 * single-row update and deleting a group never has to touch the notes it contained.
 */
@Entity(
    tableName = "note_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class NoteGroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#6366F1",
    val icon: String = "📁",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
