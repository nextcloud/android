/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v2.encrypted

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
data class EncryptedFolderMetadataFile(
    val metadata: EncryptedMetadata,
    val users: List<EncryptedUser>,
    @Transient val filedrop: MutableMap<String, EncryptedFiledrop>?,
    val version: String = "2.0"
)
