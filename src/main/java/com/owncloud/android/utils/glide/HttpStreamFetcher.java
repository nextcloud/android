/**
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * Copyright (C) 2017 Alejandro Bautista
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils.glide;

import android.accounts.Account;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.InputStream;

/**
 * Fetcher with OwnCloudClient
 */

public class HttpStreamFetcher implements DataFetcher<InputStream> {

    private static final String TAG = HttpStreamFetcher.class.getName();
    private final String mURL;

    public HttpStreamFetcher(String url) {
        this.mURL = url;

    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {

        Account mAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, MainApp.getAppContext());
        OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                getClientFor(ocAccount, MainApp.getAppContext());

        if (mClient != null) {
            if (AccountUtils.getServerVersion(mAccount).supportsRemoteThumbnails()) {
                GetMethod get = null;
                try {
                    get = new GetMethod(mURL);
                    get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
                    get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);
                    int status = mClient.executeMethod(get);
                    if (status == HttpStatus.SC_OK) {
                        return get.getResponseBodyAsStream();
                    } else {
                        mClient.exhaustResponse(get.getResponseBodyAsStream());
                    }
                } catch (Exception e) {
                    Log_OC.d(TAG, e.getMessage(), e);
                }
            } else {
                Log_OC.d(TAG, "Server too old");
            }
        }
        return null;
    }

    @Override
    public void cleanup() {
        Log_OC.i(TAG,"Cleanup");
    }

    @Override
    public String getId() {
        return mURL;
    }

    @Override
    public void cancel() {
        Log_OC.i(TAG,"Cancel");
    }
}
