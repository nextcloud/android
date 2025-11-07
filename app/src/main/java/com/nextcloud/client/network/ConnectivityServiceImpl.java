/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;

import android.content.Context;
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

public class ConnectivityServiceImpl implements ConnectivityService {

    private static final String TAG = "ConnectivityServiceImpl";
    private static final String CONNECTIVITY_CHECK_ROUTE = "/index.php/204";

    private final ConnectivityManager connectivityManager;
    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;
    private final GetRequestBuilder requestBuilder;
    private final WalledCheckCache walledCheckCache;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Connectivity currentConnectivity = Connectivity.DISCONNECTED;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            updateConnectivity();
        }

        @Override
        public void onLost(@NonNull Network network) {
            updateConnectivity();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            updateConnectivity();
        }
    };

    static class GetRequestBuilder implements kotlin.jvm.functions.Function1<String, GetMethod> {
        @Override
        public GetMethod invoke(String url) {
            return new GetMethod(url, false);
        }
    }

    public ConnectivityServiceImpl(@NonNull Context context,
                                   @NonNull UserAccountManager accountManager,
                                   @NonNull ClientFactory clientFactory,
                                   @NonNull GetRequestBuilder requestBuilder,
                                   @NonNull WalledCheckCache walledCheckCache) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
        this.walledCheckCache = walledCheckCache;

        // Register callback for real-time network updates
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        updateConnectivity();
    }

    public void updateConnectivity() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            currentConnectivity = Connectivity.DISCONNECTED;
            return;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            currentConnectivity = Connectivity.DISCONNECTED;
            return;
        }

        boolean isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || isSupportedTransport(capabilities);
        boolean isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        boolean isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

        currentConnectivity = new Connectivity(isConnected, isMetered, isWifi, null);
    }

    private boolean isSupportedTransport(@NonNull NetworkCapabilities capabilities) {
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB));
    }

    @Override
    public void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback) {
        executor.execute(() -> {
            boolean available = !isInternetWalled();
            mainThreadHandler.post(() -> callback.onComplete(available));
        });
    }

    @Override
    public boolean isConnected() {
        return currentConnectivity.isConnected();
    }

    @Override
    public boolean isInternetWalled() {
        Boolean cached = walledCheckCache.getValue();
        if (cached != null) {
            return cached;
        }

        Server server = accountManager.getUser().getServer();
        String baseServerAddress = server.getUri().toString();

        if (!currentConnectivity.isConnected() || baseServerAddress.isEmpty()) {
            walledCheckCache.setValue(true);
            return true;
        }

        boolean isWalled;
        GetMethod get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE);
        PlainClient client = clientFactory.createPlainClient();

        try {
            int status = get.execute(client);

            // Server is reachable and responds correctly = NOT walled
            isWalled = !(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0);
            if (isWalled) {
                Log_OC.w(TAG, "isInternetWalled(): Server returned unexpected response");
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "isInternetWalled(): Exception during server check", e);
            isWalled = true;
        } finally {
            get.releaseConnection();
        }

        walledCheckCache.setValue(isWalled);
        return isWalled;
    }

    @Override
    public Connectivity getConnectivity() {
        return currentConnectivity;
    }

    public void unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
