/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android.authenticator;

import com.owncloud.android.ui.activity.AuthenticatorActivity;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    /**
     * Is used by android system to assign accounts to authenticators. Should be
     * used by application and all extensions.
     */
    public static final String ACCOUNT_TYPE = "owncloud";
    public static final String AUTH_TOKEN_TYPE = "org.owncloud";

    public static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_LOGIN_OPTIONS = "loginOptions";
    public static final String KEY_ACCOUNT = "account";
    /**
     * Value under this key should handle path to webdav php script. Will be
     * removed and usage should be replaced by combining
     * {@link com.owncloud.android.authenticator.AuthenticatorActivity.KEY_OC_BASE_URL} and
     * {@link com.owncloud.android.utils.OwnCloudVersion}
     * 
     * @deprecated
     */
    public static final String KEY_OC_URL = "oc_url";
    /**
     * Version should be 3 numbers separated by dot so it can be parsed by
     * {@link com.owncloud.android.utils.OwnCloudVersion}
     */
    public static final String KEY_OC_VERSION = "oc_version";
    /**
     * Base url should point to owncloud installation without trailing / ie:
     * http://server/path or https://owncloud.server
     */
    public static final String KEY_OC_BASE_URL = "oc_base_url";

    private static final String TAG = "AccountAuthenticator";
    private Context mContext;

    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
            String accountType, String authTokenType,
            String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        Log.i(TAG, "Adding account with type " + accountType
                + " and auth token " + authTokenType);
        try {
            validateAccountType(accountType);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Failed to validate account type " + accountType + ": "
                    + e.getMessage());
            e.printStackTrace();
            return e.getFailureBundle();
        }
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);

        setIntentFlags(intent);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options) throws NetworkErrorException {
        try {
            validateAccountType(account.type);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Failed to validate account type " + account.type + ": "
                    + e.getMessage());
            e.printStackTrace();
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
    public Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        try {
            validateAccountType(account.type);
            validateAuthTokenType(authTokenType);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Failed to validate account type " + account.type + ": "
                    + e.getMessage());
            e.printStackTrace();
            return e.getFailureBundle();
        }
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, password);
            return result;
        }

        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);

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
            Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
        setIntentFlags(intent);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAccountRemovalAllowed(
            AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        return super.getAccountRemovalAllowed(response, account);
    }

    private void setIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
    }

    private void validateAccountType(String type)
            throws UnsupportedAccountTypeException {
        if (!type.equals(ACCOUNT_TYPE)) {
            throw new UnsupportedAccountTypeException();
        }
    }

    private void validateAuthTokenType(String authTokenType)
            throws UnsupportedAuthTokenTypeException {
        if (!authTokenType.equals(AUTH_TOKEN_TYPE)) {
            throw new UnsupportedAuthTokenTypeException();
        }
    }

    public static class AuthenticatorException extends Exception {
        private static final long serialVersionUID = 1L;
        private Bundle mFailureBundle;

        public AuthenticatorException(int code, String errorMsg) {
            mFailureBundle = new Bundle();
            mFailureBundle.putInt(AccountManager.KEY_ERROR_CODE, code);
            mFailureBundle
                    .putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg);
        }

        public Bundle getFailureBundle() {
            return mFailureBundle;
        }
    }

    public static class UnsupportedAccountTypeException extends
            AuthenticatorException {
        private static final long serialVersionUID = 1L;

        public UnsupportedAccountTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported account type");
        }
    }

    public static class UnsupportedAuthTokenTypeException extends
            AuthenticatorException {
        private static final long serialVersionUID = 1L;

        public UnsupportedAuthTokenTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported auth token type");
        }
    }

    public static class UnsupportedFeaturesException extends
            AuthenticatorException {
        public static final long serialVersionUID = 1L;

        public UnsupportedFeaturesException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported features");
        }
    }

    public static class AccessDeniedException extends AuthenticatorException {
        public AccessDeniedException(int code, String errorMsg) {
            super(AccountManager.ERROR_CODE_INVALID_RESPONSE, "Access Denied");
        }

        private static final long serialVersionUID = 1L;

    }
}
