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
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Suppress("TooGenericExceptionCaught")
class HttpStreamFetcher internal constructor(private val url: String) : DataFetcher<InputStream> {

    private var stream: InputStream? = null

    @Throws(Exception::class)
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val ownCloudAccount =
            UserAccountManagerImpl.fromContext(MainApp.getAppContext()).currentOwnCloudAccount
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(ownCloudAccount, MainApp.getAppContext())
        if (client == null || url.isBlank()) {
            callback.onLoadFailed(IllegalStateException("Invalid client or URL"))
            return
        }

        var get: GetMethod? = null
        try {
            get = GetMethod(url)
            get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
            get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)

            val status = client.executeMethod(get)
            if (status == HttpStatus.SC_OK) {
                val inputStream = getResponseAsInputStream(get)
                this.stream = inputStream
                callback.onDataReady(inputStream)
            } else {
                client.exhaustResponse(get.responseBodyAsStream)
                callback.onLoadFailed(IOException("Unexpected HTTP status $status"))
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message, e)
            callback.onLoadFailed(e)
        } finally {
            get?.releaseConnection()
        }
    }

    private fun getResponseAsInputStream(getMethod: GetMethod): ByteArrayInputStream {
        val byteOutputStream = ByteArrayOutputStream()
        getMethod.responseBodyAsStream.use { input ->
            byteOutputStream.use { output ->
                input.copyTo(output)
            }
        }

        return ByteArrayInputStream(byteOutputStream.toByteArray())
    }

    override fun cleanup() {
        Log_OC.i(TAG, "Cleanup")
        try {
            stream?.close()
        } catch (e: IOException) {
            Log_OC.w(TAG, "Cleanup failed$e")
        }
    }

    fun getId(): String = url

    override fun cancel() {
        Log_OC.i(TAG, "Cancel")
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE

    companion object {
        private val TAG = HttpStreamFetcher::class.java.name
    }
}
