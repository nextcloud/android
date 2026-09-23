/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android

import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.network.NetworkModule
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.e2e.E2EDeletionService
import com.owncloud.android.ui.dialog.setupEncryption.EncryptionKeyGenerator
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
        val TAG = EncryptedFoldersIT::class.simpleName
        val FOLDER = "/encryptedFolder/"
        val SUBFOLDER = "${FOLDER}encryptedSubfolder/"
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

    @Test
    fun testCreateEncryptedFolder() {
        val result = createEncryptedFolder(FOLDER)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testCreateEncryptedSubfolder() {
        // Create parent folder only for Nextcloud < 32; 32+ handles this automatically.
        if (capability.version.isOlderThan(NextcloudVersion.nextcloud_32)) {
            assertTrue(createEncryptedFolder(FOLDER).isSuccess)
        }
        val result = createEncryptedFolder(SUBFOLDER)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testReadEncryptedFolder() {
        val remotePath = FOLDER
        // Create folder
        assertTrue(createEncryptedFolder(remotePath).isSuccess)
        val files = listEncryptedFolder(remotePath)
        assertEquals(files.size, 0)
    }

    @Test
    fun testReadEncryptedSubfolder() {
        val remotePath = SUBFOLDER
        // Create folder
        assertTrue(createEncryptedFolder(remotePath).isSuccess)
        val files = listEncryptedFolder(remotePath)
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
        val capability = storageManager.getCapability(user.accountName)
        if (capability.endToEndEncryption.isFalse || capability.endToEndEncryption.isUnknown) {
            Log_OC.e(TAG, "Server does not support E2EE")
        }
        // Delete existing encryption key, if any
        E2EDeletionService(NetworkModule().clientFactory(targetContext)).deleteKeysAndFiles(user)
        // Create new encryption key
        val privateKey: String = runBlocking {
            EncryptionKeyGenerator(targetContext, user).generatePrivateKey(KEYWORDS)
        }
        // Check the key was generated
        assertNotEquals(privateKey, "")
    }

    @After
    fun encryptionCleanup() {
        // Delete existing encryption key, if any
        E2EDeletionService(NetworkModule().clientFactory(targetContext)).deleteKeysAndFiles(user)
    }

    private fun createEncryptedFolder(remotePath: String): RemoteOperationResult<*> =
        CreateFolderOperation(remotePath, user, targetContext, storageManager).apply {
            setEncrypt(true)
        }.execute(client)

    private fun listEncryptedFolder(remotePath: String): List<FileEntity> {
        // Obtain folder id
        val ocFile = storageManager.getFileByRemotePath(remotePath)
        assertNotNull(ocFile)
        // Open folder
        return runBlocking {
            storageManager.fileDao.getFolderContentSuspended(ocFile!!.fileId)
        }
    }
}
