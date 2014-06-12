/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
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

package com.owncloud.android.authentication;

import com.owncloud.android.lib.common.OwnCloudCredentials;

import android.accounts.Account;
import android.content.Context;
import android.net.Uri;

/**
 * OwnCloud Account
 * 
 * @author David A. Velasco
 */
public class OwnCloudAccount {

    private Uri mBaseUri; 
    
    private OwnCloudCredentials mCredentials;
    
    public OwnCloudAccount(Account savedAccount, Context context) {
        
    }
    
    public OwnCloudAccount(Uri baseUri, OwnCloudCredentials credentials) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be null");
        }
        mBaseUri = baseUri;
        mCredentials = credentials;
    }
    
    public boolean isAnonymous() {
        return (mCredentials == null);
    }
    
    public Uri getBaseUri() {
        return mBaseUri;
    }
            
    public OwnCloudCredentials getCredentials() {
        return mCredentials;
    }

}
