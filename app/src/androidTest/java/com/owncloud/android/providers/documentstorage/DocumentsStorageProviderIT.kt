/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.test.RandomStringGenerator
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile.ROOT_PATH
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.assertContainsAllFiles
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.assertExistsOnServer
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.assertRecentlyModified
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.assertRegularFile
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.assertRegularFolder
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.findChild
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.findOCFile
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.listChildren
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.requireNewDirectory
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.requireNewFile
import com.owncloud.android.providers.documentstorage.TestDocumentsProviderUtils.requireOCFile
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider.Companion.DOCUMENTID_SEPARATOR
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity
import org.apache.jackrabbit.webdav.client.methods.PutMethod
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class DocumentsStorageProviderIT : AbstractOnServerIT() {

    companion object {
        private const val MAX_FILE_NAME_LENGTH = 225
        private const val MIN_RANDOM_KILOBYTES = 1
        private const val MAX_RANDOM_KILOBYTES = 99
        private const val BYTES_PER_KILOBYTE = 1024
        private const val FULL_PERMISSIONS = "RSMCKGWDNV"
        private const val WRITE_TRUNCATE_MODE = "wt"
        private const val HTML_MIME_TYPE = "text/html"
        private const val TEXT_MIME_TYPE = "text/plain"
        private const val BINARY_MIME_TYPE = "application/octet-stream"
        private const val INITIAL_CONTENT = "initial content"
        private const val CHANGED_CONTENT = "new content"
        private val TEST_TIMEOUT = 5.minutes
        private val AWAIT_TIMEOUT = 90.seconds
        private val FOLDER_MODIFICATION_TOLERANCE = 1.minutes
    }

    private val context = targetContext
    private val contentResolver = context.contentResolver
    private val authority = context.getString(R.string.document_provider_authority)

    private val rootFileId = storageManager.getFileByEncryptedRemotePath(ROOT_PATH).fileId
    private val documentId = "${DocumentsStorageProvider.rootIdForUser(user)}$DOCUMENTID_SEPARATOR$rootFileId"
    private val uri = DocumentsContract.buildTreeDocumentUri(authority, documentId)
    private val rootDir: DocumentFile
        get() = checkNotNull(DocumentFile.fromTreeUri(context, uri)) { "Storage root $uri is not accessible" }

    private val uploadsStorageManager = UploadsStorageManager(
        UserAccountManagerImpl.fromContext(context),
        contentResolver
    )

    @get:Rule
    val timeout: Timeout = Timeout.millis(TEST_TIMEOUT.inWholeMilliseconds)

    @Before
    fun before() {
        cancelPendingUploads()
        grantFullPermissionsOnRoot()
        initializeProviderRoots()

        assertTrue("Storage root does not exist", rootDir.exists())
        assertTrue(rootDir.isDirectory)
    }

    @After
    override fun after() = runBlocking {
        cancelPendingUploads()
        rootDir.listChildren(context).forEach { it.delete() }
    }

    @Test
    fun testWhenFilesCreatedAndDeletedGivenShouldUpdateListingAndServer() = runBlocking {
        assertContainsAllFiles(emptyList(), rootDir.listChildren(context))

        val name1 = RandomStringGenerator.make()
        val file1 = rootDir.requireNewFile(HTML_MIME_TYPE, name1)
        file1.assertRegularFile(name = name1, size = 0L, parent = rootDir)
        file1.assertRecentlyModified()
        assertContainsAllFiles(listOf(file1), rootDir.listChildren(context))
        val remotePath1 = file1.requireOCFile(storageManager).remotePath
        assertExistsOnServer(client, remotePath1, true)

        val name2 = RandomStringGenerator.make(MAX_FILE_NAME_LENGTH)
        val file2 = rootDir.requireNewFile(BINARY_MIME_TYPE, name2)
        val remotePath2 = file2.requireOCFile(storageManager).remotePath
        assertExistsOnServer(client, remotePath2, true)
        file2.assertRegularFile(name = name2, size = 0L, mimeType = BINARY_MIME_TYPE, parent = rootDir)
        file2.assertRecentlyModified()
        assertContainsAllFiles(listOf(file1, file2), rootDir.listChildren(context))

        assertTrue(file1.delete())
        assertFalse(file1.exists())
        assertExistsOnServer(client, remotePath1, false)
        assertContainsAllFiles(listOf(file2), rootDir.listChildren(context))

        assertTrue(file2.delete())
        assertFalse(file2.exists())
        assertExistsOnServer(client, remotePath2, false)
        assertContainsAllFiles(emptyList(), rootDir.listChildren(context))
    }

    @Test
    fun testWhenBytesWrittenGivenShouldReadBackSameBytesAndUpdateSize() {
        val file = rootDir.requireNewFile(BINARY_MIME_TYPE, RandomStringGenerator.make())
        file.assertRegularFile(size = 0L)

        val data = Random.nextBytes(Random.nextInt(MIN_RANDOM_KILOBYTES, MAX_RANDOM_KILOBYTES) * BYTES_PER_KILOBYTE)
        file.writeBytes(data)
        awaitStoredLength(file, data.size.toLong())

        assertArrayEquals(data, file.readBytes())
        file.assertRegularFile(size = data.size.toLong())
    }

    @Test
    fun testWhenFolderDeletedGivenShouldRemoveFolderAndChildrenFromServer() = runBlocking {
        val folderName = RandomStringGenerator.make()
        val folder = rootDir.requireNewDirectory(folderName)
        folder.assertRegularFolder(name = folderName, parent = rootDir)
        folder.assertRecentlyModified(FOLDER_MODIFICATION_TOLERANCE)
        val folderRemotePath = folder.requireOCFile(storageManager).remotePath
        assertExistsOnServer(client, folderRemotePath, true)

        val file = folder.requireNewFile(HTML_MIME_TYPE, RandomStringGenerator.make())
        file.assertRegularFile(parent = folder)
        val fileRemotePath = file.requireOCFile(storageManager).remotePath
        assertExistsOnServer(client, fileRemotePath, true)

        val fileName = checkNotNull(file.name) { "${file.uri} has no name" }
        assertEquals(file.uri.toString(), folder.findChild(context, fileName)?.uri.toString())
        assertContainsAllFiles(listOf(file), folder.listChildren(context))

        assertTrue(folder.delete())
        assertFalse(folder.exists())
        assertExistsOnServer(client, folderRemotePath, false)
        assertExistsOnServer(client, fileRemotePath, false)
    }

    @Test
    fun testWhenServerContentChangedGivenShouldReadServerContent() {
        val file = createUploadedTextFile()

        overwriteOnServerWithChangedContent(file.requireOCFile(storageManager).remotePath)

        assertEquals(CHANGED_CONTENT, file.readText())
    }

    @Test
    fun testWhenWrittenSuccessivelyGivenShouldReadLatestContent() {
        val file = createUploadedTextFile()

        file.writeText(CHANGED_CONTENT)
        awaitStoredLength(file, CHANGED_CONTENT.toByteArray().size.toLong())

        assertEquals(CHANGED_CONTENT, file.readText())
    }

    private fun cancelPendingUploads() {
        FileUploadHelper.instance().cancel(user.accountName)
    }

    private fun grantFullPermissionsOnRoot() {
        val root = storageManager.getFileByEncryptedRemotePath(ROOT_PATH).apply { permissions = FULL_PERMISSIONS }
        storageManager.saveFile(root)
    }

    private fun initializeProviderRoots() {
        contentResolver.query(DocumentsContract.buildRootsUri(authority), null, null, null)?.close()
    }

    private fun createUploadedTextFile(): DocumentFile {
        val file = rootDir.requireNewFile(TEXT_MIME_TYPE, RandomStringGenerator.make())
        file.assertRegularFile(size = 0L)
        assertTrue(file.requireOCFile(storageManager).etagOnServer.isNotEmpty())

        file.writeText(INITIAL_CONTENT)
        awaitUploadFinished(file)
        return file
    }

    private fun overwriteOnServerWithChangedContent(remotePath: String) {
        val putMethod = PutMethod(client.getFilesDavUri(remotePath)).apply {
            requestEntity = ByteArrayRequestEntity(CHANGED_CONTENT.toByteArray())
        }

        try {
            assertEquals(HttpStatus.SC_NO_CONTENT, client.executeMethod(putMethod))
            client.exhaustResponse(putMethod.responseBodyAsStream)
        } finally {
            putMethod.releaseConnection()
        }
    }

    private fun DocumentFile.writeBytes(content: ByteArray) {
        val outputStream = contentResolver.openOutputStream(uri, WRITE_TRUNCATE_MODE)
        checkNotNull(outputStream) { "$uri could not be opened for writing" }.use { it.write(content) }
    }

    private fun DocumentFile.writeText(content: String) = writeBytes(content.toByteArray())

    private fun DocumentFile.readBytes(): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        return checkNotNull(inputStream) { "$uri could not be opened for reading" }.use { it.readBytes() }
    }

    private fun DocumentFile.readText(): String = readBytes().decodeToString()

    private fun awaitStoredLength(file: DocumentFile, expectedLength: Long) {
        awaitUntil({ "write of ${file.uri} was not saved with length $expectedLength" }) {
            file.findOCFile(storageManager)?.fileLength == expectedLength
        }
    }

    private fun awaitUploadFinished(file: DocumentFile) {
        val remotePath = file.requireOCFile(storageManager).remotePath

        awaitUntil({ "upload of $remotePath did not finish, stored uploads: ${describeUploads()}" }) {
            val upload = findUpload(remotePath)
            if (upload?.hasFailed() == true) {
                fail("upload of $remotePath failed with ${upload.lastResult}")
            }

            upload?.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && !isUploadRunning(remotePath)
        }
    }

    private fun isUploadRunning(remotePath: String): Boolean =
        FileUploadWorker.isUploading(remotePath, user.accountName)

    private fun OCUpload.hasFailed(): Boolean = uploadStatus == UploadStatus.UPLOAD_FAILED ||
        (uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && lastResult != UploadResult.UPLOADED)

    private fun awaitUntil(failureMessage: () -> String, condition: () -> Boolean) {
        val deadline = TimeSource.Monotonic.markNow() + AWAIT_TIMEOUT
        while (deadline.hasNotPassedNow()) {
            if (condition()) return
            shortSleep()
        }

        if (!condition()) fail(failureMessage())
    }

    private fun findUpload(remotePath: String): OCUpload? = uploadsStorageManager
        .getUploadsForAccount(user.accountName)
        .firstOrNull { it.remotePath == remotePath }

    private fun describeUploads(): String = uploadsStorageManager
        .getUploadsForAccount(user.accountName)
        .joinToString { "${it.remotePath} is ${it.uploadStatus} with ${it.lastResult}" }
}
