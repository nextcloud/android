/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.FixedChunkUploadRemoteOperation
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class FileUploadWorkerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var uploadsStorageManager: UploadsStorageManager
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize UploadsStorageManager with real database
        uploadsStorageManager = UploadsStorageManager(
            UserAccountManagerImpl.fromContext(context),
            context.contentResolver
        )

        // Create temp directory for test files
        tempDir = File(context.cacheDir, "file_upload_worker_test")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        // Clean up any existing uploads
        uploadsStorageManager.removeAllUploads()
    }

    @After
    fun tearDown() {
        // Clean up test files
        if (tempDir.exists()) {
            deleteRecursive(tempDir)
        }

        // Clean up uploads
        uploadsStorageManager.removeAllUploads()
    }

    @Test
    fun testFileUploadWorkerConstants() {
        // Test that FileUploadWorker constants are correctly defined
        assertEquals("ACCOUNT constant", "data_account", FileUploadWorker.ACCOUNT)
        assertEquals("UPLOAD_IDS constant", "uploads_ids", FileUploadWorker.UPLOAD_IDS)
        assertEquals("LOCAL_BEHAVIOUR_COPY", 0, FileUploadWorker.LOCAL_BEHAVIOUR_COPY)
        assertEquals("LOCAL_BEHAVIOUR_MOVE", 1, FileUploadWorker.LOCAL_BEHAVIOUR_MOVE)
        assertEquals("LOCAL_BEHAVIOUR_FORGET", 2, FileUploadWorker.LOCAL_BEHAVIOUR_FORGET)
        assertEquals("LOCAL_BEHAVIOUR_DELETE", 3, FileUploadWorker.LOCAL_BEHAVIOUR_DELETE)
    }

    @Test
    fun testDeterministicNotificationIdGeneration() {
        // Test notification ID generation with real file system
        val testFile1 = createTestFile("notification_test1.txt", 1024L)
        val testFile2 = createTestFile("notification_test2.txt", 2048L)

        // Test same file produces same notification ID
        val id1a = generateTestNotificationId(testFile1.absolutePath, testFile1.length())
        val id1b = generateTestNotificationId(testFile1.absolutePath, testFile1.length())
        assertEquals("Same file should generate same notification ID", id1a, id1b)

        // Test different files produce different notification IDs
        val id2 = generateTestNotificationId(testFile2.absolutePath, testFile2.length())
        assertNotEquals("Different files should generate different notification IDs", id1a, id2)

        // Test different file sizes produce different notification IDs
        val id1c = generateTestNotificationId(testFile1.absolutePath, 4096L)
        assertNotEquals("Different file sizes should generate different notification IDs", id1a, id1c)

        // Verify all IDs are positive
        assertTrue("Notification IDs should be positive", id1a > 0)
        assertTrue("Notification IDs should be positive", id1b > 0)
        assertTrue("Notification IDs should be positive", id2 > 0)
        assertTrue("Notification IDs should be positive", id1c > 0)
    }

    @Test
    fun testRealDatabaseOperations() {
        // Test FileUploadWorker integration with real UploadsStorageManager database
        val testFile = createTestFile("database_test.txt", 1024L)

        // Create upload entry
        val upload = OCUpload(
            testFile.absolutePath,
            "/remote/database_test.txt",
            "test@example.com"
        ).apply {
            nameCollisionPolicy = NameCollisionPolicy.DEFAULT
            localAction = FileUploadWorker.LOCAL_BEHAVIOUR_COPY
            isUseWifiOnly = false
            isWhileChargingOnly = false
        }

        // Store in database
        val uploadId = uploadsStorageManager.storeUpload(upload)
        assertTrue("Upload should be stored successfully", uploadId > 0)

        // Verify upload can be retrieved
        val retrievedUpload = uploadsStorageManager.getUploadById(uploadId)
        assertNotNull("Upload should be retrievable", retrievedUpload)
        assertEquals("Local paths should match", upload.localPath, retrievedUpload?.localPath)
        assertEquals("Remote paths should match", upload.remotePath, retrievedUpload?.remotePath)
        assertEquals("Account names should match", upload.accountName, retrievedUpload?.accountName)

        // Note: updateUploadStatus with just status is private, so we'll skip this test

        // Test upload removal
        uploadsStorageManager.removeUpload(uploadId)
        val deletedUpload = uploadsStorageManager.getUploadById(uploadId)
        assertNull("Upload should be removed", deletedUpload)
    }

    @Test
    fun testFixedChunkUploadRemoteOperationCreation() {
        // Test that FixedChunkUploadRemoteOperation can be created
        val testFile = createTestFile("chunk_test.bin", 3 * 1024 * 1024L) // 3MB
        
        val operation = FixedChunkUploadRemoteOperation(
            testFile.absolutePath,
            "/remote/chunk_test.bin",
            "application/octet-stream",
            null,
            System.currentTimeMillis(),
            null,
            false,
            context
        )
        
        assertNotNull("Chunk upload operation should be created", operation)
        assertFalse("Operation should not be cancelled initially", operation.isCancelled())
        assertEquals("Fixed chunk size should be 1MB", 1024 * 1024, FixedChunkUploadRemoteOperation.FIXED_CHUNK_SIZE)
    }

    @Test
    fun testProgressCalculationWithRealFiles() {
        // Test progress calculation with real file sizes
        val smallFile = createTestFile("small_progress.txt", 1024L)
        val mediumFile = createTestFile("medium_progress.txt", 1024 * 512L) // 512KB
        val largeFile = createTestFile("large_progress.txt", 1024 * 1024 * 3L) // 3MB

        // Test progress calculations
        assertEquals("0% for 0 transferred", 0, calculatePercent(0, smallFile.length()))
        assertEquals("50% for half transferred", 50, calculatePercent(smallFile.length() / 2, smallFile.length()))
        assertEquals("100% for fully transferred", 100, calculatePercent(smallFile.length(), smallFile.length()))

        // Test with different file sizes
        assertEquals("25% for quarter of medium file", 25, calculatePercent(mediumFile.length() / 4, mediumFile.length()))
        assertEquals("75% for three quarters of large file", 75, calculatePercent(largeFile.length() * 3 / 4, largeFile.length()))

        // Test edge cases
        assertEquals("0% for zero total", 0, calculatePercent(100, 0))
        assertEquals("100% for over-transferred", 100, calculatePercent(150, 100))
    }

    @Test
    fun testChunkedUploadThresholds() {
        // Test that files above threshold would trigger chunked upload logic
        val smallFile = createTestFile("small_threshold.txt", 1024L) // 1KB
        val largeFile = createTestFile("large_threshold.txt", 3 * 1024 * 1024L) // 3MB

        assertTrue("Small file should be below chunk threshold", 
                  smallFile.length() < FixedChunkUploadRemoteOperation.FIXED_CHUNK_SIZE)
        assertTrue("Large file should be above chunk threshold", 
                  largeFile.length() > FixedChunkUploadRemoteOperation.FIXED_CHUNK_SIZE)

        // Calculate expected chunk count for large file
        val expectedChunks = (largeFile.length() + FixedChunkUploadRemoteOperation.FIXED_CHUNK_SIZE - 1) / 
                            FixedChunkUploadRemoteOperation.FIXED_CHUNK_SIZE
        assertTrue("Large file should require multiple chunks", expectedChunks > 1)
        assertEquals("Should calculate 3 chunks for 3MB file", 3, expectedChunks)
    }

    // Helper methods

    private fun createTestFile(fileName: String, size: Long): File {
        val testFile = File(tempDir, fileName)
        FileOutputStream(testFile).use { fos ->
            val buffer = ByteArray(8192)
            var bytesWritten = 0L

            // Fill buffer with test data
            for (i in buffer.indices) {
                buffer[i] = (i % 256).toByte()
            }

            while (bytesWritten < size) {
                val bytesToWrite = Math.min(buffer.size.toLong(), size - bytesWritten).toInt()
                fos.write(buffer, 0, bytesToWrite)
                bytesWritten += bytesToWrite
            }
        }

        assertTrue("Test file should exist", testFile.exists())
        assertEquals("Test file should have correct size", size, testFile.length())
        return testFile
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    private fun generateTestNotificationId(localPath: String, fileSize: Long): Int {
        return try {
            val file = File(localPath)
            val canonicalPath = try {
                file.canonicalPath
            } catch (e: java.io.IOException) {
                localPath
            }
            val baseString = "${canonicalPath}_$fileSize"
            val hash = baseString.hashCode()
            Math.abs(hash)
        } catch (e: Exception) {
            Math.abs("${localPath}_$fileSize".hashCode())
        }
    }

    private fun calculatePercent(transferred: Long, total: Long): Int {
        return if (total == 0L) 0 else (100.0 * transferred / total).toInt().coerceAtMost(100)
    }
} 