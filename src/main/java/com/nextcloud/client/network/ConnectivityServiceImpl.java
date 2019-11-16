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

import com.evernote.android.job.JobRequest;
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

    private final ConnectivityManager connectivityManager;
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

    ConnectivityServiceImpl(ConnectivityManager connectivityManager,
                            UserAccountManager accountManager,
                            ClientFactory clientFactory,
                            GetRequestBuilder requestBuilder,
                            Logger logger) {
        this.connectivityManager = connectivityManager;
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
        this.logger = logger;
    }

    @Override
    public boolean isInternetWalled() {
        if (isOnlineWithWifi()) {
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

                GetMethod get = requestBuilder.invoke(url);
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
            }
        } else {
            return getActiveNetworkType() == JobRequest.NetworkType.ANY;
        }

        return true;
    }

    @Override
    public boolean isOnlineWithWifi() {
        try {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            if (activeNetwork.isConnectedOrConnecting()) {
                switch (activeNetwork.getType()) {
                    case ConnectivityManager.TYPE_VPN:
                        // check if any other network is wifi
                        for (NetworkInfo networkInfo : connectivityManager.getAllNetworkInfo()) {
                            if (networkInfo.isConnectedOrConnecting() &&
                                networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                                return true;
                            }
                        }
                        return false;

                    case ConnectivityManager.TYPE_WIFI:
                        return true;

                    default:
                        return false;
                }
            } else {
                return false;
            }
        } catch (NullPointerException exception) {
            return false;
        }
    }

    @Override
    public JobRequest.NetworkType getActiveNetworkType() {
        NetworkInfo networkInfo;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Throwable t) {
            return JobRequest.NetworkType.ANY;
        }

        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            return JobRequest.NetworkType.ANY;
        }

        boolean metered = ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager);
        if (!metered) {
            return JobRequest.NetworkType.UNMETERED;
        }

        if (networkInfo.isRoaming()) {
            return JobRequest.NetworkType.CONNECTED;
        } else {
            return JobRequest.NetworkType.NOT_ROAMING;
        }
    }
}
