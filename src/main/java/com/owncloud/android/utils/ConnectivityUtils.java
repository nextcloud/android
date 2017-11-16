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
import android.content.Context;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ConnectivityUtils {

    private final static String TAG = ConnectivityUtils.class.getName();

    public static boolean isInternetWalled(Context context) {
        if (!Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
            try {
                Account account = AccountUtils.getCurrentOwnCloudAccount(context);
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                OwnCloudVersion serverVersion = AccountUtils.getServerVersion(account);

                URL url;
                if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                    url = new URL(ocAccount.getBaseUri() + "/index.php/204");
                } else {
                    url = new URL(ocAccount.getBaseUri() + "/status.php");
                }
                HttpsURLConnection urlc = (HttpsURLConnection) (url.openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(2500);
                urlc.connect();

                if (serverVersion.compareTo(OwnCloudVersion.nextcloud_13) > 0) {
                    return !(urlc.getResponseCode() == 204 && urlc.getContentLength() == 0);
                } else {
                    if (urlc.getResponseCode() == 200) {
                        // try parsing json to verify response
                        try {
                            new JSONObject(urlc.getResponseMessage());
                            return false;
                        } catch (JSONException e) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } catch (IOException e) {
                Log_OC.e(TAG, "Error checking internet connection", e);
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Account not found", e);
            }
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.d(TAG, "No account found");
        }

        return true;

    }
}
