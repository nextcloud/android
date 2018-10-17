/*
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

import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.InputStream;

/**
 * Fetcher with Nextcloud client
 */
public class HttpStreamGlideContainerFetcher implements DataFetcher<InputStream> {

    private static final String TAG = HttpStreamGlideContainerFetcher.class.getName();
    private final GlideContainer container;

    public HttpStreamGlideContainerFetcher(GlideContainer container) {
        this.container = container;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        Log_OC.d(TAG, "load thumbnail for: " + container.url);

        GetMethod get;
        try {
            get = new GetMethod(container.url);
            get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true");
            get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

            int status = container.client.executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                callback.onDataReady(get.getResponseBodyAsStream());
            } else {
                container.client.exhaustResponse(get.getResponseBodyAsStream());
                callback.onLoadFailed(new Exception("Thumbnail failed"));
            }
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage(), e);
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
