/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.database.migrations.model.SQLiteColumnType

fun SupportSQLiteDatabase.addColumnIfNotExists(tableName: String, columnName: String, columnType: SQLiteColumnType) {
    val cursor = query("PRAGMA table_info($tableName)")
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
        execSQL("ALTER TABLE $tableName ADD COLUMN `$columnName` ${columnType.value}")
    }
}
