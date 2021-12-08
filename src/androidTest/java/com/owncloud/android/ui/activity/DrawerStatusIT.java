/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.net.Uri;
import android.os.Bundle;

import com.nextcloud.client.GrantStoragePermissionRule;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;


/**
 * Implements a test for drawer menu behavior
 * when navigating to an empty folder
 */
public class DrawerStatusIT extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final TestRule permissionRule = GrantStoragePermissionRule.grant();
    private static Account userAccount;
    private static User userName;


    /**
     * Sets up the test scenario
     * by logging into an account
     * using the test user data and test server
     */
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

        Account tempAccount = new Account(loginName + "@" + baseUrl, MainApp.getAccountType(targetContext));
        platformAccountManager.addAccountExplicitly(tempAccount, password, null);
        platformAccountManager.setUserData(tempAccount, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                           Integer.toString(UserAccountManager.ACCOUNT_VERSION));
        platformAccountManager.setUserData(tempAccount, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
        platformAccountManager.setUserData(tempAccount, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString());
        platformAccountManager.setUserData(tempAccount, AccountUtils.Constants.KEY_USER_ID, loginName); // same as userId

        userAccount = userAccountManager.getAccountByName(loginName + "@" + baseUrl);
        userName = userAccountManager.getUser(userAccount.name).orElseThrow(IllegalAccessError::new);
    }

    /**
     * Creates drawer click behavior.
     * Navigating to empty folder and
     * verifies that All Files is highlighted in the menu.
     */
    @Test
    public void selectMenuItem() {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        sut.setUser(userName);


        // Click on the menu button
        onView(withId(R.id.menu_button)).perform(click());

        onView(withId(R.id.nav_favorites)).perform(click());

        onView(withId(R.id.menu_button)).perform(click());


        // Assert that the menu id has been updated
        onView(allOf(withId(R.id.nav_all_files), isChecked()));
    }
}
