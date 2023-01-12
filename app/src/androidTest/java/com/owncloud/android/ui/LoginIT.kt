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
package com.owncloud.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class LoginIT : AbstractIT() {
    @get:Rule
    val permissionRule = GrantStoragePermissionRule.grant()

    @get:Rule
    var retryTestRule = RetryTestRule()

    @Before
    fun setUp() {
        tearDown()
        ActivityScenario.launch(AuthenticatorActivity::class.java)
    }

    @Test
    @Throws(InterruptedException::class)
    @Suppress("MagicNumber", "SwallowedException")
    fun login() {
        val arguments = InstrumentationRegistry.getArguments()
        val baseUrl = arguments.getString("TEST_SERVER_URL")!!
        val loginName = arguments.getString("TEST_SERVER_USERNAME")!!
        val password = arguments.getString("TEST_SERVER_PASSWORD")!!
        Espresso.onView(ViewMatchers.withId(R.id.login)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.host_url_input)).perform(ViewActions.typeText(baseUrl))
        Espresso.onView(ViewMatchers.withId(R.id.host_url_input)).perform(ViewActions.typeTextIntoFocusedView("\n"))
        Thread.sleep(3000)
        Web.onWebView().forceJavascriptEnabled()

        // click on login
        try {
            // NC 25+
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.XPATH, "//form[@id='login-form']/input[@type='submit']"))
                .perform(DriverAtoms.webClick())
        } catch (e: RuntimeException) {
            // NC < 25
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.XPATH, "//p[@id='redirect-link']/a"))
                .perform(DriverAtoms.webClick())
        }

        // username
        Web.onWebView()
            .withElement(DriverAtoms.findElement(Locator.XPATH, "//input[@id='user']"))
            .perform(DriverAtoms.webKeys(loginName))

        // password
        Web.onWebView()
            .withElement(DriverAtoms.findElement(Locator.XPATH, "//input[@id='password']"))
            .perform(DriverAtoms.webKeys(password))

        // click login
        try {
            // NC 25+
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.XPATH, "//button[@type='submit']"))
                .perform(DriverAtoms.webClick())
        } catch (e: RuntimeException) {
            // NC < 25
            Web.onWebView()
                .withElement(DriverAtoms.findElement(Locator.XPATH, "//input[@type='submit']"))
                .perform(DriverAtoms.webClick())
        }

        Thread.sleep(2000)

        // grant access
        Web.onWebView()
            .withElement(DriverAtoms.findElement(Locator.XPATH, "//input[@type='submit']"))
            .perform(DriverAtoms.webClick())
        Thread.sleep((5 * 1000).toLong())

        // check for account
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val accountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)
        Assert.assertEquals(1, accountManager.accounts.size.toLong())
        val account = accountManager.accounts[0]

        // account.name is loginName@baseUrl (without protocol)
        Assert.assertEquals(loginName, account.name.split("@".toRegex()).toTypedArray()[0])
        Assert.assertEquals(
            baseUrl.split("//".toRegex()).toTypedArray()[1],
            account.name.split("@".toRegex()).toTypedArray()[1]
        )
    }

    companion object {
        @AfterClass
        fun tearDown() {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val accountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)
            if (accountManager.accounts.isNotEmpty()) {
                accountManager.removeAllAccounts()
            }
        }
    }
}
