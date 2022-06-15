/**
 *   ownCloud Android client application
 *
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

/**
 * Dialog to request the user about a certificate that could not be validated with the certificates store in the system.
 */
public class SslValidatorDialog extends Dialog {

    private final static String TAG = SslValidatorDialog.class.getSimpleName();

    private OnSslValidatorListener mListener;
    private CertificateCombinedException mException;
    private View mView;
    
    
    /**
     * Creates a new SslValidatorDialog to ask the user if an untrusted certificate from a server should
     * be trusted.
     * 
     * @param context       Android context where the dialog will live.
     * @param result        Result of a failed remote operation.
     * @param listener      Object to notice when the server certificate was added to the local certificates store.
     * @return              A new SslValidatorDialog instance. NULL if the operation can not be recovered
     *                      by setting the certificate as reliable.
     */
    public static SslValidatorDialog newInstance(Context context, RemoteOperationResult result, OnSslValidatorListener listener) {
        if (result != null && result.isSslRecoverableException()) {
            return new SslValidatorDialog(context, listener);
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
                            if (mListener != null) {
                                mListener.onSavedCertificate();
                            } else {
                                Log_OC.d(TAG, "Nobody there to notify the certificate was saved");
                            }
                            
                        } catch (GeneralSecurityException | IOException e) {
                            dismiss();
                            if (mListener != null) {
                                mListener.onFailedSavingCertificate();
                            }
                            Log_OC.e(TAG, "Server certificate could not be saved in the known servers trust store ", e);
                            
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
        
        mView.findViewById(R.id.details_btn).setOnClickListener(
                new View.OnClickListener() {
                   @Override
                    public void onClick(View v) {
                       View detailsScroll = findViewById(R.id.details_scroll);
                       if (detailsScroll.getVisibility() == View.VISIBLE) {
                           detailsScroll.setVisibility(View.GONE);
                           ((Button) v).setText(R.string.ssl_validator_btn_details_see);
                       } else {
                           detailsScroll.setVisibility(View.VISIBLE);
                           ((Button) v).setText(R.string.ssl_validator_btn_details_hide);
                       }
                    }
                });
    }
    
    
    public void updateResult(RemoteOperationResult result) {
        if (result.isSslRecoverableException()) {
            mException = (CertificateCombinedException) result.getException();
            
            /// clean
            mView.findViewById(R.id.reason_cert_not_trusted).setVisibility(View.GONE);
            mView.findViewById(R.id.reason_cert_expired).setVisibility(View.GONE);
            mView.findViewById(R.id.reason_cert_not_yet_valid).setVisibility(View.GONE);
            mView.findViewById(R.id.reason_hostname_not_verified).setVisibility(View.GONE);
            mView.findViewById(R.id.details_scroll).setVisibility(View.GONE);

            /// refresh
            if (mException.getCertPathValidatorException() != null) {
                mView.findViewById(R.id.reason_cert_not_trusted).setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateExpiredException() != null) {
                mView.findViewById(R.id.reason_cert_expired).setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateNotYetValidException() != null) {
                mView.findViewById(R.id.reason_cert_not_yet_valid).setVisibility(View.VISIBLE);
            } 

            if (mException.getSslPeerUnverifiedException() != null ) {
                mView.findViewById(R.id.reason_hostname_not_verified).setVisibility(View.VISIBLE);
            }
            
            showCertificateData(mException.getServerCertificate());
        }
    }
    
    private void showCertificateData(X509Certificate cert) {

        if (cert != null) {
            showSubject(cert.getSubjectX500Principal());
            showIssuer(cert.getIssuerX500Principal());
            showValidity(cert.getNotBefore(), cert.getNotAfter());
            showSignature(cert);
            
        } else {
            // this should not happen, TODO
            Log_OC.d("certNull", "This should not happen");
        }
    }

    private void showSignature(X509Certificate cert) {
        TextView sigView = mView.findViewById(R.id.value_signature);
        TextView algorithmView = mView.findViewById(R.id.value_signature_algorithm);
        sigView.setText(getHex(cert.getSignature()));
        algorithmView.setText(cert.getSigAlgName());
    }
    
    public String getHex(final byte [] raw) {
        if (raw == null) {
           return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
           final int hiVal = (b & 0xF0) >> 4;
           final int loVal = b & 0x0F;
           hex.append((char) ('0' + (hiVal + (hiVal / 10 * 7))));
           hex.append((char) ('0' + (loVal + (loVal / 10 * 7))));
        }
        return hex.toString();
     }    

    @SuppressWarnings("deprecation")
    private void showValidity(Date notBefore, Date notAfter) {
        TextView fromView = mView.findViewById(R.id.value_validity_from);
        TextView toView = mView.findViewById(R.id.value_validity_to);
        fromView.setText(notBefore.toLocaleString());
        toView.setText(notAfter.toLocaleString());
    }

    private void showSubject(X500Principal subject) {
        Map<String, String> s = parsePrincipal(subject);
        TextView cnView = mView.findViewById(R.id.value_subject_CN);
        TextView oView = mView.findViewById(R.id.value_subject_O);
        TextView ouView = mView.findViewById(R.id.value_subject_OU);
        TextView cView = mView.findViewById(R.id.value_subject_C);
        TextView stView = mView.findViewById(R.id.value_subject_ST);
        TextView lView = mView.findViewById(R.id.value_subject_L);
        
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
    
    private void showIssuer(X500Principal issuer) {
        Map<String, String> s = parsePrincipal(issuer);
        TextView cnView = mView.findViewById(R.id.value_issuer_CN);
        TextView oView = mView.findViewById(R.id.value_issuer_O);
        TextView ouView = mView.findViewById(R.id.value_issuer_OU);
        TextView cView = mView.findViewById(R.id.value_issuer_C);
        TextView stView = mView.findViewById(R.id.value_issuer_ST);
        TextView lView = mView.findViewById(R.id.value_issuer_L);
        
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

    private void saveServerCert() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (mException.getServerCertificate() != null) {
            // TODO make this asynchronously, it can take some time
            NetworkUtils.addCertToKnownServersStore(mException.getServerCertificate(), getContext());
        }
    }
    
    public interface OnSslValidatorListener {
        public void onSavedCertificate();
        public void onFailedSavingCertificate();
    }
}
