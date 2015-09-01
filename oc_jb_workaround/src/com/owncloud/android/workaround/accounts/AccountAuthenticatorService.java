/* ownCloud Jelly Bean Workaround for lost credentials
 *   Copyright (C) 2015 ownCloud Inc.
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


package com.owncloud.android.workaround.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
//import android.util.Log;

public class AccountAuthenticatorService extends Service {

    private AccountAuthenticator mAuthenticator;
    //static final public String ACCOUNT_TYPE = "owncloud";

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new AccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
    
    
    public static class AccountAuthenticator extends AbstractAccountAuthenticator {

        public static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
        public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
        public static final String KEY_LOGIN_OPTIONS = "loginOptions";
    	
    	public AccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response,
                String accountType, String authTokenType,
                String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
        	//Log.e("WORKAROUND", "Yes, WORKAROUND takes the control here");
            final Intent intent = new Intent("com.owncloud.android.workaround.accounts.CREATE");
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                    response);
            intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
            intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
            intent.putExtra(KEY_LOGIN_OPTIONS, options);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);

            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
            //return getCommonResultBundle();
        }

        
		@Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                Account account, Bundle options) throws NetworkErrorException {
            return getCommonResultBundle();
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response,
                String accountType) {
            return getCommonResultBundle();
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response,
                Account account, String authTokenType, Bundle options)
                throws NetworkErrorException {
            return getCommonResultBundle();
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return "";
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response,
                Account account, String[] features) throws NetworkErrorException {
            return getCommonResultBundle();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response,
                Account account, String authTokenType, Bundle options)
                throws NetworkErrorException {
            return getCommonResultBundle();
        }

        @Override
        public Bundle getAccountRemovalAllowed(
                AccountAuthenticatorResponse response, Account account)
                throws NetworkErrorException {
            return super.getAccountRemovalAllowed(response, account);
        }

        private Bundle getCommonResultBundle() {
        	Bundle resultBundle = new Bundle();
            resultBundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            resultBundle.putString(AccountManager.KEY_ERROR_MESSAGE, "This is just a workaround, not a real account authenticator");
            return resultBundle;
		}

    }
}
