/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
        assertFalse(test(NextcloudVersion.nextcloud_28))
        assertFalse(test(NextcloudVersion.nextcloud_27))

        assertTrue(test(NextcloudVersion.nextcloud_26))
        assertTrue(test(NextcloudVersion.nextcloud_25))
        assertTrue(test(NextcloudVersion.nextcloud_24))
        assertTrue(test(NextcloudVersion.nextcloud_23))
        assertTrue(test(NextcloudVersion.nextcloud_22))
        assertTrue(test(NextcloudVersion.nextcloud_21))
        assertTrue(test(OwnCloudVersion.nextcloud_20))
        assertTrue(test(OwnCloudVersion.nextcloud_19))
        assertTrue(test(OwnCloudVersion.nextcloud_18))
        assertTrue(test(OwnCloudVersion.nextcloud_17))
    }

    private fun test(version: OwnCloudVersion): Boolean {
        return CapabilityUtils.checkOutdatedWarning(targetContext.resources, version, false)
    }
}
