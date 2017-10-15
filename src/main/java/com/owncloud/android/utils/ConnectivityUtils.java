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

import android.content.Context;
import android.util.Log;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;

import org.apache.commons.httpclient.util.HttpURLConnection;

import java.io.IOException;
import java.net.URL;

public class ConnectivityUtils {

    private final static String TAG = ConnectivityUtils.class.getName();

    public static boolean isInternetWalled(Context context) {
        if (!Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("http://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return !(urlc.getResponseCode() == 204 &&
                        urlc.getContentLength() == 0);
            } catch (IOException e) {
                Log.e(TAG, "Error checking internet connection", e);
            }
        } else {
            Log.d(TAG, "No network available!");
        }
        return true;

    }
}
