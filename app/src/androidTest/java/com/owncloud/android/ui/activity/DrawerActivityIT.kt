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
import android.content.ContentValues
import android.net.Uri
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.test.Flaky
import com.nextcloud.test.GrantTestPermissionRule
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.accounts.AccountUtils
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

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantTestPermissionRule.grantStorageAndNotification()

    @Test
    @Flaky(reason = "Account switch relaunches FileDisplayActivity, which races with the drawer assertions")
    fun switchAccountViaAccountList() {
        // Switching accounts finishes and relaunches FileDisplayActivity (see
        // FileDisplayActivity.handleRestartIntent). That self-relaunch is incompatible with
        // ActivityScenario#close(), which cannot drive its tracked instance to DESTROYED, so the
        // scenario is launched without auto-closing.
        val scenario = launchActivity<FileDisplayActivity>()
        lateinit var sut: FileDisplayActivity
        scenario.onActivity { activity ->
            sut = activity
        }

        Assert.assertEquals(account1, sut.user.get().toPlatformAccount())

        onView(ViewMatchers.withId(R.id.switch_account_button)).perform(ViewActions.click())
        onView(
            Matchers.anyOf(
                ViewMatchers.withText(account2Name),
                ViewMatchers.withText(
                    account2DisplayName
                )
            )
        ).perform(ViewActions.click())

        Assert.assertEquals(account2, sut.user.get().toPlatformAccount())

        onView(ViewMatchers.withId(R.id.switch_account_button)).perform(ViewActions.click())
        onView(ViewMatchers.withText(account1?.name)).perform(ViewActions.click())
    }

    /**
     * External link menu items get the item id [MENU_ITEM_EXTERNAL_LINK] + the link's local database id, and that
     * id grows without bound because the table is cleared and refilled on every refresh. A link whose id has grown
     * past the legacy 0-100 window must still open.
     */
    @Test
    fun externalLinkWithIdBeyondLegacyRangeIsOpened() {
        insertExternalLink()

        Intents.init()
        try {
            launchActivity<FileDisplayActivity>().use {
                waitForIdleSync()

                onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
                onView(ViewMatchers.withId(R.id.nav_view))
                    .perform(NavigationViewActions.navigateTo(MENU_ITEM_EXTERNAL_LINK + EXTERNAL_LINK_ID))

                Intents.intended(IntentMatchers.hasComponent(ExternalSiteWebView::class.java.name))
                Intents.intended(IntentMatchers.hasExtra(ExternalSiteWebView.EXTRA_URL, EXTERNAL_LINK_URL))
            }
        } finally {
            Intents.release()
            deleteExternalLink()
        }
    }

    private fun insertExternalLink() {
        val values = ContentValues().apply {
            put(ProviderTableMeta._ID, EXTERNAL_LINK_ID)
            put(ProviderTableMeta.EXTERNAL_LINKS_ICON_URL, "")
            put(ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE, "en")
            put(ProviderTableMeta.EXTERNAL_LINKS_TYPE, ExternalLinkType.LINK.toString())
            put(ProviderTableMeta.EXTERNAL_LINKS_NAME, EXTERNAL_LINK_NAME)
            put(ProviderTableMeta.EXTERNAL_LINKS_URL, EXTERNAL_LINK_URL)
            put(ProviderTableMeta.EXTERNAL_LINKS_REDIRECT, false)
        }

        val uri = targetContext.contentResolver.insert(ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, values)
        Assert.assertNotNull("External link could not be stored", uri)

        // the whole point of the test is an id outside the legacy window, so fail loudly if it was not honoured
        Assert.assertTrue(
            "External link id must exceed the legacy window",
            EXTERNAL_LINK_ID > LEGACY_EXTERNAL_LINK_RANGE
        )
    }

    private fun deleteExternalLink() {
        targetContext.contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
            "${ProviderTableMeta._ID} = ?",
            arrayOf(EXTERNAL_LINK_ID.toString())
        )
    }

    companion object {
        // kept in sync with DrawerActivity, where both values are private
        private const val MENU_ITEM_EXTERNAL_LINK = 111
        private const val LEGACY_EXTERNAL_LINK_RANGE = 100

        private const val EXTERNAL_LINK_ID = 501
        private const val EXTERNAL_LINK_NAME = "High ID Test"
        private const val EXTERNAL_LINK_URL = "https://nextcloud.com"

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
