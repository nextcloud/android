/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import android.content.Context
import com.nextcloud.common.PlainClient
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.utils.Log_OC
import okhttp3.OkHttpClient
import java.io.IOException
import java.security.GeneralSecurityException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

/**
 * Library client for requests that the app does not send through a library client itself (web views, Glide).
 */
object CustomHeadersHttpClient {
    private const val TAG = "CustomHeadersHttpClient"

    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    private var clientWithoutRedirects: OkHttpClient? = null

    /**
     * The library client accepts any host name. Web views reject a mismatching certificate unless the user
     * accepted it, so this transport does the same before it sends custom headers.
     */
    fun get(context: Context): OkHttpClient = client ?: synchronized(this) {
        val appContext = context.applicationContext
        client ?: PlainClient(appContext).client.newBuilder()
            .hostnameVerifier(hostnameVerifier(appContext))
            .build()
            .also { client = it }
    }

    /**
     * Web views must see redirects themselves: following them here would serve the final body under the original URL.
     */
    fun withoutRedirects(context: Context): OkHttpClient = clientWithoutRedirects ?: synchronized(this) {
        clientWithoutRedirects ?: get(context).newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
            .also { clientWithoutRedirects = it }
    }

    private fun hostnameVerifier(context: Context) = HostnameVerifier { host, session ->
        HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session) || isAcceptedByUser(context, session)
    }

    private fun isAcceptedByUser(context: Context, session: SSLSession): Boolean = try {
        val certificate = session.peerCertificates.firstOrNull()
        certificate != null && NetworkUtils.isCertInKnownServersStore(certificate, context)
    } catch (e: GeneralSecurityException) {
        Log_OC.e(TAG, "accepted certificates not readable: ${e.javaClass.simpleName}")
        false
    } catch (e: IOException) {
        Log_OC.e(TAG, "peer certificate or accepted certificates not readable: ${e.javaClass.simpleName}")
        false
    }
}
