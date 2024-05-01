/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
