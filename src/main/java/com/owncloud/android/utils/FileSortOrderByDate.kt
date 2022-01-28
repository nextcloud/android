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
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import java.io.File

/**
 * Created by srkunze on 28.08.17.
 */
class FileSortOrderByDate(name: String, ascending: Boolean) : FileSortOrder(name, ascending) {
    /**
     * Sorts list by Date.
     *
     * @param files list of files to sort
     */
    override fun sortCloudFiles(files: MutableList<OCFile>): List<OCFile> {
        val multiplier = if (isAscending) 1 else -1
        files.sortWith { o1: OCFile, o2: OCFile ->
            multiplier * o1.modificationTimestamp.compareTo(o2.modificationTimestamp)
        }
        return super.sortCloudFiles(files)
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
