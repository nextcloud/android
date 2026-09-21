/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.operations.CreateFolderOperation
import org.junit.After
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
        // Check keys
        val state = e2eeActionResolver.checkKeys()
        // Create folder
        assertTrue(createEncryptedFolder(FOLDER).isSuccess)
        // Open folder

    }

    @Test
    fun testReadEncryptedSubfolder() {

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
    fun testUploadInEncryptedFolder() {
    }

    @Test
    fun testUploadInEncryptedSubfolder() {

    }

    @Test
    fun testEncryptExistingFolder() {

    }

    @After
    fun cleanup() {
        storageManager.deleteAllFiles()
    }

    @Before
    fun encryptionSetup() {
        //val privateKey: String = EncryptionKeyGenerator.generatePrivateKey(targetContext, user ?: return, KEYWORDS)
        val capability = storageManager.getCapability(user.accountName)
        if (capability.endToEndEncryption.isFalse || capability.endToEndEncryption.isUnknown) {
            Log_OC.e(TAG, "Server does not support E2EE")
        }
    }

    private fun createEncryptedFolder(remotePath: String): RemoteOperationResult<*> {
        return CreateFolderOperation(remotePath, user, targetContext, storageManager).apply {
            setEncrypt(true)
        }.execute(client)
    }

    private fun listEncryptedFolder() {

    }


}
