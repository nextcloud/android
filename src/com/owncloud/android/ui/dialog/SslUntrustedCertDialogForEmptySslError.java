/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import java.text.DateFormat;
import java.util.Date;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;

import android.app.Activity;
import android.app.Dialog;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

/**
 * Dialog to show an Untrusted Certificate
 * 
 * @author masensio
 * @author David A. Velasco
 */
public class SslUntrustedCertDialogForEmptySslError extends SslUntrustedCertDialogABSTRACT {
    
    //private final static String TAG = SslUntrustedCertDialogForEmptySslError.class.getSimpleName();
    
    private SslError mError;
    private SslErrorHandler mHandler;
    private View mView;

    
    /**
     * Factory method.
     * 
     * @param error     Error occurred; details about it will be shown in the dialog.
     * @param handler   Handler to indicate to the {@link WebView} where the error was found what to do next.
     * @return          New dialog.
     */
    public static SslUntrustedCertDialogForEmptySslError newInstance(SslError error, SslErrorHandler handler) {
        return new SslUntrustedCertDialogForEmptySslError(error, handler);
    }
    
    
    /**
     * Empty constructor.
     * 
     * Required by Android framework. Never used, since the state is retained; see {@link #onCreate(Bundle)}  
     */
    public SslUntrustedCertDialogForEmptySslError() {}
    

    /**
     * Private constructor.
     * 
     * Used by the factory method {@link #newInstance(SslError, SslErrorHandler)}.
     * 
     * @param error     Error occurred; details about it will be shown in the dialog.
     * @param handler   Handler to indicate to the {@link WebView} where the error was found what to do next.
     */
    private SslUntrustedCertDialogForEmptySslError(SslError error, SslErrorHandler handler) {
        mError = error;
        mHandler = handler;
    }

    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*if (!(activity instanceof OnSslUntrustedCertListener)) {
            throw new IllegalArgumentException("Trying to attach to an Activity not implementing " + OnSslUntrustedCertListener.class.getCanonicalName());
        }*/
    }
    
    
    // TODO try to move to the parent class ?
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // force to keep the state of the fragment on configuration changes (such as device rotations)
        setCancelable(false);
        mView = null;
    }
    
    // try to move to the parent class ?
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a view by inflating desired layout
        if (mView == null) {
            mView = inflater.inflate(R.layout.ssl_untrusted_cert_layout, container,  false);
        } else {
            ((ViewGroup)mView.getParent()).removeView(mView);
        }
        
        Button ok = (Button) mView.findViewById(R.id.untrusted_ok);
        ok.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                //AuthenticatorActivity act = ((AuthenticatorActivity)getSherlockActivity());
                mHandler.proceed();
                dismiss();
            }
        });
        
        Button cancel = (Button) mView.findViewById(R.id.untrusted_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                AuthenticatorActivity act = ((AuthenticatorActivity)getSherlockActivity());
                getDialog().cancel();
                mHandler.cancel();
                act.cancelWebView();
            }
        });
        
        Button details = (Button) mView.findViewById(R.id.untrusted_details_btn);
        details.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View detailsScroll = mView.findViewById(R.id.untrusted_details_scroll);
                if (detailsScroll.getVisibility() == View.VISIBLE) {
                    detailsScroll.setVisibility(View.GONE);
                    ((Button) v).setText(R.string.ssl_validator_btn_details_see);

                } else {
                    detailsScroll.setVisibility(View.VISIBLE);
                    ((Button) v).setText(R.string.ssl_validator_btn_details_hide);
                    showCertificateData();
                }
            }
        });
        
        return mView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }
    
    private void showCertificateData() {
        TextView nullCerView = (TextView) mView.findViewById(R.id.untrusted_null_cert);
        SslCertificate cert = mError.getCertificate();
        if (cert != null) {
            nullCerView.setVisibility(View.GONE);
            showSubject(cert.getIssuedTo());
            showIssuer(cert.getIssuedBy());
            showValidity(cert.getValidNotBeforeDate(), cert.getValidNotAfterDate());
            // TODO showSignatureNotAvailabe/Provided
            
        } else {
            nullCerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showValidity(Date notBefore, Date notAfter) {
        TextView fromView = ((TextView)mView.findViewById(R.id.untrusted_value_validity_from));
        TextView toView = ((TextView)mView.findViewById(R.id.untrusted_value_validity_to));
        DateFormat dateFormat = DateFormat.getDateInstance();
        fromView.setText(dateFormat.format(notBefore));
        toView.setText(dateFormat.format(notAfter));
    }

    
    private void showSubject(SslCertificate.DName subject) {
        TextView cnView = ((TextView)mView.findViewById(R.id.untrusted_value_subject_CN));
        cnView.setText(subject.getCName());
        cnView.setVisibility(View.VISIBLE);
        
        TextView oView = ((TextView)mView.findViewById(R.id.untrusted_value_subject_O));
        oView.setText(subject.getOName());
        oView.setVisibility(View.VISIBLE);
        
        TextView ouView = ((TextView)mView.findViewById(R.id.untrusted_value_subject_OU));
        ouView.setText(subject.getUName());
        ouView.setVisibility(View.VISIBLE);
        
        ((TextView)mView.findViewById(R.id.untrusted_value_subject_C)).setVisibility(View.GONE);
        ((TextView)mView.findViewById(R.id.untrusted_value_subject_ST)).setVisibility(View.GONE);
        ((TextView)mView.findViewById(R.id.untrusted_value_subject_L)).setVisibility(View.GONE);
    }
    
    
    private void showIssuer(SslCertificate.DName issuer) {
        TextView cnView = ((TextView)mView.findViewById(R.id.untrusted_value_issuer_CN));
        cnView.setText(issuer.getCName());
        cnView.setVisibility(View.VISIBLE);
        
        TextView oView = ((TextView)mView.findViewById(R.id.untrusted_value_issuer_O));
        oView.setText(issuer.getOName());
        oView.setVisibility(View.VISIBLE);

        TextView ouView = ((TextView)mView.findViewById(R.id.untrusted_value_issuer_OU));
        ouView.setText(issuer.getUName());
        ouView.setVisibility(View.VISIBLE);
        
        ((TextView)mView.findViewById(R.id.untrusted_value_issuer_C)).setVisibility(View.GONE);
        ((TextView)mView.findViewById(R.id.untrusted_value_issuer_ST)).setVisibility(View.GONE);
        ((TextView)mView.findViewById(R.id.untrusted_value_issuer_L)).setVisibility(View.GONE);
    }
    
}
