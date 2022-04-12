/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.widget.Button;

import com.owncloud.android.R;
import com.owncloud.android.databinding.SslUntrustedCertLayoutBinding;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.CertificateCombinedExceptionViewAdapter;
import com.owncloud.android.ui.adapter.SslCertificateViewAdapter;
import com.owncloud.android.ui.adapter.SslErrorViewAdapter;
import com.owncloud.android.ui.adapter.X509CertificateViewAdapter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to show information about an untrusted certificate and allow the user
 * to decide trust on it or not.
 *
 * Abstract implementation of common functionality for different dialogs that
 * get the information about the error and the certificate from different classes.
 */
public class SslUntrustedCertDialog extends DialogFragment {

    private final static String TAG = SslUntrustedCertDialog.class.getSimpleName();

    protected SslUntrustedCertLayoutBinding binding;
    protected SslErrorHandler mHandler;
    protected X509Certificate m509Certificate;

    private ErrorViewAdapter mErrorViewAdapter;
    private CertificateViewAdapter mCertificateViewAdapter;

    public static SslUntrustedCertDialog newInstanceForEmptySslError(SslError error, SslErrorHandler handler) {
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter error == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter handler == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.mHandler = handler;
        dialog.mErrorViewAdapter = new SslErrorViewAdapter(error);
        dialog.mCertificateViewAdapter = new SslCertificateViewAdapter(error.getCertificate());
        return dialog;
    }

    public static SslUntrustedCertDialog newInstanceForFullSslError(CertificateCombinedException sslException) {
        if (sslException == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter sslException == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.m509Certificate = sslException.getServerCertificate();
        dialog.mErrorViewAdapter = new CertificateCombinedExceptionViewAdapter(sslException);
        dialog.mCertificateViewAdapter = new X509CertificateViewAdapter(sslException.getServerCertificate());
        return dialog;
    }

    public static SslUntrustedCertDialog newInstanceForFullSslError(X509Certificate cert, SslError error, SslErrorHandler handler) {
        if (cert == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter cert == null");
        }
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter error == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter handler == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.m509Certificate = cert;
        dialog.mHandler = handler;
        dialog.mErrorViewAdapter = new SslErrorViewAdapter(error);
        dialog.mCertificateViewAdapter = new X509CertificateViewAdapter(cert);
        return dialog;
    }


    @Override
    public void onAttach(@NonNull Activity activity) {
        Log_OC.d(TAG, "onAttach");
        super.onAttach(activity);
        if (!(activity instanceof OnSslUntrustedCertListener)) {
            throw new IllegalArgumentException("The host activity must implement " + OnSslUntrustedCertListener.class.getCanonicalName());
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate, savedInstanceState is " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // force to keep the state of the fragment on configuration changes (such as device rotations)
        setCancelable(false);
        binding = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInsanceState is " + savedInstanceState);
        // Create a view by inflating desired layout
        if (binding == null) {
            binding = SslUntrustedCertLayoutBinding.inflate(inflater, container, false);
            binding.detailsScroll.setVisibility(View.GONE);
            mErrorViewAdapter.updateErrorView(binding);
        } else {
            ((ViewGroup) binding.getRoot().getParent()).removeView(binding.getRoot());
        }

        binding.ok.setOnClickListener(new OnCertificateTrusted());

        binding.cancel.setOnClickListener(new OnCertificateNotTrusted());

        binding.detailsBtn.setOnClickListener(v -> {
            if (binding.detailsScroll.getVisibility() == View.VISIBLE) {
                binding.detailsScroll.setVisibility(View.GONE);
                ((Button) v).setText(R.string.ssl_validator_btn_details_see);

            } else {
                binding.detailsScroll.setVisibility(View.VISIBLE);
                ((Button) v).setText(R.string.ssl_validator_btn_details_hide);
                mCertificateViewAdapter.updateCertificateView(binding);
            }
        });

        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateDialog, savedInstanceState is " + savedInstanceState);
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "onDestroyView");
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private class OnCertificateNotTrusted implements OnClickListener {

        @Override
        public void onClick(View v) {
            getDialog().cancel();
            if (mHandler != null) {
                mHandler.cancel();
            }
        }
    }


    private class OnCertificateTrusted implements OnClickListener {

        @Override
        public void onClick(View v) {
            dismiss();
            if (mHandler != null) {
                mHandler.proceed();
            }
            if (m509Certificate != null) {
                Activity activity = getActivity();
                try {
                    NetworkUtils.addCertToKnownServersStore(m509Certificate, activity);   // TODO make this asynchronously, it can take some time
                    ((OnSslUntrustedCertListener)activity).onSavedCertificate();
                } catch (GeneralSecurityException | IOException e) {
                    ((OnSslUntrustedCertListener)activity).onFailedSavingCertificate();
                    Log_OC.e(TAG, "Server certificate could not be saved in the known-servers trust store ", e);
                }
            }
        }

    }


    public interface OnSslUntrustedCertListener {
        void onSavedCertificate();

        void onFailedSavingCertificate();
    }

    public interface ErrorViewAdapter {
        void updateErrorView(SslUntrustedCertLayoutBinding binding);
    }

    public interface CertificateViewAdapter {
        void updateCertificateView(SslUntrustedCertLayoutBinding binding);
    }
}
