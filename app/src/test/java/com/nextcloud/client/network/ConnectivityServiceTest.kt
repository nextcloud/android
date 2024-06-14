/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import com.nextcloud.client.account.Server
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.logger.Logger
import com.nextcloud.common.PlainClient
import com.nextcloud.operations.GetMethod
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
        lateinit var network: Network

        @Mock
        lateinit var walledCheckCache: WalledCheckCache

        @Mock
        lateinit var networkCapabilities: NetworkCapabilities

        @Mock
        lateinit var logger: Logger

        val baseServerUri = URI.create(SERVER_BASE_URL)
        val newServer = Server(baseServerUri, OwnCloudVersion.nextcloud_20)
        val legacyServer = Server(baseServerUri, OwnCloudVersion.nextcloud_17)

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
                requestBuilder,
                walledCheckCache
            )

            whenever(networkCapabilities.hasCapability(eq(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)))
                .thenReturn(true)
            whenever(platformConnectivityManager.activeNetwork).thenReturn(network)
            whenever(platformConnectivityManager.activeNetworkInfo).thenReturn(networkInfo)
            whenever(platformConnectivityManager.allNetworkInfo).thenReturn(arrayOf(networkInfo))
            whenever(platformConnectivityManager.getNetworkCapabilities(any())).thenReturn(networkCapabilities)
            whenever(requestBuilder.invoke(any())).thenReturn(getRequest)
            whenever(clientFactory.createPlainClient()).thenReturn(client)
            whenever(user.server).thenReturn(newServer)
            whenever(accountManager.user).thenReturn(user)
            whenever(walledCheckCache.getValue()).thenReturn(null)
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
            connectivityService.connectivity.let {
                assertTrue(it.isConnected)
                assertTrue(it.isWifi)
                assertFalse(it.isMetered)
            }
        }

        @Test
        fun `request not sent when not connected`() {
            // GIVEN
            //      network is not connected
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(false)
            whenever(networkInfo.isConnected).thenReturn(false)

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

        @Test
        fun `request not sent when wifi is metered`() {
            // GIVEN
            //      network is connected to wifi
            //      wifi is metered
            whenever(networkCapabilities.hasCapability(any())).thenReturn(false) // this test is mocked for API M
            whenever(platformConnectivityManager.isActiveNetworkMetered).thenReturn(true)
            connectivityService.connectivity.let {
                assertTrue("should be connected", it.isConnected)
                assertTrue("should be connected to wifi", it.isWifi)
                assertTrue("check mocking, this check is complicated and depends on SDK version", it.isMetered)
            }

            // WHEN
            //      connectivity is checked
            val result = connectivityService.isInternetWalled

            // THEN
            //      assume internet is not walled
            //      request is not sent
            assertFalse("Server should not be accessible", result)
            verify(requestBuilder, never()).invoke(any())
            verify(getRequest, never()).execute(any<PlainClient>())
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
            verify(getRequest, never()).execute(any<PlainClient>())
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
            verify(getRequest, times(1)).execute(client)
        }

        @Test
        fun `status 204 and no content length means internet is not walled`() {
            mockResponse(contentLength = -1, status = HttpStatus.SC_NO_CONTENT)
            assertFalse(connectivityService.isInternetWalled)
            verify(getRequest, times(1)).execute(client)
        }

        @Test
        fun `other status than 204 means internet is walled`() {
            mockResponse(contentLength = 0, status = HttpStatus.SC_GONE)
            assertTrue(connectivityService.isInternetWalled)
            verify(getRequest, times(1)).execute(client)
        }

        @Test
        fun `index endpoint is used to determine internet state`() {
            mockResponse()
            connectivityService.isInternetWalled
            val urlCaptor = ArgumentCaptor.forClass(String::class.java)
            verify(requestBuilder).invoke(urlCaptor.capture())
            assertTrue("Invalid URL used to check status", urlCaptor.value.endsWith("/index.php/204"))
            verify(getRequest, times(1)).execute(client)
        }
    }
}
