/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.SslErrorHandler
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.SslUntrustedCertLayoutBinding
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.CertificateCombinedExceptionViewAdapter
import com.owncloud.android.ui.adapter.SslCertificateViewAdapter
import com.owncloud.android.ui.adapter.SslErrorViewAdapter
import com.owncloud.android.ui.adapter.X509CertificateViewAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.cert.X509Certificate
import javax.inject.Inject

/**
 * Dialog to show information about an untrusted certificate and allow the user to decide trust on it or not.
 * Abstract implementation of common functionality for different dialogs that get the information about the error and
 * the certificate from different classes.
 */
open class SslUntrustedCertDialog : DialogFragment(), Injectable {
    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    protected var binding: SslUntrustedCertLayoutBinding? = null
    protected var mHandler: SslErrorHandler? = null
    protected var m509Certificate: X509Certificate? = null

    private var mErrorViewAdapter: ErrorViewAdapter? = null
    private var mCertificateViewAdapter: CertificateViewAdapter? = null

    override fun onAttach(context: Context) {
        Log_OC.d(TAG, "onAttach")
        super.onAttach(context)
        require(activity is OnSslUntrustedCertListener) {
            "The host activity must implement " + OnSslUntrustedCertListener::class.java.canonicalName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate, savedInstanceState is $savedInstanceState")
        super.onCreate(savedInstanceState)

        // force to keep the state of the fragment on configuration changes (such as device rotations)
        retainInstance = true
        isCancelable = false
        binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log_OC.d(TAG, "onCreateDialog, savedInstanceState is $savedInstanceState")

        binding = SslUntrustedCertLayoutBinding.inflate(layoutInflater, null, false)
        binding?.detailsScroll?.visibility = View.GONE
        mErrorViewAdapter?.updateErrorView(binding)

        binding?.ok?.setOnClickListener(OnCertificateTrusted())

        binding?.cancel?.setOnClickListener(OnCertificateNotTrusted())

        binding?.detailsBtn?.setOnClickListener { v: View ->
            if (binding?.detailsScroll?.visibility == View.VISIBLE) {
                binding?.detailsScroll?.visibility = View.GONE
                (v as Button).setText(R.string.ssl_validator_btn_details_see)
            } else {
                binding?.detailsScroll?.visibility = View.VISIBLE
                (v as Button).setText(R.string.ssl_validator_btn_details_hide)
                mCertificateViewAdapter?.updateCertificateView(binding)
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding?.getRoot())

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create().apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onDestroyView() {
        Log_OC.d(TAG, "onDestroyView")
        if (retainInstance) {
            dialog?.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    private inner class OnCertificateNotTrusted : View.OnClickListener {
        override fun onClick(v: View) {
            dialog?.cancel()
            mHandler?.cancel()
        }
    }

    private inner class OnCertificateTrusted : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            mHandler?.proceed()

            if (m509Certificate == null) {
                Log_OC.d(TAG, "m509Certificate is null onClick dismissed")
                return
            }

            if (activity == null) {
                Log_OC.d(TAG, "activity is null onClick dismissed")
                return
            }

            try {
                // TODO make this asynchronously, it can take some time
                NetworkUtils.addCertToKnownServersStore(m509Certificate, activity)
                (activity as OnSslUntrustedCertListener?)?.onSavedCertificate()
            } catch (e: GeneralSecurityException) {
                (activity as OnSslUntrustedCertListener?)?.onFailedSavingCertificate()
                Log_OC.e(TAG, "Server certificate could not be saved in the known-servers trust store ", e)
            } catch (e: IOException) {
                (activity as OnSslUntrustedCertListener?)?.onFailedSavingCertificate()
                Log_OC.e(TAG, "Server certificate could not be saved in the known-servers trust store ", e)
            }
        }
    }

    interface OnSslUntrustedCertListener {
        fun onSavedCertificate()
        fun onFailedSavingCertificate()
    }

    interface ErrorViewAdapter {
        fun updateErrorView(binding: SslUntrustedCertLayoutBinding?)
    }

    interface CertificateViewAdapter {
        fun updateCertificateView(binding: SslUntrustedCertLayoutBinding?)
    }

    companion object {
        private val TAG: String = SslUntrustedCertDialog::class.java.simpleName

        @JvmStatic
        fun newInstanceForEmptySslError(error: SslError?, handler: SslErrorHandler?): SslUntrustedCertDialog {
            requireNotNull(error) { "Trying to create instance with parameter error == null" }
            requireNotNull(handler) { "Trying to create instance with parameter handler == null" }

            return SslUntrustedCertDialog().apply {
                mHandler = handler
                mErrorViewAdapter = SslErrorViewAdapter(error)
                mCertificateViewAdapter = SslCertificateViewAdapter(error.certificate)
            }
        }

        @JvmStatic
        fun newInstanceForFullSslError(sslException: CertificateCombinedException?): SslUntrustedCertDialog {
            requireNotNull(sslException) { "Trying to create instance with parameter sslException == null" }

            return SslUntrustedCertDialog().apply {
                m509Certificate = sslException.serverCertificate
                mErrorViewAdapter = CertificateCombinedExceptionViewAdapter(sslException)
                mCertificateViewAdapter = X509CertificateViewAdapter(sslException.serverCertificate)
            }
        }

        fun newInstanceForFullSslError(
            cert: X509Certificate?,
            error: SslError?,
            handler: SslErrorHandler?
        ): SslUntrustedCertDialog {
            requireNotNull(cert) { "Trying to create instance with parameter cert == null" }
            requireNotNull(error) { "Trying to create instance with parameter error == null" }
            requireNotNull(handler) { "Trying to create instance with parameter handler == null" }

            return SslUntrustedCertDialog().apply {
                m509Certificate = cert
                mHandler = handler
                mErrorViewAdapter = SslErrorViewAdapter(error)
                mCertificateViewAdapter = X509CertificateViewAdapter(cert)
            }
        }
    }
}
