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

    /**
     * Returns the latest supported E2EE version.
     *
     * @param isV2 indicates whether the E2EE v2 series should be used
     */
    fun latestVersion(isV2: Boolean): E2EVersion = if (isV2) {
        E2EVersion.V2_1
    } else {
        E2EVersion.V1_2
    }

    /**
     * Maps a raw version string to an [E2EVersion].
     *
     * @param version version string
     * @return resolved [E2EVersion] or [E2EVersion.UNKNOWN] if unsupported
     */
    fun fromVersionString(version: String?): E2EVersion = when (version?.trim()) {
        "1.0" -> E2EVersion.V1_0
        "1.1" -> E2EVersion.V1_1
        "1.2" -> E2EVersion.V1_2
        "2", "2.0" -> E2EVersion.V2_0
        "2.1" -> E2EVersion.V2_1
        else -> E2EVersion.UNKNOWN
    }

    /**
     * Determines the E2EE version by inspecting encrypted folder metadata.
     *
     * Supports both V1 and V2 metadata formats and falls back safely
     * to [E2EVersion.UNKNOWN] if parsing fails.
     */
    fun fromMetadata(metadata: String): E2EVersion = runCatching {
        val v1 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFileV1>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFileV1>() {}
        )

        fromVersionString(v1?.metadata?.version.toString()).also {
            if (it == E2EVersion.UNKNOWN) {
                throw IllegalStateException("Unknown V1 version")
            }
        }
    }.recoverCatching {
        val v2 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFile>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        fromVersionString(v2.version)
    }.getOrDefault(E2EVersion.UNKNOWN)
}
