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

import android.content.ComponentName
import android.content.Context
import com.nextcloud.client.account.MockUser
import com.owncloud.android.datamodel.OCFile
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransferManagerConnectionTest {

    lateinit var connection: TransferManagerConnection

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var firstDownloadListener: (Transfer) -> Unit

    @MockK
    lateinit var secondDownloadListener: (Transfer) -> Unit

    @MockK
    lateinit var firstStatusListener: (TransferManager.Status) -> Unit

    @MockK
    lateinit var secondStatusListener: (TransferManager.Status) -> Unit

    @MockK
    lateinit var binder: FileTransferService.Binder

    val file get() = OCFile("/path")
    val componentName = ComponentName("", FileTransferService::class.java.simpleName)
    val user = MockUser()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        connection = TransferManagerConnection(context, user)
    }

    @Test
    fun listeners_are_set_after_connection() {
        // GIVEN
        //      not connected
        //      listener is added
        connection.registerTransferListener(firstDownloadListener)
        connection.registerTransferListener(secondDownloadListener)

        // WHEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      all listeners are passed to the service
        val listeners = mutableListOf<(Transfer) -> Unit>()
        verify { binder.registerTransferListener(capture(listeners)) }
        assertEquals(listOf(firstDownloadListener, secondDownloadListener), listeners)
    }

    @Test
    fun listeners_are_set_immediately_when_connected() {
        // GIVEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // WHEN
        //      listeners are added
        connection.registerTransferListener(firstDownloadListener)

        // THEN
        //      listener is forwarded to service
        verify { binder.registerTransferListener(firstDownloadListener) }
    }

    @Test
    fun listeners_are_removed_when_unbinding() {
        // GIVEN
        //      service is bound
        //      service has some listeners
        connection.onServiceConnected(componentName, binder)
        connection.registerTransferListener(firstDownloadListener)
        connection.registerTransferListener(secondDownloadListener)

        // WHEN
        //      service unbound
        connection.unbind()

        // THEN
        //      listeners removed from service
        verify { binder.removeTransferListener(firstDownloadListener) }
        verify { binder.removeTransferListener(secondDownloadListener) }
    }

    @Test
    fun missed_updates_are_delivered_on_connection() {
        // GIVEN
        //      not bound
        //      has listeners
        //      download is scheduled and is progressing
        connection.registerTransferListener(firstDownloadListener)
        connection.registerTransferListener(secondDownloadListener)

        val request1 = DownloadRequest(user, file)
        connection.enqueue(request1)
        val download1 = Transfer(request1.uuid, TransferState.RUNNING, 50, request1.file, request1)

        val request2 = DownloadRequest(user, file)
        connection.enqueue(request2)
        val download2 = Transfer(request2.uuid, TransferState.RUNNING, 50, request2.file, request1)

        every { binder.getTransfer(request1.uuid) } returns download1
        every { binder.getTransfer(request2.uuid) } returns download2

        // WHEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      listeners receive current download state for pending downloads
        val firstListenerNotifications = mutableListOf<Transfer>()
        verify { firstDownloadListener(capture(firstListenerNotifications)) }
        assertEquals(listOf(download1, download2), firstListenerNotifications)

        val secondListenerNotifications = mutableListOf<Transfer>()
        verify { secondDownloadListener(capture(secondListenerNotifications)) }
        assertEquals(listOf(download1, download2), secondListenerNotifications)
    }

    @Test
    fun downloader_status_updates_are_delivered_on_connection() {
        // GIVEN
        //      not bound
        //      has status listeners
        val mockStatus: TransferManager.Status = mockk()
        every { binder.status } returns mockStatus
        connection.registerStatusListener(firstStatusListener)
        connection.registerStatusListener(secondStatusListener)

        // WHEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      downloader status is delivered
        verify { firstStatusListener(mockStatus) }
        verify { secondStatusListener(mockStatus) }
    }

    @Test
    fun downloader_status_not_requested_if_no_listeners() {
        // GIVEN
        //      not bound
        //      no status listeners

        // WHEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      downloader status is not requested
        verify(exactly = 0) { binder.status }
    }

    @Test
    fun not_running_if_not_connected() {
        // GIVEN
        //      downloader is running
        //      connection not bound
        every { binder.isRunning } returns true

        // THEN
        //      not running
        assertFalse(connection.isRunning)
    }

    @Test
    fun is_running_from_binder_if_connected() {
        // GIVEN
        //      service bound
        every { binder.isRunning } returns true
        connection.onServiceConnected(componentName, binder)

        // WHEN
        //      is runnign flag accessed
        val isRunning = connection.isRunning

        // THEN
        //      call delegated to binder
        assertTrue(isRunning)
        verify(exactly = 1) { binder.isRunning }
    }

    @Test
    fun missed_updates_not_tracked_before_listeners_registered() {
        // GIVEN
        //      not bound
        //      some downloads requested without listener
        val request = DownloadRequest(user, file)
        connection.enqueue(request)
        val download = Transfer(request.uuid, TransferState.RUNNING, 50, request.file, request)
        connection.registerTransferListener(firstDownloadListener)
        every { binder.getTransfer(request.uuid) } returns download

        // WHEN
        //      service is bound
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      missed updates not redelivered
        verify(exactly = 0) { firstDownloadListener(any()) }
    }
}
