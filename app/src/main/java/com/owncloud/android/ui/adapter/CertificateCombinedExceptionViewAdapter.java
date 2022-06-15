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
package com.owncloud.android.ui.adapter;

import android.view.View;

import com.owncloud.android.databinding.SslUntrustedCertLayoutBinding;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;

/**
 * TODO
 */
public class CertificateCombinedExceptionViewAdapter implements SslUntrustedCertDialog.ErrorViewAdapter {

    //private final static String TAG = CertificateCombinedExceptionViewAdapter.class.getSimpleName();

    private CertificateCombinedException mSslException;

    public CertificateCombinedExceptionViewAdapter(CertificateCombinedException sslException) {
        mSslException = sslException;
    }

    @Override
    public void updateErrorView(SslUntrustedCertLayoutBinding binding) {
        /// clean
        binding.reasonNoInfoAboutError.setVisibility(View.GONE);

        /// refresh
        if (mSslException.getCertPathValidatorException() != null) {
            binding.reasonCertNotTrusted.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotTrusted.setVisibility(View.GONE);
        }

        if (mSslException.getCertificateExpiredException() != null) {
            binding.reasonCertExpired.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertExpired.setVisibility(View.GONE);
        }

        if (mSslException.getCertificateNotYetValidException() != null) {
            binding.reasonCertNotYetValid.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotYetValid.setVisibility(View.GONE);
        }

        if (mSslException.getSslPeerUnverifiedException() != null) {
            binding.reasonHostnameNotVerified.setVisibility(View.VISIBLE);
        } else {
            binding.reasonHostnameNotVerified.setVisibility(View.GONE);
        }

    }
}
