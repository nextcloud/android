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
