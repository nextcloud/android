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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.network.CertificateCombinedException;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.RemoteOperationResult;

/**
 * Dialog to request the user about a certificate that could not be validated with the certificates store in the system.
 * 
 * @author David A. Velasco
 */
public class SslValidatorDialog extends Dialog {

    private final static String TAG = SslValidatorDialog.class.getSimpleName();

    private OnSslValidatorListener mListener;
    private CertificateCombinedException mException = null;
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
        if (result.isSslRecoverableException()) {
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
        if (result.isSslRecoverableException()) {
            mException = (CertificateCombinedException) result.getException();
            
            /// clean
            ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.reason_hostname_not_verified)).setVisibility(View.GONE);
            ((TextView)mView.findViewById(R.id.subject)).setVisibility(View.GONE);

            /// refresh
            if (mException.getCertPathValidatorException() != null) {
                ((TextView)mView.findViewById(R.id.reason_cert_not_trusted)).setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateExpiredException() != null) {
                ((TextView)mView.findViewById(R.id.reason_cert_expired)).setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateNotYetValidException() != null) {
                ((TextView)mView.findViewById(R.id.reason_cert_not_yet_valid)).setVisibility(View.VISIBLE);
            } 

            if (mException.getSslPeerUnverifiedException() != null ) {
                ((TextView)mView.findViewById(R.id.reason_hostname_not_verified)).setVisibility(View.VISIBLE);
            }
            
            
            showCertificateData(mException.getServerCertificate());
        }
        
    }
    
    private void showCertificateData(X509Certificate cert) {
        TextView subject = (TextView)mView.findViewById(R.id.subject);
        if (cert != null) {
            String text = cert.getSubjectDN().getName();
            text = text.substring(text.indexOf(",") + 1);
            subject.setVisibility(View.VISIBLE);
            subject.setText(text);
        } else {
            // this should not happen
            subject.setText(R.string.ssl_validator_certificate_not_available);
        }
    }

    private void saveServerCert() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (mException.getServerCertificate() != null) {
            OwnCloudClientUtils.addCertToKnownServersStore(mException.getServerCertificate(), getContext());
        }
    }

    
    public interface OnSslValidatorListener {
        public void onSavedCertificate();
        public void onFailedSavingCertificate();
    }
}

