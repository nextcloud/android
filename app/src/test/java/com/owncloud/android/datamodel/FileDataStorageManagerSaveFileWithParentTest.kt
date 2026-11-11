/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.content.Context
import android.text.TextUtils
import com.nextcloud.client.account.User
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.OfflineOperationDao
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.model.OfflineOperationType
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RemoteOperationFailedException
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.nextcloud.common.User as LibraryUser

class FileDataStorageManagerSaveFileWithParentTest {

    companion object {
        private const val ROOT_ID = 1L
        private const val LOCAL_PARENT_ID = 42L
        private const val FIRST_GENERATED_ID = 100L
        private const val LOG_MESSAGE = "server unreachable"
        private const val LOCAL_UPLOAD_PATH = "/storage/emulated/0/upload"
    }

    private lateinit var manager: FileDataStorageManager
    private lateinit var context: Context
    private val localFiles = mutableMapOf<String, OCFile>()
    private val savedFiles = mutableListOf<OCFile>()
    private val queuedOperations = mutableListOf<OfflineOperationEntity>()
    private var nextFileId = FIRST_GENERATED_ID

    @Before
    fun setUp() {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { arg<CharSequence?>(0).isNullOrEmpty() }

        context = mockk(relaxed = true)
        mockkObject(MainApp.Companion)
        every { MainApp.getAppContext() } returns context

        mockkStatic(MimeTypeUtil::class)
        every { MimeTypeUtil.getMimeTypeFromPath(any()) } returns "text/plain"

        val offlineOperationDao = mockk<OfflineOperationDao>(relaxed = true)
        every { offlineOperationDao.insert(any()) } answers {
            queuedOperations.addAll(
                args.flatMap { if (it is Array<*>) it.toList() else listOf(it) }
                    .filterIsInstance<OfflineOperationEntity>()
            )
        }

        val db = mockk<NextcloudDatabase>(relaxed = true)
        every { db.offlineOperationDao() } returns offlineOperationDao
        mockkObject(NextcloudDatabase.Companion)
        every { NextcloudDatabase.getInstance(any()) } returns db
        every { NextcloudDatabase.instance() } returns db

        mockkConstructor(ReadFileRemoteOperation::class)

        manager = spyk(FileDataStorageManager(mockk<User>(relaxed = true), null as ContentResolver?))
        every { manager.getFileByEncryptedRemotePath(any()) } answers { localFiles[firstArg()] }
        every { manager.getFileByDecryptedRemotePath(any()) } answers { localFiles[firstArg()] }
        every { manager.saveFile(any()) } answers {
            val file = firstArg<OCFile>()
            if (file.fileId == -1L) {
                file.fileId = nextFileId++
            }
            savedFiles.add(file)
            localFiles[file.remotePath] = file
            false
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `root folder is saved without resolving a parent`() {
        val root = OCFile(OCFile.ROOT_PATH).apply { mimeType = MimeType.DIRECTORY }

        manager.saveFileWithParent(root, context)

        assertEquals(0L, root.parentId)
        assertEquals(listOf(root), savedFiles)
        verify(exactly = 0) { manager.getFileByEncryptedRemotePath(any()) }
        verifyNoServerFetch()
    }

    @Test
    fun `file with a parent id is saved without resolving a parent`() {
        val file = OCFile("/documents/report.pdf").apply { parentId = LOCAL_PARENT_ID }

        manager.saveFileWithParent(file, context)

        assertEquals(LOCAL_PARENT_ID, file.parentId)
        assertEquals(listOf(file), savedFiles)
        verify(exactly = 0) { manager.getFileByEncryptedRemotePath(any()) }
        verifyNoServerFetch()
    }

    @Test
    fun `file gets the id of its locally stored parent and the same instance is returned`() {
        givenLocalFolder("/documents/", LOCAL_PARENT_ID)
        val file = OCFile("/documents/report.pdf")

        val result = manager.saveFileWithParent(file, context)

        assertSame(file, result)
        assertEquals(LOCAL_PARENT_ID, file.parentId)
        assertEquals(listOf(file), savedFiles)
        verifyNoServerFetch()
    }

    @Test
    fun `file in root gets the id of the locally stored root`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)
        val file = OCFile("/report.pdf")

        manager.saveFileWithParent(file, context)

        assertEquals(ROOT_ID, file.parentId)
        verifyNoServerFetch()
    }

    @Test
    fun `folder path with trailing separator resolves its parent folder`() {
        givenLocalFolder("/documents/", LOCAL_PARENT_ID)
        val folder = OCFile("/documents/invoices/").apply { mimeType = MimeType.DIRECTORY }

        manager.saveFileWithParent(folder, context)

        assertEquals(LOCAL_PARENT_ID, folder.parentId)
        verifyNoServerFetch()
    }

