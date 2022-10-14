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

import com.owncloud.android.R;
import com.owncloud.android.databinding.SslValidatorLayoutBinding;
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
    private SslValidatorLayoutBinding binding;
    
    
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
        binding = SslValidatorLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ok.setOnClickListener(
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

        binding.cancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancel();
                    }
                });

        binding.detailsBtn.setOnClickListener(
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
            binding.reasonCertNotTrusted.setVisibility(View.GONE);
            binding.reasonCertExpired.setVisibility(View.GONE);
            binding.reasonCertNotYetValid.setVisibility(View.GONE);
            binding.reasonHostnameNotVerified.setVisibility(View.GONE);
            binding.detailsScroll.setVisibility(View.GONE);

            /// refresh
            if (mException.getCertPathValidatorException() != null) {
                binding.reasonCertNotTrusted.setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateExpiredException() != null) {
                binding.reasonCertExpired.setVisibility(View.VISIBLE);
            }
            
            if (mException.getCertificateNotYetValidException() != null) {
                binding.reasonCertNotYetValid.setVisibility(View.VISIBLE);
            } 

            if (mException.getSslPeerUnverifiedException() != null ) {
                binding.reasonHostnameNotVerified.setVisibility(View.VISIBLE);
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
        binding.valueSignature.setText(getHex(cert.getSignature()));
        binding.valueSignatureAlgorithm.setText(cert.getSigAlgName());
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
        binding.valueValidityFrom.setText(notBefore.toLocaleString());
        binding.valueValidityTo.setText(notAfter.toLocaleString());
    }

    private void showSubject(X500Principal subject) {
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
    
    private void showIssuer(X500Principal issuer) {
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
