/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.ArbitraryDataProvider
import java.security.MessageDigest

class EncryptedFileComparisonCache(private val provider: ArbitraryDataProvider, private val account: String) {
    fun read(identity: String, version: String): Pair<Boolean, Long>? {
        val fields = provider.getValue(account, PREFIX + identity).split(':')
        if (fields.size != FIELD_COUNT || fields[0] != version) return null
        val same = fields[1].toBooleanStrictOrNull() ?: return null
        val length = fields[2].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        return same to length
    }

    fun write(identity: String, version: String, same: Boolean, length: Long) {
        provider.storeOrUpdateKeyValue(account, PREFIX + identity, "$version:$same:$length")
    }

    companion object {
        private const val PREFIX = "encrypted-file-comparison-v1-"
        private const val FIELD_COUNT = 3

        fun version(localLength: Long, localModified: Long, remoteVersion: String): String =
            fingerprint(localLength.toString(), localModified.toString(), remoteVersion)

        fun fingerprint(vararg fields: String): String = MessageDigest.getInstance("SHA-256")
            .digest(fields.joinToString("\u0000").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
