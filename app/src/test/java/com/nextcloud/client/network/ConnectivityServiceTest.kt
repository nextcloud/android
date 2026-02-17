/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.nextcloud.client.account.Server
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.common.PlainClient
import com.nextcloud.operations.GetMethod
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.apache.commons.httpclient.HttpStatus
import org.junit.Assert.assertEquals
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
            const val SERVER_BASE_URL = "https://test.nextcloud.localhost"
        }

        @Mock
        lateinit var context: Context

        @Mock
        lateinit var platformConnectivityManager: ConnectivityManager

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

        val baseServerUri: URI = URI.create(SERVER_BASE_URL)
        val newServer = Server(baseServerUri, NextcloudVersion.nextcloud_31)
        val legacyServer = Server(baseServerUri, OwnCloudVersion.nextcloud_20)

        @Mock
        lateinit var user: User

        lateinit var connectivityService: ConnectivityServiceImpl

        @Before
        fun setUpMocks() {
            MockitoAnnotations.openMocks(this)

            whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(platformConnectivityManager)

            whenever(platformConnectivityManager.activeNetwork).thenReturn(network)
            whenever(platformConnectivityManager.getNetworkCapabilities(network))
                .thenReturn(networkCapabilities)

            whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .thenReturn(true)
            whenever(
                networkCapabilities
                    .hasCapability(eq(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED))
            )
                .thenReturn(true)

            whenever(requestBuilder.invoke(any())).thenReturn(getRequest)
            whenever(clientFactory.createPlainClient()).thenReturn(client)
            whenever(user.server).thenReturn(newServer)
            whenever(accountManager.user).thenReturn(user)
            whenever(walledCheckCache.getValue()).thenReturn(null)

            connectivityService = ConnectivityServiceImpl(
                context,
                accountManager,
                clientFactory,
                requestBuilder,
                walledCheckCache
            )
        }
    }

    internal class Disconnected : Base() {
        @Test
        fun `no active network`() {
            // GIVEN
            whenever(platformConnectivityManager.activeNetwork).thenReturn(null)
            // WHEN
            connectivityService.updateConnectivity()
            // THEN
            assertSame(Connectivity.DISCONNECTED, connectivityService.connectivity)
            assertFalse(connectivityService.isConnected)
        }

        @Test
        fun `no network capabilities`() {
            // GIVEN
            whenever(platformConnectivityManager.getNetworkCapabilities(network)).thenReturn(null)
            // WHEN
            connectivityService.updateConnectivity()
            // THEN
            assertSame(Connectivity.DISCONNECTED, connectivityService.connectivity)
            assertFalse(connectivityService.isConnected)
        }
    }

    internal class IsConnected : Base() {
        @Test
        fun `connected to wifi`() {
            // GIVEN: Default setup is connected Wi-Fi
            // WHEN
            connectivityService.updateConnectivity()
            // THEN
            assertTrue(connectivityService.connectivity.isConnected)
            assertTrue(connectivityService.connectivity.isWifi)
        }

        @Test
        fun `connected to mobile network`() {
            // GIVEN
            whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .thenReturn(false)
            whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                .thenReturn(true)
            // WHEN
            connectivityService.updateConnectivity()
            // THEN
            connectivityService.connectivity.let {
                assertTrue(it.isConnected)
                assertFalse(it.isWifi)
            }
        }

        @Test
        fun `connected to vpn`() {
            // GIVEN
            whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .thenReturn(false)
            whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                .thenReturn(true)
            // WHEN
            connectivityService.updateConnectivity()
            // THEN
            connectivityService.connectivity.let {
                assertTrue(it.isConnected)
                assertFalse(it.isWifi)
            }
        }
    }

    internal class WifiConnectionWalledStatusOnLegacyServer : Base() {
        @Before
        fun setUp() {
            whenever(user.server).thenReturn(legacyServer)
            connectivityService.updateConnectivity()
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
            connectivityService.updateConnectivity()
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
            whenever(platformConnectivityManager.activeNetwork).thenReturn(null)
            connectivityService.updateConnectivity()
            assertFalse("Precondition failed", connectivityService.isConnected)

            // WHEN
            //      connectivity is checked
            val result = connectivityService.isInternetWalled

            // THEN
            //      connection is walled
            //      request is not sent
            assertTrue("Should be walled if not connected", result)
            verify(requestBuilder, never()).invoke(any())
            verify(client, never()).execute(any())
        }

        @Test
        fun `request IS sent when wifi is metered`() {
            // GIVEN
            //      network is connected to wifi, but metered
            whenever(
                networkCapabilities
                    .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            )
                .thenReturn(false)
            connectivityService.updateConnectivity()

            connectivityService.connectivity.let {
                assertTrue("should be connected", it.isConnected)
                assertTrue("should be connected to wifi", it.isWifi)
                assertTrue("should be metered", it.isMetered)
            }
            // Mock a successful 204 response
            mockResponse(contentLength = 0, status = HttpStatus.SC_NO_CONTENT)

            // WHEN
            //      connectivity is checked
            val result = connectivityService.isInternetWalled

            // THEN
            //      assume internet is not walled
            //      request IS sent
            assertEquals(false, result)
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
            assertTrue(
                "Invalid URL used to check status",
                urlCaptor
                    .value
                    .endsWith("/index.php/204")
            )
            verify(getRequest, times(1)).execute(client)
        }
    }
}
