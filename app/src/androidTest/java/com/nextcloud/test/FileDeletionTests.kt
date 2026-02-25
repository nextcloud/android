/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.test

import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FileDeletionTests : AbstractIT() {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        val parent = System.getProperty("java.io.tmpdir")
        val childPath = "file_deletion_test_${System.currentTimeMillis()}"
        tempDir = File(parent, childPath)
        tempDir.mkdirs()
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    private fun getRandomRemoteId(): String = Random
        .nextLong(10_000_000L, 99_999_999L)
        .toString()
        .padEnd(32, '0')

    private fun createAndSaveSingleFileWithLocalCopy(): OCFile {
        val now = System.currentTimeMillis()

        val file = OCFile("/TestFile.txt").apply {
            fileId = Random.nextLong(1, 10_000)
            parentId = 0
            remoteId = getRandomRemoteId()
            fileLength = 1024
            mimeType = MimeType.TEXT_PLAIN
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNV"
        }

        val localFile = File(tempDir, "TestFile_${file.fileId}.txt").apply {
            parentFile?.mkdirs()
            createNewFile()
            writeText("Temporary test content")
        }
        file.storagePath = localFile.absolutePath

        storageManager.saveFile(file)

        return file
    }

    private fun createAndSaveFolderTree(): OCFile {
        val now = System.currentTimeMillis()
        val rootFolder = OCFile("/TestFolder").apply {
            fileId = Random.nextLong(1, 10_000)
            parentId = 0
            remoteId = getRandomRemoteId()
            mimeType = MimeType.DIRECTORY
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNVCK"
        }

        val subFolder = OCFile("/TestFolder/Sub").apply {
            fileId = rootFolder.fileId + 1
            parentId = rootFolder.fileId
            remoteId = getRandomRemoteId()
            mimeType = MimeType.DIRECTORY
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNVCK"
        }

        val file1 = OCFile("/TestFolder/file1.txt").apply {
            fileId = rootFolder.fileId + 2
            parentId = rootFolder.fileId
            remoteId = getRandomRemoteId()
            fileLength = 512
            mimeType = MimeType.TEXT_PLAIN
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNV"
        }

        val file2 = OCFile("/TestFolder/Sub/file2.txt").apply {
            fileId = rootFolder.fileId + 3
            parentId = subFolder.fileId
            remoteId = getRandomRemoteId()
            fileLength = 256
            mimeType = MimeType.TEXT_PLAIN
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNV"
        }

        listOf(rootFolder, subFolder, file1, file2).forEach { storageManager.saveFile(it) }

        val file1Path = File(tempDir, "file1_${file1.fileId}.txt").apply { createNewFile() }
        val file2Path = File(tempDir, "file2_${file2.fileId}.txt").apply { createNewFile() }

        file1.storagePath = file1Path.absolutePath
        file2.storagePath = file2Path.absolutePath

        storageManager.saveFile(file1)
        storageManager.saveFile(file2)

        return rootFolder
    }

    private fun getMixedOcFiles(): List<OCFile> {
        val now = System.currentTimeMillis()

        fun createFolder(id: Long, parentId: Long, path: String): OCFile = OCFile(path).apply {
            fileId = id
            this.parentId = parentId
            remoteId = getRandomRemoteId()
            mimeType = MimeType.DIRECTORY
            creationTimestamp = now
            modificationTimestamp = now
            permissions = "RWDNVCK"
        }

        fun createFile(id: Long, parentId: Long, path: String, size: Long, mime: String): OCFile = OCFile(path).apply {
            fileId = id
            this.parentId = parentId
            remoteId = getRandomRemoteId()
            fileLength = size
            creationTimestamp = now
            mimeType = mime
            modificationTimestamp = now
            permissions = "RWDNV"
        }

        val list = mutableListOf<OCFile>()

        list.add(createFolder(1, 0, "/"))

        list.add(createFolder(5, 2, "/Documents/Projects"))
        list.add(createFile(9, 5, "/Documents/Projects/spec.txt", 12000, MimeType.TEXT_PLAIN))
        list.add(createFolder(2, 1, "/Documents"))
        list.add(createFile(11, 7, "/Photos/Vacation/img2.jpg", 300000, MimeType.JPEG))
        list.add(createFolder(7, 3, "/Photos/Vacation"))
        list.add(createFile(4, 2, "/Documents/example.pdf", 150000, MimeType.PDF))
        list.add(createFolder(3, 1, "/Photos"))
        list.add(createFile(12, 3, "/Photos/cover.png", 80000, MimeType.PNG))
        list.add(createFile(6, 5, "/Documents/Projects/readme.txt", 2000, MimeType.TEXT_PLAIN))
        list.add(createFolder(8, 5, "/Documents/Projects/Archive"))
        list.add(createFile(13, 8, "/Documents/Projects/Archive/old.bmp", 900000, MimeType.BMP))
        list.add(createFile(10, 7, "/Photos/Vacation/img1.jpg", 250000, MimeType.JPEG))
        list.add(createFolder(14, 1, "/Temp"))
        list.add(createFile(15, 14, "/Temp/tmp_file_1.txt", 400, MimeType.TEXT_PLAIN))
        list.add(createFile(16, 14, "/Temp/tmp_file_2.txt", 800, MimeType.TEXT_PLAIN))
        list.add(createFolder(17, 14, "/Temp/Nested"))
        list.add(createFile(18, 17, "/Temp/Nested/deep.txt", 100, MimeType.TEXT_PLAIN))
        list.add(createFile(19, 2, "/Documents/notes.txt", 1500, MimeType.TEXT_PLAIN))
        list.add(createFolder(20, 3, "/Photos/EmptyFolder"))

        list.forEach { ocFile ->
            if (!ocFile.isFolder) {
                val localFile = File(tempDir, ocFile.remoteId).apply {
                    parentFile?.mkdirs()
                    createNewFile()
                    writeText("test content")
                }
                ocFile.storagePath = localFile.absolutePath
                storageManager.saveFile(ocFile)
            } else {
                // For folders, create the folder in tempDir
                val localFolder = File(tempDir, ocFile.remoteId).apply { mkdirs() }
                ocFile.storagePath = localFolder.absolutePath
                storageManager.saveFile(ocFile)
            }
        }

        return list
    }

    @Test
    fun deleteMixedFiles() {
        var result = false
        val files = getMixedOcFiles()

        files.forEach {
            result = storageManager.removeFile(it, true, true)
            if (!result) {
                fail("remove operation is failed")
            }
        }

        assert(result)
    }

    @Test
    fun removeNullFileShouldReturnsFalse() {
        val result = storageManager.removeFile(null, true, true)
        assertFalse(result)
    }

    @Test
    fun deleteFileOnlyFromDb() {
        val file = createAndSaveSingleFileWithLocalCopy()

        val result = storageManager.removeFile(file, true, false)

        assertTrue(result)

        // verify DB no longer contains file
        val fromDb = storageManager.getFileById(file.fileId)
        assertNull(fromDb)

        // verify local file still exists
        assertTrue(File(file.storagePath).exists())
    }

    @Test
    fun deleteFileOnlyLocalCopy() {
        val file = createAndSaveSingleFileWithLocalCopy()

        val result = storageManager.removeFile(file, false, true)

        assertTrue(result)

        // DB should still contain file
        val fromDb = storageManager.getFileById(file.fileId)
        assertNotNull(fromDb)

        // Storage path should be null
        assertNull(fromDb?.storagePath)
    }

    @Test
    fun deleteFileDBAndLocal() {
        val file = createAndSaveSingleFileWithLocalCopy()

        val result = storageManager.removeFile(file, true, true)

        assertTrue(result)

        assertNull(storageManager.getFileById(file.fileId))
        assertFalse(File(file.storagePath).exists())
    }

    @Test
    fun deleteFolderRecursive() {
        val folder = createAndSaveFolderTree()

        val result = storageManager.removeFile(folder, true, true)

        assertTrue(result)

        // Folder removed from DB
        assertNull(storageManager.getFileById(folder.fileId))

        // subdirectories and files are removed
        val children = storageManager.getFolderContent(folder, false)
        assertTrue(children.isEmpty())

        // local folder removed
        val localPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, folder)
        assertFalse(File(localPath).exists())
    }

    @Test
    fun removeFolderFileIdMinusOneSkipsDBDeletion() {
        val folder = OCFile("/Test").apply {
            fileId = -1
            mimeType = MimeType.DIRECTORY
        }

        val result = storageManager.removeFile(folder, true, false)

        assertTrue(result)
    }
}
