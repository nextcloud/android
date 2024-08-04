/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrationUtil {

    const val TYPE_TEXT = "TEXT"
    const val TYPE_INTEGER = "INTEGER"
    const val TYPE_INTEGER_PRIMARY_KEY = "INTEGER PRIMARY KEY"
    const val KEYWORD_NOT_NULL = "NOT NULL"

    /**
     * Utility method to add or remove columns from a table
     *
     * See individual functions for more details
     *
     * @param newColumns Map of column names and types on the NEW table
     * @param selectTransform a function that transforms the select statement. This can be used to change the values
     * when copying, such as for removing nulls
     */
    fun migrateTable(
        database: SupportSQLiteDatabase,
        tableName: String,
        newColumns: Map<String, String>,
        selectTransform: ((String) -> String)? = null
    ) {
        require(newColumns.isNotEmpty())
        val newTableTempName = "${tableName}_new"
        createNewTable(database, newTableTempName, newColumns)
        copyData(database, tableName, newTableTempName, newColumns.keys, selectTransform)
        replaceTable(database, tableName, newTableTempName)
    }

    fun resetCapabilities(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1")
    }

    /**
     * Utility method to create a new table with the given columns
     */
    private fun createNewTable(database: SupportSQLiteDatabase, newTableName: String, columns: Map<String, String>) {
        val columnsString = columns.entries.joinToString(",") { "${it.key} ${it.value}" }
        database.execSQL("CREATE TABLE $newTableName ($columnsString)")
    }

    /**
     * Utility method to copy data from an old table to a new table. Only the columns in [columnNames] will be copied
     *
     * @param selectTransform a function that transforms the select statement. This can be used to change the values
     * when copying, such as for removing nulls
     */
    private fun copyData(
        database: SupportSQLiteDatabase,
        tableName: String,
        newTableName: String,
        columnNames: Iterable<String>,
        selectTransform: ((String) -> String)? = null
    ) {
        val selectColumnsString = columnNames.joinToString(",", transform = selectTransform)
        val destColumnsString = columnNames.joinToString(",")

        database.execSQL(
            "INSERT INTO $newTableName ($destColumnsString) " +
                "SELECT $selectColumnsString FROM $tableName"
        )
    }

    /**
     * Utility method to replace an old table with a new one, essentially deleting the old one and renaming the new one
     */
    private fun replaceTable(database: SupportSQLiteDatabase, tableName: String, newTableTempName: String) {
        database.execSQL("DROP TABLE $tableName")
        database.execSQL("ALTER TABLE $newTableTempName RENAME TO $tableName")
    }

    /**
     * Room AutoMigrationSpec to reset capabilities post migration.
     */
    class ResetCapabilitiesPostMigration : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            resetCapabilities(db)
            super.onPostMigrate(db)
        }
    }
}
