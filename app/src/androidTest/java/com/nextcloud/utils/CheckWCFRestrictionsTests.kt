/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import com.nextcloud.utils.extensions.checkWCFRestrictions
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MagicNumber")
class CheckWCFRestrictionsTests {

    private fun createCapability(
        version: NextcloudVersion,
        isWCFEnabled: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN
    ): OCCapability = OCCapability().apply {
        this.versionMayor = version.majorVersionNumber
        this.isWCFEnabled = isWCFEnabled
    }

    @Test
    fun testReturnsFalseForVersionsOlderThan30() {
        val capability = createCapability(NextcloudVersion.nextcloud_29)
        assertFalse(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion30WhenWCFAlwaysEnabled() {
        val capability = createCapability(NextcloudVersion.nextcloud_30)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion31WhenWCFAlwaysEnabled() {
        val capability = createCapability(NextcloudVersion.nextcloud_31)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion32WhenWCFEnabled() {
        val capability = createCapability(NextcloudVersion.nextcloud_32, CapabilityBooleanType.TRUE)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsFalseForVersion32WhenWCFDisabled() {
        val capability = createCapability(NextcloudVersion.nextcloud_32, CapabilityBooleanType.FALSE)
        assertFalse(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsFalseForVersion32WhenWCFIsUnknown() {
        val capability = createCapability(NextcloudVersion.nextcloud_32)
        assertFalse(capability.checkWCFRestrictions())
    }
}
