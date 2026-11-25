/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.theme

import android.content.Context
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ServerBrandingTest {

    private val onDeviceName = "On device"
    private val defaultRootName = "Nextcloud"
    private val serverName = "My Cloud"

    private lateinit var context: Context
    private lateinit var capability: OCCapability
    private lateinit var capabilityUtils: MockedStatic<CapabilityUtils>
    private lateinit var sut: ServerBranding

    @Before
    fun setUp() {
        context = mock()
        whenever(context.getString(R.string.drawer_item_on_device)).thenReturn(onDeviceName)
        whenever(context.getString(R.string.default_display_name_for_root_folder)).thenReturn(defaultRootName)

        capability = OCCapability()
        capabilityUtils = Mockito.mockStatic(CapabilityUtils::class.java)
        capabilityUtils.`when`<OCCapability> { CapabilityUtils.getCapability(context) }.thenReturn(capability)

        sut = ServerBranding(context)
    }

    @After
    fun tearDown() {
        capabilityUtils.close()
        MainApp.showOnlyFilesOnDevice(false)
    }

    @Test
    fun `returns My Cloud server name when server name is set`() {
        capability.serverName = serverName

        assertEquals(serverName, sut.getDefaultDisplayNameForRootFolder())
    }

    @Test
    fun `returns Nextcloud default name when server name is null`() {
        capability.serverName = null

        assertEquals(defaultRootName, sut.getDefaultDisplayNameForRootFolder())
    }

    @Test
    fun `returns Nextcloud default name when server name is empty`() {
        capability.serverName = ""

        assertEquals(defaultRootName, sut.getDefaultDisplayNameForRootFolder())
    }

    @Test
    fun `returns On device when only files on device are shown`() {
        capability.serverName = serverName
        MainApp.showOnlyFilesOnDevice(true)

        assertEquals(onDeviceName, sut.getDefaultDisplayNameForRootFolder())
    }
}
