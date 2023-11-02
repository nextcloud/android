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
package com.owncloud.android.authentication

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Authenticator for ownCloud accounts.
 *
 * Controller class accessed from the system AccountManager,
 * providing integration of ownCloud accounts with the Android system.
 *
 * TODO - better separation in operations for OAuth-capable and regular ownCloud accounts.
 * TODO - review completeness
 */
class AccountAuthenticator(private val mContext: Context) : AbstractAccountAuthenticator(
    mContext
) {

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * {@inheritDoc}
     */
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String, authTokenType: String,
        requiredFeatures: Array<String>, options: Bundle
    ): Bundle {

        Log_OC.i(TAG, "Adding account with type $accountType and auth token $authTokenType")

        val accountManager = AccountManager.get(mContext)
        val accounts = accountManager.getAccountsByType(MainApp.getAccountType(mContext))
        val bundle = Bundle()

        if (mContext.resources.getBoolean(R.bool.multiaccount_support) || accounts.isEmpty()) {
            try {
                validateAccountType(accountType)
            } catch (e: AuthenticatorException) {
                Log_OC.e(
                    TAG, "Failed to validate account type " + accountType + ": "
                        + e.message, e
                )
                return e.failureBundle
            }

            val intent = Intent(mContext, AuthenticatorActivity::class.java)
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
            intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType)
            intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures)
            intent.putExtra(KEY_LOGIN_OPTIONS, options)
            intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE)
            setIntentFlags(intent)
            bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        } else {
            // Return an error
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
            val message = String.format(
                mContext.getString(R.string.auth_unsupported_multiaccount), mContext.getString(
                    R.string.app_name
                )
            )
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, message)
            mHandler.post { Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show() }
        }

        return bundle
    }

    /**
     * {@inheritDoc}
     */
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account, options: Bundle
    ): Bundle {
        try {
            validateAccountType(account.type)
        } catch (e: AuthenticatorException) {
            Log_OC.e(TAG, "Failed to validate account type " + account.type + ": " + e.message, e)
            return e.failureBundle
        }

        val intent = Intent(mContext, AuthenticatorActivity::class.java)
        intent.putExtra(
            AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
            response
        )
        intent.putExtra(KEY_ACCOUNT, account)
        intent.putExtra(KEY_LOGIN_OPTIONS, options)
        setIntentFlags(intent)

        val resultBundle = Bundle()
        resultBundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return resultBundle
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle? {
        return null
    }

    /**
     * {@inheritDoc}
     */
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account, authTokenType: String, options: Bundle
    ): Bundle {
        // validate parameters
        try {
            validateAccountType(account.type)
            validateAuthTokenType(authTokenType)
        } catch (e: AuthenticatorException) {
            Log_OC.e(TAG, "Failed to validate account type " + account.type + ": " + e.message, e)
            return e.failureBundle
        }

        /// check if required token is stored
        val am = AccountManager.get(mContext)
        val accessToken: String? =
            if (authTokenType == AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType(mContext))) {
                am.getPassword(account)
            } else {
                am.peekAuthToken(account, authTokenType)
            }

        if (accessToken != null) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.getAccountType(mContext))
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
            return result
        }

        /// if not stored, return Intent to access the AuthenticatorActivity and UPDATE the token for the account
        val intent = Intent(mContext, AuthenticatorActivity::class.java)

        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType)
        intent.putExtra(KEY_LOGIN_OPTIONS, options)
        intent.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account)
        intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        return null
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account, features: Array<String>
    ): Bundle {
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
        return result
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account, authTokenType: String, options: Bundle
    ): Bundle {
        val intent = Intent(mContext, AuthenticatorActivity::class.java)

        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(KEY_ACCOUNT, account)
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType)
        intent.putExtra(KEY_LOGIN_OPTIONS, options)
        setIntentFlags(intent)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse, account: Account): Bundle {
        return super.getAccountRemovalAllowed(response, account)
    }

    private fun setIntentFlags(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
    }

    @Throws(UnsupportedAccountTypeException::class)
    private fun validateAccountType(type: String) {
        if (type != MainApp.getAccountType(mContext)) {
            throw UnsupportedAccountTypeException
        }
    }

    @Throws(UnsupportedAuthTokenTypeException::class)
    private fun validateAuthTokenType(authTokenType: String) {
        val accountType = MainApp.getAccountType(mContext)

        if (authTokenType != accountType &&
            authTokenType != AccountTypeUtils.getAuthTokenTypePass(accountType)
        ) {
            throw UnsupportedAuthTokenTypeException
        }
    }

    open class AuthenticatorException(code: Int, errorMsg: String?) : Exception() {
        val failureBundle: Bundle = Bundle()

        init {
            failureBundle.putInt(AccountManager.KEY_ERROR_CODE, code)
            failureBundle.putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg)
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    object UnsupportedAccountTypeException :
        AuthenticatorException(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported account type")

    object UnsupportedAuthTokenTypeException :
        AuthenticatorException(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "Unsupported auth token type")

    companion object {
        /**
         * Is used by android system to assign accounts to authenticators.
         * Should be used by application and all extensions.
         */
        const val KEY_AUTH_TOKEN_TYPE = "authTokenType"
        const val KEY_REQUIRED_FEATURES = "requiredFeatures"
        const val KEY_LOGIN_OPTIONS = "loginOptions"
        const val KEY_ACCOUNT = "account"
        private val TAG = AccountAuthenticator::class.java.simpleName
    }
}
