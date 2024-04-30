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
