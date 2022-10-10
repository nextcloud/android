/*
 * Nextcloud Android client application
 *
 * @author Sven R. Kunze
 * Copyright (C) 2017 Sven R. Kunze
 * Copyright (C) 2022 Álvaro Brey Vilas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    override fun sortCloudFiles(files: MutableList<OCFile>): List<OCFile> {
        val sortedBySize = sortServerFiles(files)
        return super.sortCloudFiles(sortedBySize)
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
        files.sortWith { o1: File, o2: File ->
            when {
                o1.isDirectory && o2.isDirectory -> sortMultiplier * FileStorageUtils.getFolderSize(o1)
                    .compareTo(FileStorageUtils.getFolderSize(o2))
                o1.isDirectory -> -1
                o2.isDirectory -> 1
                else -> sortMultiplier * o1.length().compareTo(o2.length())
            }
        }
        return files
    }
}
