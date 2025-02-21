/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import org.junit.Assert
import org.junit.Test

class FileDataStorageManagerContentResolverIT : FileDataStorageManagerIT() {
    companion object {
        private const val MANY_FILES_AMOUNT = 5000
    }

    override fun before() {
        sut = FileDataStorageManager(user, targetContext.contentResolver)
        super.before()
    }

    /**
     * only on FileDataStorageManager
     */
    @Test
    fun testFolderWithManyFiles() {
        // create folder
        val folderA = OCFile("/folderA/")
        folderA.setFolder().parentId = sut.getFileByDecryptedRemotePath("/")!!.fileId
        sut.saveFile(folderA)
        Assert.assertTrue(sut.fileExists("/folderA/"))
        Assert.assertEquals(0, sut.getFolderContent(folderA, false).size)
        val folderAId = sut.getFileByDecryptedRemotePath("/folderA/")!!.fileId

        // create files
        val newFiles = (1..MANY_FILES_AMOUNT).map {
            val file = OCFile("/folderA/file$it")
            file.parentId = folderAId
            sut.saveFile(file)

            val storedFile = sut.getFileByDecryptedRemotePath("/folderA/file$it")
            Assert.assertNotNull(storedFile)
            storedFile
        }

        // save files in folder
        sut.saveFolder(
            folderA,
            newFiles,
            ArrayList()
        )
        // check file count is correct
        Assert.assertEquals(MANY_FILES_AMOUNT, sut.getFolderContent(folderA, false).size)
    }

    @Test
    fun runQuery() {
        testQuery()
    }
}
