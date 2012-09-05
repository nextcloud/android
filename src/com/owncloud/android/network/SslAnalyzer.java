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
 */
package com.owncloud.android.network;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import com.owncloud.android.operations.RemoteOperationResult;

/**
 * Helper class to check if a SSL error is related to a condition that could be avoided with assistance from the user.
 * 
 * @author David A. Velasco
 */
public class SslAnalyzer {
    
    /**
     * Search for a SSL-related exception in a remote operation result that can be recoverable
     * by allowing the user to state the reliability of the certificate from the server. 
     * 
     * @param result        Result of a remote operation.
     * @return              An exception instance that caused the failure of the remote operation and that can be avoided if the user
     *                      states the certificate from the server as reliable; or NULL if the result is that's not possible
     */
    public static Exception getRecoverableException(RemoteOperationResult result) {
        Exception ret = null;
        SSLException e = null;
        Throwable cause = null;
        if (result.getException() instanceof SSLException) {
            e = (SSLException)result.getException();
            if (e instanceof SSLPeerUnverifiedException) {
                ret = e;
                
            } else { 
                cause = e.getCause();
                Throwable previousCause = null;
                boolean recoverableCertException = false;
                while (cause != null && cause != previousCause && !recoverableCertException) {     // getCause() is not funny
                    recoverableCertException = (  cause instanceof CertPathValidatorException ||
                                                cause instanceof CertificateExpiredException ||
                                                cause instanceof CertificateNotYetValidException );
                    if (recoverableCertException)
                        ret = (Exception)cause;
                    previousCause = cause;
                    cause = cause.getCause();
                }
            }
        }
        return ret;
    }
    
    
    /**
     * Checks if a remote operation result can be recoverable
     * by allowing the user to state the reliability of the certificate from the server. 
     * 
     * @param result        Result of a remote operation.
     * @return              An exception instance that caused the failure of the remote operation and that can be avoided if the user
     *                      states the certificate from the server as reliable; or NULL if the result is that's not possible
     */
    public static boolean isRecoverable(RemoteOperationResult result) {
        return (getRecoverableException(result) != null);
    }

}
