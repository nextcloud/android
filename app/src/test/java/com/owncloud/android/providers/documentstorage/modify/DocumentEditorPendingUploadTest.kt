/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.modify

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.providers.documentstorage.model.DocumentResolver
import com.owncloud.android.providers.documentstorage.open.PendingDocumentWrites
import com.owncloud.android.providers.documentstorage.open.PendingWriteState
import com.owncloud.android.providers.documentstorage.sync.FolderRefresher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.FileNotFoundException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DocumentEditorPendingUploadTest {

    private val context = mock<Context>()
    private val client = mock<OwnCloudClient>()
    private val user = mock<User> { on { accountName } doReturn ACCOUNT_NAME }
    private val storageManager = mock<FileDataStorageManager> { on { user } doReturn user }
    private val resolver = mock<DocumentResolver>()
    private val nameValidator = mock<DocumentNameValidator> {
        on { isValidFolderPath(any()) } doReturn true
    }
    private val success = mock<RemoteOperationResult<Any>> { on { isSuccess } doReturn true }
    private val folderRefresher = mock<FolderRefresher> {
        on { refresh(any(), any(), any(), any()) } doReturn success
    }
    private val pendingWrites = PendingDocumentWrites()

    private var uploadPending = false
    private var lastUploadProgress = 0L
    private var now = 0L
    private var polls = 0
    private var onPoll: () -> Unit = {}
    private val operationsRunWhilePending = mutableListOf<Boolean>()

    private val uploadHelper = mock<FileUploadHelper> {
        on { isUploadPending(any(), any()) } doAnswer { uploadPending }
        on { getLastUploadProgress(any()) } doAnswer { lastUploadProgress }
    }

    private val editor = DocumentEditor(
        context,
        resolver,
        mock(),
        folderRefresher,
        nameValidator,
        { uploadHelper },
        pendingWrites,
        elapsedRealtime = { now },
        sleep = { millis ->
            now += millis
            polls++
            onPoll()
        }
    )

    private val constructions = mutableListOf<MockedConstruction<*>>()

    @Before
    fun setUp() {
        givenDocument(SOURCE_ID, FILE_ID, FILE_PATH, isFolder = false)
        givenDocument(TARGET_FOLDER_ID, TARGET_FOLDER_FILE_ID, TARGET_FOLDER_PATH, isFolder = true)
        givenDocument(SOURCE_PARENT_ID, SOURCE_PARENT_FILE_ID, SOURCE_PARENT_PATH, isFolder = true)
        whenever(storageManager.getFileByEncryptedRemotePath(any())).thenReturn(mock<OCFile>())

        constructions += recordExecution(RenameFileOperation::class.java)
        constructions += recordExecution(MoveFileOperation::class.java)
        constructions += recordExecution(CopyFileOperation::class.java)
    }

    @After
    fun tearDown() {
        constructions.forEach { it.close() }
    }

    @Test
    fun `rename without pending upload renames immediately`() {
        editor.rename(SOURCE_ID, NEW_NAME)

        assertEquals(0, polls)
        assertEquals(listOf(false), operationsRunWhilePending)
    }

    @Test
    fun `rename, move and copy wait for queued upload`() {
        val operations = listOf(
            { editor.rename(SOURCE_ID, NEW_NAME) },
            { editor.move(SOURCE_ID, SOURCE_PARENT_ID, TARGET_FOLDER_ID) },
            { editor.copy(SOURCE_ID, TARGET_FOLDER_ID) }
        )

        operations.forEach { operation ->
            uploadPending = true
            polls = 0
            onPoll = { if (polls == 2) uploadPending = false }

            operation()

            assertEquals(2, polls)
        }
        assertEquals(listOf(false, false, false), operationsRunWhilePending)
    }

    @Test
    fun `rename waits for close that is not reported yet and then for the upload`() {
        val write = pendingWrites.begin(FILE_ID)
        onPoll = {
            when (polls) {
                1 -> pendingWrites.closed(write)
                2 -> pendingWrites.end(write)
            }
        }

        editor.rename(SOURCE_ID, NEW_NAME)

        assertEquals(2, polls)
        assertEquals(listOf(false), operationsRunWhilePending)
    }

    @Test
    fun `rename of document that stays open proceeds after close grace period`() {
        pendingWrites.begin(FILE_ID)

        editor.rename(SOURCE_ID, NEW_NAME)

        assertEquals(CLOSE_GRACE_PERIOD, now)
        assertEquals(listOf(false), operationsRunWhilePending)
    }

    @Test
    fun `rename waits for every concurrent write of the document`() {
        val first = pendingWrites.begin(FILE_ID)
        val second = pendingWrites.begin(FILE_ID)
        pendingWrites.closed(first)
        pendingWrites.closed(second)
        onPoll = {
            when (polls) {
                1 -> pendingWrites.end(first)
                2 -> pendingWrites.end(second)
            }
        }

        editor.rename(SOURCE_ID, NEW_NAME)

        assertEquals(2, polls)
        assertEquals(listOf(false), operationsRunWhilePending)
    }

    @Test
    fun `waiting continues past idle timeout while upload bytes are transferred`() {
        uploadPending = true
        onPoll = {
            lastUploadProgress = now
            if (now > 3 * IDLE_TIMEOUT) uploadPending = false
        }

        editor.rename(SOURCE_ID, NEW_NAME)

        assertEquals(listOf(false), operationsRunWhilePending)
    }

    @Test
    fun `rename fails without renaming when no upload bytes are transferred within idle timeout`() {
        uploadPending = true

        assertThrows(FileNotFoundException::class.java) { editor.rename(SOURCE_ID, NEW_NAME) }

        assertEquals(emptyList<Boolean>(), operationsRunWhilePending)
    }

    private fun givenDocument(documentId: String, fileId: Long, remotePath: String, isFolder: Boolean) {
        val file = givenFile(fileId, remotePath, isFolder)
        val document = mock<Document> {
            on { requireFile() } doReturn file
            on { user } doReturn user
            on { storageManager } doReturn storageManager
            on { requireClient(context) } doReturn client
        }
        whenever(resolver.toDocument(documentId)).thenReturn(document)
    }

    private fun givenFile(fileId: Long, remotePath: String, isFolder: Boolean): OCFile = mock {
        on { this.fileId } doReturn fileId
        on { this.remotePath } doReturn remotePath
        on { this.isFolder } doReturn isFolder
        on { fileName } doReturn remotePath.trimEnd('/').substringAfterLast('/')
        on { canRename() } doReturn true
        on { canMove() } doReturn true
    }

    private fun <T : RemoteOperation<*>> recordExecution(type: Class<T>): MockedConstruction<T> =
        mockConstruction(type) { operation, _ ->
            whenever(operation.execute(client)).thenAnswer {
                operationsRunWhilePending += uploadPending ||
                    PendingWriteState.CLOSED_UPLOAD_NOT_QUEUED in pendingWrites.states { true }
                success
            }
        }

    companion object {
        private const val ACCOUNT_NAME = "test@server"
        private const val NEW_NAME = "renamed.bin"
        private const val SOURCE_ID = "root;1"
        private const val TARGET_FOLDER_ID = "root;2"
        private const val SOURCE_PARENT_ID = "root;3"
        private const val FILE_ID = 1L
        private const val TARGET_FOLDER_FILE_ID = 2L
        private const val SOURCE_PARENT_FILE_ID = 3L
        private const val FILE_PATH = "/folder/.document.tmp"
        private const val TARGET_FOLDER_PATH = "/target/"
        private const val SOURCE_PARENT_PATH = "/folder/"
        private val IDLE_TIMEOUT = 10.minutes.inWholeMilliseconds
        private val CLOSE_GRACE_PERIOD = 5.seconds.inWholeMilliseconds
    }
}
