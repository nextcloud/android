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

/**
 * Sorts list by Date.
 */
class FileSortOrderByDate(name: String, ascending: Boolean) : FileSortOrder(name, ascending) {
    /**
     * Sorts list by Date.
     *
     * @param files list of files to sort
     */
    override fun sortCloudFiles(
        files: MutableList<OCFile>,
        foldersBeforeFiles: Boolean,
        favoritesFirst: Boolean
    ): MutableList<OCFile> {
        val multiplier = if (isAscending) 1 else -1
        files.sortWith { o1: OCFile, o2: OCFile ->
            multiplier * o1.modificationTimestamp.compareTo(o2.modificationTimestamp)
        }
        return super.sortCloudFiles(files, foldersBeforeFiles, favoritesFirst)
    }

    /**
     * Sorts list by Date.
     *
     * @param files list of files to sort
     */
    override fun sortTrashbinFiles(files: MutableList<TrashbinFile>): List<TrashbinFile> {
        val multiplier = if (isAscending) 1 else -1
        files.sortWith { o1: TrashbinFile, o2: TrashbinFile ->
            multiplier * o1.deletionTimestamp.compareTo(o2.deletionTimestamp)
        }
        return super.sortTrashbinFiles(files)
    }

    /**
     * Sorts list by Date.
     *
     * @param files list of files to sort
     */
    override fun sortLocalFiles(files: MutableList<File>): List<File> {
        val multiplier = if (isAscending) 1 else -1
        files.sortWith { o1: File, o2: File ->
            multiplier * o1.lastModified().compareTo(o2.lastModified())
        }
        return files
    }
}
