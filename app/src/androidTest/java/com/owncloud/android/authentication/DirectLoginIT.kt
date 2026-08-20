/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication

import android.accounts.AccountManager
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class DirectLoginIT : AbstractOnServerIT() {

    @get:Rule
    var retryTestRule = RetryTestRule()

    private lateinit var scenario: ActivityScenario<AuthenticatorActivity>

    @Before
    fun setUp() {
        AccountManager.get(targetContext).removeAccountExplicitly(account)
        scenario = ActivityScenario.launch(AuthenticatorActivity::class.java)
    }

    @After
    override fun after() {
        scenario.close()
        AccountManager.get(targetContext).removeAccountExplicitly(account)
        super.after()
    }

    @Test
    fun directLoginSectionHiddenInitially() {
        onView(withId(R.id.direct_login_section)).check(matches(not(isDisplayed())))
        onView(withId(R.id.direct_login_toggle)).check(matches(not(isDisplayed())))
        onView(withId(R.id.browser_login_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun directLoginToggleAppearsAfterServerValidation() {
        submitServerUrl()

        onView(withId(R.id.direct_login_toggle)).check(matches(isDisplayed()))
        onView(withId(R.id.browser_login_button)).check(matches(isDisplayed()))
    }

    @Test
    fun directLoginShowsFormOnToggleClick() {
        showDirectLoginForm()

        onView(withId(R.id.direct_login_section)).check(matches(isDisplayed()))
        onView(withId(R.id.direct_login_username)).check(matches(isDisplayed()))
        onView(withId(R.id.direct_login_password)).check(matches(isDisplayed()))
        onView(withId(R.id.direct_login_button)).check(matches(isDisplayed()))
        onView(withId(R.id.browser_login_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun directLoginEmptyFieldsShowsErrors() {
        showDirectLoginForm()
        onView(withId(R.id.direct_login_button)).perform(click())

        onView(withText(R.string.direct_login_username_required)).check(matches(isDisplayed()))
    }

    @Test
    fun directLoginEmptyPasswordShowsError() {
        showDirectLoginForm()
        onView(withId(R.id.direct_login_username)).perform(typeText("testuser"), closeSoftKeyboard())
        onView(withId(R.id.direct_login_button)).perform(click())

        onView(withText(R.string.direct_login_password_required)).check(matches(isDisplayed()))
    }

    @Test
    fun directLoginWrongCredentialsShowsError() {
        showDirectLoginForm()
        onView(withId(R.id.direct_login_username)).perform(typeText("wronguser"), closeSoftKeyboard())
        onView(withId(R.id.direct_login_password)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.direct_login_button)).perform(click())
        Thread.sleep(AUTHENTICATION_DELAY_MILLIS)

        onView(withText(R.string.auth_unauthorized)).check(matches(isDisplayed()))
    }

    @Test
    fun directLoginWithValidAppPasswordCreatesAccount() {
        showDirectLoginForm()
        onView(withId(R.id.direct_login_username)).perform(typeText(testServerUsername()), closeSoftKeyboard())
        onView(withId(R.id.direct_login_password)).perform(typeText(testServerAppPassword()), closeSoftKeyboard())
        onView(withId(R.id.direct_login_button)).perform(click())
        Thread.sleep(AUTHENTICATION_DELAY_MILLIS)

        assertTrue(UserAccountManagerImpl.fromContext(targetContext).accounts.isNotEmpty())
    }

    private fun showDirectLoginForm() {
        submitServerUrl()
        onView(withId(R.id.direct_login_toggle)).perform(click())
    }

    private fun submitServerUrl() {
        onView(withId(R.id.host_url_input)).perform(
            typeText(testServerUrl()),
            pressKey(KeyEvent.KEYCODE_ENTER),
            closeSoftKeyboard()
        )
        Thread.sleep(SERVER_VALIDATION_DELAY_MILLIS)
    }

    private fun testServerUrl(): String = testServerArgument("TEST_SERVER_URL", "server URL")

    private fun testServerUsername(): String = testServerArgument("TEST_SERVER_USERNAME", "username")

    private fun testServerAppPassword(): String =
        testServerArgument("TEST_SERVER_PASSWORD", "dedicated test user's app-specific password")

    private fun testServerArgument(argumentName: String, description: String): String {
        val value = InstrumentationRegistry.getArguments().getString(argumentName)
        require(!value.isNullOrBlank() && value != "null") {
            "$argumentName must be configured with the $description"
        }
        return value
    }

    companion object {
        private const val SERVER_VALIDATION_DELAY_MILLIS = 5_000L
        private const val AUTHENTICATION_DELAY_MILLIS = 10_000L
    }
}
