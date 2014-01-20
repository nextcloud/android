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

package com.owncloud.android.lib.accounts;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Account with extra information specific for ownCloud accounts.
 * 
 * TODO integrate in the main app
 * 
 * @author David A. Velasco
 */
public class OwnCloudAccount extends Account {

	public static class Constants {
	    /**
	     * Value under this key should handle path to webdav php script. Will be
	     * removed and usage should be replaced by combining
	     * {@link com.owncloud.android.authentication.AuthenticatorActivity.KEY_OC_BASE_URL} and
	     * {@link com.owncloud.android.lib.utils.utils.OwnCloudVersion}
	     * 
	     * @deprecated
	     */
	    public static final String KEY_OC_URL = "oc_url";
	    /**
	     * Version should be 3 numbers separated by dot so it can be parsed by
	     * {@link com.owncloud.android.lib.utils.utils.OwnCloudVersion}
	     */
	    public static final String KEY_OC_VERSION = "oc_version";
	    /**
	     * Base url should point to owncloud installation without trailing / ie:
	     * http://server/path or https://owncloud.server
	     */
	    public static final String KEY_OC_BASE_URL = "oc_base_url";
	    /**
	     * Flag signaling if the ownCloud server can be accessed with OAuth2 access tokens.
	     */
	    public static final String KEY_SUPPORTS_OAUTH2 = "oc_supports_oauth2";
	    /**
	     * Flag signaling if the ownCloud server can be accessed with session cookies from SAML-based web single-sign-on.
	     */
	    public static final String KEY_SUPPORTS_SAML_WEB_SSO = "oc_supports_saml_web_sso";
	}
	
	private String mAuthTokenType;

	public OwnCloudAccount(String name, String type, String authTokenType) {
		super(name, type);
		// TODO validate authTokentype as supported
		mAuthTokenType = authTokenType;
	}
	
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */
	public OwnCloudAccount(Parcel source) {
		super(source);
        mAuthTokenType = source.readString();
	}
	
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
        dest.writeString(mAuthTokenType);
    }
	
	
	public String getAuthTokenType() {
		return mAuthTokenType;
	}

	
    public static final Parcelable.Creator<OwnCloudAccount> CREATOR = new Parcelable.Creator<OwnCloudAccount>() {
        @Override
        public OwnCloudAccount createFromParcel(Parcel source) {
            return new OwnCloudAccount(source);
        }

        @Override
        public OwnCloudAccount [] newArray(int size) {
            return new OwnCloudAccount[size];
        }
    };
	
}
