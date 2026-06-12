/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.lib.resources.status.E2EVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class EndToEndEncryptionVersionTests {

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenUnknownShouldReturnUnknown() {
        assertEquals(E2EVersion.UNKNOWN, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.UNKNOWN))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV1_0ShouldReturnV1_0() {
        assertEquals(E2EVersion.V1_0, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_0))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV1_1ShouldReturnV1_1() {
        assertEquals(E2EVersion.V1_1, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_1))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV1_2ShouldReturnV1_2() {
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV1AboveClientMaxShouldReturnClientV1Max() {
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV2_0ShouldReturnV2_0() {
        assertEquals(E2EVersion.V2_0, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_0))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV2_1ShouldReturnV2_1() {
        assertEquals(E2EVersion.V2_1, E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_1))
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV1_2ShouldNotApplyV2CeilingShouldReturnV1_2() {
        val result = E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2)
        assertEquals(E2EVersion.V1_2, result)
    }

    @Test
    fun testGetMaxCompatibleE2EEVersionWhenGivenV2_0ShouldNotApplyV1CeilingShouldReturnV2_0() {
        val result = E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_0)
        assertEquals(E2EVersion.V2_0, result)
    }
}
