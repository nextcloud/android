/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PrefillLocalFilesMapTest {

    private fun localFile(id: Long, path: String, onDevice: Boolean = false): OCFile = mock {
        on { fileId } doReturn id
        on { remotePath } doReturn path
        on { existsOnDevice() } doReturn onDevice
    }

    @Test
    fun withoutDuplicates_mapsEveryFile() {
        val photos = localFile(1, "/Photos/")
        val notes = localFile(2, "/Notes/")
        val duplicates = mutableListOf<OCFile>()

        val map = RefreshFolderOperation.prefillLocalFilesMap(null, listOf(photos, notes), duplicates)

        assertEquals(mapOf("/Photos/" to photos, "/Notes/" to notes), map)
        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun withDuplicates_keepsLowestIdAndReportsTheOthers() {
        val newer = localFile(7, "/Photos/")
        val oldest = localFile(3, "/Photos/")
        val middle = localFile(5, "/Photos/")
        val duplicates = mutableListOf<OCFile>()

        val map = RefreshFolderOperation.prefillLocalFilesMap(null, listOf(newer, oldest, middle), duplicates)

        assertSame(oldest, map["/Photos/"])
        assertEquals(setOf(newer, middle), duplicates.toSet())
        assertEquals(2, duplicates.size)
    }

    @Test
    fun withDuplicates_keepsEntryWithLocalCopy() {
        val oldest = localFile(3, "/Documents/report.pdf")
        val downloaded = localFile(9, "/Documents/report.pdf", onDevice = true)
        val duplicates = mutableListOf<OCFile>()

        val map = RefreshFolderOperation.prefillLocalFilesMap(null, listOf(oldest, downloaded), duplicates)

        assertSame(downloaded, map["/Documents/report.pdf"])
        assertEquals(listOf(oldest), duplicates)
    }

    @Test
    fun encryptedFolder_sameKeyButDifferentRemotePath_isNoDuplicate() {
        val first: OCFile = mock {
            on { remotePath } doReturn "/secret/aaa"
            on { parentRemotePath } doReturn "/secret/"
        }
        val second: OCFile = mock {
            on { remotePath } doReturn "/secret/bbb"
            on { parentRemotePath } doReturn "/secret/"
        }
        val duplicates = mutableListOf<OCFile>()

        RefreshFolderOperation.prefillLocalFilesMap(Any(), listOf(first, second), duplicates)

        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun withoutDuplicatesList_mapsTheSameEntry() {
        val newer = localFile(7, "/zotero/")
        val oldest = localFile(3, "/zotero/")

        val map = RefreshFolderOperation.prefillLocalFilesMap(null, listOf(newer, oldest))

        assertSame(oldest, map["/zotero/"])
    }
}
