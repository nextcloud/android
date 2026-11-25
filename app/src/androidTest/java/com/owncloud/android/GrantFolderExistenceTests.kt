/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.utils.PowerManagementFactory
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class GrantFolderExistenceTests : AbstractOnServerIT() {

    private val root = "/autoupload/"
    private val yearFolder = root + "2026/"
    private val monthFolder = yearFolder + "07/"

    private val uploadsStorageManager = UploadsStorageManager(
        UserAccountManagerImpl.fromContext(targetContext),
        targetContext.contentResolver
    )

    private val powerManagementServiceMock = PowerManagementFactory.mock

    private lateinit var previousCapability: OCCapability

    @Before
    @Throws(IOException::class)
    fun before() {
        createDummyFiles()
        previousCapability = storageManager.getCapability(user.accountName)

        // UploadFileOperation picks its folder handling by server version, so it needs the real capabilities;
        // the smallest chunk size keeps the chunked test file small
        applyCapability(serverCapability())
    }

    @After
    fun restoreCapability() {
        applyCapability(previousCapability)
    }

    @Test
    fun testUploadFileThenDeleteYearFolderOnServerOnlyThenUploadAgainShouldRecreateMonthFolderAndReturnOk() {
        uploadAndAssertSuccess("first.txt")

        assertTrue("month folder should exist on server", existsOnServer(monthFolder))
        assertNotNull("month folder should be cached locally", storageManager.getFileByDecryptedRemotePath(monthFolder))

        removeOnServer(yearFolder)

        assertFalse("month folder should be deleted", existsOnServer(monthFolder))
        assertNotNull(
            "app still has a cached entry for the removed folder",
            storageManager.getFileByDecryptedRemotePath(monthFolder)
        )

        // attempting upload to the same folder
        val result = upload("nonEmpty.txt", monthFolder + "nonEmpty.txt")

        assertEquals(
            "upload must not fail with a conflict, the missing folder has to be recreated",
            RemoteOperationResult.ResultCode.OK,
            result.code
        )
        assertTrue("month folder should be recreated on server", existsOnServer(monthFolder))
        assertTrue("uploaded file should exist on server", existsOnServer(monthFolder + "nonEmpty.txt"))
    }

    @Test
    fun testUploadFileThenDeleteRootOnServerOnlyThenUploadAgainShouldRecreateAllFolderLevelsAndReturnOk() {
        uploadAndAssertSuccess("first.txt")

        removeOnServer(root)
        assertFalse(existsOnServer(root))

        val result = upload("nonEmpty.txt", monthFolder + "nonEmpty.txt")

        assertEquals(
            "every missing folder level has to be recreated",
            RemoteOperationResult.ResultCode.OK,
            result.code
        )
        assertTrue(existsOnServer(yearFolder))
        assertTrue(existsOnServer(monthFolder))
        assertTrue(existsOnServer(monthFolder + "nonEmpty.txt"))
    }

    @Test
    fun testChunkedUploadIntoMissingFoldersShouldCreateThemAndReturnOk() {
        val remoteFile = monthFolder + CHUNKED_FILE
        assertFalse("year folder must not exist before the upload", existsOnServer(yearFolder))

        val result = upload(CHUNKED_FILE, remoteFile, CHUNKED_FILE_LINE_COUNT)

        assertEquals(result.logMessage, RemoteOperationResult.ResultCode.OK, result.code)
        assertTrue(existsOnServer(yearFolder))
        assertTrue(existsOnServer(monthFolder))
        assertTrue(existsOnServer(remoteFile))
        assertNotNull(
            "month folder should be cached locally",
            storageManager.getFileByDecryptedRemotePath(monthFolder)
        )
    }

    @Test
    fun testChunkedUploadAfterDeletingCachedFolderOnServerShouldRecreateItAndReturnOk() {
        uploadAndAssertSuccess("first.txt")
        removeOnServer(yearFolder)
        assertNotNull(
            "app still has a cached entry for the removed folder",
            storageManager.getFileByDecryptedRemotePath(monthFolder)
        )

        val result = upload(CHUNKED_FILE, monthFolder + CHUNKED_FILE, CHUNKED_FILE_LINE_COUNT)

        assertEquals(result.logMessage, RemoteOperationResult.ResultCode.OK, result.code)
        assertTrue(existsOnServer(monthFolder))
        assertTrue(existsOnServer(monthFolder + CHUNKED_FILE))
    }

    @Test
    fun testChunkedUploadIntoMissingFolderWithoutFolderCreationShouldFailWithoutStartingChunkedUpload() {
        val remoteFile = monthFolder + CHUNKED_FILE

        // without a temporal copy the uploaded file stays in place, so its chunk session name can be derived
        val result = upload(
            CHUNKED_FILE,
            remoteFile,
            CHUNKED_FILE_LINE_COUNT,
            createRemoteFolder = false,
            localBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
        )

        assertEquals(result.logMessage, RemoteOperationResult.ResultCode.FILE_NOT_FOUND, result.code)
        val uploadedFile = File(FileStorageUtils.getTemporalPath(account.name), CHUNKED_FILE)
        assertFalse("chunked upload must not be started", chunkSessionExists(uploadedFile))
        assertFalse("missing folder must not be created", existsOnServer(monthFolder))
        assertFalse("file must not be uploaded", existsOnServer(remoteFile))
    }

    @Test
    fun testChunkedUploadIntoExistingFolderShouldReturnOk() {
        uploadAndAssertSuccess("first.txt")

        val result = upload(CHUNKED_FILE, monthFolder + CHUNKED_FILE, CHUNKED_FILE_LINE_COUNT)

        assertEquals(result.logMessage, RemoteOperationResult.ResultCode.OK, result.code)
        assertTrue(existsOnServer(monthFolder + CHUNKED_FILE))
    }

    @Test
    fun testSingleRequestUploadIntoMissingFoldersShouldReturnOk() {
        assertFalse("year folder must not exist before the upload", existsOnServer(yearFolder))

        uploadAndAssertSuccess("first.txt")

        assertTrue(existsOnServer(monthFolder))
    }

    @Test
    fun testChunkedUploadIntoMissingFoldersOnServerOlderThan32ShouldCreateThemAndReturnOk() {
        applyCapability(serverCapability().apply { versionMayor = OLDER_THAN_AUTO_MKCOL_VERSION })
        val remoteFile = monthFolder + CHUNKED_FILE

        val result = upload(CHUNKED_FILE, remoteFile, CHUNKED_FILE_LINE_COUNT)

        assertEquals(result.logMessage, RemoteOperationResult.ResultCode.OK, result.code)
        assertTrue(existsOnServer(monthFolder))
        assertTrue(existsOnServer(remoteFile))
    }

    private fun uploadAndAssertSuccess(filename: String) {
        val result = upload(filename, monthFolder + filename)
        assertTrue(result.logMessage, result.isSuccess)
        assertTrue("uploaded file should exist on server", existsOnServer(monthFolder + filename))
    }

    private fun removeOnServer(remotePath: String) {
        // the server keeps a transient lock on a just uploaded file, so a DELETE on one of its
        // parent folders answers 423 until that lock expires
        repeat(REMOVE_ATTEMPTS) {
            if (RemoveFileRemoteOperation(remotePath).execute(client).isSuccess) {
                return
            }

            shortSleep()
        }

        fail("$remotePath should be removed on server")
    }

    private fun existsOnServer(remotePath: String): Boolean =
        ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess

    private fun upload(
        localFileName: String,
        remotePath: String,
        lineCount: Int = FILE_LINE_COUNT,
        createRemoteFolder: Boolean = true,
        localBehaviour: Int = FileUploadWorker.LOCAL_BEHAVIOUR_COPY
    ): RemoteOperationResult<*> {
        val localFile = createFile(localFileName, lineCount)
        assertTrue("local file must exist before uploading", localFile.exists())
        if (lineCount == CHUNKED_FILE_LINE_COUNT) {
            assertTrue("file must be large enough for a chunked upload", localFile.length() > chunkSize())
        }

        val ocUpload = OCUpload(
            localFile.absolutePath,
            remotePath,
            account.name
        ).apply {
            isCreateRemoteFolder = createRemoteFolder
            createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        }

        return UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.ASK_USER,
            localBehaviour,
            targetContext,
            false,
            false,
            storageManager
        ).execute(client)
    }

    private fun serverCapability(): OCCapability =
        GetCapabilitiesRemoteOperation(null).execute(client).resultData.apply {
            accountName = user.accountName
            chunkedUploadMaxSize = ChunkedFileUploadRemoteOperation.MIN_CHUNK_SIZE
        }

    private fun applyCapability(capability: OCCapability) {
        storageManager.saveCapabilities(capability)
        CapabilityUtils.updateCapability(capability)
    }

    // the server names a chunked upload session after the uploaded file's name, modification time and size
    private fun chunkSessionExists(uploadedFile: File): Boolean {
        val sessionUri = "${client.uploadUri}/${client.userId}/${FileUtils.md5Sum(uploadedFile)}"
        val propFind = PropFindMethod(sessionUri, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0)
        return try {
            client.executeMethod(propFind)
            propFind.succeeded()
        } finally {
            propFind.releaseConnection()
        }
    }

    private fun chunkSize(): Long {
        val serverMaxChunkSize = CapabilityUtils.getCapability(user, targetContext).chunkedUploadMaxSize
        return ChunkedFileUploadRemoteOperation.chunkSize(false, serverMaxChunkSize)
    }

    companion object {
        private const val CHUNKED_FILE = "chunked.txt"
        private const val CHUNKED_FILE_LINE_COUNT = 500_000
        private const val OLDER_THAN_AUTO_MKCOL_VERSION = 31
        private const val FILE_LINE_COUNT = 100
        private const val REMOVE_ATTEMPTS = 5
    }
}
