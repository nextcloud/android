/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Fetcher with OwnCloudClient
 */
@Suppress("TooGenericExceptionCaught")
class HttpStreamFetcher internal constructor(
    private val user: User,
    private val clientFactory: ClientFactory,
    private val url: String
) : DataFetcher<InputStream?> {
    @Throws(Exception::class)
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream?>) {
        val client = clientFactory.create(user)

        if (client != null && url.isNotBlank()) {
            var get: GetMethod? = null
            try {
                get = GetMethod(url)
                get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
                get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)
                val status = client.executeMethod(get)
                if (status == HttpStatus.SC_OK) {
                    callback.onDataReady(getResponseAsInputStream(get))
                } else {
                    client.exhaustResponse(get.responseBodyAsStream)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, e.message, e)
            } finally {
                get?.releaseConnection()
            }
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
    }

    fun getId(): String {
        return url
    }

    override fun cancel() {
        Log_OC.i(TAG, "Cancel")
    }

    override fun getDataClass(): Class<InputStream?> {
        TODO("Not yet implemented")
    }

    override fun getDataSource(): DataSource {
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = HttpStreamFetcher::class.java.name
    }
}
