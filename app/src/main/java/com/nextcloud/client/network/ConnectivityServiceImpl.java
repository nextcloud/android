/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.network;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.NetworkOnMainThreadException;

import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.common.PlainClient;
import com.nextcloud.operations.GetMethod;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;

import androidx.core.net.ConnectivityManagerCompat;
import kotlin.jvm.functions.Function1;

class ConnectivityServiceImpl implements ConnectivityService {

    private static final String TAG = "ConnectivityServiceImpl";
    private static final String CONNECTIVITY_CHECK_ROUTE = "/index.php/204";

    private final ConnectivityManager platformConnectivityManager;
    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;
    private final GetRequestBuilder requestBuilder;
    private final WalledCheckCache walledCheckCache;

    static class GetRequestBuilder implements Function1<String, GetMethod> {
        @Override
        public GetMethod invoke(String url) {
            return new GetMethod(url, false);
        }
    }

    ConnectivityServiceImpl(ConnectivityManager platformConnectivityManager,
                            UserAccountManager accountManager,
                            ClientFactory clientFactory,
                            GetRequestBuilder requestBuilder,
                            final WalledCheckCache walledCheckCache) {
        this.platformConnectivityManager = platformConnectivityManager;
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
        this.walledCheckCache = walledCheckCache;
    }

    @Override
    public boolean isNetworkAndServerAvailable() throws NetworkOnMainThreadException {
        Network activeNetwork = platformConnectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = platformConnectivityManager.getNetworkCapabilities(activeNetwork);
        boolean hasInternet = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        if (!hasInternet) {
            return false;
        }

        return !isInternetWalled();
    }

    @Override
    public boolean isConnected() {
        Network nw = platformConnectivityManager.getActiveNetwork();
        NetworkCapabilities actNw = platformConnectivityManager.getNetworkCapabilities(nw);

        if (actNw == null) {
            return false;
        }

        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH);
    }

    @Override
    public boolean isInternetWalled() {
        final Boolean cachedValue = walledCheckCache.getValue();
        if (cachedValue != null) {
            return cachedValue;
        } else {
            boolean result;
            Connectivity c = getConnectivity();
            if (c.isConnected() && c.isWifi() && !c.isMetered()) {

                Server server = accountManager.getUser().getServer();
                String baseServerAddress = server.getUri().toString();
                if (baseServerAddress.isEmpty()) {
                    result = true;
                } else {

                    GetMethod get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE);
                    PlainClient client = clientFactory.createPlainClient();

                    int status = get.execute(client);

                    // Content-Length is not available when using chunked transfer encoding, so check for -1 as well
                    result = !(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0);
                    get.releaseConnection();
                    if (result) {
                        Log_OC.w(TAG, "isInternetWalled(): Failed to GET " + CONNECTIVITY_CHECK_ROUTE + "," +
                            " assuming connectivity is impaired");
                    }
                }
            } else {
                result = !c.isConnected();
            }

            walledCheckCache.setValue(result);
            return result;
        }
    }

    @Override
    public Connectivity getConnectivity() {
        NetworkInfo networkInfo;
        try {
            networkInfo = platformConnectivityManager.getActiveNetworkInfo();
        } catch (Throwable t) {
            networkInfo = null; // no network available or no information (permission denied?)
        }

        if (networkInfo != null) {
            boolean isConnected = networkInfo.isConnectedOrConnecting();
            // more detailed check
            boolean isMetered;
            isMetered = isNetworkMetered();
            boolean isWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI || hasNonCellularConnectivity();
            return new Connectivity(isConnected, isMetered, isWifi, null);
        } else {
            return Connectivity.DISCONNECTED;
        }
    }

    private boolean isNetworkMetered() {
        final Network network = platformConnectivityManager.getActiveNetwork();
        try {
            NetworkCapabilities networkCapabilities = platformConnectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null) {
                return !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
            } else {
                return ConnectivityManagerCompat.isActiveNetworkMetered(platformConnectivityManager);
            }
        } catch (RuntimeException e) {
            Log_OC.e(TAG, "Exception when checking network capabilities", e);
            return false;
        }
    }

    private boolean hasNonCellularConnectivity() {
        for (NetworkInfo networkInfo : platformConnectivityManager.getAllNetworkInfo()) {
            if (networkInfo.isConnectedOrConnecting() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET)) {
                return true;
            }
        }
        return false;
    }
}
