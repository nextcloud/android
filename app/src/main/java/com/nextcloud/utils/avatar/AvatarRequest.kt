/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.avatar

import com.nextcloud.client.account.AccountNameUtils
import com.nextcloud.client.account.User

data class AvatarRequest(val user: User, val userId: String, val displayName: String?, val avatarRadius: Float) {
    val serverName: String = AccountNameUtils.getServerPart(user.accountName)

    val accountName: String = AccountNameUtils.build(userId, serverName)

    fun cacheKey(eTag: String): String = "a_${userId}_${serverName}_$eTag"
}
