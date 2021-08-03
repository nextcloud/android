/*
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
package com.nextcloud.client.network

import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.nextcloud.client.account.Server
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.logger.Logger
import com.nextcloud.common.PlainClient
import com.nextcloud.operations.GetMethod
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.apache.commons.httpclient.HttpStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.net.URI

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ConnectivityServiceTest.Disconnected::class,
    ConnectivityServiceTest.IsConnected::class,
    ConnectivityServiceTest.WifiConnectionWalledStatusOnLegacyServer::class,
    ConnectivityServiceTest.WifiConnectionWalledStatus::class
)
class ConnectivityServiceTest {

    internal abstract class Base {
        companion object {
            fun mockNetworkInfo(connected: Boolean, connecting: Boolean, type: Int): NetworkInfo {
                val networkInfo = mock<NetworkInfo>()
                whenever(networkInfo.isConnectedOrConnecting).thenReturn(connected or connecting)
                whenever(networkInfo.isConnected).thenReturn(connected)
                whenever(networkInfo.type).thenReturn(type)
                return networkInfo
            }

            const val SERVER_BASE_URL = "https://test.nextcloud.localhost"
        }

        @Mock
        lateinit var platformConnectivityManager: ConnectivityManager

        @Mock
        lateinit var networkInfo: NetworkInfo

        @Mock
        lateinit var accountManager: UserAccountManager

        @Mock
        lateinit var clientFactory: ClientFactory

        @Mock
        lateinit var client: PlainClient

        @Mock
        lateinit var getRequest: GetMethod

        @Mock
        lateinit var requestBuilder: ConnectivityServiceImpl.GetRequestBuilder

        @Mock
        lateinit var logger: Logger

        val baseServerUri = URI.create(SERVER_BASE_URL)
        val newServer = Server(baseServerUri, OwnCloudVersion.nextcloud_20)
        val legacyServer = Server(baseServerUri, OwnCloudVersion.nextcloud_16)

        @Mock
        lateinit var user: User

        lateinit var connectivityService: ConnectivityServiceImpl

        @Before
        fun setUpMocks() {
            MockitoAnnotations.initMocks(this)
            connectivityService = ConnectivityServiceImpl(
                platformConnectivityManager,
                accountManager,
                clientFactory,
                requestBuilder
            )

            whenever(platformConnectivityManager.activeNetworkInfo).thenReturn(networkInfo)
            whenever(platformConnectivityManager.allNetworkInfo).thenReturn(arrayOf(networkInfo))
            whenever(requestBuilder.invoke(any())).thenReturn(getRequest)
            whenever(clientFactory.createPlainClient()).thenReturn(client)
            whenever(user.server).thenReturn(newServer)
            whenever(accountManager.user).thenReturn(user)
        }
    }

    internal class Disconnected : Base() {
        @Test
        fun `wifi is disconnected`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(false)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            connectivityService.connectivity.apply {
                assertFalse(isConnected)
                assertTrue(isWifi)
            }
        }

        @Test
        fun `no active network`() {
            whenever(platformConnectivityManager.activeNetworkInfo).thenReturn(null)
            assertSame(Connectivity.DISCONNECTED, connectivityService.connectivity)
        }
    }

    internal class IsConnected : Base() {

        @Test
        fun `connected to wifi`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            assertTrue(connectivityService.connectivity.isConnected)
            assertTrue(connectivityService.connectivity.isWifi)
        }

        @Test
        fun `connected to wifi and vpn`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_VPN)
            val wifiNetworkInfoList = arrayOf(
                mockNetworkInfo(
                    connected = true,
                    connecting = true,
                    type = ConnectivityManager.TYPE_VPN
                ),
                mockNetworkInfo(
                    connected = true,
                    connecting = true,
                    type = ConnectivityManager.TYPE_WIFI
                )
            )
            whenever(platformConnectivityManager.allNetworkInfo).thenReturn(wifiNetworkInfoList)
            connectivityService.connectivity.let {
                assertTrue(it.isConnected)
                assertTrue(it.isWifi)
            }
        }

        @Test
        fun `connected to mobile network`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_MOBILE)
            whenever(platformConnectivityManager.allNetworkInfo).thenReturn(arrayOf(networkInfo))
            connectivityService.connectivity.let {
                assertTrue(it.isConnected)
                assertFalse(it.isWifi)
            }
        }
    }

    internal class WifiConnectionWalledStatusOnLegacyServer : Base() {

        @Before
        fun setUp() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            whenever(user.server).thenReturn(legacyServer)
            assertTrue(
                "Precondition failed",
                connectivityService.connectivity.let {
                    it.isConnected && it.isWifi
                }
            )
        }

        fun mockResponse(maintenance: Boolean = true, httpStatus: Int = HttpStatus.SC_OK) {
            whenever(client.execute(getRequest)).thenReturn(httpStatus)
            val body =
                """{"maintenance":$maintenance}"""
            whenever(getRequest.getResponseContentLength()).thenReturn(body.length.toLong())
            whenever(getRequest.getResponseBodyAsString()).thenReturn(body)
        }

        @Test
        fun `true maintenance status flag is used`() {
            mockResponse(maintenance = true, httpStatus = HttpStatus.SC_OK)
            assertTrue(connectivityService.isInternetWalled)
        }

        @Test
        fun `maintenance flag is ignored when non-200 HTTP code is returned`() {
            mockResponse(maintenance = false, httpStatus = HttpStatus.SC_NO_CONTENT)
            assertTrue(connectivityService.isInternetWalled)
        }

        @Test
        fun `status endpoint is used to determine internet state`() {
            mockResponse()
            connectivityService.isInternetWalled
            val urlCaptor = ArgumentCaptor.forClass(String::class.java)
            verify(requestBuilder).invoke(urlCaptor.capture())
            assertTrue("Invalid URL used to check status", urlCaptor.value.endsWith("/204"))
        }
    }

    internal class WifiConnectionWalledStatus : Base() {

        @Before
        fun setUp() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            whenever(accountManager.getServerVersion(any())).thenReturn(OwnCloudVersion.nextcloud_20)
            assertTrue(
                "Precondition failed",
                connectivityService.connectivity.let {
                    it.isConnected && it.isWifi
                }
            )
        }

        @Test
        fun `check request is not sent when server uri is not set`() {
            // GIVEN
            //      network connectivity is present
            //      user has no server URI (empty)
            val serverWithoutUri = Server(URI(""), OwnCloudVersion.nextcloud_20)
            whenever(user.server).thenReturn(serverWithoutUri)

            // WHEN
            //      connectivity is checked
            val result = connectivityService.isInternetWalled

            // THEN
            //      connection is walled
            //      request is not sent
            assertTrue("Server should not be accessible", result)
            verify(requestBuilder, never()).invoke(any())
            verify(client, never()).execute(any())
        }

        fun mockResponse(contentLength: Long = 0, status: Int = HttpStatus.SC_OK) {
            whenever(client.execute(any())).thenReturn(status)
            whenever(getRequest.getStatusCode()).thenReturn(status)
            whenever(getRequest.getResponseContentLength()).thenReturn(contentLength)
            whenever(getRequest.execute(client)).thenReturn(status)
        }

        @Test
        fun `status 204 means internet is not walled`() {
            mockResponse(contentLength = 0, status = HttpStatus.SC_NO_CONTENT)
            assertFalse(connectivityService.isInternetWalled)
        }

        @Test
        fun `status 204 and no content length means internet is not walled`() {
            mockResponse(contentLength = -1, status = HttpStatus.SC_NO_CONTENT)
            assertFalse(connectivityService.isInternetWalled)
        }

        @Test
        fun `other status than 204 means internet is walled`() {
            mockResponse(contentLength = 0, status = HttpStatus.SC_GONE)
            assertTrue(connectivityService.isInternetWalled)
        }

        @Test
        fun `index endpoint is used to determine internet state`() {
            mockResponse()
            connectivityService.isInternetWalled
            val urlCaptor = ArgumentCaptor.forClass(String::class.java)
            verify(requestBuilder).invoke(urlCaptor.capture())
            assertTrue("Invalid URL used to check status", urlCaptor.value.endsWith("/index.php/204"))
        }
    }
}
