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

import android.net.http.SslCertificate;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;

import java.text.DateFormat;
import java.util.Date;

/**
 * TODO
 */
public class SslCertificateViewAdapter implements SslUntrustedCertDialog.CertificateViewAdapter {
    
    //private final static String TAG = SslCertificateViewAdapter.class.getSimpleName();
    
    private SslCertificate mCertificate;

    
    /**
     * Constructor
     * 
     * @param 
     */
    public SslCertificateViewAdapter(SslCertificate certificate) {
        mCertificate = certificate;
    }

    @Override
    public void updateCertificateView(View dialogView) {
        TextView nullCerView = (TextView) dialogView.findViewById(R.id.null_cert);
        if (mCertificate != null) {
            nullCerView.setVisibility(View.GONE);
            showSubject(mCertificate.getIssuedTo(), dialogView);
            showIssuer(mCertificate.getIssuedBy(), dialogView);
            showValidity(mCertificate.getValidNotBeforeDate(), mCertificate.getValidNotAfterDate(), dialogView);
            hideSignature(dialogView);
            
        } else {
            nullCerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showValidity(Date notBefore, Date notAfter, View dialogView) {
        TextView fromView = ((TextView)dialogView.findViewById(R.id.value_validity_from));
        TextView toView = ((TextView)dialogView.findViewById(R.id.value_validity_to));
        DateFormat dateFormat = DateFormat.getDateInstance();
        fromView.setText(dateFormat.format(notBefore));
        toView.setText(dateFormat.format(notAfter));
    }

    
    private void showSubject(SslCertificate.DName subject, View dialogView) {
        TextView cnView = ((TextView)dialogView.findViewById(R.id.value_subject_CN));
        cnView.setText(subject.getCName());
        cnView.setVisibility(View.VISIBLE);
        
        TextView oView = ((TextView)dialogView.findViewById(R.id.value_subject_O));
        oView.setText(subject.getOName());
        oView.setVisibility(View.VISIBLE);
        
        TextView ouView = ((TextView)dialogView.findViewById(R.id.value_subject_OU));
        ouView.setText(subject.getUName());
        ouView.setVisibility(View.VISIBLE);

        // SslCertificates don't offer this information
        ((TextView)dialogView.findViewById(R.id.value_subject_C)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_subject_ST)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_subject_L)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_subject_C)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_subject_ST)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_subject_L)).setVisibility(View.GONE);
    }
    
    
    private void showIssuer(SslCertificate.DName issuer, View dialogView) {
        TextView cnView = ((TextView)dialogView.findViewById(R.id.value_issuer_CN));
        cnView.setText(issuer.getCName());
        cnView.setVisibility(View.VISIBLE);
        
        TextView oView = ((TextView)dialogView.findViewById(R.id.value_issuer_O));
        oView.setText(issuer.getOName());
        oView.setVisibility(View.VISIBLE);

        TextView ouView = ((TextView)dialogView.findViewById(R.id.value_issuer_OU));
        ouView.setText(issuer.getUName());
        ouView.setVisibility(View.VISIBLE);
        
        // SslCertificates don't offer this information
        ((TextView)dialogView.findViewById(R.id.value_issuer_C)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_issuer_ST)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_issuer_L)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_issuer_C)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_issuer_ST)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_issuer_L)).setVisibility(View.GONE);
    }
    
    private void hideSignature(View dialogView) {
        ((TextView)dialogView.findViewById(R.id.label_signature)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.label_signature_algorithm)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_signature_algorithm)).setVisibility(View.GONE);
        ((TextView)dialogView.findViewById(R.id.value_signature)).setVisibility(View.GONE);
    }

}
