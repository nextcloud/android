/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser

class AvatarShareesProvider {

    fun get(file: OCFile, userId: String?): List<ShareeUser> {
        val sharees = file.sharees

        val ownerSharee = file.ownerId
            ?.takeIf { it.isNotEmpty() && it != userId }
            ?.let { ShareeUser(it, file.ownerDisplayName, ShareType.USER) }
            ?.takeIf { it !in sharees }

        return listOfNotNull(ownerSharee) + sharees.asReversed()
    }
}
