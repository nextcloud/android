/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v2.decrypted

import com.owncloud.android.utils.EncryptionUtils

data class DecryptedMetadata(
    val keyChecksums: MutableList<String> = mutableListOf(),
    val deleted: Boolean = false,
    var counter: Long = 0,
    val folders: MutableMap<String, String> = mutableMapOf(),
    val files: MutableMap<String, DecryptedFile> = mutableMapOf(),
    @Transient
    var metadataKey: ByteArray = EncryptionUtils.generateKey()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecryptedMetadata

        if (keyChecksums != other.keyChecksums) return false
        if (deleted != other.deleted) return false
        if (counter != other.counter) return false
        if (folders != other.folders) return false
        if (files != other.files) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyChecksums.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + folders.hashCode()
        result = 31 * result + files.hashCode()
        return result
    }
}
