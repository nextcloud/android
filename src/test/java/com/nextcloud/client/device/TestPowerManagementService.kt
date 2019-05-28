/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

package com.nextcloud.client.device

import android.os.Build
import android.os.PowerManager
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TestPowerManagementService {

    @Mock
    lateinit var platformPowerManager: PowerManager

    @Mock
    lateinit var deviceInfo: DeviceInfo

    private lateinit var powerManagementService: PowerManagementServiceImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        powerManagementService = PowerManagementServiceImpl(
            powerManager = platformPowerManager,
            deviceInfo = deviceInfo
        )
    }

    @Test
    fun `power saving queries power manager on API 21+`() {
        // GIVEN
        //      API level >= 21
        //      power save mode is on
        whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.LOLLIPOP)
        whenever(platformPowerManager.isPowerSaveMode).thenReturn(true)

        // WHEN
        //      power save mode is checked
        // THEN
        //      power save mode is enabled
        //      state is obtained from platform power manager
        assertTrue(powerManagementService.isPowerSavingEnabled)
        verify(platformPowerManager).isPowerSaveMode
    }

    @Test
    fun `power saving is not available below API 21`() {
        // GIVEN
        //      API level <21
        whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.KITKAT)

        // WHEN
        //      power save mode is checked

        // THEN
        //      power save mode is disabled
        //      power manager is not queried
        assertFalse(powerManagementService.isPowerSavingEnabled)
        verify(platformPowerManager, never()).isPowerSaveMode
    }

    @Test
    fun `power save exclusion is available for flagged vendors`() {
        for (vendor in PowerManagementServiceImpl.OVERLY_AGGRESSIVE_POWER_SAVING_VENDORS) {
            whenever(deviceInfo.vendor).thenReturn(vendor)
            assertTrue("Vendor $vendor check failed", powerManagementService.isPowerSavingExclusionAvailable)
        }
    }

    @Test
    fun `power save exclusion is not available for other vendors`() {
        whenever(deviceInfo.vendor).thenReturn("some_other_nice_vendor")
        assertFalse(powerManagementService.isPowerSavingExclusionAvailable)
    }
}
