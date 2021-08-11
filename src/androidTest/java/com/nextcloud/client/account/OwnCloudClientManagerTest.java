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

package com.nextcloud.client.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.net.Uri;
import android.os.Bundle;

import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManager;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import org.junit.Test;

import java.io.IOException;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

public class OwnCloudClientManagerTest extends AbstractOnServerIT {

    /**
     * Like on files app we create & store an account in Android's account manager.
     */
    @Test
    public void testUserId() throws OperationCanceledException, AuthenticatorException, IOException,
        AccountUtils.AccountNotFoundException {
        Bundle arguments = InstrumentationRegistry.getArguments();

        Uri url = Uri.parse(arguments.getString("TEST_SERVER_URL"));
        String loginName = arguments.getString("TEST_SERVER_USERNAME");
        String password = arguments.getString("TEST_SERVER_PASSWORD");

        AccountManager accountManager = AccountManager.get(targetContext);
        String accountName = AccountUtils.buildAccountName(url, loginName);
        Account newAccount = new Account(accountName, "nextcloud");

        accountManager.addAccountExplicitly(newAccount, password, null);
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, url.toString());
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, loginName);

        OwnCloudClientManager manager = new OwnCloudClientManager();
        OwnCloudAccount account = new OwnCloudAccount(newAccount, targetContext);

        OwnCloudClient client = manager.getClientFor(account, targetContext);

        assertEquals(loginName, client.getUserId());

        accountManager.removeAccountExplicitly(newAccount);

        assertEquals(1, accountManager.getAccounts().length);
    }
}
