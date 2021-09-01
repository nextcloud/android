/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
import com.nextcloud.client.preferences.AppPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
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
    TestPowerManagementService.Battery::class
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

        @Mock
        lateinit var preferences: AppPreferences

        @Before
        fun setUpBase() {
            MockitoAnnotations.initMocks(this)
            powerManagementService = PowerManagementServiceImpl(
                context,
                platformPowerManager,
                preferences,
                deviceInfo
            )
        }
    }

    class PowerSaveMode : Base() {

        @Test
        fun `power saving queries power manager on API 21+`() {
            // GIVEN
            //      API level >= 22 (since 22+ is supported)
            //      power save mode is on
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.LOLLIPOP_MR1)
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
        fun `power saving exclusion is available for flagged vendors`() {
            for (vendor in PowerManagementServiceImpl.OVERLY_AGGRESSIVE_POWER_SAVING_VENDORS) {
                whenever(deviceInfo.vendor).thenReturn(vendor)
                assertTrue("Vendor $vendor check failed", powerManagementService.isPowerSavingExclusionAvailable)
            }
        }

        @Test
        fun `power saving exclusion is not available for other vendors`() {
            whenever(deviceInfo.vendor).thenReturn("some_other_nice_vendor")
            assertFalse(powerManagementService.isPowerSavingExclusionAvailable)
        }

        @Test
        fun `power saving check is disabled`() {
            // GIVEN
            //      a device which falsely returns power save mode enabled
            //      power check is overridden by user
            whenever(preferences.isPowerCheckDisabled).thenReturn(true)
            whenever(platformPowerManager.isPowerSaveMode).thenReturn(true)

            // WHEN
            //      power save mode is checked
            // THEN
            //      power saving is disabled
            assertFalse(powerManagementService.isPowerSavingEnabled)
        }
    }

    class Battery : Base() {

        companion object {
            const val FULL_RAGE = 32
            const val HALF_RANGE = 16
        }

        val intent: Intent = mock()

        @Before
        fun setUp() {
            whenever(context.registerReceiver(anyOrNull(), anyOrNull())).thenReturn(intent)
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
                whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
                    .thenReturn(row)

                // THEN
                //      charging flag is true
                assertTrue(powerManagementService.battery.isCharging)
            }
        }

        @Test
        fun `battery charging status on API 16`() {
            // GIVEN
            //      device has API level 16
            //      battery status sticky intent is available
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.JELLY_BEAN)
            val powerSources = setOf(
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_USB
            )

            for (row in powerSources) {
                // WHEN
                //      device is charging using AC or USB
                whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
                    .thenReturn(row)

                // THEN
                //      charging flag is true
                assertTrue(powerManagementService.battery.isCharging)
            }
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
            assertFalse(powerManagementService.battery.isCharging)
            verify(context).registerReceiver(anyOrNull(), any())
        }

        @Test
        @Suppress("MagicNumber")
        fun `battery level is available`() {
            // GIVEN
            //      battery level info is available
            //      battery is at 50%
            whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_LEVEL), any()))
                .thenReturn(HALF_RANGE)
            whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_SCALE), any()))
                .thenReturn(FULL_RAGE)

            // THEN
            //      battery level is calculated from extra values
            assertEquals(50, powerManagementService.battery.level)
        }

        @Test
        fun `battery level is not available`() {
            // GIVEN
            //      battery level is not available
            val defaultValueArgIndex = 1
            whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_LEVEL), any()))
                .thenAnswer { it.getArgument(defaultValueArgIndex) }
            whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_SCALE), any()))
                .thenAnswer { it.getArgument(defaultValueArgIndex) }

            // THEN
            //      battery level is 100
            assertEquals(BatteryStatus.BATTERY_FULL, powerManagementService.battery.level)
        }
    }
}
