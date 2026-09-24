/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.network.NetworkModule
import com.nextcloud.utils.e2ee.E2EEActionResolver
import com.nextcloud.utils.e2ee.E2EEKeyInspector
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.operations.e2e.E2EDeletionService
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.EncryptionKeyGenerator
import com.owncloud.android.utils.EncryptionUtils
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

open class EncryptedFoldersIT : AbstractOnServerIT() {
    companion object {
        val FOLDER = "/encryptedFolder/"
        val SUBFOLDER = "encryptedSubfolder/"
        val KEYWORDS = arrayListOf(
            "ability",
            "able",
            "about",
            "above",
            "absent",
            "absorb",
            "abstract",
            "absurd",
            "abuse",
            "access",
            "accident",
            "account",
            "accuse"
        )
    }

    private var e2eeActionResolver: E2EEActionResolver
    private var encryptionKeyGenerator: EncryptionKeyGenerator

    init {
        val accountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)
        val inspector = E2EEKeyInspector(
            targetContext,
            storageManager,
            CertificateValidator(),
            arbitraryDataProvider,
            accountManager
        )
        e2eeActionResolver = E2EEActionResolver(
            storageManager,
            arbitraryDataProvider,
            accountManager,
            connectivityServiceMock,
            inspector
        )
        encryptionKeyGenerator = EncryptionKeyGenerator(targetContext, user)
    }

    @Test
    fun testCreateEncryptedFolder() {
        createEncryptedFolder(FOLDER)
    }

    @Test
    fun testCreateEncryptedSubfolder() {
        val parent = createEncryptedFolder(FOLDER)
        createEncryptedSubfolder(SUBFOLDER, parent)
    }

    @Test
    fun testReadEncryptedFolder() {
        val remotePath = FOLDER
        val ocFile = createEncryptedFolder(remotePath)
        val files = listEncryptedFolder(ocFile)
        assertEquals(files.size, 0)
    }

    @Test
    fun testReadEncryptedSubfolder() {
        createEncryptedFolder(FOLDER)
        val subOCFile = createEncryptedFolder(SUBFOLDER)
        val files = listEncryptedFolder(subOCFile)
        assertEquals(files.size, 0)
    }

    @Test
    fun testUpdateEncryptedFolder() {
        // Rename folder
    }

    @Test
    fun testUpdateEncryptedSubfolder() {
    }

    @Test
    fun testDeleteEncryptedFolder() {
    }

    @Test
    fun testDeleteEncryptedSubfolder() {
    }

    @Test
    fun testEncryptExistingFolder() {
    }

    @Before
    fun encryptionSetup() {
        // Fetch capability
        val capability = GetCapabilitiesRemoteOperation(null).execute(client).getResultData()
        storageManager.saveCapabilities(capability)

        // Check if server supports end2end capability
        assertTrue(capability.endToEndEncryption.isTrue)

        // Delete existing encryption key, if any
        assertTrue(
            E2EDeletionService(NetworkModule().clientFactory(targetContext)).deleteKeysAndFiles(user)
        )

        // Create new encryption key
        val privateKey: String = runBlocking {
            encryptionKeyGenerator.generatePrivateKey(KEYWORDS)
        }

        // Check the key was generated
        assertNotEquals(privateKey, "")
    }

    @After
    fun encryptionCleanup() {
        // Delete existing encryption key, if any
        assertTrue(
            E2EDeletionService(NetworkModule().clientFactory(targetContext)).deleteKeysAndFiles(user)
        )
    }

    private fun createEncryptedFolder(remotePath: String): OCFile {
        val created = CreateFolderOperation(remotePath, user, targetContext, storageManager).apply {
            setEncrypt(true)
        }.execute(client)
        assertTrue(created.toString(), created.isSuccess)

        val ocFile = storageManager.getFileByRemotePath(remotePath)
        assertNotNull(ocFile)
        val encrypted = ToggleEncryptionRemoteOperation(ocFile!!.localId, remotePath, true)
            .execute(client)
        assertTrue(encrypted.toString(), encrypted.isSuccess)

        val publicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY)
        val privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY)
        val uploadedMetadata = encryptionKeyGenerator.uploadEncryptedFolderMetadata(
            ocFile,
            client,
            publicKey,
            privateKey,
            storageManager,
            arbitraryDataProvider
        )
        assertTrue(uploadedMetadata)

        // Set file as encrypted locally
        ocFile.isEncrypted = true
        assertTrue(storageManager.saveFile(ocFile))

        return ocFile
    }

    fun createEncryptedSubfolder(folderName: String, parent: OCFile) {
        // An encrypted subfolder is a normal folder inside an encrypted one
        assertTrue(parent.isFolder && parent.isEncrypted)

        // Create folder
        val path = "${parent.remotePath}${OCFile.PATH_SEPARATOR}${folderName}${OCFile.PATH_SEPARATOR}"
        val syncOp: SyncOperation = CreateFolderOperation(
            path,
            user,
            targetContext,
            storageManager
        )
        val result = syncOp.execute(client)
        assertTrue(result.toString(), result.isSuccess)

        // Check folder exists
        val ocFile = storageManager.getFileByRemotePath(path)
        assertTrue(ocFile?.isFolder ?: false)
    }

    private fun listEncryptedFolder(ocFile: OCFile): List<FileEntity> {
        assertNotNull(ocFile)
        val parent = storageManager.getFileById(ocFile.parentId)
        assertNotNull(parent)

        // Refresh folder
        val refreshResult = RefreshFolderOperation(
            parent,
            System.currentTimeMillis(),
            false,
            false,
            storageManager,
            user,
            targetContext
        ).execute(client)
        assertTrue(refreshResult.toString(), refreshResult.isSuccess)

        // Check folder metadata
        runBlocking {
            assertTrue(e2eeActionResolver.checkFolderMetadataKey(ocFile))
        }

        // Open folder
        return runBlocking {
            storageManager.fileDao.getFolderContentSuspended(ocFile.fileId)
        }
    }
}
