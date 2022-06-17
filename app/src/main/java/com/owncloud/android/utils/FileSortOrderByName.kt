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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import third_parties.daveKoeller.AlphanumComparator
import java.io.File
import java.util.Locale

/**
 * Sorts list by Name.
 *
 * Created by srkunze on 28.08.17.
 */
class FileSortOrderByName internal constructor(name: String?, ascending: Boolean) : FileSortOrder(name!!, ascending) {
    /**
     *
     * @param files files to sort
     */
    @SuppressFBWarnings("Bx")
    override fun sortCloudFiles(files: MutableList<OCFile>): List<OCFile> {
        val sortedByName = sortServerFiles(files)
        return super.sortCloudFiles(sortedByName)
    }

    /**
     * Sorts list by Name.
     *
     * @param files files to sort
     */
    override fun sortTrashbinFiles(files: MutableList<TrashbinFile>): List<TrashbinFile> {
        val sortedByName = sortServerFiles(files)
        return super.sortTrashbinFiles(sortedByName)
    }

    private fun <T : ServerFileInterface> sortServerFiles(files: MutableList<T>): MutableList<T> {
        files.sortWith { o1: ServerFileInterface, o2: ServerFileInterface ->
            when {
                o1.isFolder && o2.isFolder -> sortMultiplier * AlphanumComparator.compare(o1, o2)
                o1.isFolder -> -1
                o2.isFolder -> 1
                else -> sortMultiplier * AlphanumComparator.compare(o1, o2)
            }
        }
        return files
    }

    /**
     * Sorts list by Name.
     *
     * @param files files to sort
     */
    override fun sortLocalFiles(files: MutableList<File>): List<File> {
        files.sortWith { o1: File, o2: File ->
            when {
                o1.isDirectory && o2.isDirectory -> sortMultiplier * o1.path.lowercase(Locale.getDefault())
                    .compareTo(o2.path.lowercase(Locale.getDefault()))
                o1.isDirectory -> -1
                o2.isDirectory -> 1
                else -> sortMultiplier * AlphanumComparator.compare(
                    o1.path.lowercase(Locale.getDefault()),
                    o2.path.lowercase(Locale.getDefault())
                )
            }
        }
        return files
    }
}
