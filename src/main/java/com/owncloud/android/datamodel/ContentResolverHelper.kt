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
                putString(ContentResolver.QUERY_ARG_SORT_COLUMNS, sortColumn)
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
