/* ownCloud Jelly Bean Workaround for lost credentials
 *
 *   Copyright (C) 2013 ownCloud Inc.
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

public class AccountAuthenticatorService extends Service {

    private AccountAuthenticator mAuthenticator;
    static final public String ACCOUNT_TYPE = "owncloud";

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

    	public AccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response,
                String accountType, String authTokenType,
                String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
            return getCommonResultBundle();
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
            return getCommonResultBundle();
        }

        private Bundle getCommonResultBundle() {
        	Bundle resultBundle = new Bundle();
            resultBundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            resultBundle.putString(AccountManager.KEY_ERROR_MESSAGE, "This is just a workaround, not a real account authenticator");
            return resultBundle;
		}

    }
}
