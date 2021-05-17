/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.CancellationSignal
import com.owncloud.android.R
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import java.util.Locale

class ExternalFileContentProvider : ContentProvider() {
    private lateinit var uriMatcher: UriMatcher
    private lateinit var dataBaseHelper: SQLiteOpenHelper

    override fun onCreate(): Boolean {
        context?.let {
            dataBaseHelper = DataBaseHelper(it)
            val authority = it.resources.getString(R.string.authorityExternal)

            uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
                addURI(authority, null, ROOT_DIRECTORY)
                addURI(authority, "file/", SINGLE_FILE)
                addURI(authority, "file/#", SINGLE_FILE)
                addURI(authority, "dir/", DIRECTORY)
                addURI(authority, "dir/#", DIRECTORY)
            }

            return true
        }

        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val result: Cursor?
        val db: SQLiteDatabase = dataBaseHelper.readableDatabase
        db.beginTransaction()
        try {
            result = query(db, uri, projection, selection, selectionArgs, sortOrder)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return result
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        val sqlQuery = SQLiteQueryBuilder()

        sqlQuery.tables = ProviderTableMeta.FILE_TABLE_NAME

        return super.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal)
    }

    @Throws(IllegalArgumentException::class)
    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            ROOT_DIRECTORY -> ProviderTableMeta.CONTENT_TYPE
            SINGLE_FILE -> ProviderTableMeta.CONTENT_TYPE_ITEM
            else -> throw IllegalArgumentException(String.format(Locale.US, "Unknown Uri id: %s", uri))
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    @Throws(IllegalArgumentException::class)
    fun query(
        db: SQLiteDatabase,
        uri: Uri,
        projectionArray: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {

        val sqlQuery = SQLiteQueryBuilder()

        sqlQuery.tables = ProviderTableMeta.FILE_TABLE_NAME

        when (uriMatcher.match(uri)) {
            ROOT_DIRECTORY -> {
                // do nothing
            }
            DIRECTORY -> if (uri.pathSegments.size > SINGLE_PATH_SEGMENT) {
                sqlQuery.appendWhere(ProviderTableMeta.FILE_PARENT + "=" + uri.pathSegments[1])
            }
            SINGLE_FILE -> if (uri.pathSegments.size > SINGLE_PATH_SEGMENT) {
                sqlQuery.appendWhere(ProviderTableMeta._ID + "=" + uri.pathSegments[1])
            }
            else -> throw IllegalArgumentException("Unknown uri id: $uri")
        }

        // val callingPackage = context?.packageManager?.getNameForUid(Binder.getCallingUid())
        // val sameApp = callingPackage != null && callingPackage == context?.getPackageName()
        //
        // val projectionMap: MutableMap<String, String> = mutableMapOf()
        //
        // if (!sameApp) {
        //     for (projection in ProviderTableMeta.FILE_RESTRICTED_COLUMNS) {
        //         projectionMap[projection] = projection
        //     }
        //     sqlQuery.projectionMap = projectionMap
        // }

        // DB case_sensitive
        db.execSQL("PRAGMA case_sensitive_like = true")

        sqlQuery.isStrict = true
        val c = sqlQuery.query(
            db,
            projectionArray,
            selection,
            selectionArgs,
            null,
            null,
            ProviderTableMeta.FILE_DEFAULT_SORT_ORDER
        )

        c.setNotificationUri(context?.contentResolver, uri)
        return c
    }

    companion object {
        const val SINGLE_FILE = 1
        const val DIRECTORY = 2
        const val ROOT_DIRECTORY = 3

        const val SINGLE_PATH_SEGMENT = 1
    }

    inner class DataBaseHelper(context: Context) : SQLiteOpenHelper(
        context,
        ProviderMeta.DB_NAME,
        null,
        ProviderMeta.DB_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase?) {
            // is done via FileContentProvider
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // is done via FileContentProvider
        }
    }
}
