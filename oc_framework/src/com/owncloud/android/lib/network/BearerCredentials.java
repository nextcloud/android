/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.network;

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

