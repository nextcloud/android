/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

val MIGRATION_88_89 = object : Migration(88, 89) {
    override fun migrate(database: SupportSQLiteDatabase) {
        addUploadedColumnIfNotExists(database)
        addMissingNotesFolderPathToCapabilitiesEntity(database)
    }

    fun addUploadedColumnIfNotExists(database: SupportSQLiteDatabase) {
        val tableName = ProviderTableMeta.FILE_TABLE_NAME
        val cursor = database.query("PRAGMA table_info($tableName)")
        var columnExists = false

        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                val existingColumnName = cursor.getString(nameIndex)
                if (existingColumnName == ProviderTableMeta.FILE_UPLOADED) {
                    columnExists = true
                    break
                }
            }
        }
        cursor.close()

        if (!columnExists) {
            database
                .execSQL("ALTER TABLE $tableName ADD COLUMN `${ProviderTableMeta.FILE_UPLOADED}` INTEGER DEFAULT NULL")
        }
    }

    fun addMissingNotesFolderPathToCapabilitiesEntity(database: SupportSQLiteDatabase) {
        val tableName = ProviderTableMeta.CAPABILITIES_TABLE_NAME
        val columnName = ProviderTableMeta.CAPABILITIES_NOTES_FOLDER_PATH

        val cursor = database.query("PRAGMA table_info($tableName)")
        var columnExists = false

        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                val existingColumnName = cursor.getString(nameIndex)
                if (existingColumnName == columnName) {
                    columnExists = true
                    break
                }
            }
        }
        cursor.close()

        if (!columnExists) {
            database.execSQL("ALTER TABLE $tableName ADD COLUMN `$columnName` TEXT DEFAULT NULL")
        }
    }

}
