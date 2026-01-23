/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.google.gson.reflect.TypeToken
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.utils.EncryptionUtils

fun E2EVersion.isV2orAbove(): Boolean {
    return this == E2EVersion.V2_0 || this == E2EVersion.V2_1
}

fun E2EVersion.isV1(): Boolean {
    return this == E2EVersion.V1_0 || this == E2EVersion.V1_1 || this == E2EVersion.V1_2
}

fun determineE2EVersion(metadata: String): E2EVersion {
    return runCatching {
        val v1 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFileV1>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFileV1>() {}
        )
        when (val version = v1?.metadata?.version?.toString()) {
            "1.0" -> E2EVersion.V1_0
            "1.1" -> E2EVersion.V1_1
            "1.2" -> E2EVersion.V1_2
            else -> throw IllegalStateException("Unknown V1 version: $version")
        }
    }.recoverCatching {
        val v2 = EncryptionUtils.deserializeJSON<EncryptedFolderMetadataFile>(
            metadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        when (v2?.version) {
            "2.0", "2" -> E2EVersion.V2_0
            "2.1" -> E2EVersion.V2_1
            else -> E2EVersion.UNKNOWN
        }
    }.getOrDefault(E2EVersion.UNKNOWN)
}
