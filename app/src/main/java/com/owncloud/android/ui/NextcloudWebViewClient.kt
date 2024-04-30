/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Elv1zz <elv1zz.git@gmail.com>
 * SPDX-FileCopyrightText: 2022 Unpublished <unpublished@gmx.net>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import android.annotation.SuppressLint
import android.net.http.SslCertificate
import android.net.http.SslError
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentManager
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.network.AdvancedX509KeyManager
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import org.apache.commons.httpclient.HttpStatus
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

open class NextcloudWebViewClient(val supportFragmentManager: FragmentManager) : WebViewClient() {

    private val tag: String? = NextcloudWebViewClient::class.simpleName

    @Suppress("TooGenericExceptionCaught")
    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
        val cert = error?.let { getX509CertificateFromError(it) }
        try {
            if (NetworkUtils.isCertInKnownServersStore(cert, view?.context?.applicationContext)) {
                handler.proceed()
            } else {
                showUntrustedCertDialog(cert, error, handler)
            }
        } catch (e: Exception) {
            Log_OC.e(tag, "Cert could not be verified")
        }
    }

    /**
     * Obtain the X509Certificate from SslError
     *
     * @param error SslError
     * @return X509Certificate from error
     */
    open fun getX509CertificateFromError(error: SslError): X509Certificate? {
        val bundle = SslCertificate.saveState(error.certificate)
        val x509Certificate: X509Certificate?
        val bytes = bundle.getByteArray("x509-certificate")
        x509Certificate = if (bytes == null) {
            null
        } else {
            try {
                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(ByteArrayInputStream(bytes))
                cert as X509Certificate
            } catch (e: CertificateException) {
                null
            }
        }
        return x509Certificate
    }

    /**
     * Show untrusted cert dialog
     */
    private fun showUntrustedCertDialog(
        x509Certificate: X509Certificate?,
        error: SslError?,
        handler: SslErrorHandler?
    ) {
        // Show a dialog with the certificate info
        val dialog: SslUntrustedCertDialog = if (x509Certificate == null) {
            SslUntrustedCertDialog.newInstanceForEmptySslError(error, handler)
        } else {
            SslUntrustedCertDialog.newInstanceForFullSslError(x509Certificate, error, handler)
        }
        val fm: FragmentManager = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog.show(ft, AuthenticatorActivity.UNTRUSTED_CERT_DIALOG_TAG)
    }

    /**
     * Handle request for a TLS client certificate.
     */
    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        if (view == null || request == null) {
            return
        }
        AdvancedX509KeyManager(view.context).handleWebViewClientCertRequest(request)
    }

    /**
     * Handle HTTP errors.
     *
     * We might receive an HTTP status code 400 (bad request), which probably tells us that our certificate
     * is not valid (anymore), e.g. because it expired. In that case we forget the selected client certificate,
     * so it can be re-selected.
     */
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        val errorCode = errorResponse?.statusCode ?: return
        if (errorCode == HttpStatus.SC_BAD_REQUEST) {
            // chosen client certificate alias does not seem to work -> discard it
            val failingUrl = request?.url
            val context = view?.context
            if (failingUrl == null || context == null) {
                return
            }
            Log_OC.w(tag, "WebView failed with error code $errorCode; remove key chain aliases")
            AdvancedX509KeyManager(context).removeKeys(failingUrl)
        }
    }
}
