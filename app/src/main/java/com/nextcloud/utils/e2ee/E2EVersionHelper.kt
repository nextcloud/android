/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.google.gson.reflect.TypeToken
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.utils.EncryptionUtils

object E2EVersionHelper {

    fun isV2orAbove(version: E2EVersion): Boolean = version == E2EVersion.V2_0 || version == E2EVersion.V2_1

    fun isV1(version: E2EVersion): Boolean =
        version == E2EVersion.V1_0 || version == E2EVersion.V1_1 || version == E2EVersion.V1_2

    fun getLatestE2EVersion(isV2: Boolean): E2EVersion = if (isV2) {
        E2EVersion.V2_1
    } else {
        E2EVersion.V1_2
    }

    fun determineE2EFromVersionString(version: String?): E2EVersion = when (version?.trim()) {
        "1.0" -> E2EVersion.V1_0
        "1.1" -> E2EVersion.V1_1
        "1.2" -> E2EVersion.V1_2
        "2", "2.0" -> E2EVersion.V2_0
        "2.1" -> E2EVersion.V2_1
        else -> E2EVersion.UNKNOWN
    }

    fun determineE2EVersion(metadata: String): E2EVersion = runCatching {
        val v1 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFileV1>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFileV1>() {}
        )

        determineE2EFromVersionString(v1?.metadata?.version.toString()).also {
            if (it == E2EVersion.UNKNOWN) {
                throw IllegalStateException("Unknown V1 version")
            }
        }
    }.recoverCatching {
        val v2 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFile>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        determineE2EFromVersionString(v2.version)
    }.getOrDefault(E2EVersion.UNKNOWN)
}
