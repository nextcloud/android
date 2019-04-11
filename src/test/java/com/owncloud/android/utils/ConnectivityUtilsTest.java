/*
 * Nextcloud Android client application
 *
 * @author Edvard Holst
 * Copyright (C) 2019 Edvard Holst
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*"})
@PrepareForTest({AccountUtils.class, OwnCloudClientFactory.class, ConnectivityUtils.class})
public class ConnectivityUtilsTest {

    @Mock
    private Context context;

    @Mock
    private ConnectivityManager connectivityManager;

    @Mock
    private NetworkInfo networkInfo;

    @Mock
    private Account account;

    @Mock
    private OwnCloudAccount ocAccount;

    @Mock
    private OwnCloudClient client;

    @Mock
    private GetMethod getMethod;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);

        mockStatic(AccountUtils.class);
        mockStatic(OwnCloudClientFactory.class);
    }

    @Test
    public void isOnlineWithWifi_assertTrueWhenOnWifi() {
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

        assertTrue("Falsely indicated connection not on WiFi", ConnectivityUtils.isOnlineWithWifi(context));
    }

    @Test
    public void isOnlineWithWifi_assertTrueWhenOnVPNWithAdditionalWiFiConnection() {
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_VPN);
        NetworkInfo[] networkInfoList = new NetworkInfo[1];
        NetworkInfo wifiNetworkInfo = mock(NetworkInfo.class);
        when(wifiNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(wifiNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        networkInfoList[0] = wifiNetworkInfo;
        when(connectivityManager.getAllNetworkInfo()).thenReturn(networkInfoList);

        assertTrue("Falsely indicated connection not on WiFi", ConnectivityUtils.isOnlineWithWifi(context));
    }

    @Test
    public void isOnlineWithWifi_assertFalseWhenNotOnWifi() {
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);

        assertFalse("Falsely indicated connection on WiFi", ConnectivityUtils.isOnlineWithWifi(context));
    }

    @Test
    public void isInternetWalled_assertFalseWhenOnOlderNC() throws Exception {
        // Ensure we are on WiFi
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

        PowerMockito.when(AccountUtils.getCurrentOwnCloudAccount(eq(context))).thenReturn(account);
        PowerMockito.whenNew(OwnCloudAccount.class).withAnyArguments().thenReturn(ocAccount);
        PowerMockito.when(AccountUtils.getServerVersion(eq(account))).thenReturn(OwnCloudVersion.nextcloud_13);
        PowerMockito.when(OwnCloudClientFactory.createOwnCloudClient(eq(account), eq(context))).thenReturn(client);
        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn(getMethod);

        // Return SC_OK
        when(client.executeMethod(getMethod)).thenReturn(HttpStatus.SC_OK);

        // Content length should be > 0.
        when(getMethod.getResponseContentLength()).thenReturn(1024L);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("maintenance", false);

        when(getMethod.getResponseBodyAsString()).thenReturn(jsonObj.toString());

        assertFalse("internet was falsely claimed to be walled",
                    ConnectivityUtils.isInternetWalled(context));
    }

    @Test
    public void isInternetWalled_assertFalseWhenOnNewerNC() throws Exception {
        // Ensure we are on WiFi
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

        PowerMockito.when(AccountUtils.getCurrentOwnCloudAccount(eq(context))).thenReturn(account);
        PowerMockito.whenNew(OwnCloudAccount.class).withAnyArguments().thenReturn(ocAccount);
        PowerMockito.when(AccountUtils.getServerVersion(eq(account))).thenReturn(OwnCloudVersion.nextcloud_14);
        PowerMockito.when(OwnCloudClientFactory.createOwnCloudClient(eq(account), eq(context))).thenReturn(client);
        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn(getMethod);

        // Return SC_NO_CONTENT
        when(client.executeMethod(getMethod)).thenReturn(HttpStatus.SC_NO_CONTENT);

        // Content length should be 0.
        when(getMethod.getResponseContentLength()).thenReturn(0L);

        assertFalse("internet was falsely claimed to be walled",
                    ConnectivityUtils.isInternetWalled(context));
    }
}
