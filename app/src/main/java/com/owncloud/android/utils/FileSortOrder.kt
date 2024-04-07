/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017 Sven R. Kunze <srkunze@mail.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
        SIZE, ALPHABET, DATE
    }

    companion object {
        const val sort_a_to_z_id = "sort_a_to_z"
        const val sort_z_to_a_id = "sort_z_to_a"
        const val sort_old_to_new_id = "sort_old_to_new"
        const val sort_new_to_old_id = "sort_new_to_old"
        const val sort_small_to_big_id = "sort_small_to_big"
        const val sort_big_to_small_id = "sort_big_to_small"

        @JvmField
        val sort_a_to_z: FileSortOrder = FileSortOrderByName(sort_a_to_z_id, true)

        @JvmField
        val sort_z_to_a: FileSortOrder = FileSortOrderByName(sort_z_to_a_id, false)

        @JvmField
        val sort_old_to_new: FileSortOrder = FileSortOrderByDate(sort_old_to_new_id, true)

        @JvmField
        val sort_new_to_old: FileSortOrder = FileSortOrderByDate(sort_new_to_old_id, false)

        @JvmField
        val sort_small_to_big: FileSortOrder = FileSortOrderBySize(sort_small_to_big_id, true)

        @JvmField
        val sort_big_to_small: FileSortOrder = FileSortOrderBySize(sort_big_to_small_id, false)

        @JvmField
        val sortOrders: Map<String, FileSortOrder> = Collections.unmodifiableMap(
            mapOf(
                sort_a_to_z.name to sort_a_to_z,
                sort_z_to_a.name to sort_z_to_a,
                sort_old_to_new.name to sort_old_to_new,
                sort_new_to_old.name to sort_new_to_old,
                sort_small_to_big.name to sort_small_to_big,
                sort_big_to_small.name to sort_big_to_small
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
            sort_z_to_a_id,
            sort_a_to_z_id -> SortType.ALPHABET

            sort_small_to_big_id,
            sort_big_to_small_id -> SortType.SIZE

            sort_new_to_old_id,
            sort_old_to_new_id -> SortType.DATE

            else -> SortType.ALPHABET
        }
    }
}
