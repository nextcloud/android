/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 Wikinaut <mail@wikinaut.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.SslUntrustedCertLayoutBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import androidx.annotation.NonNull;

/**
 * Show certificate information.
 */
public class X509CertificateViewAdapter implements SslUntrustedCertDialog.CertificateViewAdapter {

    private final X509Certificate mCertificate;

    private static final String TAG = X509CertificateViewAdapter.class.getSimpleName();

    public X509CertificateViewAdapter(X509Certificate certificate) {
        mCertificate = certificate;
    }

    @Override
    public void updateCertificateView(@NonNull SslUntrustedCertLayoutBinding binding) {
        if (mCertificate != null) {
            binding.nullCert.setVisibility(View.GONE);
            showSubject(mCertificate.getSubjectX500Principal(), binding);
            showIssuer(mCertificate.getIssuerX500Principal(), binding);
            showValidity(mCertificate.getNotBefore(), mCertificate.getNotAfter(), binding);
            showSignature(binding);

        } else {
            binding.nullCert.setVisibility(View.VISIBLE);
        }
    }

    private byte[] getDigest(String algorithm, byte[] message) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md.reset();
        return md.digest(message);
    }

    private void showSignature(@NonNull SslUntrustedCertLayoutBinding binding) {
        byte[] cert;

        try {
            cert = mCertificate.getEncoded();
            if (cert == null) {
                binding.valueCertificateFingerprint.setText(R.string.certificate_load_problem);
                binding.valueSignatureAlgorithm.setText(R.string.certificate_load_problem);
            } else {
                binding.valueCertificateFingerprint.setText(getDigestString(binding.valueCertificateFingerprint.getContext(), cert));
                binding.valueSignatureAlgorithm.setText(mCertificate.getSigAlgName());
            }
        } catch (CertificateEncodingException e) {
            Log_OC.e(TAG, "Problem while trying to decode the certificate.");
        }
    }

    @NonNull
    private String getDigestString(Context context, byte[] cert) {
        return getDigestHexBytesWithColonsAndNewLines(context, "SHA-256", cert)
                + getDigestHexBytesWithColonsAndNewLines(context, "SHA-1", cert)
                + getDigestHexBytesWithColonsAndNewLines(context, "MD5", cert);
    }

    private String getDigestHexBytesWithColonsAndNewLines(Context context, final String digestType, final byte [] cert) {
        final byte[] rawDigest;
        final String newLine = System.lineSeparator();

        rawDigest = getDigest(digestType, cert);

        if ( rawDigest == null) {
            return digestType + ":" + newLine + context.getString(R.string.digest_algorithm_not_available) + newLine + newLine;
        }

        final StringBuilder hex = new StringBuilder(3 * rawDigest.length);

        for (final byte b : rawDigest) {
           final int hiVal = (b & 0xF0) >> 4;
           final int loVal = b & 0x0F;
           hex.append((char) ('0' + (hiVal + (hiVal / 10 * 7))));
           hex.append((char) ('0' + (loVal + (loVal / 10 * 7))));
           hex.append(':');
        }
        return digestType + ":" + newLine + hex.toString().replaceFirst("\\:$","") + newLine + newLine;
    }

    private void showValidity(Date notBefore, Date notAfter, @NonNull SslUntrustedCertLayoutBinding binding) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        binding.valueValidityFrom.setText(dateFormat.format(notBefore));
        binding.valueValidityTo.setText(dateFormat.format(notAfter));
    }

    private void showSubject(X500Principal subject, @NonNull SslUntrustedCertLayoutBinding binding) {
        Map<String, String> s = parsePrincipal(subject);

        if (s.get("CN") != null) {
            binding.valueSubjectCN.setText(s.get("CN"));
            binding.valueSubjectCN.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectCN.setVisibility(View.GONE);
        }
        if (s.get("O") != null) {
            binding.valueSubjectO.setText(s.get("O"));
            binding.valueSubjectO.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectO.setVisibility(View.GONE);
        }
        if (s.get("OU") != null) {
            binding.valueSubjectOU.setText(s.get("OU"));
            binding.valueSubjectOU.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectOU.setVisibility(View.GONE);
        }
        if (s.get("C") != null) {
            binding.valueSubjectC.setText(s.get("C"));
            binding.valueSubjectC.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectC.setVisibility(View.GONE);
        }
        if (s.get("ST") != null) {
            binding.valueSubjectST.setText(s.get("ST"));
            binding.valueSubjectST.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectST.setVisibility(View.GONE);
        }
        if (s.get("L") != null) {
            binding.valueSubjectL.setText(s.get("L"));
            binding.valueSubjectL.setVisibility(View.VISIBLE);
        } else {
            binding.valueSubjectL.setVisibility(View.GONE);
        }
    }

    private void showIssuer(X500Principal issuer, @NonNull SslUntrustedCertLayoutBinding binding) {
        Map<String, String> s = parsePrincipal(issuer);

        if (s.get("CN") != null) {
            binding.valueIssuerCN.setText(s.get("CN"));
            binding.valueIssuerCN.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerCN.setVisibility(View.GONE);
        }
        if (s.get("O") != null) {
            binding.valueIssuerO.setText(s.get("O"));
            binding.valueIssuerO.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerO.setVisibility(View.GONE);
        }
        if (s.get("OU") != null) {
            binding.valueIssuerOU.setText(s.get("OU"));
            binding.valueIssuerOU.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerOU.setVisibility(View.GONE);
        }
        if (s.get("C") != null) {
            binding.valueIssuerC.setText(s.get("C"));
            binding.valueIssuerC.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerC.setVisibility(View.GONE);
        }
        if (s.get("ST") != null) {
            binding.valueIssuerST.setText(s.get("ST"));
            binding.valueIssuerST.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerST.setVisibility(View.GONE);
        }
        if (s.get("L") != null) {
            binding.valueIssuerL.setText(s.get("L"));
            binding.valueIssuerL.setVisibility(View.VISIBLE);
        } else {
            binding.valueIssuerL.setVisibility(View.GONE);
        }
    }


    private Map<String, String> parsePrincipal(Principal principal) {
        Map<String, String> result = new HashMap<>();
        String toParse = principal.getName();
        String[] pieces = toParse.split(",");
        String[] tokens = {"CN", "O", "OU", "C", "ST", "L"};
        for (String piece : pieces) {
            for (String token : tokens) {
                if (piece.startsWith(token + "=")) {
                    result.put(token, piece.substring(token.length() + 1));
                }
            }
        }
        return result;
    }
}
