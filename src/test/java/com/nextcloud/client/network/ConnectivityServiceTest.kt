/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.accounts.Account
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import com.nextcloud.client.account.UserAccountManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
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

            const val SERVER_BASE_URL = "https://test.server.com"
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
        lateinit var client: HttpClient

        @Mock
        lateinit var getRequest: GetMethod

        @Mock
        lateinit var requestBuilder: ConnectivityServiceImpl.GetRequestBuilder

        @Mock
        lateinit var platformAccount: Account

        @Mock
        lateinit var ownCloudAccount: OwnCloudAccount

        @Mock
        lateinit var baseServerUri: Uri

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
            whenever(requestBuilder.invoke(any())).thenReturn(getRequest)
            whenever(clientFactory.createPlainClient()).thenReturn(client)
            whenever(accountManager.currentOwnCloudAccount).thenReturn(ownCloudAccount)
            whenever(accountManager.currentAccount).thenReturn(platformAccount)
            whenever(baseServerUri.toString()).thenReturn(SERVER_BASE_URL)
            whenever(ownCloudAccount.baseUri).thenReturn(baseServerUri)
        }
    }

    internal class IsConnected : Base() {

        @Test
        fun `connected to wifi`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            assertTrue(connectivityService.isOnlineWithWifi)
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
            assertTrue(connectivityService.isOnlineWithWifi)
        }

        @Test
        fun `connected to mobile network`() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_MOBILE)
            assertFalse(connectivityService.isOnlineWithWifi)
        }
    }

    internal class WifiConnectionWalledStatusOnLegacyServer : Base() {

        @Before
        fun setUp() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            whenever(accountManager.getServerVersion(any())).thenReturn(OwnCloudVersion.nextcloud_13)
            assertTrue("Precondition failed", connectivityService.isOnlineWithWifi)
        }

        fun mockResponse(maintenance: Boolean = true, httpStatus: Int = HttpStatus.SC_OK) {
            whenever(client.executeMethod(getRequest)).thenReturn(httpStatus)
            val body = """{"maintenance":$maintenance}"""
            whenever(getRequest.responseContentLength).thenReturn(body.length.toLong())
            whenever(getRequest.responseBodyAsString).thenReturn(body)
        }

        @Test
        fun `false maintenance status flag is used`() {
            mockResponse(maintenance = false, httpStatus = HttpStatus.SC_OK)
            assertFalse(connectivityService.isInternetWalled)
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
            assertTrue("Invalid URL used to check status", urlCaptor.value.endsWith("/status.php"))
        }
    }

    internal class WifiConnectionWalledStatus : Base() {

        @Before
        fun setUp() {
            whenever(networkInfo.isConnectedOrConnecting).thenReturn(true)
            whenever(networkInfo.type).thenReturn(ConnectivityManager.TYPE_WIFI)
            whenever(accountManager.getServerVersion(any())).thenReturn(OwnCloudVersion.nextcloud_14)
            assertTrue("Precondition failed", connectivityService.isOnlineWithWifi)
        }

        fun mockResponse(contentLength: Long = 0, status: Int = HttpStatus.SC_OK) {
            whenever(client.executeMethod(any())).thenReturn(status)
            whenever(getRequest.statusCode).thenReturn(status)
            whenever(getRequest.responseContentLength).thenReturn(contentLength)
        }

        @Test
        fun `status 204 means internet is not walled`() {
            mockResponse(contentLength = 0, status = HttpStatus.SC_NO_CONTENT)
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
