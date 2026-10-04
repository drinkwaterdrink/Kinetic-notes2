package com.example

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.NoteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrade regression for real user data: a v2 database must become v3 without losing notes,
 * and legacy folder names must be converted into stable group ids.
 */
@RunWith(AndroidJUnit4::class)
class NoteDatabaseMigrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NoteDatabase::class.java
    )

    @Test
    fun migrate2To3BackfillsGroupsWithoutDroppingNotes() {
        helper.createDatabase("migration-test", 2).apply {
            createV2Schema(this)
            execSQL(
                "INSERT INTO notes " +
                    "(id,title,content,type,colorHex,tag,folder,x,y,isPinned,isLocked,dueDateText,codeSnippet,zIndex,strokeData,audioPath,audioDurationMs,createdAt,updatedAt) " +
                    "VALUES ('legacy-note','Legacy','body','DOC','#6366F1','Ideas','Ideas',12.0,24.0,0,0,NULL,NULL,10,NULL,NULL,0,100,200)"
            )
            execSQL(
                "INSERT INTO notes " +
                    "(id,title,content,type,colorHex,tag,folder,x,y,isPinned,isLocked,dueDateText,codeSnippet,zIndex,strokeData,audioPath,audioDurationMs,createdAt,updatedAt) " +
                    "VALUES ('legacy-target','Target','target body','DOC','#22C55E','Ideas','Ideas',80.0,96.0,1,0,NULL,NULL,11,NULL,NULL,0,101,201)"
            )
            execSQL(
                "INSERT INTO note_links " +
                    "(id,sourceId,targetId,colorHex,label) " +
                    "VALUES ('legacy-link','legacy-note','legacy-target','#6366F1','related')"
            )
            execSQL(
                "INSERT INTO checklist_items " +
                    "(id,noteId,text,isChecked,orderIndex) " +
                    "VALUES ('legacy-check','legacy-note','Keep this row',0,0)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            "migration-test",
            3,
            true,
            NoteDatabase.MIGRATION_2_3
        )
        migrated.query("SELECT groupId, folder FROM notes WHERE id = 'legacy-note'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            val groupId = cursor.getString(cursor.getColumnIndexOrThrow("groupId"))
            assertNotNull(groupId)
            assertEquals("Ideas", cursor.getString(cursor.getColumnIndexOrThrow("folder")))
        }
        migrated.query("SELECT name FROM note_groups WHERE id = (SELECT groupId FROM notes WHERE id = 'legacy-note')").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Ideas", cursor.getString(0))
        }
        migrated.query("SELECT title, content, x, y, isPinned FROM notes WHERE id = 'legacy-target'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Target", cursor.getString(0))
            assertEquals("target body", cursor.getString(1))
            assertEquals(80.0, cursor.getDouble(2), 0.001)
            assertEquals(96.0, cursor.getDouble(3), 0.001)
            assertEquals(1, cursor.getInt(4))
        }
        migrated.query("SELECT COUNT(*) FROM note_links WHERE id = 'legacy-link'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT text FROM checklist_items WHERE id = 'legacy-check'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Keep this row", cursor.getString(0))
        }
        migrated.close()
    }

    private fun createV2Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS notes (" +
                "id TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, type TEXT NOT NULL, " +
                "colorHex TEXT NOT NULL, tag TEXT NOT NULL, folder TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, " +
                "isPinned INTEGER NOT NULL, isLocked INTEGER NOT NULL, dueDateText TEXT, codeSnippet TEXT, " +
                "zIndex INTEGER NOT NULL, strokeData TEXT, audioPath TEXT, audioDurationMs INTEGER NOT NULL, " +
                "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS note_links (" +
                "id TEXT NOT NULL, sourceId TEXT NOT NULL, targetId TEXT NOT NULL, colorHex TEXT NOT NULL, label TEXT, " +
                "PRIMARY KEY(id), FOREIGN KEY(sourceId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(targetId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_note_links_sourceId ON note_links(sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_note_links_targetId ON note_links(targetId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS checklist_items (" +
                "id TEXT NOT NULL, noteId TEXT NOT NULL, text TEXT NOT NULL, isChecked INTEGER NOT NULL, orderIndex INTEGER NOT NULL, " +
                "PRIMARY KEY(id), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_items_noteId ON checklist_items(noteId)")
    }
}
