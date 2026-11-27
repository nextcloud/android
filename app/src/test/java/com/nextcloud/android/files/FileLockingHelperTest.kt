/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.files

import com.nextcloud.android.files.FileLockingHelper.canUserUnlockFile
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import com.owncloud.android.lib.resources.files.model.FileLockType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileLockingHelperTest {

    companion object {
        private const val USER = "user"
        private const val OTHER_USER = "other"
        private const val APP_ID = "text"
    }

    private fun file(
        isLocked: Boolean = true,
        lockType: FileLockType? = FileLockType.MANUAL,
        lockOwnerId: String? = OTHER_USER,
        ownerId: String = OTHER_USER,
        mountType: MountType = MountType.INTERNAL
    ) = OCFile("/foo.md").apply {
        this.isLocked = isLocked
        this.lockType = lockType
        this.lockOwnerId = lockOwnerId
        this.ownerId = ownerId
        this.mountType = mountType
    }

    @Test
    fun `unlocked file cannot be unlocked by file owner`() {
        val file = file(isLocked = false, lockType = null, lockOwnerId = null, ownerId = USER)

        assertFalse(canUserUnlockFile(USER, file))
    }

    @Test
    fun `manual lock can be unlocked by lock owner`() {
        assertTrue(canUserUnlockFile(USER, file(lockOwnerId = USER)))
    }

    @Test
    fun `manual lock can be unlocked by file owner`() {
        assertTrue(canUserUnlockFile(USER, file(ownerId = USER)))
    }

    @Test
    fun `manual lock cannot be unlocked by other user`() {
        assertFalse(canUserUnlockFile(USER, file()))
    }

    @Test
    fun `collaborative lock can be unlocked by file owner`() {
        val file = file(lockType = FileLockType.COLLABORATIVE, lockOwnerId = APP_ID, ownerId = USER)

        assertTrue(canUserUnlockFile(USER, file))
    }

    @Test
    fun `collaborative lock cannot be unlocked by other user`() {
        assertFalse(canUserUnlockFile(USER, file(lockType = FileLockType.COLLABORATIVE, lockOwnerId = APP_ID)))
    }

    @Test
    fun `token lock can be unlocked by lock owner`() {
        assertTrue(canUserUnlockFile(USER, file(lockType = FileLockType.TOKEN, lockOwnerId = USER)))
    }

    @Test
    fun `token lock can be unlocked by file owner`() {
        assertTrue(canUserUnlockFile(USER, file(lockType = FileLockType.TOKEN, ownerId = USER)))
    }

    @Test
    fun `token lock cannot be unlocked by other user`() {
        assertFalse(canUserUnlockFile(USER, file(lockType = FileLockType.TOKEN)))
    }
}
