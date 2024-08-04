/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.files.download

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import com.nextcloud.client.account.MockUser
import com.nextcloud.client.jobs.transfer.FileTransferService
import io.mockk.MockKAnnotations
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DownloaderServiceTest {

    @get:Rule
    val service = ServiceTestRule.withTimeout(3, TimeUnit.SECONDS)

    val user = MockUser()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test(expected = TimeoutException::class)
    fun cannot_bind_to_service_without_user() {
        val intent = FileTransferService.createBindIntent(getApplicationContext(), user)
        intent.removeExtra(FileTransferService.EXTRA_USER)
        service.bindService(intent)
    }

    @Test
    fun bind_with_user() {
        val intent = FileTransferService.createBindIntent(getApplicationContext(), user)
        val binder = service.bindService(intent)
        assertTrue(binder is FileTransferService.Binder)
    }
}
