/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017 Sven R. Kunze <srkunze@mail.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import java.io.File
import java.util.Collections

/**
 * Sort order
 */
open class FileSortOrder(@JvmField var name: String, var isAscending: Boolean) {

    val sortMultiplier: Int
        get() = if (isAscending) 1 else -1

    @Suppress("EnumNaming", "EnumEntryName") // already saved in user preferences -.-'
    enum class Type {
        trashBinView,

        @Suppress("ktlint:standard:enum-entry-name-case")
        localFileListView
    }

    enum class SortType {
        SIZE,
        ALPHABET,
        DATE
    }

    companion object {
        const val SORT_A_TO_Z_ID = "sort_a_to_z"
        const val SORT_Z_TO_A_ID = "sort_z_to_a"
        const val SORT_OLD_TO_NEW_ID = "sort_old_to_new"
        const val SORT_NEW_TO_OLD_ID = "sort_new_to_old"
        const val SORT_SMALL_TO_BIG_ID = "sort_small_to_big"
        const val SORT_BIG_TO_SMALL_ID = "sort_big_to_small"

        @JvmField
        val SORT_A_TO_Z: FileSortOrder = FileSortOrderByName(SORT_A_TO_Z_ID, true)

        @JvmField
        val SORT_Z_TO_A: FileSortOrder = FileSortOrderByName(SORT_Z_TO_A_ID, false)

        @JvmField
        val SORT_OLD_TO_NEW: FileSortOrder = FileSortOrderByDate(SORT_OLD_TO_NEW_ID, true)

        @JvmField
        val SORT_NEW_TO_OLD: FileSortOrder = FileSortOrderByDate(SORT_NEW_TO_OLD_ID, false)

        @JvmField
        val SORT_SMALL_TO_BIG: FileSortOrder = FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, true)

        @JvmField
        val SORT_BIG_TO_SMALL: FileSortOrder = FileSortOrderBySize(SORT_BIG_TO_SMALL_ID, false)

        @JvmField
        val sortOrders: Map<String, FileSortOrder> = Collections.unmodifiableMap(
            mapOf(
                SORT_A_TO_Z.name to SORT_A_TO_Z,
                SORT_Z_TO_A.name to SORT_Z_TO_A,
                SORT_OLD_TO_NEW.name to SORT_OLD_TO_NEW,
                SORT_NEW_TO_OLD.name to SORT_NEW_TO_OLD,
                SORT_SMALL_TO_BIG.name to SORT_SMALL_TO_BIG,
                SORT_BIG_TO_SMALL.name to SORT_BIG_TO_SMALL
            )
        )

        /**
         * Sorts list by Favourites.
         *
         * @param files files to sort
         */
        @JvmStatic
        fun sortCloudFilesByFavourite(files: MutableList<OCFile>): List<OCFile> {
            files.sortWith { o1: OCFile, o2: OCFile ->
                when {
                    o1.isFavorite && o2.isFavorite -> 0
                    o1.isFavorite -> -1
                    o2.isFavorite -> 1
                    else -> 0
                }
            }
            return files
        }
    }

    open fun sortCloudFiles(files: MutableList<OCFile>): List<OCFile> {
        return sortCloudFilesByFavourite(files)
    }

    open fun sortLocalFiles(files: MutableList<File>): List<File> {
        return files
    }

    open fun sortTrashbinFiles(files: MutableList<TrashbinFile>): List<TrashbinFile> {
        return files
    }

    open fun getType(): SortType {
        return when (name) {
            SORT_Z_TO_A_ID,
            SORT_A_TO_Z_ID -> SortType.ALPHABET

            SORT_SMALL_TO_BIG_ID,
            SORT_BIG_TO_SMALL_ID -> SortType.SIZE

            SORT_NEW_TO_OLD_ID,
            SORT_OLD_TO_NEW_ID -> SortType.DATE

            else -> SortType.ALPHABET
        }
    }
}
