/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.avatar

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.seconds

// Coil's default strategy serves a cached picture forever and also caches 404s. This one follows the server's
// max-age and revalidates with the ETag afterwards, like a browser does on the web.
@OptIn(ExperimentalCoilApi::class)
class CoilCacheStrategy : CacheStrategy {

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options
    ): CacheStrategy.ReadResult {
        if (cacheResponse.isFresh()) {
            return CacheStrategy.ReadResult(cacheResponse)
        }

        val eTag = cacheResponse.headers[ETAG] ?: return CacheStrategy.ReadResult(networkRequest)
        val headers = networkRequest.headers.newBuilder().set(IF_NONE_MATCH, eTag).build()
        return CacheStrategy.ReadResult(networkRequest.copy(headers = headers))
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options
    ): CacheStrategy.WriteResult {
        if (networkResponse.code == HttpURLConnection.HTTP_NOT_MODIFIED && cacheResponse != null) {
            val headers = cacheResponse.headers.mergedWith(networkResponse.headers)
            return CacheStrategy.WriteResult(networkResponse.copy(headers = headers, body = null))
        }

        if (networkResponse.code !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
            return CacheStrategy.WriteResult.DISABLED
        }

        return CacheStrategy.WriteResult(networkResponse)
    }

    private fun NetworkResponse.isFresh(): Boolean {
        val cacheControl = headers[CACHE_CONTROL] ?: return false
        val maxAge = MAX_AGE.find(cacheControl)?.groupValues?.get(1)?.toLongOrNull() ?: return false
        return System.currentTimeMillis() - responseMillis < maxAge.seconds.inWholeMilliseconds
    }

    private fun NetworkHeaders.mergedWith(other: NetworkHeaders): NetworkHeaders {
        val builder = newBuilder()
        other.asMap().forEach { (key, values) -> builder[key] = values }
        return builder.build()
    }

    companion object {
        private const val CACHE_CONTROL = "Cache-Control"
        private const val ETAG = "ETag"
        private const val IF_NONE_MATCH = "If-None-Match"
        private val MAX_AGE = Regex("max-age=(\\d+)")
    }
}
