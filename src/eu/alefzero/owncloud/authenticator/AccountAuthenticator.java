package eu.alefzero.owncloud.authenticator;

import eu.alefzero.owncloud.ui.AuthenticatorActivity;
import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AccountAuthenticator extends AbstractAccountAuthenticator {
    public static final String OPTIONS_USERNAME = "username";
    public static final String OPTIONS_PASSWORD = "password";
    public static final String OPTIONS_FILE_LIST_SYNC_ENABLED = "filelist";
    public static final String OPTIONS_PINNED_FILE_SYNC_ENABLED = "pinned";

    public static final String ACCOUNT_TYPE = "owncloud";
    public static final String AUTH_TOKEN_TYPE = "org.owncloud";

    public static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
    public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_LOGIN_OPTIONS = "loginOptions";
    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_OC_URL = "oc_url";
    public static final String KEY_CONTACT_URL = "oc_contact_url";

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
                             String accountType, String authTokenType, String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        Log.i(getClass().getName(), "Adding account with type " + accountType +
                " and auth token " + authTokenType);
        try {
            validateAccountType(accountType);
            //validateAuthTokenType(authTokenType);
            validateRequiredFeatures(requiredFeatures);
        } catch (AuthenticatorException e) {
            e.printStackTrace();
            return e.getFailureBundle();
        }
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);

        setIntentFlags(intent);
        Log.i(getClass().getName(), intent.toString());
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
            return e.getFailureBundle();
        }
        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        Log.i(getClass().getName(), "Getting authToken");
        try {
            validateAccountType(account.type);
            validateAuthTokenType(authTokenType);
        } catch (AuthenticatorException e) {
            Log.w(getClass().getName(), "Validating failded in getAuthToken");
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
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
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
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
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
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
                                           Account account) throws NetworkErrorException {
        return super.getAccountRemovalAllowed(response, account);
    }

    private void setIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
    }

    private void validateAccountType(String type) throws UnsupportedAccountTypeException {
        if (!type.equals(ACCOUNT_TYPE)) {
            throw new UnsupportedAccountTypeException();
        }
    }

    private void validateAuthTokenType(String authTokenType) throws UnsupportedAuthTokenTypeException {
        if (!authTokenType.equals(AUTH_TOKEN_TYPE)) {
            throw new UnsupportedAuthTokenTypeException();
        }
    }

    private void validateRequiredFeatures(String[] requiredFeatures) throws UnsupportedFeaturesException {
        // TODO
    }

    private void validateCreaditials(String username, String password, String path) throws AccessDeniedException {

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
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported account type");
        }
    }

    public static class UnsupportedAuthTokenTypeException extends AuthenticatorException {
        private static final long serialVersionUID = 1L;

        public UnsupportedAuthTokenTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported auth token type");
        }
    }

    public static class UnsupportedFeaturesException extends AuthenticatorException {
        public static final long serialVersionUID = 1L;

        public UnsupportedFeaturesException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported features");
        }
    }

    public static class AccessDeniedException extends AuthenticatorException {
        public AccessDeniedException(int code, String errorMsg) {
            super(AccountManager.ERROR_CODE_INVALID_RESPONSE, "Access Denied");
        }

        private static final long serialVersionUID = 1L;

    }
}
