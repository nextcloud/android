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

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
    TestPowerManagementService.PowerSaveMode::class,
    TestPowerManagementService.BatteryCharging::class
)
class TestPowerManagementService {

    abstract class Base {
        @Mock
        lateinit var context: Context

        @Mock
        lateinit var platformPowerManager: PowerManager

        @Mock
        lateinit var deviceInfo: DeviceInfo

        internal lateinit var powerManagementService: PowerManagementServiceImpl

        @Before
        fun setUpBase() {
            MockitoAnnotations.initMocks(this)
            powerManagementService = PowerManagementServiceImpl(
                context = context,
                powerManager = platformPowerManager,
                deviceInfo = deviceInfo
            )
        }
    }

    class PowerSaveMode : Base() {

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

    class BatteryCharging : Base() {

        val mockStickyBatteryStatusIntent: Intent = mock()

        @Before
        fun setUp() {
            whenever(context.registerReceiver(anyOrNull(), anyOrNull())).thenReturn(mockStickyBatteryStatusIntent)
        }

        @Test
        fun `battery charging status on API 17+`() {
            // GIVEN
            //      device has API level 17+
            //      battery status sticky intent is available
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.JELLY_BEAN_MR1)
            val powerSources = setOf(
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_USB,
                BatteryManager.BATTERY_PLUGGED_WIRELESS
            )

            for (row in powerSources) {
                // WHEN
                //      device is charging using supported power source
                whenever(mockStickyBatteryStatusIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
                    .thenReturn(row)

                // THEN
                //      charging flag is true
                assertTrue(powerManagementService.isBatteryCharging)
            }
        }

        @Test
        fun `battery charging status on API 14-16`() {
            // GIVEN
            //      device has API level 16 or below
            //      battery status sticky intent is available
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            val powerSources = setOf(
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_USB
            )

            for (row in powerSources) {
                // WHEN
                //      device is charging using AC or USB
                whenever(mockStickyBatteryStatusIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
                    .thenReturn(row)

                // THEN
                //      charging flag is true
                assertTrue(powerManagementService.isBatteryCharging)
            }
        }

        @Test
        fun `wireless charging is not supported in API 14-16`() {
            // GIVEN
            //      device has API level 16 or below
            //      battery status sticky intent is available
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.JELLY_BEAN)

            // WHEN
            //      spurious wireless power source is returned
            whenever(mockStickyBatteryStatusIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
                .thenReturn(BatteryManager.BATTERY_PLUGGED_WIRELESS)

            // THEN
            //      power source value is ignored on this API level
            //      charging flag is false
            assertFalse(powerManagementService.isBatteryCharging)
        }

        @Test
        fun `battery status sticky intent is not available`() {
            // GIVEN
            //      device has API level P or below
            //      battery status sticky intent is NOT available
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.P)
            whenever(context.registerReceiver(anyOrNull(), anyOrNull())).thenReturn(null)

            // THEN
            //     charging flag is false
            assertFalse(powerManagementService.isBatteryCharging)
            verify(context).registerReceiver(anyOrNull(), any())
        }
    }
}
