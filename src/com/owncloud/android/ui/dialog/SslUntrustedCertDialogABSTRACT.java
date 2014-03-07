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

import android.net.http.SslError;
import android.webkit.SslErrorHandler;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Dialog to show information about an untrusted certificate and allow the user
 * to decide trust on it or not.
 * 
 * Abstract implementation of common functionality for different dialogs that
 * get the information about the error and the certificate from different classes. 
 * 
 * @author masensio
 * @author David A. Velasco
 */
public abstract class SslUntrustedCertDialogABSTRACT extends SherlockDialogFragment {
    
    //private final static String TAG = SslUntrustedCertDialogABSTRACT.class.getSimpleName();
    
    public static SslUntrustedCertDialogABSTRACT newInstanceForEmptySslError(SslError error, SslErrorHandler handler) {
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with a parameter error == null");
        }
        return SslUntrustedCertDialogForEmptySslError.newInstance(error, handler);
    }
    
    // TODO - complete when merged
    /*
    public static SslUntrustedCertDialogABSTRACT newInstanceForFullSslError(X509Certificate cert, SslError error, OnSslUntrustedCertListener listener, SslErrorHandler handler) {
        if (cert == null) {
            throw new IllegalArgumentException("Trying to create instance with a parameter error == null");
        }
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with a parameter error == null");
        }
        return new SslUntrustedCertDialog(cert, listener, handler);
    }
    */
    
    
    public interface OnSslUntrustedCertListener {
        public void onSavedCertificate();
        public void onFailedSavingCertificate();
    }

}
