package com.owncloud.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectivityUtilsTest {

    @Mock
    private Context mContext;

    @Mock
    private ConnectivityManager mConnectivityManager;

    @Mock
    private NetworkInfo mNetworkInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mConnectivityManager);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(mNetworkInfo);
    }
    

    @Test
    public void isOnlineWithWifi_assertTrueWhenOnWifi() {
        when(mNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);

        assertTrue("Falsely indicated connection not on WiFi", ConnectivityUtils.isOnlineWithWifi(mContext));
    }

    @Test
    public void isOnlineWithWifi_assertTrueWhenOnVPNWithAdditionalWiFiConnection() {
        when(mNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_VPN);
        NetworkInfo[] networkInfoList = new NetworkInfo[1];
        NetworkInfo wifiNetworkInfo = mock(NetworkInfo.class);
        when(wifiNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(wifiNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        networkInfoList[0] = wifiNetworkInfo;
        when(mConnectivityManager.getAllNetworkInfo()).thenReturn(networkInfoList);

        assertTrue("Falsely indicated connection not on WiFi", ConnectivityUtils.isOnlineWithWifi(mContext));
    }

    @Test
    public void isOnlineWithWifi_assertFalseWhenNotOnWifi() {
        when(mNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);

        assertFalse("Falsely indicated connection on WiFi", ConnectivityUtils.isOnlineWithWifi(mContext));
    }
}
