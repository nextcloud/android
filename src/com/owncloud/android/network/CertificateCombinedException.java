package com.owncloud.android.network;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

public class CertificateCombinedException extends CertificateException {

    private X509Certificate mServerCert = null;
    private CertificateExpiredException mCertificateExpiredException = null;
    private CertificateNotYetValidException mCertificateNotYetValidException = null;
    private CertPathValidatorException mCertPathValidatorException = null;
    private CertificateException mOtherCertificateException = null;
    private SSLPeerUnverifiedException mSslPeerUnverifiedException = null;
    
    public CertificateCombinedException(X509Certificate x509Certificate) {
        mServerCert = x509Certificate;
    }

    public X509Certificate getServerCertificate() {
        return mServerCert;
    }

    public CertificateExpiredException getCertificateExpiredException() {
        return mCertificateExpiredException;
    }

    public void setCertificateExpiredException(CertificateExpiredException c) {
        mCertificateExpiredException  = c;
    }

    public CertificateNotYetValidException getCertificateNotYetValidException() {
        return mCertificateNotYetValidException;
    }

    public void setCertificateNotYetException(CertificateNotYetValidException c) {
        mCertificateNotYetValidException = c;
    }

    public CertPathValidatorException getCertPathValidatorException() {
        return mCertPathValidatorException;
    }

    public void setCertPathValidatorException(CertPathValidatorException c) {
        mCertPathValidatorException = c;
    }

    public CertificateException getOtherCertificateException() {
        return mOtherCertificateException;
    }

    public void setOtherCertificateException(CertificateException c) {
        mOtherCertificateException = c;
    }

    public SSLPeerUnverifiedException getSslPeerUnverifiedException() {
        return mSslPeerUnverifiedException ; 
    }

    public void setSslPeerUnverifiedException(SSLPeerUnverifiedException s) {
        mSslPeerUnverifiedException = s;
    }

    public boolean isException() {
        return (mCertificateExpiredException != null ||
                mCertificateNotYetValidException != null ||
                mCertPathValidatorException != null ||
                mOtherCertificateException != null);
    }

    public boolean isRecoverable() {
        return (mCertificateExpiredException != null ||
                mCertificateNotYetValidException != null ||
                mCertPathValidatorException != null);
    }

}
