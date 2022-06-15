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
import org.junit.Assert
import org.junit.Test

class FileLockingHelperTest {

    @Test
    fun fileNotLocked_cannotUnlock() {
        val file = OCFile("/foo.md").apply {
            isLocked = false
            lockOwnerId = USER_NAME
            lockType = FileLockType.MANUAL
        }
        Assert.assertFalse(FileLockingHelper.canUserUnlockFile(USER_NAME, file))
    }

    @Test
    fun ownerNotUser_cannotUnlock() {
        val file = OCFile("/foo.md").apply {
            isLocked = true
            lockOwnerId = "bloop"
            lockType = FileLockType.MANUAL
        }
        Assert.assertFalse(FileLockingHelper.canUserUnlockFile(USER_NAME, file))
    }

    @Test
    fun typeNotManual_cannotUnlock() {
        val file = OCFile("/foo.md").apply {
            isLocked = true
            lockOwnerId = USER_NAME
            lockType = FileLockType.COLLABORATIVE
        }
        Assert.assertFalse(FileLockingHelper.canUserUnlockFile(USER_NAME, file))
    }

    @Test
    fun canUnlock() {
        val file = OCFile("/foo.md").apply {
            isLocked = true
            lockOwnerId = USER_NAME
            lockType = FileLockType.MANUAL
        }
        Assert.assertTrue(FileLockingHelper.canUserUnlockFile(USER_NAME, file))
    }

    companion object {
        private const val USER_NAME = "user"
    }
}
