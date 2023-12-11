/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
