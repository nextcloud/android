/*
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Alejandro Bautista
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.owncloud.android.utils.glide

import com.bumptech.glide.Priority
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
    override fun loadData(priority: Priority): InputStream? {
        val client = clientFactory.create(user)
        if (client != null) {
            var get: GetMethod? = null
            try {
                get = GetMethod(url)
                get.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
                get.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)
                val status = client.executeMethod(get)
                if (status == HttpStatus.SC_OK) {
                    val byteOutputStream = ByteArrayOutputStream()
                    get.responseBodyAsStream.use { input ->
                        byteOutputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    return ByteArrayInputStream(byteOutputStream.toByteArray())
                } else {
                    client.exhaustResponse(get.responseBodyAsStream)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, e.message, e)
            } finally {
                get?.releaseConnection()
            }
        }
        return null
    }

    override fun cleanup() {
        Log_OC.i(TAG, "Cleanup")
    }

    override fun getId(): String {
        return url
    }

    override fun cancel() {
        Log_OC.i(TAG, "Cancel")
    }

    companion object {
        private val TAG = HttpStreamFetcher::class.java.name
    }
}
