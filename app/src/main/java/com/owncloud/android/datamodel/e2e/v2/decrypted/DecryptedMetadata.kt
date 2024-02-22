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
