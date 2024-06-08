/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.adapter;

import android.net.http.SslCertificate;
import android.view.View;

import com.owncloud.android.databinding.SslUntrustedCertLayoutBinding;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;

import java.text.DateFormat;
import java.util.Date;

import androidx.annotation.NonNull;

public class SslCertificateViewAdapter implements SslUntrustedCertDialog.CertificateViewAdapter {

    //private final static String TAG = SslCertificateViewAdapter.class.getSimpleName();

    private final SslCertificate mCertificate;

    /**
     * Constructor
     *
     * @param certificate the SSL certificate
     */
    public SslCertificateViewAdapter(SslCertificate certificate) {
        mCertificate = certificate;
    }

    @Override
    public void updateCertificateView(@NonNull SslUntrustedCertLayoutBinding binding) {
        if (mCertificate != null) {
            binding.nullCert.setVisibility(View.GONE);
            showSubject(mCertificate.getIssuedTo(), binding);
            showIssuer(mCertificate.getIssuedBy(), binding);
            showValidity(mCertificate.getValidNotBeforeDate(), mCertificate.getValidNotAfterDate(), binding);
        } else {
            binding.nullCert.setVisibility(View.VISIBLE);
        }
    }

    private void showValidity(Date notBefore, Date notAfter, @NonNull SslUntrustedCertLayoutBinding binding) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        binding.valueValidityFrom.setText(dateFormat.format(notBefore));
        binding.valueValidityTo.setText(dateFormat.format(notAfter));
    }


    private void showSubject(SslCertificate.DName subject, @NonNull SslUntrustedCertLayoutBinding binding) {
        binding.valueSubjectCN.setText(subject.getCName());
        binding.valueSubjectCN.setVisibility(View.VISIBLE);

        binding.valueSubjectO.setText(subject.getOName());
        binding.valueSubjectO.setVisibility(View.VISIBLE);

        binding.valueSubjectOU.setText(subject.getUName());
        binding.valueSubjectOU.setVisibility(View.VISIBLE);

        // SslCertificates don't offer this information
        binding.valueSubjectC.setVisibility(View.GONE);
        binding.valueSubjectST.setVisibility(View.GONE);
        binding.valueSubjectL.setVisibility(View.GONE);

        binding.labelSubjectC.setVisibility(View.GONE);
        binding.labelSubjectST.setVisibility(View.GONE);
        binding.labelSubjectL.setVisibility(View.GONE);
    }

    private void showIssuer(SslCertificate.DName issuer, @NonNull SslUntrustedCertLayoutBinding binding) {
        binding.valueIssuerCN.setText(issuer.getCName());
        binding.valueIssuerCN.setVisibility(View.VISIBLE);

        binding.valueIssuerO.setText(issuer.getOName());
        binding.valueIssuerO.setVisibility(View.VISIBLE);

        binding.valueIssuerOU.setText(issuer.getUName());
        binding.valueIssuerOU.setVisibility(View.VISIBLE);

        // SslCertificates don't offer this information
        binding.valueIssuerC.setVisibility(View.GONE);
        binding.valueIssuerST.setVisibility(View.GONE);
        binding.valueIssuerL.setVisibility(View.GONE);

        binding.labelIssuerC.setVisibility(View.GONE);
        binding.labelIssuerST.setVisibility(View.GONE);
        binding.labelIssuerL.setVisibility(View.GONE);
    }
}
