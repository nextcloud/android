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

package com.nextcloud.client.network;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.common.PlainClient;
import com.nextcloud.operations.GetMethod;

import org.apache.commons.httpclient.HttpStatus;

import androidx.core.net.ConnectivityManagerCompat;
import kotlin.jvm.functions.Function1;

class ConnectivityServiceImpl implements ConnectivityService {

    private final ConnectivityManager platformConnectivityManager;
    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;
    private final GetRequestBuilder requestBuilder;

    static class GetRequestBuilder implements Function1<String, GetMethod> {
        @Override
        public GetMethod invoke(String url) {
            return new GetMethod(url, false);
        }
    }

    ConnectivityServiceImpl(ConnectivityManager platformConnectivityManager,
                            UserAccountManager accountManager,
                            ClientFactory clientFactory,
                            GetRequestBuilder requestBuilder) {
        this.platformConnectivityManager = platformConnectivityManager;
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
    }

    @Override
    public boolean isInternetWalled() {
        Connectivity c = getConnectivity();
        if (c.isConnected() && c.isWifi()) {

            Server server = accountManager.getUser().getServer();
            String baseServerAddress = server.getUri().toString();
            if (baseServerAddress.isEmpty()) {
                return true;
            }

            GetMethod get = requestBuilder.invoke(baseServerAddress + "/index.php/204");
            PlainClient client = clientFactory.createPlainClient();

            int status = get.execute(client);

            // Content-Length is not available when using chunked transfer encoding, so check for -1 as well
            boolean result = !(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0);

            get.releaseConnection();

            return result;
        } else {
            return !c.isConnected();
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = platformConnectivityManager.getNetworkCapabilities(
                    platformConnectivityManager.getActiveNetwork());

                if (networkCapabilities != null) {
                    isMetered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
                } else {
                    isMetered = ConnectivityManagerCompat.isActiveNetworkMetered(platformConnectivityManager);
                }
            } else {
                isMetered = ConnectivityManagerCompat.isActiveNetworkMetered(platformConnectivityManager);
            }
            boolean isWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI || hasNonCellularConnectivity();
            return new Connectivity(isConnected, isMetered, isWifi, null);
        } else {
            return Connectivity.DISCONNECTED;
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
