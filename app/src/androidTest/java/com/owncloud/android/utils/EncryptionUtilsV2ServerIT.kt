/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptionUtilsV2ServerIT : AbstractOnServerIT() {
    @Throws(Throwable::class)
    @Test
    fun testStoreAndUpdate() {
        val encryptionTestUtils = EncryptionTestUtils()
        val encryptionUtilsV2 = EncryptionUtilsV2()
        val arbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)

        // save keys
        arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            EncryptionUtils.PUBLIC_KEY,
            encryptionTestUtils.t1PublicKey
        )
        arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            EncryptionUtils.PRIVATE_KEY,
            encryptionTestUtils.t1PrivateKey
        )

        val folder = createFolder("/Android2/$randomName")

        // create metadata
        val metadataFile = encryptionTestUtils.generateFolderMetadataV2(client.userId, encryptionTestUtils.t1PublicKey)

        // lock folder
        var token = EncryptionUtils.lockFolder(folder, client)

        // store it
        encryptionUtilsV2.serializeAndUploadMetadata(
            folder,
            metadataFile,
            token,
            client,
            false,
            targetContext,
            user,
            storageManager
        )

        // unlock it
        EncryptionUtils.unlockFolder(folder, client, token)

        // compare it, check metadata key
        val compare = encryptionUtilsV2.retrieveMetadata(folder, client, user, targetContext)

        metadataFile.metadata.counter = metadataFile.metadata.counter + 1

        // lock it
        token = EncryptionUtils.lockFolder(folder, client, metadataFile.metadata.counter)

        // update it
        encryptionUtilsV2.serializeAndUploadMetadata(
            folder,
            metadataFile,
            token,
            client,
            true,
            targetContext,
            user,
            storageManager
        )

        // unlock it
        EncryptionUtils.unlockFolder(folder, client, token)

        // compare it, check metadata key
        val compare2 = encryptionUtilsV2.retrieveMetadata(folder, client, user, targetContext)

        compare.second.metadata.counter = compare.second.metadata.counter + 1
        assertEquals(compare.second, compare2.second)
    }
}
