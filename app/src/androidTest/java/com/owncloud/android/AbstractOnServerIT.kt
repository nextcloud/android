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
import com.owncloud.android.utils.MimeType
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import java.io.File
import java.io.IOException

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

        val remoteFile = File(ocUpload.remotePath)
        val parentFolder = storageManager.getFileByEncryptedRemotePath(remoteFile.parent + "/")
        val uploadedFile = storageManager.getFileByDecryptedRemotePath(
            parentFolder.decryptedRemotePath + remoteFile.name
        )

        assertNotNull(uploadedFile)
        assertNotNull(uploadedFile!!.remoteId)
        assertNotNull(uploadedFile.permissions)

        if (localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_COPY ||
            localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
        ) {
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
        private const val SERVER_READY_ATTEMPTS = 3
        private const val SERVER_READY_RETRY_DELAY_MILLIS = 60_000L

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            rethrowClientSetupFailure {
                targetContext = InstrumentationRegistry.getInstrumentation().targetContext
                removeAccountsOfType(SERVER_ACCOUNT_TYPE)

                val arguments = InstrumentationRegistry.getArguments()
                val baseUrl = Uri.parse(arguments.getString("TEST_SERVER_URL"))
                val loginName = arguments.getString("TEST_SERVER_USERNAME")
                val password = arguments.getString("TEST_SERVER_PASSWORD")
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

        private fun addServerAccountIfMissing(
            accountName: String,
            loginName: String?,
            password: String?,
            baseUrl: Uri
        ) {
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

        private fun isFolder(file: RemoteFile): Boolean =
            MimeType.DIRECTORY == file.mimeType || MimeType.WEBDAV_FOLDER == file.mimeType

        private fun isTeamFolder(file: RemoteFile): Boolean = file.permissions!!.contains(TEAM_FOLDER_PERMISSION)

        @JvmStatic
        fun deleteAllFilesOnServer() {
            val result = ReadFolderRemoteOperation(ROOT_PATH).execute(client)
            assertTrue(result.getLogMessage(targetContext), result.isSuccess)

            for (data in result.data) {
                val remoteFile = data as RemoteFile
                if (remoteFile.remotePath != ROOT_PATH) {
                    deleteRemoteFile(remoteFile)
                }
            }
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
            val get = GetMethod("$baseUrl/status.php")

            try {
                var attempts = 0
                while (client.executeMethod(get) != HttpStatus.SC_OK && attempts < SERVER_READY_ATTEMPTS) {
                    println("wait…")
                    Thread.sleep(SERVER_READY_RETRY_DELAY_MILLIS)
                    attempts++
                }

                if (attempts == SERVER_READY_ATTEMPTS) {
                    fail("Server not ready!")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
