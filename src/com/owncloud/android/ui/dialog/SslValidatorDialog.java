/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.network.SslAnalyzer;
import com.owncloud.android.operations.RemoteOperationResult;

/**
 * Dialog to request the user about a certificate that could not be validated with the certificates store in the system.
 * 
 * @author David A. Velasco
 */
public class SslValidatorDialog extends Dialog {

    private final static String TAG = SslValidatorDialog.class.getSimpleName();

    private OnSslValidatorListener mListener;
    private Exception mException = null;
    private View mView;
    
    
    /**
     * Creates a new SslValidatorDialog to ask the user if an untrusted certificate from a server should
     * be trusted.
     * 
     * @param context       Android context where the dialog will live.
     * @param result        Result of a failed remote operation.
     * @param listener      Object to notice when the server certificate was added to the local certificates store.
     * @return              A new SslValidatorDialog instance, or NULL if the operation can not be recovered
     *                      by setting the certificate as reliable.
     */
    public static SslValidatorDialog newInstance(Context context, RemoteOperationResult result, OnSslValidatorListener listener) {
        Exception e = SslAnalyzer.getRecoverableException(result);
        if (e != null) {
            SslValidatorDialog dialog = new SslValidatorDialog(context, listener);
            return dialog;
        } else {
            return null;
        }
    }

    /**
     * Private constructor. 
     * 
     * Instances have to be created through static {@link SslValidatorDialog#newInstance}.
     * 
     * @param context       Android context where the dialog will live
     * @param e             Exception causing the need of prompt the user about the server certificate.
     * @param listener      Object to notice when the server certificate was added to the local certificates store.
     */
    private SslValidatorDialog(Context context, OnSslValidatorListener listener) {
        super(context);
        mListener = listener;
    }
    
    
    /**
     * {@inheritDoc}
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mView = getLayoutInflater().inflate(R.layout.ssl_validator_layout, null);
        setContentView(mView); 
        //setTitle(R.string.ssl_validator_title);
        
        mView.findViewById(R.id.ok).setOnClickListener( 
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            saveServerCert();
                            dismiss();
                            if (mListener != null)
                                mListener.onSavedCertificate();
                            else
                                Log.d(TAG, "Nobody there to notify the certificate was saved");
                            
                        } catch (Exception e) {
                            dismiss();
                            if (mListener != null)
                                mListener.onFailedSavingCertificate();
                            Log.e(TAG, "Server certificate could not be saved in the known servers trust store ", e);
                        }
                    }
                });
        
        mView.findViewById(R.id.cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancel();
                    }
                });
    }
    
    
    public void updateResult(RemoteOperationResult result) {
        mException = SslAnalyzer.getRecoverableException(result);
        if (mException instanceof CertPathValidatorException ) {
            showCertificateData(((CertPathValidatorException)mException).getCertPath());
            ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.VISIBLE);
            ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_hostname_not_vertified)).setVisibility(View.GONE);
            
        } else if (mException instanceof CertificateExpiredException ) {
            ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.VISIBLE);
            ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_hostname_not_vertified)).setVisibility(View.GONE);
            
        } else if (mException instanceof CertificateNotYetValidException ) {
            ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.VISIBLE);
            ((TextView)mView.findViewById(R.id.reason_hostname_not_vertified)).setVisibility(View.GONE);
            
        } else if (mException instanceof SSLPeerUnverifiedException ) {
            ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_hostname_not_vertified)).setVisibility(View.VISIBLE);
        }
        
    }
    
    private void showCertificateData(CertPath certPath) {
        final List<? extends Certificate> certs = certPath.getCertificates();
        /*X509Certificate badCert = null;
        if (e.getIndex() >= 0 && e.getIndex() < certs.size()) 
            badCert = (X509Certificate) certs.get(e.getIndex());*/
        if (certs.size() > 0) {
            X509Certificate serverCert = (X509Certificate) certs.get(0);
            String text = serverCert.getSubjectDN().getName();
            text = text.substring(text.indexOf(",") + 1);
            ((TextView)mView.findViewById(R.id.issuer)).setText(text);
        }
    }

    private void saveServerCert() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        // TODO be able to add certificate for any recoverable exception
        if (mException instanceof CertPathValidatorException) {
            OwnCloudClientUtils.addCertToKnownServersStore(((CertPathValidatorException) mException).getCertPath().getCertificates().get(0), getContext());
        }
    }

    
    public interface OnSslValidatorListener {
        public void onSavedCertificate();
        public void onFailedSavingCertificate();
    }
}

