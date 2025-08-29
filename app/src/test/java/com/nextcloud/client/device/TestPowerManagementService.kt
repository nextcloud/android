/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.nextcloud.client.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
                platformPowerManager
            )
        }
    }

    class PowerSaveMode : Base() {

        @Test
        fun `power saving queries power manager on API 21+`() {
            // GIVEN
            //      API level >= 21
            //      power save mode is on
            whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.Q)
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
        fun `power saving check is disabled`() {
            // GIVEN
            //      a device which falsely returns power save mode enabled
            //      power check is overridden by user
            whenever(platformPowerManager.isPowerSaveMode).thenReturn(true)

            // WHEN
            //      power save mode is checked
            // THEN
            //      power saving is disabled
            assertFalse(powerManagementService.isPowerSavingEnabled)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    class Battery : Base() {

        companion object {
            const val FULL_RAGE = 32
            const val HALF_RANGE = 16
        }

        val intent: Intent = mock()

        @Before
        fun setUp() {
            whenever(context.registerReceiver(anyOrNull(), anyOrNull())).thenReturn(
                intent
            )
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
            whenever(context.registerReceiver(anyOrNull(), anyOrNull())).thenReturn(
                null
            )

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
