/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import androidx.annotation.VisibleForTesting
import com.google.gson.reflect.TypeToken
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.EncryptionUtils

object E2EVersionHelper {

    /**
     * Returns true if the given E2EE version is v2 or newer.
     */
    fun isV2Plus(capability: OCCapability): Boolean = isV2Plus(capability.endToEndEncryptionApiVersion)

    /**
     * Returns true if the given E2EE version is v2 or newer.
     */
    fun isV2Plus(version: E2EVersion): Boolean = version == E2EVersion.V2_0 || version == E2EVersion.V2_1

    /**
     * Returns true if the given E2EE version is v1.x.
     */
    fun isV1(capability: OCCapability): Boolean = isV1(capability.endToEndEncryptionApiVersion)

    /**
     * Returns true if the given E2EE version is v1.x.
     */
    fun isV1(version: E2EVersion): Boolean =
        version == E2EVersion.V1_0 || version == E2EVersion.V1_1 || version == E2EVersion.V1_2

    fun getMaxCompatibleE2EEVersion(serverE2EEVersion: E2EVersion): E2EVersion {
        if (serverE2EEVersion == E2EVersion.UNKNOWN) {
            return E2EVersion.UNKNOWN
        }

        val clientMax = E2EVersion.max()
        return minOf(serverE2EEVersion, clientMax)
    }

    /**
     * Use only for test
     */
    @VisibleForTesting
    fun getMaxCompatibleE2EEVersionFromString(value: String): E2EVersion {
        if (value == E2EVersion.UNKNOWN.value) {
            return E2EVersion.UNKNOWN
        }

        val clientMax = E2EVersion.max().value
        return E2EVersion.fromValue(minOf(value, clientMax))
    }

    fun fromMetadata(metadata: String): E2EVersion = runCatching {
        val v1 = EncryptionUtils.deserializeJSON(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFileV1>() {}
        )

        E2EVersion.fromValue(v1?.metadata?.version.toString()).also {
            if (it == E2EVersion.UNKNOWN) {
                throw IllegalStateException("Unknown V1 version")
            }
        }
    }.recoverCatching {
        val v2 = EncryptionUtils.deserializeJSON(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        E2EVersion.fromValue(v2.version)
    }.getOrDefault(E2EVersion.UNKNOWN)
}
