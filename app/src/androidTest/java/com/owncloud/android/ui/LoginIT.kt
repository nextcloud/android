/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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

    /**
     * The CI/CD pipeline is encountering issues related to the Android version for this functionality.
     * Therefore the test will only be executed on Android versions 10 and above.
     */
    @Test
    @Throws(InterruptedException::class)
    @Suppress("MagicNumber", "SwallowedException")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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
        } catch (_: RuntimeException) {
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
        } catch (_: RuntimeException) {
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
