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
import java.io.IOException
import java.io.InputStream

/**
 * Fetcher with Nextcloud client
 */
class GlideStringStreamFetcher(private val url: String?) : DataFetcher<InputStream> {

    private var stream: InputStream? = null
    private var get: GetMethod? = null

    @Suppress("TooGenericExceptionCaught")
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val ownCloudAccount = UserAccountManagerImpl.fromContext(MainApp.getAppContext()).currentOwnCloudAccount
            val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ownCloudAccount, MainApp.getAppContext())

            get = GetMethod(url)
            get?.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
            get?.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)

            val status = client?.executeMethod(get)
            if (status == HttpStatus.SC_OK) {
                stream = get?.responseBodyAsStream
                stream?.let { callback.onDataReady(it) } ?: callback.onLoadFailed(IOException("Stream is null"))
            } else {
                client?.exhaustResponse(get?.responseBodyAsStream)
                callback.onLoadFailed(IOException("Unexpected HTTP status $status"))
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception GlideStringStreamFetcher.loadData: $e")
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {
        try {
            stream?.close()
        } catch (_: IOException) {
        } finally {
            get?.releaseConnection()
        }
    }

    override fun cancel() {
        try {
            get?.abort()
        } catch (_: Exception) {
        }
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE

    companion object {
        private val TAG: String = GlideStringStreamFetcher::class.java.getName()
    }
}
