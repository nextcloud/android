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

package com.owncloud.android.authentication;

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
