/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 *  Authenticator for ownCloud accounts.
 *
 *  Controller class accessed from the system AccountManager,
 *  providing integration of ownCloud accounts with the Android system.
 *
 *  TODO - better separation in operations for OAuth-capable and regular ownCloud accounts.
 *  TODO - review completeness
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    /**
     * Is used by android system to assign accounts to authenticators.
     * Should be used by application and all extensions.
     */
    public static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_LOGIN_OPTIONS = "loginOptions";
    public static final String KEY_ACCOUNT = "account";

    private static final String TAG = AccountAuthenticator.class.getSimpleName();

    private Context mContext;

    private Handler mHandler;

    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
        mHandler = new Handler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) {
        Log_OC.i(TAG, "Adding account with type " + accountType + " and auth token " + authTokenType);

        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccountsByType(MainApp.getAccountType(mContext));

        final Bundle bundle = new Bundle();

        if (mContext.getResources().getBoolean(R.bool.multiaccount_support) || accounts.length < 1) {
            try {
                validateAccountType(accountType);
            } catch (AuthenticatorException e) {
                Log_OC.e(TAG, "Failed to validate account type " + accountType + ": "
                        + e.getMessage(), e);
                return e.getFailureBundle();
            }

            Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
            intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
            intent.putExtra(KEY_LOGIN_OPTIONS, options);
            intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE);

            setIntentFlags(intent);

            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        } else {
            // Return an error
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            final String message = String.format(mContext.getString(R.string.auth_unsupported_multiaccount), mContext.getString(R.string.app_name));
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, message);

            mHandler.post(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
        }

        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account, Bundle options) {
        try {
            validateAccountType(account.type);
        } catch (AuthenticatorException e) {
            Log_OC.e(TAG, "Failed to validate account type " + account.type + ": " + e.getMessage(), e);
            return e.getFailureBundle();
        }

        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);

        setIntentFlags(intent);

        Bundle resultBundle = new Bundle();
        resultBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return resultBundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account, String authTokenType, Bundle options) {
        // validate parameters
        try {
            validateAccountType(account.type);
            validateAuthTokenType(authTokenType);
        } catch (AuthenticatorException e) {
            Log_OC.e(TAG, "Failed to validate account type " + account.type + ": " + e.getMessage(), e);
            return e.getFailureBundle();
        }

        /// check if required token is stored
        final AccountManager am = AccountManager.get(mContext);
        String accessToken;
        if (authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType(mContext)))) {
            accessToken = am.getPassword(account);
        } else {
            accessToken = am.peekAuthToken(account, authTokenType);
        }
        if (accessToken != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.getAccountType(mContext));
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
            return result;
        }

        /// if not stored, return Intent to access the AuthenticatorActivity and UPDATE the token for the account
        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
        intent.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
        intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);


        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle options) {

        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
        setIntentFlags(intent);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        return super.getAccountRemovalAllowed(response, account);
    }

    private void setIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
    }

    private void validateAccountType(String type) throws UnsupportedAccountTypeException {
        if (!type.equals(MainApp.getAccountType(mContext))) {
            throw new UnsupportedAccountTypeException();
        }
    }

    private void validateAuthTokenType(String authTokenType) throws UnsupportedAuthTokenTypeException {
        String accountType = MainApp.getAccountType(mContext);

        if (!authTokenType.equals(accountType) &&
            !authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(accountType))) {
            throw new UnsupportedAuthTokenTypeException();
        }
    }

    public static class AuthenticatorException extends Exception {
        private static final long serialVersionUID = 1L;
        private Bundle mFailureBundle;

        public AuthenticatorException(int code, String errorMsg) {
            mFailureBundle = new Bundle();
            mFailureBundle.putInt(AccountManager.KEY_ERROR_CODE, code);
            mFailureBundle.putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg);
        }

        public Bundle getFailureBundle() {
            return mFailureBundle;
        }
    }

    public static class UnsupportedAccountTypeException extends AuthenticatorException {
        private static final long serialVersionUID = 1L;

        public UnsupportedAccountTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported account type");
        }
    }

    public static class UnsupportedAuthTokenTypeException extends AuthenticatorException {
        private static final long serialVersionUID = 1L;

        public UnsupportedAuthTokenTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported auth token type");
        }
    }
}
