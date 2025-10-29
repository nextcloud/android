/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import com.nextcloud.utils.extensions.checkWCFRestrictions
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MagicNumber")
class CheckWCFRestrictionsTests {

    private val version29 = Triple(29, 0, 0)
    private val version30 = Triple(30, 0, 0)
    private val version31 = Triple(31, 0, 0)
    private val version32 = Triple(32, 0, 0)
    private val version33 = Triple(33, 0, 0)

    private fun createCapability(
        version: Triple<Int, Int, Int>,
        isWCFEnabled: CapabilityBooleanType = CapabilityBooleanType.UNKNOWN
    ): OCCapability = OCCapability().apply {
        this.versionMayor = version.first
        this.versionMinor = version.second
        this.versionMicro = version.third
        this.isWCFEnabled = isWCFEnabled
    }

    @Test
    fun testReturnsFalseForVersionsOlderThan30() {
        val capability = createCapability(version29)
        assertFalse(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion30WhenWCFAlwaysEnabled() {
        val capability = createCapability(version30)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion31WhenWCFAlwaysEnabled() {
        val capability = createCapability(version31)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForVersion32WhenWCFEnabled() {
        val capability = createCapability(version32, CapabilityBooleanType.TRUE)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsFalseForVersion32WhenWCFDisabled() {
        val capability = createCapability(version32, CapabilityBooleanType.FALSE)
        assertFalse(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsFalseForVersion32WhenWCFIsUnknown() {
        val capability = createCapability(version32)
        assertFalse(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsTrueForNewerVersionWhenWCFEnabled() {
        val capability = createCapability(version33, CapabilityBooleanType.TRUE)
        assertTrue(capability.checkWCFRestrictions())
    }

    @Test
    fun testReturnsFalseForNewerVersionWhenWCFDisabled() {
        val capability = createCapability(version33, CapabilityBooleanType.FALSE)
        assertFalse(capability.checkWCFRestrictions())
    }
}
