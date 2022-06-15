/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        if (!file.isLocked || file.lockOwnerId == null || file.lockType != FileLockType.MANUAL) {
            return false
        }
        return file.lockOwnerId == userId
    }
}
