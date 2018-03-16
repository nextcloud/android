/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Inspired by: https://stackoverflow.com/questions/6493517/detect-if-android-device-has-internet-connection
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

import java.io.IOException;

public class ConnectivityUtils {

    private final static String TAG = ConnectivityUtils.class.getName();

    public static boolean isInternetWalled(Context context) {
        if (isOnlineWithWifi(context)) {
            try {
                Account account = AccountUtils.getCurrentOwnCloudAccount(context);
                if (account != null) {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                    OwnCloudVersion serverVersion = AccountUtils.getServerVersion(account);

                    String url;
                    if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                        url = ocAccount.getBaseUri() + "/index.php/204";
                    } else {
                        url = ocAccount.getBaseUri() + "/status.php";
                    }

                    GetMethod get = new GetMethod(url);
                    OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(account, context);

                    int status = client.executeMethod(get);

                    if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                        return !(status == 204 &&
                                (get.getResponseContentLength() == -1 || get.getResponseContentLength() == 0));
                    } else {
                        if (status == 200) {
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
            } catch (OperationCanceledException e) {
                Log_OC.e(TAG, e.getMessage());
            } catch (AuthenticatorException e) {
                Log_OC.e(TAG, e.getMessage());
            }
        } else if (!Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
            return false;
        }

        return true;
    }

    private static boolean isOnlineWithWifi(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork.isConnectedOrConnecting()) {
                switch (activeNetwork.getType()) {
                    case ConnectivityManager.TYPE_VPN:
                        // check if any other network is wifi
                        for (NetworkInfo networkInfo : cm.getAllNetworkInfo()) {
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
}
