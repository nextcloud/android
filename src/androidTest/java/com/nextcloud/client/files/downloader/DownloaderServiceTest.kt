/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.files.downloader

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.rule.ServiceTestRule
import com.nextcloud.client.account.MockUser
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
