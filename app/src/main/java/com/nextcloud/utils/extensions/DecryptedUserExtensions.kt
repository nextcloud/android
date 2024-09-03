/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser

fun List<DecryptedUser?>.findMetadataKeyByUserId(userId: String): String? {
    var result: String? = null

    for (decryptedUser in this) {
        if (decryptedUser != null && decryptedUser.userId == userId) {
            result = decryptedUser.decryptedMetadataKey
        }
    }

    return result
}
