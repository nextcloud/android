/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Assert.assertEquals
import org.junit.Test

class OCCapabilityIT : AbstractIT() {
    @Test
    fun saveCapability() {
        val fileDataStorageManager = FileDataStorageManager(user, targetContext.contentResolver)

        val capability = OCCapability()
        capability.etag = "123"
        capability.userStatus = CapabilityBooleanType.TRUE
        capability.userStatusSupportsEmoji = CapabilityBooleanType.TRUE
        capability.dropAccount = CapabilityBooleanType.TRUE

        fileDataStorageManager.saveCapabilities(capability)

        val newCapability = fileDataStorageManager.getCapability(user.accountName)

        assertEquals(capability.etag, newCapability.etag)
        assertEquals(capability.userStatus, newCapability.userStatus)
        assertEquals(capability.userStatusSupportsEmoji, newCapability.userStatusSupportsEmoji)
        assertEquals(capability.dropAccount, newCapability.dropAccount)
    }
}
