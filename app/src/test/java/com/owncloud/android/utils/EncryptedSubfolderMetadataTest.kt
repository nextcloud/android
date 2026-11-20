/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.content.Context
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import com.owncloud.android.lib.common.OwnCloudClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EncryptedSubfolderMetadataTest {
    private val sut = spy(EncryptionUtilsV2())
    private val folder: OCFile = mock()
    private val storageManager: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val user: User = mock()
    private val context: Context = mock()
    private val provider: ArbitraryDataProvider = mock()
    private val key = byteArrayOf(1, 2, 3)
    private val encryptedMetadata = EncryptedMetadata("ciphertext", "nonce", "tag")
    private val metadata = DecryptedFolderMetadataFile(
        DecryptedMetadata(metadataKey = key),
        version = VERSION
    )

    @Before
    fun setUp() {
        whenever(storageManager.getE2EEVersion(user)).thenReturn(VERSION)
    }

    @Test
    fun subfolderOmitsUsersSoWebClientCanInheritRootKey() {
        doReturn(key).whenever(sut).retrieveTopMostMetadataKey(
            folder,
            storageManager,
            client,
            USER_ID,
            PRIVATE_KEY,
            user,
            context,
            provider
        )
        doReturn(encryptedMetadata).whenever(sut).encryptMetadata(metadata.metadata, key)

        val encrypted = encryptFolder()
        val json = JsonParser.parseString(EncryptionUtils.serializeJSON(encrypted, true)).asJsonObject

        assertFalse(json.has("users"))
        assertEquals(VERSION, json["version"].asString)
        verify(sut).encryptMetadata(metadata.metadata, key)
    }

    @Test
    fun rootRetainsRecipientAndEncryptedMetadataKey() {
        val recipient = DecryptedUser(USER_ID, "certificate", null)
        val encryptedRecipient = EncryptedUser(USER_ID, "certificate", "encrypted-key")
        metadata.users.add(recipient)
        doReturn(encryptedRecipient).whenever(sut).encryptUser(recipient, key)
        doReturn(encryptedMetadata).whenever(sut).encryptMetadata(metadata.metadata, key)

        val json = JsonParser.parseString(EncryptionUtils.serializeJSON(encryptFolder(), true)).asJsonObject
        val users = json["users"].asJsonArray

        assertEquals(1, users.size())
        assertEquals(USER_ID, users[0].asJsonObject["userId"].asString)
        assertEquals("encrypted-key", users[0].asJsonObject["encryptedMetadataKey"].asString)
    }

    @Test
    fun readingExistingMetadataPreservesSignedRepresentationWithOrWithoutUsers() {
        val base = """{"metadata":{"ciphertext":"ciphertext","nonce":"nonce","authenticationTag":"tag"}"""
        listOf("", ",\"users\":[]").forEach { users ->
            val json = "$base$users,\"version\":\"$VERSION\"}"
            val parsed = EncryptionUtils.deserializeJSON(
                json,
                object : TypeToken<EncryptedFolderMetadataFile>() {}
            )

            assertEquals(json, EncryptionUtils.serializeJSON(parsed, true))
        }
    }

    private fun encryptFolder() = sut.encryptFolderMetadataFile(
        metadata, USER_ID, folder, storageManager, client, PRIVATE_KEY, user, context, provider
    )

    companion object {
        private const val USER_ID = "alice"
        private const val PRIVATE_KEY = "private-key"
        private const val VERSION = "2.1"
    }
}
