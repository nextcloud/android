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

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.evernote.android.job.JobRequest;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

import java.io.IOException;

import androidx.core.net.ConnectivityManagerCompat;
import kotlin.jvm.functions.Function1;

class ConnectivityServiceImpl implements ConnectivityService {

    private final static String TAG = ConnectivityServiceImpl.class.getName();

    private ConnectivityManager connectivityManager;
    private UserAccountManager accountManager;
    private ClientFactory clientFactory;
    private GetRequestBuilder requestBuilder;

    static class GetRequestBuilder implements Function1<String, GetMethod> {
        @Override
        public GetMethod invoke(String url) {
            return new GetMethod(url);
        }
    }

    ConnectivityServiceImpl(ConnectivityManager connectivityManager,
                            UserAccountManager accountManager,
                            ClientFactory clientFactory,
                            GetRequestBuilder requestBuilder) {
        this.connectivityManager = connectivityManager;
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
        this.requestBuilder = requestBuilder;
    }

    @Override
    public boolean isInternetWalled() {
        if (isOnlineWithWifi()) {
            try {
                Account account = accountManager.getCurrentAccount();
                if (account != null) {
                    OwnCloudAccount ocAccount = accountManager.getCurrentOwnCloudAccount();
                    OwnCloudVersion serverVersion = accountManager.getServerVersion(account);

                    String url;
                    if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                        url = ocAccount.getBaseUri() + "/index.php/204";
                    } else {
                        url = ocAccount.getBaseUri() + "/status.php";
                    }

                    GetMethod get = requestBuilder.invoke(url);
                    OwnCloudClient client = clientFactory.create(account);

                    int status = client.executeMethod(get);

                    if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
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
                }
            } catch (IOException e) {
                Log_OC.e(TAG, "Error checking internet connection", e);
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Account not found", e);
            } catch (OperationCanceledException | AuthenticatorException e) {
                Log_OC.e(TAG, e.getMessage());
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
