/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.uploadList

import android.content.Context
import android.view.MenuItem
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.AlbumFileUploadWorker
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UploadListMenuProviderTest {

    private val preferences = mockk<AppPreferences>(relaxed = true)
    private val manager = mockk<BackgroundJobManager>(relaxed = true)
    private val accountManager = mockk<UserAccountManager>()
    private val onPauseToggled = mockk<() -> Unit>(relaxed = true)
    private val menuItem = mockk<MenuItem>(relaxed = true)
    private var paused = false
    private val provider = UploadListMenuProvider(
        mockk<Context>(relaxed = true),
        preferences,
        accountManager,
        mockk(),
        manager,
        onPauseToggled
    )

    @Before
    fun setUp() {
        mockkObject(FileUploadHelper, FileUploadWorker, AlbumFileUploadWorker)
        every { FileUploadHelper.instance() } returns mockk(relaxed = true)
        every { FileUploadWorker.pauseActiveUploads() } just Runs
        every { AlbumFileUploadWorker.pauseActiveUploads() } just Runs
        every { accountManager.allUsers } returns emptyList()
        every { preferences.isGlobalUploadPaused() } answers { paused }
        every { preferences.setGlobalUploadPaused(any()) } answers { paused = firstArg() }
        every { menuItem.itemId } returns R.id.action_toggle_global_pause
    }

    @After
    fun tearDown() {
        unmockkObject(FileUploadHelper, FileUploadWorker, AlbumFileUploadWorker)
    }

    @Test
    fun `resume clears the pause before resetting the automatic scan`() {
        paused = true

        assertTrue(provider.onMenuItemSelected(menuItem))

        assertFalse(paused)
        verifyOrder {
            preferences.setGlobalUploadPaused(false)
            manager.schedulePeriodicAutoUpload(resetSchedule = true)
            onPauseToggled()
        }
        verify(exactly = 1) { manager.schedulePeriodicAutoUpload(true) }
    }

    @Test
    fun `pause stops active transfers without restarting automatic scans`() {
        assertTrue(provider.onMenuItemSelected(menuItem))

        assertTrue(paused)
        verify { FileUploadWorker.pauseActiveUploads() }
        verify { AlbumFileUploadWorker.pauseActiveUploads() }
        verify(exactly = 0) { manager.schedulePeriodicAutoUpload(any()) }
    }

    @Test
    fun `unrelated menu actions leave the scan schedule alone`() {
        every { menuItem.itemId } returns R.id.action_search

        assertFalse(provider.onMenuItemSelected(menuItem))

        verify(exactly = 0) { preferences.setGlobalUploadPaused(any()) }
        verify(exactly = 0) { manager.schedulePeriodicAutoUpload(any()) }
    }
}
