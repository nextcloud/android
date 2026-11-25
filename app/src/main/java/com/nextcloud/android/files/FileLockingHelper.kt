/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import com.owncloud.android.lib.resources.files.model.FileLockType

object FileLockingHelper {
    private val mountTypesWithoutFileOwnership = setOf(MountType.GROUP, MountType.EXTERNAL)

    /**
     * Checks whether the given `userId` can unlock the [OCFile].
     */
    @JvmStatic
    fun canUserUnlockFile(userId: String, file: OCFile): Boolean {
        if (!file.isLocked) return false
        if (isFileOwner(userId, file)) return true

        return when (file.lockType) {
            FileLockType.MANUAL, FileLockType.TOKEN -> file.lockOwnerId == userId
            FileLockType.COLLABORATIVE, null -> false
        }
    }

    private fun isFileOwner(userId: String, file: OCFile): Boolean =
        file.ownerId == userId && file.mountType !in mountTypesWithoutFileOwnership
}
