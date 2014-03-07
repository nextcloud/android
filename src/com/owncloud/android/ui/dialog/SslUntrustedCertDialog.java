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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.utils.Log_OC;

import android.app.Activity;
import android.app.Dialog;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Dialog to show an Untrusted Certificate
 * 
 * @author masensio
 * @author David A. Velasco
 *
 */
public class SslUntrustedCertDialog extends SherlockDialogFragment{
    
    private final static String TAG = SslUntrustedCertDialog.class.getSimpleName();
    
    private X509Certificate mCertificate;
    private View mView;
    private OnSslUntrustedCertListener mListener;
    private SslError mError;
    private CertificateCombinedException mException = null;
    
    public SslUntrustedCertDialog() {
    }
    
    public SslUntrustedCertDialog(X509Certificate cert, SslError error) {
        mCertificate = cert;
        mError = error;
    }
    
    /**
     * Private constructor. 
     * 
     * Instances have to be created through static {@link SslUntrustedCertDialog#newInstance}.
     * 
     * @param context       Android context where the dialog will live
     * @param e             Exception causing the need of prompt the user about the server certificate.
     * @param listener      Object to notice when the server certificate was added to the local certificates store.
     */
    private SslUntrustedCertDialog(RemoteOperationResult result, OnSslUntrustedCertListener listener) {
        mListener = listener;
        if (result.isSslRecoverableException()) {
            mException = (CertificateCombinedException) result.getException();
            mCertificate = mException.getServerCertificate();
        }
    }
    

    public static SslUntrustedCertDialog newInstance(X509Certificate cert, SslError error) {
        if (cert != null){
            SslUntrustedCertDialog dialog = new SslUntrustedCertDialog(cert, error);
            return dialog;
        } else  { // TODO Review this case
            SslUntrustedCertDialog dialog = new  SslUntrustedCertDialog();
            return  dialog;
        }
    }
    
    
    
    /**
     * Creates a new SslUntrustedCertDialog to ask the user if an untrusted certificate from a server should
     * be trusted.
     * 
     * @param context       Android context where the dialog will live.
     * @param result        Result of a failed remote operation.
     * @param listener      Object to notice when the server certificate was added to the local certificates store.
     * @return              A new SslUntrustedCertDialog instance. NULL if the operation can not be recovered
     *                      by setting the certificate as reliable.
     */
    public static SslUntrustedCertDialog newInstance(RemoteOperationResult result, OnSslUntrustedCertListener listener) {
        if (result != null && result.isSslRecoverableException()) {
            SslUntrustedCertDialog dialog = new SslUntrustedCertDialog(result, listener);
            return dialog;
        } else {
            return null;
        }
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(true);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SherlockActivity) {
            mListener = (OnSslUntrustedCertListener) activity;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a view by inflating desired layout
        mView = inflater.inflate(R.layout.ssl_untrusted_cert_layout, container,  false);
        
        updateException(mException);
        
        Button ok = (Button) mView.findViewById(R.id.ok);
        ok.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                try {
                  saveServerCert();
                  dismiss();
                  if (mListener != null) {
                      mListener.onSavedCertificate();
                  }
                  else
                      Log_OC.d(TAG, "Nobody there to notify the certificate was saved");
                  
              } catch (GeneralSecurityException e) {
                  dismiss();
                  if (mListener != null) {
                      mListener.onFailedSavingCertificate();
                  }
                  Log_OC.e(TAG, "Server certificate could not be saved in the known servers trust store ", e);
                  
              } catch (IOException e) {
                  dismiss();
                  if (mListener != null) {
                      mListener.onFailedSavingCertificate();
                  }
                  Log_OC.e(TAG, "Server certificate could not be saved in the known servers trust store ", e);
              }
                
            }
        });
        
        Button cancel = (Button) mView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                mListener.onCancelCertificate();
            }
        });
        
        Button details = (Button) mView.findViewById(R.id.details_btn);
        details.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                View detailsScroll = mView.findViewById(R.id.details_scroll);
                if (detailsScroll.getVisibility() == View.VISIBLE) {
                    detailsScroll.setVisibility(View.GONE);
                    ((Button) v).setText(R.string.ssl_validator_btn_details_see);

                } else {
                    detailsScroll.setVisibility(View.VISIBLE);
                    ((Button) v).setText(R.string.ssl_validator_btn_details_hide);
                    
                    showCertificateData(mCertificate);
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
    
    
    private void updateException(CertificateCombinedException exception) {
        
        /// clean
        mView.findViewById(R.id.reason_cert_not_trusted).setVisibility(View.GONE);
        mView.findViewById(R.id.reason_cert_expired).setVisibility(View.GONE);
        mView.findViewById(R.id.reason_cert_not_yet_valid).setVisibility(View.GONE);
        mView.findViewById(R.id.reason_hostname_not_verified).setVisibility(View.GONE);
        mView.findViewById(R.id.details_scroll).setVisibility(View.GONE);
        
        
        if (mException != null) {
            
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
            
        }
        
    }
    
    private void showCertificateData(X509Certificate cert) {

        TextView nullCerView = (TextView) mView.findViewById(R.id.null_cert);
        
        if (cert != null) {
            nullCerView.setVisibility(View.GONE);
            showSubject(cert.getSubjectX500Principal());
            showIssuer(cert.getIssuerX500Principal());
            showValidity(cert.getNotBefore(), cert.getNotAfter());
            showSignature(cert);
            
        } else {
            nullCerView.setVisibility(View.VISIBLE);
        }
    }

    private void showSignature(X509Certificate cert) {
        TextView sigView = ((TextView)mView.findViewById(R.id.value_signature));
        TextView algorithmView = ((TextView)mView.findViewById(R.id.value_signature_algorithm));
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
        TextView fromView = ((TextView)mView.findViewById(R.id.value_validity_from));
        TextView toView = ((TextView)mView.findViewById(R.id.value_validity_to));
        fromView.setText(notBefore.toLocaleString());
        toView.setText(notAfter.toLocaleString());
    }

    private void showSubject(X500Principal subject) {
        Map<String, String> s = parsePrincipal(subject);
        TextView cnView = ((TextView)mView.findViewById(R.id.value_subject_CN));
        TextView oView = ((TextView)mView.findViewById(R.id.value_subject_O));
        TextView ouView = ((TextView)mView.findViewById(R.id.value_subject_OU));
        TextView cView = ((TextView)mView.findViewById(R.id.value_subject_C));
        TextView stView = ((TextView)mView.findViewById(R.id.value_subject_ST));
        TextView lView = ((TextView)mView.findViewById(R.id.value_subject_L));
        
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
        TextView cnView = ((TextView)mView.findViewById(R.id.value_issuer_CN));
        TextView oView = ((TextView)mView.findViewById(R.id.value_issuer_O));
        TextView ouView = ((TextView)mView.findViewById(R.id.value_issuer_OU));
        TextView cView = ((TextView)mView.findViewById(R.id.value_issuer_C));
        TextView stView = ((TextView)mView.findViewById(R.id.value_issuer_ST));
        TextView lView = ((TextView)mView.findViewById(R.id.value_issuer_L));
        
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

    private void saveServerCert() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (mCertificate != null) {
            // TODO make this asynchronously, it can take some time
            NetworkUtils.addCertToKnownServersStore(mCertificate, getSherlockActivity());
        }
    }

    
    public interface OnSslUntrustedCertListener {
        public void onSavedCertificate();
        public void onCancelCertificate();
        public void onFailedSavingCertificate();
    }


   

}
