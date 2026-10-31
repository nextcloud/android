/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException
import java.io.InputStream

class CustomHeadersGlideUrlFetcher(private val model: GlideUrl, private val client: OkHttpClient) :
    DataFetcher<InputStream> {

    private var call: Call? = null
    private var body: ResponseBody? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val request = Request.Builder().url(model.toStringUrl())
            .apply { model.headers.forEach { (name, value) -> header(name, value) } }
            .build()
        try {
            val newCall = client.newCall(request).also { call = it }
            val response = newCall.execute()
            body = response.body
            if (!response.isSuccessful) {
                callback.onLoadFailed(IOException("Unexpected HTTP status ${response.code}"))
                return
            }
            callback.onDataReady(response.body.byteStream())
        } catch (e: IOException) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {
        body?.close()
    }

    override fun cancel() {
        call?.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
