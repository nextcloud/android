/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: Sven R. Kunze
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
 */
class FileSortOrderByName internal constructor(name: String?, ascending: Boolean) : FileSortOrder(name!!, ascending) {
    /**
     *
     * @param files files to sort
     */
    @SuppressFBWarnings("Bx")
    override fun sortCloudFiles(
        files: MutableList<OCFile>,
        foldersBeforeFiles: Boolean,
        favoritesFirst: Boolean
    ): MutableList<OCFile> {
        val sortedByName = sortOnlyByName(files)
        return super.sortCloudFiles(sortedByName, foldersBeforeFiles, favoritesFirst)
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

    private fun sortOnlyByName(files: MutableList<OCFile>): MutableList<OCFile> {
        files.sortWith { o1: OCFile, o2: OCFile -> sortMultiplier * AlphanumComparator.compare(o1, o2) }
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
