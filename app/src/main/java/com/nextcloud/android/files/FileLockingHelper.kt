/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.FileLockType

object FileLockingHelper {
    /**
     * Checks whether the given `userId` can unlock the [OCFile].
     */
    @JvmStatic
    fun canUserUnlockFile(userId: String, file: OCFile): Boolean {
        if (!file.isLocked) {
            return false
        }
        if (file.lockType == FileLockType.MANUAL && file.lockOwnerId == userId) {
            return true
        }
        // App locks (Text, OnlyOffice, ...) carry no lock owner id and are reported as
        // COLLABORATIVE. The file owner may still unlock them: LockService::canUnlock
        // accepts any TYPE_USER request from the file owner.
        return file.lockType != FileLockType.TOKEN && file.ownerId == userId
    }
}
