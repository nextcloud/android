/* Nextcloud Android Library is available under MIT license
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2019 Tobias Kaminsky
 *   Copyright (C) 2019 Nextcloud GmbH
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
package com.nextcloud.client.account

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.accounts.AccountUtils
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class OwnCloudClientManagerTest : AbstractOnServerIT() {
    /**
     * Like on files app we create & store an account in Android's account manager.
     */
    @Test
    @Throws(
        OperationCanceledException::class,
        AuthenticatorException::class,
        IOException::class,
        AccountUtils.AccountNotFoundException::class
    )
    fun testUserId() {
        val arguments = InstrumentationRegistry.getArguments()
        val url = Uri.parse(arguments.getString("TEST_SERVER_URL"))
        val loginName = arguments.getString("TEST_SERVER_USERNAME")
        val password = arguments.getString("TEST_SERVER_PASSWORD")
        val accountManager = AccountManager.get(targetContext)
        val accountName = AccountUtils.buildAccountName(url, loginName)
        val newAccount = Account(accountName, "nextcloud")
        accountManager.addAccountExplicitly(newAccount, password, null)
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString())
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName)
        val manager = OwnCloudClientManager()
        val account = OwnCloudAccount(newAccount, targetContext)
        val client = manager.getClientFor(account, targetContext)
        Assert.assertEquals(loginName, client.userId)
        accountManager.removeAccountExplicitly(newAccount)
        Assert.assertEquals(1, accountManager.accounts.size.toLong())
    }
}