/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
}
