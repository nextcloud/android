/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017 Sven R. Kunze <srkunze@mail.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import java.io.File

/**
 * Sorts files by sizes
 */
class FileSortOrderBySize internal constructor(name: String?, ascending: Boolean) : FileSortOrder(name!!, ascending) {

    override fun sortCloudFiles(
        files: MutableList<OCFile>,
        foldersBeforeFiles: Boolean,
        favoritesFirst: Boolean
    ): MutableList<OCFile> {
        val sortedBySize = sortServerFiles(files)
        return super.sortCloudFiles(sortedBySize, foldersBeforeFiles, favoritesFirst)
    }

    override fun sortTrashbinFiles(files: MutableList<TrashbinFile>): List<TrashbinFile> {
        val sortedBySize = sortServerFiles(files)
        return super.sortTrashbinFiles(sortedBySize)
    }

    private fun <T : ServerFileInterface> sortServerFiles(files: MutableList<T>): MutableList<T> {
        files.sortWith { o1: ServerFileInterface, o2: ServerFileInterface ->
            when {
                o1.isFolder && o2.isFolder -> sortMultiplier * o1.fileLength.compareTo(o2.fileLength)
                o1.isFolder -> -1
                o2.isFolder -> 1
                else -> sortMultiplier * o1.fileLength.compareTo(o2.fileLength)
            }
        }
        return files
    }

    override fun sortLocalFiles(files: MutableList<File>): List<File> {
        val folderSizes =
            files.associateWith { file -> FileStorageUtils.getFolderSize(file) }

        files.sortWith { o1: File, o2: File ->
            when {
                o1.isDirectory && o2.isDirectory -> sortMultiplier * (folderSizes[o1] ?: 0L).compareTo(
                    folderSizes[o2] ?: 0L
                )
                o1.isDirectory -> -1
                o2.isDirectory -> 1
                else -> sortMultiplier * o1.length().compareTo(o2.length())
            }
        }
        return files
    }
}