    @Test
    fun `encrypted file resolves its parent from the encrypted remote path`() {
        givenLocalFolder("/A1B2C3/", LOCAL_PARENT_ID)
        val file = OCFile("/A1B2C3/D4E5F6").apply {
            isEncrypted = true
            decryptedRemotePath = "/secret/photo.jpg"
        }

        manager.saveFileWithParent(file, context)

        assertEquals(LOCAL_PARENT_ID, file.parentId)
        verify(exactly = 0) { manager.getFileByEncryptedRemotePath("/secret/") }
        verifyNoServerFetch()
    }

    @Test
    fun `missing parent is fetched from the server and saved before the file`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)
        givenServerFolder("/documents/")
        val file = OCFile("/documents/report.pdf")

        manager.saveFileWithParent(file, context)

        assertEquals(listOf("/documents/", "/documents/report.pdf"), savedFiles.map { it.remotePath })
        val fetchedParent = savedFiles.first()
        assertEquals(ROOT_ID, fetchedParent.parentId)
        assertEquals(fetchedParent.fileId, file.parentId)
    }

    @Test
    fun `all missing ancestors are fetched and saved from the top down`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)
        givenServerFolder("/a/")
        givenServerFolder("/a/b/")
        val file = OCFile("/a/b/c.txt")

        manager.saveFileWithParent(file, context)

        assertEquals(listOf("/a/", "/a/b/", "/a/b/c.txt"), savedFiles.map { it.remotePath })
        val (folderA, folderB) = savedFiles
        assertEquals(ROOT_ID, folderA.parentId)
        assertEquals(folderA.fileId, folderB.parentId)
        assertEquals(folderB.fileId, file.parentId)
    }

    @Test
    fun `missing root is fetched and saved so its child gets a valid parent id`() {
        givenServerFolder(OCFile.ROOT_PATH)
        val file = OCFile("/report.pdf")

        manager.saveFileWithParent(file, context)

        assertEquals(listOf(OCFile.ROOT_PATH, "/report.pdf"), savedFiles.map { it.remotePath })
        assertEquals(FIRST_GENERATED_ID, file.parentId)
    }

    @Test
    fun `failed fetch with an exception throws it as the cause and saves nothing`() {
        val cause = IllegalStateException("timeout")
        givenServerResult("/documents/", serverResult(isSuccess = false, exception = cause))

        val thrown = assertThrows(RemoteOperationFailedException::class.java) {
            manager.saveFileWithParent(OCFile("/documents/report.pdf"), context)
        }

        assertSame(cause, thrown.cause)
        assertTrue(thrown.message.orEmpty().contains("/documents/"))
        assertTrue(savedFiles.isEmpty())
    }

    @Test
    fun `failed fetch without an exception throws without a cause`() {
        givenServerResult("/documents/", serverResult(isSuccess = false))

        val thrown = assertThrows(RemoteOperationFailedException::class.java) {
            manager.saveFileWithParent(OCFile("/documents/report.pdf"), context)
        }

        assertNull(thrown.cause)
        assertTrue(thrown.message.orEmpty().contains(LOG_MESSAGE))
        assertTrue(savedFiles.isEmpty())
    }

    @Test
    fun `successful fetch with empty data throws`() {
        givenServerResult("/documents/", serverResult(isSuccess = true, data = arrayListOf()))

        assertThrows(RemoteOperationFailedException::class.java) {
            manager.saveFileWithParent(OCFile("/documents/report.pdf"), context)
        }
        assertTrue(savedFiles.isEmpty())
    }

    @Test
    fun `successful fetch with null data throws`() {
        givenServerResult("/documents/", serverResult(isSuccess = true, data = null))

        assertThrows(RemoteOperationFailedException::class.java) {
            manager.saveFileWithParent(OCFile("/documents/report.pdf"), context)
        }
        assertTrue(savedFiles.isEmpty())
    }

    @Test
    fun `successful fetch without a remote file throws`() {
        givenServerResult("/documents/", serverResult(isSuccess = true, data = arrayListOf("not a remote file")))

        assertThrows(RemoteOperationFailedException::class.java) {
            manager.saveFileWithParent(OCFile("/documents/report.pdf"), context)
        }
        assertTrue(savedFiles.isEmpty())
    }

    @Test
    fun `offline file in root queues no create folder operation`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)

        queueOfflineUpload("/report.pdf")

        assertEquals(listOf("/report.pdf"), queuedOperations.map { it.path })
        assertEquals(ROOT_ID, queuedOperations.single().parentOCFileId)
        verifyNoServerFetch()
    }

    @Test
    fun `offline file in a locally stored folder queues no create folder operation`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)
        givenLocalFolder("/documents/", LOCAL_PARENT_ID)

        queueOfflineUpload("/documents/report.pdf")

        assertEquals(listOf("/documents/report.pdf"), queuedOperations.map { it.path })
        assertEquals(LOCAL_PARENT_ID, queuedOperations.single().parentOCFileId)
        verifyNoServerFetch()
    }

    @Test
    fun `missing parent of an offline file is queued as a create folder operation before the file`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)

        queueOfflineUpload("/documents/report.pdf")

        val (folderOperation, fileOperation) = queuedOperations
        assertEquals("/documents/", folderOperation.path)
        assertEquals("documents", folderOperation.filename)
        assertEquals(ROOT_ID, folderOperation.parentOCFileId)
        assertTrue(folderOperation.type is OfflineOperationType.CreateFolder)
        assertTrue(fileOperation.type is OfflineOperationType.CreateFile)
        assertEquals(localFiles.getValue("/documents/").fileId, fileOperation.parentOCFileId)
        verifyNoServerFetch()
    }

    @Test
    fun `all missing ancestors of an offline file are queued from the top down`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)

        queueOfflineUpload("/a/b/c.txt")

        assertEquals(listOf("/a/", "/a/b/", "/a/b/c.txt"), queuedOperations.map { it.path })
        val (folderA, folderB, file) = queuedOperations
        assertEquals(ROOT_ID, folderA.parentOCFileId)
        assertEquals(localFiles.getValue("/a/").fileId, folderB.parentOCFileId)
        assertEquals(localFiles.getValue("/a/b/").fileId, file.parentOCFileId)
        verifyNoServerFetch()
    }

    @Test
    fun `missing ancestors are stored as pending local folders`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)

        queueOfflineUpload("/a/b/c.txt")

        val folderA = localFiles.getValue("/a/")
        val folderB = localFiles.getValue("/a/b/")
        assertTrue(folderA.isFolder && folderB.isFolder)
        assertEquals(ROOT_ID, folderA.parentId)
        assertEquals(folderA.fileId, folderB.parentId)
        assertEquals(folderB.fileId, localFiles.getValue("/a/b/c.txt").parentId)
    }

    @Test
    fun `only folders below the deepest locally stored folder are queued`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)
        givenLocalFolder("/documents/", LOCAL_PARENT_ID)

        queueOfflineUpload("/documents/x/y/z.txt")

        assertEquals(
            listOf("/documents/x/", "/documents/x/y/", "/documents/x/y/z.txt"),
            queuedOperations.map { it.path }
        )
        assertEquals(LOCAL_PARENT_ID, queuedOperations.first().parentOCFileId)
    }

    @Test
    fun `offline files sharing a missing parent queue that folder only once`() {
        givenLocalFolder(OCFile.ROOT_PATH, ROOT_ID)

        queueOfflineUpload("/documents/first.pdf", "/documents/second.pdf")

        assertEquals(
            listOf("/documents/", "/documents/first.pdf", "/documents/second.pdf"),
            queuedOperations.map { it.path }
        )
        verifyNoServerFetch()
    }

    private fun queueOfflineUpload(vararg remotePaths: String) {
        val localPaths = remotePaths.map { LOCAL_UPLOAD_PATH + it }.toTypedArray()
        manager.addCreateFileOfflineOperation(localPaths, arrayOf(*remotePaths))
    }

    private fun givenLocalFolder(path: String, id: Long) {
        localFiles[path] = OCFile(path).apply {
            fileId = id
            parentId = if (path == OCFile.ROOT_PATH) 0L else ROOT_ID
            mimeType = MimeType.DIRECTORY
        }
    }

    private fun givenServerFolder(path: String) {
        val remoteFolder = RemoteFile(path).apply { mimeType = MimeType.DIRECTORY }
        givenServerResult(path, serverResult(isSuccess = true, data = arrayListOf(remoteFolder)))
    }

    private fun givenServerResult(path: String, result: RemoteOperationResult<Any>) {
        every {
            constructedWith<ReadFileRemoteOperation>(EqMatcher(path)).execute(any<LibraryUser>(), any())
        } returns result
    }

    private fun serverResult(
        isSuccess: Boolean,
        data: ArrayList<Any>? = null,
        exception: Exception? = null
    ): RemoteOperationResult<Any> = mockk(relaxed = true) {
        every { this@mockk.isSuccess } returns isSuccess
        every { this@mockk.data } returns data
        every { this@mockk.exception } returns exception
        every { getLogMessage(any()) } returns LOG_MESSAGE
    }

    private fun verifyNoServerFetch() {
        verify(exactly = 0) { anyConstructed<ReadFileRemoteOperation>().execute(any<LibraryUser>(), any()) }
    }
}
