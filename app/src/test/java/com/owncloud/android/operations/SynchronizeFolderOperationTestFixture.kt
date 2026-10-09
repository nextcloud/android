/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import android.text.TextUtils
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.folderDownload.FolderDownloadWorkerNotificationManager
import com.nextcloud.utils.share.UnifiedShareSharees
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.mockito.MockedConstruction
import org.mockito.Mockito

@Suppress("TooManyFunctions")
abstract class SynchronizeFolderOperationTestFixture {
    @get:Rule
    val localFolder = TemporaryFolder()

    protected lateinit var notifications: MockedConstruction<FolderDownloadWorkerNotificationManager>
    protected lateinit var downloads: MockedConstruction<DownloadFileOperation>
    protected lateinit var fileSynchronizations: MockedConstruction<SynchronizeFileOperation>
    protected val context: Context = mockk(relaxed = true)
    protected val user: User = mockk(relaxed = true)
    protected val client: OwnCloudClient = Mockito.mock(OwnCloudClient::class.java)
    protected val storage: FileDataStorageManager = mockk(relaxed = true)
    protected val remoteChildren = linkedMapOf<String, List<String>>()
    protected val cachedFiles = mutableMapOf<String, OCFile>()
    protected lateinit var rootOperation: SynchronizeFolderOperation
    protected val downloadHelper = mockk<FileDownloadHelper>(relaxed = true)
    protected var onFileSaved: () -> Unit = {}
    protected var continueSync = true
    protected var contentResult = RemoteOperationResult<Unit>(ResultCode.OK)
    protected var downloadResult = RemoteOperationResult<Unit>(ResultCode.OK)
    protected val visitedFolders = mutableListOf<String>()
    protected val filesToSynchronize = mutableListOf<String>()

    @Before
    fun setUp() {
        notifications = Mockito.mockConstruction(FolderDownloadWorkerNotificationManager::class.java)
        mockkConstructor(ReadFileRemoteOperation::class)
        mockkConstructor(ReadFolderRemoteOperation::class)
        downloads = Mockito.mockConstruction(DownloadFileOperation::class.java) { operation, _ ->
            Mockito.`when`(operation.execute(client)).thenAnswer { downloadResult }
        }
        mockkStatic("com.nextcloud.utils.extensions.ExtensionsKt")
        every { com.nextcloud.utils.extensions.mainThread(any(), any()) } just runs
        fileSynchronizations = Mockito.mockConstruction(
            SynchronizeFileOperation::class.java
        ) { operation, construction ->
            val file = (construction.arguments()[1] ?: construction.arguments()[0]) as OCFile
            Mockito.`when`(operation.execute(context)).thenAnswer {
                filesToSynchronize.add(file.remotePath)
                contentResult
            }
            Mockito.`when`(operation.localFile).thenReturn(file)
        }
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        mockkStatic(FileStorageUtils::class)
        mockkStatic(RefreshFolderOperation::class)
        mockkStatic(UnifiedShareSharees::class)
        mockkObject(FileDownloadHelper.Companion)
        every { FileDownloadHelper.instance() } returns downloadHelper
        every { downloadHelper.saveFile(any(), any(), any()) } answers {
            filesToSynchronize.add(firstArg<OCFile>().remotePath)
            onFileSaved()
        }
        every { UnifiedShareSharees.fillBlocking(any(), any()) } just runs
        every { FileStorageUtils.checkEncryptionStatus(any(), any()) } returns false
        every { FileStorageUtils.searchForLocalFileInDefaultPath(any(), any()) } just runs
        every { RefreshFolderOperation.getDecryptedFolderMetadata(any(), any(), any(), any(), any()) } returns null
        every { RefreshFolderOperation.prefillLocalFilesMap(any(), any()) } answers {
            secondArg<List<OCFile>>().associateBy { it.remotePath }.toMutableMap()
        }
        every { storage.getFileByPath(any()) } answers { cachedFiles[firstArg()] }
        every { storage.getFolderContent(any<OCFile>(), false) } answers {
            val parent = firstArg<OCFile>()
            cachedFiles.values.filter { it.parentId == parent.fileId }
        }
        every { storage.saveFolder(any(), any(), any()) } answers {
            secondArg<List<OCFile>>().forEach { file ->
                if (file.fileId == -1L) file.fileId = cachedFiles.size.toLong() + 1
                cachedFiles[file.remotePath] = file
            }
        }
    }

    @After
    fun tearDown() {
        fileSynchronizations.close()
        downloads.close()
        notifications.close()
        unmockkAll()
    }

    protected fun tree() {
        remoteFolder("/Root/", listOf("/Root/A/"))
        remoteFolder("/Root/A/", listOf("/Root/A/B/"))
        remoteFolder("/Root/A/B/", listOf(DEEP_FILE))
    }

    protected fun remoteFolder(path: String, children: List<String>) {
        remoteChildren[path] = children
        val remoteFolder = remoteFile(path)
        every { constructedWith<ReadFileRemoteOperation>(EqMatcher(path)).execute(client) } answers {
            assertNotNull("Folder must be persisted before synchronization: $path", cachedFiles[path])
            if (path !in visitedFolders) visitedFolders.add(path)
            result(listOf(remoteFolder))
        }
        every { constructedWith<ReadFolderRemoteOperation>(EqMatcher(path)).execute(client) } returns
            result(listOf(remoteFolder) + children.map(::remoteFile))
    }

    protected fun remoteFile(path: String): RemoteFile {
        val remote = mockk<RemoteFile> {
            every { etag } returns "server-etag"
        }
        every { FileStorageUtils.fillOCFile(remote) } answers { file(path) }
        return remote
    }

    protected fun result(files: List<RemoteFile>): RemoteOperationResult<Any> = mockk(relaxed = true) {
        every { isSuccess } returns true
        every { data } returns ArrayList<Any>(files)
        every { code } returns ResultCode.OK
    }

    protected fun file(path: String): OCFile = OCFile(path).apply {
        mimeType = if (path.endsWith('/')) MimeType.DIRECTORY else "text/plain"
        etag = "server-etag"
    }

    protected fun cacheFolder(path: String, children: List<String>) {
        val parent = cachedFiles.getOrPut(path) { file(path).apply { fileId = cachedFiles.size.toLong() + 1 } }
        children.forEach { childPath ->
            cachedFiles.getOrPut(childPath) {
                file(childPath).apply {
                    fileId = cachedFiles.size.toLong() + 1
                    parentId = parent.fileId
                    etag = ""
                }
            }
        }
        parent.etag = "server-etag"
    }

    protected fun synchronizeTree() {
        assertTrue(executeTree().isSuccess)
        verify(exactly = 0) { context.startService(any()) }
    }

    protected fun executeTree(): RemoteOperationResult<*> {
        rootOperation = SynchronizeFolderOperation.forOngoingSync(context, "/Root/", user, storage) { continueSync }
        return rootOperation.execute(client)
    }

    companion object {
        const val DEEP_FILE = "/Root/A/B/file.txt"
    }
}
