package com.nextcloud.client.device

import android.os.Build
import android.os.PowerManager
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.*
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
