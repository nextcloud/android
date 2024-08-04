/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: MIT
 */
package com.nextcloud.client.utils

import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest

object HashUtil {
    private const val ALGORITHM_MD5 = "MD5"

    @JvmStatic
    fun md5Hash(input: String): String {
        val digest = MessageDigest.getInstance(ALGORITHM_MD5)
            .digest(input.toByteArray())
        return String(Hex.encodeHex(digest))
    }
}
