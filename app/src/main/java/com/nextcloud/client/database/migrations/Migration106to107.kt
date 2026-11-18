/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 PeterPablo <4340267+PeterPablo@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Makes "one row per file" an invariant of the database instead of a hope.
 *
 * `filelist` had no unique index at all: every write path used `INSERT … CONFLICT_*`, which had
 * nothing to conflict with, and the read that serves the file list,
 * `SELECT … FROM filelist WHERE parent = ?`, has no owner filter and no `DISTINCT`. So a second row
 * for the same path was silently accepted, and both rows were then handed to whoever asked — a SAF
 * consumer shows that as the same folder twice (nextcloud/android#17051). `upsertSingleFile`
 * narrows the window by looking the row up first, but a lookup cannot make the invariant hold while
 * the index is missing.
 *
 * Two steps, in this order: collapse the twins that exist, then forbid new ones.
 */
@Suppress("MagicNumber")
val MIGRATION_106_107 = object : Migration(106, 107) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Keep the oldest row per (path, file_owner). Rows without a path are left alone: they have no
        // identity to compare, and SQLite counts NULLs as distinct in a unique index anyway.
        db.execSQL(
            """
            DELETE FROM filelist
            WHERE path IS NOT NULL
              AND _id NOT IN (SELECT MIN(_id) FROM filelist WHERE path IS NOT NULL GROUP BY path, file_owner)
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_filelist_path_owner ON filelist (path, file_owner)")
    }
}
