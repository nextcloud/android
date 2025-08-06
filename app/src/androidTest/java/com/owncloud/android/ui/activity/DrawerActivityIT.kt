/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AccountManager
import android.net.Uri
import android.view.View
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.EspressoIdlingResource
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import java.util.function.Supplier

class DrawerActivityIT : AbstractIT() {
    @Rule
    @JvmField
    val retryTestRule = RetryTestRule()

    @Test
    @UiThread
    fun switchAccountViaAccountList() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.setUser(user1)

                    Assert.assertEquals(account1, sut.user.get().toPlatformAccount())
                    onView(ViewMatchers.withId(R.id.switch_account_button)).perform(ViewActions.click())
                    onView(
                        Matchers.anyOf<View?>(
                            ViewMatchers.withText(account2Name),
                            ViewMatchers.withText(
                                account2DisplayName
                            )
                        )
                    ).perform(ViewActions.click())
                    Assert.assertEquals(account2, sut.user.get().toPlatformAccount())
                    EspressoIdlingResource.decrement()

                    onView(ViewMatchers.withId(R.id.switch_account_button)).perform(ViewActions.click())
                    onView(ViewMatchers.withText(account1?.name)).perform(ViewActions.click())
                }
            }
        }
    }

    companion object {
        private var account1: Account? = null
        private var user1: User? = null
        private var account2: Account? = null
        private var account2Name: String? = null
        private var account2DisplayName: String? = null

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            val arguments = InstrumentationRegistry.getArguments()
            val baseUrl = Uri.parse(arguments.getString("TEST_SERVER_URL"))

            val platformAccountManager = AccountManager.get(targetContext)
            val userAccountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)

            for (account in platformAccountManager.accounts) {
                platformAccountManager.removeAccountExplicitly(account)
            }

            var loginName = "user1"
            var password = "user1"

            var temp = Account("$loginName@$baseUrl", MainApp.getAccountType(targetContext))
            platformAccountManager.addAccountExplicitly(temp, password, null)
            platformAccountManager.setUserData(
                temp,
                AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                UserAccountManager.ACCOUNT_VERSION.toString()
            )
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0")
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString())
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, loginName) // same as userId

            account1 = userAccountManager.getAccountByName("$loginName@$baseUrl")
            user1 = userAccountManager.getUser(account1!!.name)
                .orElseThrow<IllegalAccessError?>(Supplier { IllegalAccessError() })

            loginName = "user2"
            password = "user2"

            temp = Account("$loginName@$baseUrl", MainApp.getAccountType(targetContext))
            platformAccountManager.addAccountExplicitly(temp, password, null)
            platformAccountManager.setUserData(
                temp,
                AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                UserAccountManager.ACCOUNT_VERSION.toString()
            )
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0")
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString())
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, loginName) // same as userId

            account2 = userAccountManager.getAccountByName("$loginName@$baseUrl")
            account2Name = "$loginName@$baseUrl"
            account2DisplayName = "User Two@$baseUrl"
        }
    }
}
