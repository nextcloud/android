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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.common.PlainClient;
import com.nextcloud.operations.GetMethod;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import kotlin.jvm.functions.Function1;

class ConnectivityServiceImpl implements ConnectivityService {

    private static final String TAG = "ConnectivityServiceImpl";
    private static final String CONNECTIVITY_CHECK_ROUTE = "/index.php/204";

    private final ConnectivityManager platformConnectivityManager;
    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;
    private final GetRequestBuilder requestBuilder;
    private final WalledCheckCache walledCheckCache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

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
    public void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback) {
        executor.submit(() -> {
            boolean isAvailable = !isInternetWalled();
            mainThreadHandler.post(() -> callback.onComplete(isAvailable));
        });
    }

    /**
     * Checks whether the device is currently connected to a network
     * that has verified Internet access.
     *
     * <p>This method performs multiple levels of validation:
     * <ul>
     *     <li>Ensures there is an active network connection.</li>
     *     <li>Retrieves and checks network capabilities.</li>
     *     <li>Verifies that the active network provides and has validated Internet access.</li>
     *     <li>Confirms that the network uses a supported transport type
     *         (Wi-Fi, Cellular, Ethernet, VPN, etc.).</li>
     * </ul>
     *
     * @return {@code true} if the device is connected to the Internet via a valid transport type;
     *         {@code false} otherwise.
     */
    @Override
    public boolean isConnected() {
        Network nw = platformConnectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }

        NetworkCapabilities actNw = platformConnectivityManager.getNetworkCapabilities(nw);
        if (actNw == null) {
            return false;
        }

        // Verify that the network both claims to provide Internet
        // and has been validated (i.e., Internet is actually reachable).
        if (actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {

            // Check if the active network uses one of the recognized transport types.
            if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {

                // Connected through a valid, verified network transport.
                return true;
            }

            // If still nothing matched check Android 12 (API 31, "S") and above via USB network transport.
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_USB);
        }

        return false;
    }

    @Override
    public boolean isInternetWalled() {
        final Boolean cachedValue = walledCheckCache.getValue();
        if (cachedValue != null) {
            return cachedValue;
        }

        final Server server = accountManager.getUser().getServer();
        final String baseServerAddress = server.getUri().toString();

        if (!isConnected() || baseServerAddress.isEmpty()) {
            walledCheckCache.setValue(true);
            return true;
        }

        final GetMethod get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE);
        try {
            final PlainClient client = clientFactory.createPlainClient();
            int status = get.execute(client);

            boolean isWalled = !(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0);

            if (isWalled) {
                Log_OC.w(TAG, "isInternetWalled(): Failed to GET " + CONNECTIVITY_CHECK_ROUTE +
                    ", assuming connectivity is impaired");
            }

            // Cache and return result
            walledCheckCache.setValue(isWalled);
            return isWalled;
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while checking internet walled state", e);
            walledCheckCache.setValue(true);
            return true;
        } finally {
            get.releaseConnection();
        }
    }

    @Override
    public Connectivity getConnectivity() {
        Network nw = platformConnectivityManager.getActiveNetwork();
        if (nw == null) {
            return Connectivity.DISCONNECTED;
        }

        NetworkCapabilities nc = platformConnectivityManager.getNetworkCapabilities(nw);
        boolean isConnected = isConnected();
        boolean isMetered = (nc != null) && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        boolean isWifi = (nc != null) &&
            (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE));

        return new Connectivity(isConnected, isMetered, isWifi, null);
    }
}
