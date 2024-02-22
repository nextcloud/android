/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.owncloud.android.authentication

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.utils.extensions.getParcelableArgument

/*
 * Base class for implementing an Activity that is used to help implement an AbstractAccountAuthenticator.
 * If the AbstractAccountAuthenticator needs to use an activity to handle the request then it can have the activity extend
 * AccountAuthenticatorActivity. The AbstractAccountAuthenticator passes in the response to the intent using the following:
 * intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
 *
 * The activity then sets the result that is to be handed to the response via setAccountAuthenticatorResult(android.os.Bundle).
 * This result will be sent as the result of the request when the activity finishes. If this is never set or if it is set to null
 * then error AccountManager.ERROR_CODE_CANCELED will be called on the response.
 */
abstract class AccountAuthenticatorActivity : AppCompatActivity() {

    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    /**
     * Set the result that is to be sent as the result of the request that caused this Activity to be launched.
     * If result is null or this method is never called then the request will be canceled.
     *
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    fun setAccountAuthenticatorResult(result: Bundle?) {
        mResultBundle = result
    }

    /**
     * Retrieves the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param savedInstanceState the save instance data of this Activity, may be null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAccountAuthenticatorResponse = intent.getParcelableArgument(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, AccountAuthenticatorResponse::class.java)
        mAccountAuthenticatorResponse?.onRequestContinued()
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    override fun finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse?.onResult(mResultBundle)
            } else {
                mAccountAuthenticatorResponse?.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            mAccountAuthenticatorResponse = null
        }

        super.finish()
    }
}
