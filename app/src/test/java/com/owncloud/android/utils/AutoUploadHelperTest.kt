/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.content.Context
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.jobs.autoUpload.AutoUploadHelper
import com.nextcloud.client.jobs.autoUpload.FileSystemRepository
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.shouldSkipFile
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@Suppress("MagicNumber")
class AutoUploadHelperTest {

    private lateinit var tempDir: File
    private val helper = AutoUploadHelper()
    private val accountName = "testAccount"

    private val mockDao: FileSystemDao = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)

    private lateinit var repo: FileSystemRepository

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("auto_upload_test_").toFile()
        tempDir.mkdirs()
        assertTrue("Failed to create temp directory", tempDir.exists())

        repo = FileSystemRepository(mockDao, mockContext)
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
        clearAllMocks()
    }

    private fun createTestFolder(
        localPath: String = tempDir.absolutePath,
        excludeHidden: Boolean = false,
        lastScan: Long = -1L,
        enabledTimestamp: Long = 0L,
        alsoUploadExistingFiles: Boolean = true,
        type: MediaFolderType = MediaFolderType.CUSTOM
    ): SyncedFolder = SyncedFolder(
        localPath,
        "",
        true,
        false,
        alsoUploadExistingFiles,
        false,
        accountName,
        1,
        1,
        true,
        enabledTimestamp,
        type,
        false,
        SubFolderRule.YEAR_MONTH,
        excludeHidden,
        lastScan
    )

    @Test
    fun testInsertCustomFolderProcessedCount() {
        File(tempDir, "file1.txt").apply {
            writeText("Hello")
            assertTrue("File1 should exist", exists())
        }
        File(tempDir, "file2.txt").apply {
            writeText("World")
            assertTrue("File2 should exist", exists())
        }

        val folder = createTestFolder(
            localPath = tempDir.absolutePath,
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        assertEquals("Should process 2 files", 2, processedCount)
    }

    @Test
    fun testInsertCustomFolderWithHiddenFiles() {
        File(tempDir, "visible.txt").apply { writeText("Visible") }
        File(tempDir, ".hidden.txt").apply {
            writeText("Hidden")
        }

        val folder = createTestFolder(
            excludeHidden = true,
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        assertTrue("Should process at least 1 file", processedCount >= 1)
    }

    @Test
    fun testInsertCustomFolderWithLastScanFilter() {
        val currentTime = System.currentTimeMillis()

        // Create an old file
        val oldFile = File(tempDir, "old.txt").apply { writeText("Old") }
        val oldFileLastModified = currentTime - 10000 // 10 seconds ago

        // Create a new file
        val newFile = File(tempDir, "new.txt").apply { writeText("New") }

        val folder = createTestFolder(
            lastScan = currentTime - 5000, // Last scan was 5 seconds ago
            type = MediaFolderType.CUSTOM
        )

        val shouldSkipOldFile = folder.shouldSkipFile(oldFile, oldFileLastModified, null)
        assertTrue(shouldSkipOldFile)

        val shouldSkipNewFile = folder.shouldSkipFile(newFile, currentTime, null)
        assertTrue(!shouldSkipNewFile)
    }

    @Test
    fun testInsertCustomFolderNotExisting() {
        val currentTime = System.currentTimeMillis()

        // old file should not be scanned
        val oldFile = File(tempDir, "old.txt").apply { writeText("Old") }
        val oldFileLastModified = currentTime - 10000 // 10 seconds ago

        val newFile = File(tempDir, "new.txt").apply { writeText("New") }

        // Enabled 5 seconds ago
        val folder = createTestFolder(
            enabledTimestamp = currentTime - 5000,
            type = MediaFolderType.CUSTOM
        ).apply {
            lastScanTimestampMs = currentTime
        }

        val shouldSkipOldFile = folder.shouldSkipFile(oldFile, oldFileLastModified, null)
        assertTrue(shouldSkipOldFile)

        val shouldSkipNewFile = folder.shouldSkipFile(newFile, currentTime, null)
        assertTrue(!shouldSkipNewFile)
    }

    @Test
    fun testInsertCustomFolderEmpty() {
        val folder = createTestFolder(type = MediaFolderType.CUSTOM)
        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        assertEquals("Empty folder should process 0 files", 0, processedCount)
    }

    @Test
    fun testInsertCustomFolderNonExistentPath() {
        val nonExistentPath = File(tempDir, "does_not_exist").absolutePath
        val folder = createTestFolder(
            localPath = nonExistentPath,
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        assertEquals("Non-existent folder should return 0", 0, processedCount)
    }

    @Test
    fun testInsertCustomFolderWithSubdirectories() {
        val subDir = File(tempDir, "subdir")
        subDir.mkdirs()

        File(tempDir, "root.txt").writeText("Root file")
        File(subDir, "nested.txt").writeText("Nested file")

        val folder = createTestFolder(type = MediaFolderType.CUSTOM)
        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        assertEquals("Should process files in root and subdirectories", 2, processedCount)
    }

    @Test
    fun testInsertCustomFolderWithHiddenDirectory() {
        val currentTime = System.currentTimeMillis()

        val hiddenDir = File(tempDir, ".hidden_dir")
        hiddenDir.mkdirs()
        try {
            Files.setAttribute(hiddenDir.toPath(), "dos:hidden", true)
        } catch (_: Exception) {
        }

        File(hiddenDir, "file.txt").writeText("Hidden dir file")

        // Create regular file
        File(tempDir, "regular.txt").writeText("Regular file")

        val folder = createTestFolder(
            excludeHidden = true,
            type = MediaFolderType.CUSTOM
        ).apply {
            lastScanTimestampMs = currentTime
        }

        val processedCount = helper.insertCustomFolderIntoDB(folder, repo)

        // Should skip hidden directory and its contents
        assertEquals("Should only process regular file", 1, processedCount)
    }

    @Test
    fun testInsertCustomFolderComplexNestedStructure() {
        // Root folder: FOLDER_A
        val folderA = File(tempDir, "FOLDER_A")
        folderA.mkdirs()

        // Subfolders of FOLDER_A
        val folderB = File(folderA, "FOLDER_B")
        folderB.mkdirs()
        val folderC = File(folderA, "FOLDER_C")
        folderC.mkdirs()

        // Files in FOLDER_A
        File(folderA, "FILE_A.txt").writeText("File in A")

        // Subfolders of FOLDER_B
        val folderD = File(folderB, "FOLDER_D")
        folderD.mkdirs()

        // Files in FOLDER_B
        File(folderB, "FILE_B.txt").writeText("File in B")

        // Files in FOLDER_C
        File(folderC, "FILE_A.txt").writeText("File in C")
        File(folderC, "FILE_B.txt").writeText("Another file in C")

        // Subfolders of FOLDER_D
        val folderE = File(folderD, "FOLDER_E")
        folderE.mkdirs()

        // Files in FOLDER_E
        File(folderE, "FILE_A.txt").writeText("File in E")

        val syncedFolder = createTestFolder(
            localPath = folderA.absolutePath,
            type = MediaFolderType.CUSTOM
        )

        /*
         * Expected file count with full paths:
         * ${tempDir.absolutePath}/FOLDER_A/FILE_A.txt -> 1
         * ${tempDir.absolutePath}/FOLDER_A/FOLDER_B/FILE_B.txt -> 1
         * ${tempDir.absolutePath}/FOLDER_A/FOLDER_C/FILE_A.txt -> 1
         * ${tempDir.absolutePath}/FOLDER_A/FOLDER_C/FILE_B.txt -> 1
         * ${tempDir.absolutePath}/FOLDER_A/FOLDER_B/FOLDER_D/FOLDER_E/FILE_A.txt -> 1
         * Total = 5 files
         */
        val processedCount = helper.insertCustomFolderIntoDB(syncedFolder, repo)
        assertEquals("Should process all files in complex nested structure", 5, processedCount)
    }

    @Test
    fun testAlsoUploadExistingFiles() {
        val currentTime = System.currentTimeMillis()

        // Old file (created before enabling auto-upload)
        val oldFile = File(tempDir, "old_file.txt").apply {
            writeText("Old file")
        }
        val oldFileCreationTime = currentTime - 10_000
        val oldFileLastModified = currentTime - 5_000

        // New file (created after enabling auto-upload)
        val newFile = File(tempDir, "new_file.txt").apply {
            writeText("New file")
        }
        val newFileCreationTime = currentTime + 10_000
        val newFileLastModified = currentTime + 5_000

        val folderSkipOld = createTestFolder(
            localPath = tempDir.absolutePath,
            type = MediaFolderType.CUSTOM,
            alsoUploadExistingFiles = false
        ).apply {
            setEnabled(true, currentTime)
        }

        val shouldSkipOldFile = folderSkipOld.shouldSkipFile(oldFile, oldFileLastModified, oldFileCreationTime)
        assertTrue(shouldSkipOldFile)

        val shouldSkipNewFile = folderSkipOld.shouldSkipFile(newFile, newFileLastModified, newFileCreationTime)
        assertTrue(!shouldSkipNewFile)

        val folderUploadAll = createTestFolder(
            localPath = tempDir.absolutePath,
            type = MediaFolderType.CUSTOM,
            alsoUploadExistingFiles = true
        ).apply {
            setEnabled(true, currentTime)
        }

        val shouldSkipOldFileIfAlsoUploadExistingFile =
            folderUploadAll.shouldSkipFile(oldFile, oldFileLastModified, oldFileCreationTime)
        assertTrue(!shouldSkipOldFileIfAlsoUploadExistingFile)

        val shouldSkipNewFileIfAlsoUploadExistingFile =
            folderUploadAll.shouldSkipFile(newFile, newFileLastModified, newFileCreationTime)
        assertTrue(!shouldSkipNewFileIfAlsoUploadExistingFile)
    }
}
