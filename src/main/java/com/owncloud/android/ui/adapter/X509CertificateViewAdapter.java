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

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

/**
 *
 */
public class X509CertificateViewAdapter implements SslUntrustedCertDialog.CertificateViewAdapter {
    
    //private final static String TAG = X509CertificateViewAdapter.class.getSimpleName();
    
    private X509Certificate mCertificate = null;

    private static final String TAG = X509CertificateViewAdapter.class.getSimpleName();

    public X509CertificateViewAdapter(X509Certificate certificate) {
        mCertificate = certificate;
    }
    
    @Override
    public void updateCertificateView(View dialogView) {
        TextView nullCerView = (TextView) dialogView.findViewById(R.id.null_cert);
        
        if (mCertificate != null) {
            nullCerView.setVisibility(View.GONE);
            showSubject(mCertificate.getSubjectX500Principal(), dialogView);
            showIssuer(mCertificate.getIssuerX500Principal(), dialogView);
            showValidity(mCertificate.getNotBefore(), mCertificate.getNotAfter(), dialogView);
            showSignature(dialogView);
            
        } else {
            nullCerView.setVisibility(View.VISIBLE);
        }
    }

    private byte[] getDigest(String algorithm, byte[] message) {
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md.reset();
        return md.digest(message);
    }

    private void showSignature(View dialogView) {
        byte[] cert = null;

        TextView certFingerprintView = ((TextView) dialogView.findViewById(R.id.value_certificate_fingerprint));
        TextView algorithmView = ((TextView) dialogView.findViewById(R.id.value_signature_algorithm));

        try {
            cert = mCertificate.getEncoded();
            if (cert == null) {

                certFingerprintView.setText(R.string.certificate_load_problem);
                algorithmView.setText(R.string.certificate_load_problem);

            } else {

                certFingerprintView.setText(
                        getDigestHexBytesWithColonsAndNewLines(dialogView, "SHA-256", cert)
                                + getDigestHexBytesWithColonsAndNewLines(dialogView, "SHA-1", cert)
                                + getDigestHexBytesWithColonsAndNewLines(dialogView, "MD5", cert));
                algorithmView.setText(mCertificate.getSigAlgName());

            }

        } catch (CertificateEncodingException e) {
            Log.e(TAG, "Problem while trying to decode the certificate.");
        }


    }
    
    private final String getDigestHexBytesWithColonsAndNewLines(View dialogView, final String digestType, final byte [] cert) {
        final byte[] rawDigest;
        final String newLine = System.getProperty("line.separator");

        rawDigest = getDigest(digestType, cert);

        if ( rawDigest == null) {
            return digestType + ":" + newLine + dialogView.getContext().getString(R.string.digest_algorithm_not_available) + newLine + newLine;
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

    private void showValidity(Date notBefore, Date notAfter, View dialogView) {
        TextView fromView = ((TextView)dialogView.findViewById(R.id.value_validity_from));
        TextView toView = ((TextView)dialogView.findViewById(R.id.value_validity_to));
        DateFormat dateFormat = DateFormat.getDateInstance();
        fromView.setText(dateFormat.format(notBefore));
        toView.setText(dateFormat.format(notAfter));
    }

    private void showSubject(X500Principal subject, View dialogView) {
        Map<String, String> s = parsePrincipal(subject);
        TextView cnView = ((TextView)dialogView.findViewById(R.id.value_subject_CN));
        TextView oView = ((TextView)dialogView.findViewById(R.id.value_subject_O));
        TextView ouView = ((TextView)dialogView.findViewById(R.id.value_subject_OU));
        TextView cView = ((TextView)dialogView.findViewById(R.id.value_subject_C));
        TextView stView = ((TextView)dialogView.findViewById(R.id.value_subject_ST));
        TextView lView = ((TextView)dialogView.findViewById(R.id.value_subject_L));
        
        if (s.get("CN") != null) {
            cnView.setText(s.get("CN"));
            cnView.setVisibility(View.VISIBLE);
        } else {
            cnView.setVisibility(View.GONE);
        }
        if (s.get("O") != null) {
            oView.setText(s.get("O"));
            oView.setVisibility(View.VISIBLE);
        } else {
            oView.setVisibility(View.GONE);
        }
        if (s.get("OU") != null) {
            ouView.setText(s.get("OU"));
            ouView.setVisibility(View.VISIBLE);
        } else {
            ouView.setVisibility(View.GONE);
        }
        if (s.get("C") != null) {
            cView.setText(s.get("C"));
            cView.setVisibility(View.VISIBLE);
        } else {
            cView.setVisibility(View.GONE);
        }
        if (s.get("ST") != null) {
            stView.setText(s.get("ST"));
            stView.setVisibility(View.VISIBLE);
        } else {
            stView.setVisibility(View.GONE);
        }
        if (s.get("L") != null) {
            lView.setText(s.get("L"));
            lView.setVisibility(View.VISIBLE);
        } else {
            lView.setVisibility(View.GONE);
        }
    }
    
    private void showIssuer(X500Principal issuer, View dialogView) {
        Map<String, String> s = parsePrincipal(issuer);
        TextView cnView = ((TextView)dialogView.findViewById(R.id.value_issuer_CN));
        TextView oView = ((TextView)dialogView.findViewById(R.id.value_issuer_O));
        TextView ouView = ((TextView)dialogView.findViewById(R.id.value_issuer_OU));
        TextView cView = ((TextView)dialogView.findViewById(R.id.value_issuer_C));
        TextView stView = ((TextView)dialogView.findViewById(R.id.value_issuer_ST));
        TextView lView = ((TextView)dialogView.findViewById(R.id.value_issuer_L));
        
        if (s.get("CN") != null) {
            cnView.setText(s.get("CN"));
            cnView.setVisibility(View.VISIBLE);
        } else {
            cnView.setVisibility(View.GONE);
        }
        if (s.get("O") != null) {
            oView.setText(s.get("O"));
            oView.setVisibility(View.VISIBLE);
        } else {
            oView.setVisibility(View.GONE);
        }
        if (s.get("OU") != null) {
            ouView.setText(s.get("OU"));
            ouView.setVisibility(View.VISIBLE);
        } else {
            ouView.setVisibility(View.GONE);
        }
        if (s.get("C") != null) {
            cView.setText(s.get("C"));
            cView.setVisibility(View.VISIBLE);
        } else {
            cView.setVisibility(View.GONE);
        }
        if (s.get("ST") != null) {
            stView.setText(s.get("ST"));
            stView.setVisibility(View.VISIBLE);
        } else {
            stView.setVisibility(View.GONE);
        }
        if (s.get("L") != null) {
            lView.setText(s.get("L"));
            lView.setVisibility(View.VISIBLE);
        } else {
            lView.setVisibility(View.GONE);
        }
    }
    

    private Map<String, String> parsePrincipal(X500Principal principal) {
        Map<String, String> result = new HashMap<String, String>();
        String toParse = principal.getName();
        String[] pieces = toParse.split(",");
        String[] tokens = {"CN", "O", "OU", "C", "ST", "L"}; 
        for (int i=0; i < pieces.length ; i++) {
            for (int j=0; j<tokens.length; j++) {
                if (pieces[i].startsWith(tokens[j] + "=")) {
                    result.put(tokens[j], pieces[i].substring(tokens[j].length()+1));
                }
            }
        }
        return result;
    }

}
