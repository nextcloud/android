/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.nextcloud.client.jobs.autoUpload.AutoUploadHelper
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.toLocalPath
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.File
import java.nio.file.Files

class AutoUploadHelperTest {

    private lateinit var mocks: AutoCloseable

    @Mock
    lateinit var storageManager: FileDataStorageManager

    private lateinit var tempDir: File
    private val helper = AutoUploadHelper()
    private val accountName = "testAccount"

    @Before
    fun setup() {
        mocks = MockitoAnnotations.openMocks(this)
        tempDir = Files.createTempDirectory("auto_upload_test_").toFile()
        tempDir.mkdirs()
        assertTrue("Failed to create temp directory", tempDir.exists())
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
        mocks.close()
    }

    private fun createTestFolder(
        localPath: String = tempDir.absolutePath,
        excludeHidden: Boolean = false,
        lastScan: Long = -1L,
        enabledTimestamp: Long = 0L,
        skipExistingFilesOnRemote: Boolean = false,
        type: MediaFolderType = MediaFolderType.CUSTOM
    ): SyncedFolder = SyncedFolder(
        localPath,
        "",
        true,
        false,
        skipExistingFilesOnRemote,
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

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

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

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        assertTrue("Should process at least 1 file", processedCount >= 1)
    }

    @Test
    fun testInsertCustomFolderWithLastScanFilter() {
        val currentTime = System.currentTimeMillis()

        // Create an old file
        val oldFile = File(tempDir, "old.txt").apply { writeText("Old") }
        oldFile.setLastModified(currentTime - 10000) // 10 seconds ago

        // Create a new file
        val newFile = File(tempDir, "new.txt").apply { writeText("New") }
        newFile.setLastModified(currentTime)

        val folder = createTestFolder(
            lastScan = currentTime - 5000, // Last scan was 5 seconds ago
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        // Should only process the new file (modified after last scan)
        assertEquals("Should process only 1 new file", 1, processedCount)
    }

    @Test
    fun testInsertCustomFolderNotExisting() {
        val currentTime = System.currentTimeMillis()

        // old file should not be scanned
        val oldFile = File(tempDir, "old.txt").apply { writeText("Old") }
        oldFile.setLastModified(currentTime - 10000)

        val newFile = File(tempDir, "new.txt").apply { writeText("New") }
        newFile.setLastModified(currentTime)

        // Enabled 5 seconds ago
        val folder = createTestFolder(
            enabledTimestamp = currentTime - 5000,
            type = MediaFolderType.CUSTOM
        ).apply {
            lastScanTimestampMs = currentTime
        }

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        // Should only process files newer than enabledTimestamp
        assertEquals("Should process only files after enabled timestamp", 1, processedCount)
    }

    @Test
    fun testInsertCustomFolderEmpty() {
        val folder = createTestFolder(type = MediaFolderType.CUSTOM)
        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        assertEquals("Empty folder should process 0 files", 0, processedCount)
    }

    @Test
    fun testInsertCustomFolderNonExistentPath() {
        val nonExistentPath = File(tempDir, "does_not_exist").absolutePath
        val folder = createTestFolder(
            localPath = nonExistentPath,
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        assertEquals("Non-existent folder should return 0", 0, processedCount)
    }

    @Test
    fun testInsertCustomFolderWithSubdirectories() {
        val subDir = File(tempDir, "subdir")
        subDir.mkdirs()

        File(tempDir, "root.txt").writeText("Root file")
        File(subDir, "nested.txt").writeText("Nested file")

        val folder = createTestFolder(type = MediaFolderType.CUSTOM)
        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        assertEquals("Should process files in root and subdirectories", 2, processedCount)
    }

    @Test
    fun testInsertCustomFolderWithHiddenDirectory() {
        val currentTime = System.currentTimeMillis()

        val hiddenDir = File(tempDir, ".hidden_dir")
        hiddenDir.mkdirs()
        File(hiddenDir, "file.txt").writeText("Hidden dir file")

        // Create regular file
        File(tempDir, "regular.txt").writeText("Regular file")

        val folder = createTestFolder(
            excludeHidden = true,
            type = MediaFolderType.CUSTOM
        ).apply {
            lastScanTimestampMs = currentTime
        }

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

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
        val processedCount = helper.insertCustomFolderIntoDB(syncedFolder, null, storageManager)
        assertEquals("Should process all files in complex nested structure", 5, processedCount)
    }

    @Test
    fun testSkipExistingFiles() {
        val folder = File(tempDir, "FOLDER")
        folder.mkdirs()
        val file = File(folder, "FILE_B.txt")
        file.writeText("File in A")
        val path = file.toPath()
        val localPath = path.toLocalPath()
        val ocFile = OCFile("/my_remote_folder/FILE_B.txt").apply {
            fileId = 1
            remoteId = "00001"
            storagePath = localPath
        }
        storageManager.saveFile(ocFile)

        Mockito.`when`(storageManager.getFileByLocalPath(localPath)).thenReturn(ocFile)

        val syncedFolder = createTestFolder(
            localPath = folder.absolutePath,
            type = MediaFolderType.CUSTOM,
            skipExistingFilesOnRemote = true
        )

        val processedCount = helper.insertCustomFolderIntoDB(syncedFolder, null, storageManager)
        assertEquals("Should skip existing files", 0, processedCount)
    }

    @Test
    fun testInsertCustomFolderCombinedScenarios() {
        val currentTime = System.currentTimeMillis()
        val root = File(tempDir, "COMBINED_ROOT")
        root.mkdirs()

        val visibleSubDir = File(root, "visible_subdir")
        visibleSubDir.mkdirs()

        val hiddenSubDir = File(root, ".hidden_subdir")
        hiddenSubDir.mkdirs()

        // Normal visible file (should be processed)
        File(root, "visible.txt").apply {
            writeText("Visible file content")
            setLastModified(currentTime)
        }

        // Hidden file (should be skipped)
        File(root, ".hidden.txt").apply {
            writeText("Hidden file content")
            setLastModified(currentTime)
        }

        // File in hidden directory (should be skipped)
        File(hiddenSubDir, "nested_hidden.txt").apply {
            writeText("Should be skipped because parent is hidden")
        }

        // File that already exists remotely (should be skipped)
        val existingFile = File(visibleSubDir, "existing_remote.txt").apply {
            writeText("Already on remote")
        }

        // New file modified after last scan (should be processed)
        File(visibleSubDir, "new_after_last_scan.txt").apply {
            writeText("New file after last scan")
            setLastModified(currentTime)
        }

        // Old file modified before last scan (should be skipped)
        File(visibleSubDir, "old_before_last_scan.txt").apply {
            writeText("Old file")
            setLastModified(currentTime - 60_000) // 1 minute ago
        }

        val existingLocalPath = existingFile.toPath().toLocalPath()
        val ocFile = OCFile("/remote/existing_remote.txt").apply {
            fileId = 1
            remoteId = "R001"
            storagePath = existingLocalPath
        }
        Mockito.`when`(storageManager.getFileByLocalPath(existingLocalPath)).thenReturn(ocFile)

        val folder = createTestFolder(
            localPath = root.absolutePath,
            excludeHidden = true,
            skipExistingFilesOnRemote = true,
            lastScan = currentTime - 30_000, // 30 seconds ago
            type = MediaFolderType.CUSTOM
        )

        val processedCount = helper.insertCustomFolderIntoDB(folder, null, storageManager)

        assertEquals(
            "Should process only visible and new files, skipping hidden, existing, and old ones",
            2,
            processedCount
        )
    }
}
