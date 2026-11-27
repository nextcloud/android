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
     *
     * Mirrors `LockService.canUnlock` of the server's files_lock app, so unlock is only offered when the server
     * accepts it.
     *
     * Lock types:
     * - [FileLockType.MANUAL]: a user locked the file. `lockOwnerId` is that user.
     * - [FileLockType.TOKEN]: a client locked the file while it is open in a local editor. `lockOwnerId` is the user
     *   and the lock token is bound to that client.
     * - [FileLockType.COLLABORATIVE]: an editing app (Text, Office) locked the file for its editing session.
     *   `lockOwnerId` is the app id and the app releases the lock when the last editor leaves.
     *
     * Who can unlock:
     * - The file owner, for every lock type. An admin who shares a file can unlock a lock the recipient set.
     * - The lock owner, for their own manual or token lock. The recipient can unlock the lock they set.
     * - Nobody else. Another user the file is shared with cannot unlock it.
     *
     * Group folders and external storage report the viewing user as file owner, so ownership does not count there.
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
