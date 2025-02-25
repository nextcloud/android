/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.net.Uri;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.test.RetryTestRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.assertEquals;

public class DrawerActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final RetryTestRule retryTestRule = new RetryTestRule();

    private static Account account1;
    private static User user1;
    private static Account account2;
    private static String account2Name;
    private static String account2DisplayName;

    @BeforeClass
    public static void beforeClass() {
        Bundle arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments();
        Uri baseUrl = Uri.parse(arguments.getString("TEST_SERVER_URL"));

        AccountManager platformAccountManager = AccountManager.get(targetContext);
        UserAccountManager userAccountManager = UserAccountManagerImpl.fromContext(targetContext);

        for (Account account : platformAccountManager.getAccounts()) {
            platformAccountManager.removeAccountExplicitly(account);
        }

        String loginName = "user1";
        String password = "user1";

        Account temp = new Account(loginName + "@" + baseUrl, MainApp.getAccountType(targetContext));
        platformAccountManager.addAccountExplicitly(temp, password, null);
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                           Integer.toString(UserAccountManager.ACCOUNT_VERSION));
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString());
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, loginName); // same as userId

        account1 = userAccountManager.getAccountByName(loginName + "@" + baseUrl);
        User user = userAccountManager.getUser(account1.name);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        user1 = user;

        loginName = "user2";
        password = "user2";

        temp = new Account(loginName + "@" + baseUrl, MainApp.getAccountType(targetContext));
        platformAccountManager.addAccountExplicitly(temp, password, null);
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                           Integer.toString(UserAccountManager.ACCOUNT_VERSION));
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString());
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, loginName); // same as userId

        account2 = userAccountManager.getAccountByName(loginName + "@" + baseUrl);
        account2Name = loginName + "@" + baseUrl;
        account2DisplayName = "User Two@" + baseUrl;
    }

    @Test
    public void switchAccountViaAccountList() {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        sut.setUser(user1);

        assertEquals(account1, sut.getUser().toPlatformAccount());

        onView(withId(R.id.switch_account_button)).perform(click());

        onView(anyOf(withText(account2Name), withText(account2DisplayName))).perform(click());

        waitForIdleSync();

        assertEquals(account2, sut.getUser().toPlatformAccount());

        onView(withId(R.id.switch_account_button)).perform(click());
        onView(withText(account1.name)).perform(click());
    }
}
