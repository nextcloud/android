/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStream

/**
 * Fetcher with Nextcloud client
 */
class GlideStringStreamFetcher(private val url: String?) : DataFetcher<InputStream> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        var get: GetMethod? = null
        try {
            val ownCloudAccount =
                UserAccountManagerImpl.fromContext(MainApp.getAppContext()).currentOwnCloudAccount
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(ownCloudAccount, MainApp.getAppContext())
            get = GetMethod(url)
            get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
            get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)
            val status = client.executeMethod(get)
            if (status == HttpStatus.SC_OK) {
                callback.onDataReady(get.getResponseBodyAsStream())
            } else {
                client.exhaustResponse(get.getResponseBodyAsStream())
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message, e)
        } finally {
            get?.releaseConnection()
        }
    }

    override fun cleanup() {
        Log_OC.i(TAG, "Cleanup")
    }

    override fun cancel() {
        Log_OC.i(TAG, "Cancel")
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }

    companion object {
        private val TAG: String = GlideStringStreamFetcher::class.java.getName()
    }
}
