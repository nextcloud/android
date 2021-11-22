/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
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
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

object ContentResolverHelper {
    const val SORT_DIRECTION_ASCENDING = "ASC"
    const val SORT_DIRECTION_DESCENDING = "DESC"

    @JvmStatic
    @JvmOverloads
    @Suppress("LongParameterList")
    /**
     * Queries the content resolver with the given params using the correct API level-dependant syntax.
     * This is needed in order to use LIMIT or OFFSET from android 11.
     */
    fun queryResolver(
        contentResolver: ContentResolver,
        uri: Uri,
        projection: Array<String>,
        selection: String? = null,
        cancellationSignal: CancellationSignal? = null,
        sortColumn: String? = null,
        sortDirection: String? = null,
        limit: Int? = null
    ): Cursor? {
        require(!(sortColumn != null && sortDirection == null)) {
            "Sort direction is mandatory if sort column is provided"
        }
        require(
            listOf(null, SORT_DIRECTION_ASCENDING, SORT_DIRECTION_DESCENDING).contains(sortDirection)
        ) {
            "Invalid sort direction"
        }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val queryArgs = getQueryArgsBundle(selection, sortColumn, sortDirection, limit)
                contentResolver.query(uri, projection, queryArgs, cancellationSignal)
            }
            else -> {
                val sortOrder = getSortOrderString(sortColumn, sortDirection, limit)
                contentResolver.query(
                    uri,
                    projection,
                    selection,
                    null,
                    sortOrder,
                    cancellationSignal
                )
            }
        }
    }

    private fun getSortOrderString(sortColumn: String?, sortDirection: String?, limit: Int?): String {
        val sortOrderBuilder = StringBuilder()
        if (sortColumn != null) {
            sortOrderBuilder.append("$sortColumn $sortDirection")
        }
        if (limit != null) {
            if (sortOrderBuilder.isNotEmpty()) {
                sortOrderBuilder.append(" ")
            }
            sortOrderBuilder.append("LIMIT $limit")
        }
        return sortOrderBuilder.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun
    getQueryArgsBundle(selection: String?, sortColumn: String?, sortDirection: String?, limit: Int?): Bundle {
        return Bundle().apply {
            if (selection != null) {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            }
            if (sortColumn != null) {
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortColumn))
                val direction = when (sortDirection) {
                    SORT_DIRECTION_ASCENDING -> ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                    else -> ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                }
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, direction)
            }
            if (limit != null) {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            }
        }
    }
}
