/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v2.decrypted

import com.nextcloud.utils.e2ee.E2EVersionHelper

/**
 * Decrypted class representation of metadata json of folder metadata.
 */
data class DecryptedFolderMetadataFile(
    val metadata: DecryptedMetadata,
    var users: MutableList<DecryptedUser> = mutableListOf(),
    @Transient
    val filedrop: MutableMap<String, DecryptedFile> = HashMap(),
    val version: String = E2EVersionHelper.getLatestE2EVersion(true).value
)
