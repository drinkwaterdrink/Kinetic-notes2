package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        NoteLinkEntity::class,
        ChecklistItemEntity::class,
        NoteGroupEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        /**
         * v2 -> v3: Groups become durable rows instead of ViewModel state.
         *
         * - creates `note_groups`
         * - adds `notes.groupId` (+ index)
         * - backfills one group per distinct legacy `notes.folder` value
         * - points every note at its backfilled group
         *
         * No data is dropped: `notes.folder` is left in place as the readable legacy name.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_groups` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`colorHex` TEXT NOT NULL, " +
                        "`icon` TEXT NOT NULL, " +
                        "`orderIndex` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_note_groups_name` " +
                        "ON `note_groups` (`name`)"
                )
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `groupId` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_groupId` ON `notes` (`groupId`)")

                // One durable group per legacy folder name that actually holds notes.
                db.execSQL(
                    "INSERT OR IGNORE INTO `note_groups` " +
                        "(`id`, `name`, `colorHex`, `icon`, `orderIndex`, `createdAt`) " +
                        "SELECT lower(hex(randomblob(16))), folder, '#6366F1', '📁', 0, " +
                        "CAST(strftime('%s','now') AS INTEGER) * 1000 FROM (" +
                        "SELECT DISTINCT folder FROM `notes` " +
                        "WHERE folder IS NOT NULL AND trim(folder) <> '' AND folder <> 'All Notes')"
                )
                db.execSQL(
                    "UPDATE `notes` SET `groupId` = " +
                        "(SELECT g.id FROM `note_groups` g WHERE g.name = notes.folder)"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "kinetic_notes.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    // Explicit migration only. A notes app must never silently wipe the
                    // user's database, so there is deliberately no destructive fallback.
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database.noteDao())
                    }
                }
            }

            /**
             * Only runs for a brand new database file. Deleting every note no longer brings
             * the demo content back (the ViewModel used to re-seed whenever the table was empty).
             */
            suspend fun populateInitialDatabase(noteDao: NoteDao) {
                noteDao.upsertGroups(SampleData.getInitialGroups())
                noteDao.insertNotes(SampleData.getInitialNotes())
                noteDao.insertLinks(SampleData.getInitialLinks())
                noteDao.insertChecklistItems(SampleData.getInitialChecklist())
            }
        }
    }
}
