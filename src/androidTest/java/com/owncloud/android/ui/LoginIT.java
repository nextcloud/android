/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.junit.Assert.assertEquals;


@LargeTest
public class LoginIT extends AbstractIT {
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        tearDown();

        ActivityScenario.launch(AuthenticatorActivity.class);
    }

    @AfterClass
    public static void tearDown() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);

        if (accountManager.getAccounts().length > 0) {
            accountManager.removeAllAccounts();
        }
    }

    @Test
    public void login() throws InterruptedException {
        Bundle arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments();

        String baseUrl = arguments.getString("TEST_SERVER_URL");
        String loginName = arguments.getString("TEST_SERVER_USERNAME");
        String password = arguments.getString("TEST_SERVER_PASSWORD");

        onView(withId(R.id.login)).perform(click());
        onView(withId(R.id.host_url_input)).perform(typeText(baseUrl));
        onView(withId(R.id.host_url_input)).perform(typeTextIntoFocusedView("\n"));

        Thread.sleep(3000);

        onWebView().forceJavascriptEnabled();

        // click on login
        onWebView()
            .withElement(findElement(Locator.XPATH, "//p[@id='redirect-link']/a"))
            .perform(webClick());

        // username
        onWebView()
            .withElement(findElement(Locator.XPATH, "//input[@id='user']"))
            .perform(DriverAtoms.webKeys(loginName));

        // password
        onWebView()
            .withElement(findElement(Locator.XPATH, "//input[@id='password']"))
            .perform(DriverAtoms.webKeys(password));

        // click login
        onWebView()
            .withElement(findElement(Locator.XPATH, "//input[@id='submit-form']"))
            .perform(webClick());

        Thread.sleep(2000);

        // grant access
        onWebView()
            .withElement(findElement(Locator.XPATH, "//input[@type='submit']"))
            .perform(webClick());

        Thread.sleep(5 * 1000);

        // check for account
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);

        assertEquals(1, accountManager.getAccounts().length);

        Account account = accountManager.getAccounts()[0];

        // account.name is loginName@baseUrl (without protocol)
        assertEquals(loginName, account.name.split("@")[0]);
        assertEquals(baseUrl.split("//")[1], account.name.split("@")[1]);
    }
}
