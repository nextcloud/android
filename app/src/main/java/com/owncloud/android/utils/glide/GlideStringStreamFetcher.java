/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.glide;

import android.net.Uri;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * Fetcher with Nextcloud client
 */
public class GlideStringStreamFetcher implements DataFetcher<InputStream> {

    private static final String TAG = GlideStringStreamFetcher.class.getName();
    private final String url;

    public GlideStringStreamFetcher(String url) {
        this.url = url;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        
        GetMethod get = null;
        try {
        OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(new OwnCloudAccount(Uri.EMPTY, null), MainApp.getAppContext());
            get = new GetMethod(url);
            get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
            get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);
            int status = client.executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                callback.onDataReady(get.getResponseBodyAsStream());
            } else {
                client.exhaustResponse(get.getResponseBodyAsStream());
            }
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage(), e);
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
    }

    public void cleanup() {
        Log_OC.i(TAG, "Cleanup");
    }

    @Override
    public void cancel() {
        Log_OC.i(TAG, "Cancel");
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
