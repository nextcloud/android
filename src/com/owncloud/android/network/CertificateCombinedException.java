/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

package com.owncloud.android.network;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Exception joining all the problems that {@link AdvancedX509TrustManager} can find in
 * a certificate chain for a server.
 * 
 * This was initially created as an extension of CertificateException, but some
 * implementations of the SSL socket layer in existing devices are REPLACING the CertificateException
 * instances thrown by {@link javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[], String)}
 * with SSLPeerUnverifiedException FORGETTING THE CAUSING EXCEPTION instead of wrapping it. 
 * 
 * Due to this, extending RuntimeException is necessary to get that the CertificateCombinedException 
 * instance reaches {@link AdvancedSslSocketFactory#verifyPeerIdentity}.
 * 
 * BE CAREFUL. As a RuntimeException extensions, Java compilers do not require to handle it
 * in client methods. Be sure to use it only when you know exactly where it will go.
 * 
 * @author David A. Velasco
 */
public class CertificateCombinedException extends RuntimeException {

    /** Generated - to refresh every time the class changes */
    private static final long serialVersionUID = -8875782030758554999L;
    
    private X509Certificate mServerCert = null;
    private String mHostInUrl;

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

    public String getHostInUrl() {
        return mHostInUrl;
    }

    public void setHostInUrl(String host) {
        mHostInUrl = host;
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
                mOtherCertificateException != null ||
                mSslPeerUnverifiedException != null);
    }

    public boolean isRecoverable() {
        return (mCertificateExpiredException != null ||
                mCertificateNotYetValidException != null ||
                mCertPathValidatorException != null ||
                mSslPeerUnverifiedException != null);
    }

}
