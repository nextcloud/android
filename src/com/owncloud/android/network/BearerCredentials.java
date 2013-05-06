/* ownCloud Android client application
 *   Copyright (C) 2012  ownCloud Inc.
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

package com.owncloud.android.network;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.util.LangUtils;

/**
 * Bearer token {@link Credentials}
 *
 * @author David A. Velasco
 */
public class BearerCredentials implements Credentials {

    
    private String mAccessToken;
    
    
    /**
     * The constructor with the bearer token
     *
     * @param token     The bearer token
     */
    public BearerCredentials(String token) {
        /*if (token == null) {
            throw new IllegalArgumentException("Bearer token may not be null");            
        }*/
        mAccessToken = (token == null) ? "" : token;
    }


    /**
     * Returns the access token
     *
     * @return      The access token
     */
    public String getAccessToken() {
        return mAccessToken;
    }


    /**
     * Get this object string.
     *
     * @return  The access token
     */
    public String toString() {
        return mAccessToken;
    }

    /**
     * Does a hash of the access token.
     *
     * @return The hash code of the access token
     */
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, mAccessToken);
        return hash;
    }

    /**
     * These credentials are assumed equal if accessToken is the same.
     *
     * @param   o   The other object to compare with.
     *
     * @return      'True' if the object is equivalent.
     */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (this.getClass().equals(o.getClass())) {
            BearerCredentials that = (BearerCredentials) o;
            if (LangUtils.equals(mAccessToken, that.mAccessToken)) {
                return true;
            }
        }
        return false;
    }

}

