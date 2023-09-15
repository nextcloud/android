/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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
package com.owncloud.android.utils.theme

import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CapabilityUtilsIT : AbstractIT() {
    @Test
    fun checkOutdatedWarning() {
        assertFalse(test(NextcloudVersion.nextcloud_27))
        assertFalse(test(NextcloudVersion.nextcloud_26))
        assertFalse(test(NextcloudVersion.nextcloud_25))
        assertFalse(test(NextcloudVersion.nextcloud_24))

        assertTrue(test(NextcloudVersion.nextcloud_23))
        assertTrue(test(NextcloudVersion.nextcloud_22))
        assertTrue(test(NextcloudVersion.nextcloud_21))
        assertTrue(test(OwnCloudVersion.nextcloud_20))
        assertTrue(test(OwnCloudVersion.nextcloud_19))
        assertTrue(test(OwnCloudVersion.nextcloud_18))
        assertTrue(test(OwnCloudVersion.nextcloud_17))
        assertTrue(test(OwnCloudVersion.nextcloud_16))
    }

    private fun test(version: OwnCloudVersion): Boolean {
        return CapabilityUtils.checkOutdatedWarning(targetContext.resources, version, false)
    }
}
