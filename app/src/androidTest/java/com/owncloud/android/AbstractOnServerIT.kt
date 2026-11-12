/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import android.accounts.Account
import android.accounts.AccountManager
import android.net.Uri
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import java.io.File
import java.io.IOException

@Suppress("DEPRECATION")
abstract class AbstractOnServerIT : AbstractIT() {
    @After
    override fun after() {
        deleteAllFilesOnServer()
        super.after()
    }

    override fun uploadOCUpload(ocUpload: OCUpload) {
        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_COPY)
    }

    fun uploadOCUpload(ocUpload: OCUpload, localBehaviour: Int) {
        executeUpload(ocUpload, NameCollisionPolicy.OVERWRITE, localBehaviour)

        val remotePath = ocUpload.remotePath
        val parentPath = FileStorageUtils.getParentPath(remotePath)
            ?: throw AssertionError("$remotePath has no parent folder")
        val parentFolder = storageManager.getFileByEncryptedRemotePath(parentPath)
            ?: throw AssertionError("Parent folder $parentPath not found")
        val uploadedFile = storageManager.getFileByDecryptedRemotePath(
            parentFolder.decryptedRemotePath + File(remotePath).name
        ) ?: throw AssertionError("Uploaded file $remotePath not found")

        assertNotNull(uploadedFile.remoteId)
        assertNotNull(uploadedFile.permissions)

        if (localBehaviour in LOCAL_COPY_BEHAVIOURS) {
            assertTrue(File(uploadedFile.storagePath).exists())
        }
    }

    protected fun refreshFolder(path: String) {
        val operation = RefreshFolderOperation(
            storageManager.getFileByEncryptedRemotePath(path),
            System.currentTimeMillis(),
            false,
            false,
            storageManager,
            user,
            targetContext
        )
        assertTrue(operation.execute(client).isSuccess)
    }

    companion object {
        private const val SERVER_ACCOUNT_TYPE = "nextcloud"
        private const val SERVER_VERSION = "14.0.0.0"
        private const val ROOT_PATH = "/"
        private const val TEAM_FOLDER_PERMISSION = "M"
        private const val REMOVE_ATTEMPTS = 5
        private const val SERVER_READY_RETRIES = 3
        private const val SERVER_READY_RETRY_DELAY_MILLIS = 60_000L

        private var isServerUnreachable = false

        private val FOLDER_MIME_TYPES = setOf(MimeType.DIRECTORY, MimeType.WEBDAV_FOLDER)
        private val LOCAL_COPY_BEHAVIOURS = setOf(
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
        )

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            rethrowClientSetupFailure {
                targetContext = InstrumentationRegistry.getInstrumentation().targetContext
                removeAccountsOfType(SERVER_ACCOUNT_TYPE)

                val arguments = InstrumentationRegistry.getArguments()
                val baseUrl = Uri.parse(arguments.requireString("TEST_SERVER_URL"))
                val loginName = arguments.requireString("TEST_SERVER_USERNAME")
                val password = arguments.requireString("TEST_SERVER_PASSWORD")
                val accountName = "$loginName@$baseUrl"

                addServerAccountIfMissing(accountName, loginName, password, baseUrl)

                account = getRegisteredAccount(accountName)
                user = getUser(account)
                client = OwnCloudClientFactory.createOwnCloudClient(account, targetContext)
                nextcloudClient = OwnCloudClientFactory.createNextcloudClient(user, targetContext)

                createDummyFiles()

                waitForServer(client, baseUrl)
            }
        }

        private fun addServerAccountIfMissing(accountName: String, loginName: String, password: String, baseUrl: Uri) {
            val platformAccount = Account(accountName, MainApp.getAccountType(targetContext))
            if (UserAccountManagerImpl.fromContext(targetContext).exists(platformAccount)) return

            AccountManager.get(targetContext).run {
                addAccountExplicitly(platformAccount, password, null)
                setUserData(platformAccount, KEY_OC_ACCOUNT_VERSION, UserAccountManager.ACCOUNT_VERSION.toString())
                setUserData(platformAccount, KEY_OC_VERSION, SERVER_VERSION)
                setUserData(platformAccount, KEY_OC_BASE_URL, baseUrl.toString())
                setUserData(platformAccount, KEY_USER_ID, loginName)
            }
        }

        private fun isFolder(file: RemoteFile): Boolean = file.mimeType in FOLDER_MIME_TYPES

        private fun isTeamFolder(file: RemoteFile): Boolean = file.permissions?.contains(TEAM_FOLDER_PERMISSION) == true

        private fun deleteAllFilesOnServer() {
            val result = ReadFolderRemoteOperation(ROOT_PATH).execute(client)
            assertTrue(result.getLogMessage(targetContext), result.isSuccess)

            result.data
                .filterIsInstance<RemoteFile>()
                .filterNot { it.remotePath == ROOT_PATH }
                .forEach(::deleteRemoteFile)
        }

        private fun deleteRemoteFile(remoteFile: RemoteFile) {
            if (remoteFile.isEncrypted) {
                val isEncryptionDisabled = disableEncryption(remoteFile)

                if (!isEncryptionDisabled && isFolder(remoteFile)) return

                assertTrue(isEncryptionDisabled)
            }

            if (isTeamFolder(remoteFile)) return

            assertTrue("Remove of ${remoteFile.remotePath} failed", removeWithRetry(remoteFile))
        }

        private fun disableEncryption(remoteFile: RemoteFile): Boolean =
            ToggleEncryptionRemoteOperation(remoteFile.localId, remoteFile.remotePath, false)
                .execute(client)
                .isSuccess

        private fun removeWithRetry(remoteFile: RemoteFile): Boolean {
            repeat(REMOVE_ATTEMPTS) {
                if (RemoveFileRemoteOperation(remoteFile.remotePath).execute(client).isSuccess) return true
                shortSleep()
            }
            return false
        }

        private fun waitForServer(client: OwnCloudClient, baseUrl: Uri) {
            assertFalse("Server not ready! Already unreachable for a previous test class", isServerUnreachable)
            val statusRequest = GetMethod("$baseUrl/status.php")

            repeat(SERVER_READY_RETRIES) {
                if (isServerReady(client, statusRequest)) return
                println("wait…")
                sleepIgnoringInterrupt(SERVER_READY_RETRY_DELAY_MILLIS)
            }

            isServerUnreachable = !isServerReady(client, statusRequest)
            assertFalse("Server not ready!", isServerUnreachable)
        }

        private fun isServerReady(client: OwnCloudClient, statusRequest: GetMethod): Boolean = try {
            client.executeMethod(statusRequest) == HttpStatus.SC_OK
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }

        private fun Bundle.requireString(key: String): String =
            requireNotNull(getString(key)) { "Missing instrumentation argument $key" }
    }
}
