/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

package com.owncloud.android.oc_framework.accounts;

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
	     * {@link com.owncloud.android.oc_framework.utils.utils.OwnCloudVersion}
	     * 
	     * @deprecated
	     */
	    public static final String KEY_OC_URL = "oc_url";
	    /**
	     * Version should be 3 numbers separated by dot so it can be parsed by
	     * {@link com.owncloud.android.oc_framework.utils.utils.OwnCloudVersion}
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
