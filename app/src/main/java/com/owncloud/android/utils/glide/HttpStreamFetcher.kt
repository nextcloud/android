/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import java.io.IOException
import java.io.InputStream

@Suppress("TooGenericExceptionCaught")
class HttpStreamFetcher internal constructor(private val url: String) : DataFetcher<InputStream> {

    private var stream: InputStream? = null
    private var get: GetMethod? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val ownCloudAccount = UserAccountManagerImpl.fromContext(MainApp.getAppContext()).currentOwnCloudAccount
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(ownCloudAccount, MainApp.getAppContext())

            if (client == null || url.isBlank()) {
                callback.onLoadFailed(IllegalStateException("Invalid client or URL"))
                return
            }

            get = GetMethod(url)
            get?.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
            get?.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)

            val status = client.executeMethod(get)
            if (status == HttpStatus.SC_OK) {
                stream = get?.responseBodyAsStream
                stream?.let { callback.onDataReady(it) } ?: callback.onLoadFailed(IOException("Stream is null"))
            } else {
                client.exhaustResponse(get?.responseBodyAsStream)
                callback.onLoadFailed(IOException("Unexpected HTTP status $status"))
            }
        } catch (e: Exception) {
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

    fun getId(): String = url

    override fun cancel() {
        try {
            get?.abort()
        } catch (_: Exception) {
        }
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
