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
import android.net.NetworkInfo;

import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.logger.Logger;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

import java.io.IOException;

import androidx.core.net.ConnectivityManagerCompat;
import kotlin.jvm.functions.Function1;

class ConnectivityServiceImpl implements ConnectivityService {

    private final static String TAG = ConnectivityServiceImpl.class.getName();

    private final ConnectivityManager platformConnectivityManager;
    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;
    private final GetRequestBuilder requestBuilder;
    private final Logger logger;

    static class GetRequestBuilder implements Function1<String, GetMethod> {
        @Override
        public GetMethod invoke(String url) {
            return new GetMethod(url);
        }
    }

    ConnectivityServiceImpl(ConnectivityManager platformConnectivityManager,
                            UserAccountManager accountManager,
                            ClientFactory clientFactory,
                            GetRequestBuilder requestBuilder,
                            Logger logger) {
        this.platformConnectivityManager = platformConnectivityManager;
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
        this.logger = logger;
    }

    @Override
    public boolean isInternetWalled() {
        Connectivity c = getConnectivity();
        if (c.isConnected() && c.isWifi()) {

            GetMethod get = null;
            try {
                Server server = accountManager.getUser().getServer();
                String baseServerAddress = server.getUri().toString();
                if (baseServerAddress.isEmpty()) {
                    return true;
                }
                String url;
                if (server.getVersion().compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                    url = baseServerAddress + "/index.php/204";
                } else {
                    url = baseServerAddress + "/status.php";
                }

                get = requestBuilder.invoke(url);
                HttpClient client = clientFactory.createPlainClient();

                int status = client.executeMethod(get);

                if (server.getVersion().compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                    return !(status == HttpStatus.SC_NO_CONTENT &&
                        (get.getResponseContentLength() == -1 || get.getResponseContentLength() == 0));
                } else {
                    if (status == HttpStatus.SC_OK) {
                        try {
                            // try parsing json to verify response
                            // check if json contains maintenance and it should be false
                            String json = get.getResponseBodyAsString();
                            return new JSONObject(json).getBoolean("maintenance");
                        } catch (Exception e) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } catch (IOException e) {
                logger.e(TAG, "Error checking internet connection", e);
            } finally {
                if (get != null) {
                    get.releaseConnection();
                }
            }
        } else {
            return !getConnectivity().isConnected();
        }

        return true;
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
            boolean isMetered = ConnectivityManagerCompat.isActiveNetworkMetered(platformConnectivityManager);
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
