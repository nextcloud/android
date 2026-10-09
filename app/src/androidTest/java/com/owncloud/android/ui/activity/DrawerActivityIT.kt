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
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.navigation.NavigationView
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
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class DrawerActivityIT : AbstractIT() {
    @get:Rule
    val retryTestRule = RetryTestRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantTestPermissionRule.grantStorageAndNotification()

    @Test
    @Flaky(reason = "Account switch relaunches FileDisplayActivity, which races with the drawer assertions")
    fun switchAccountViaAccountList() {
        val scenario = launchActivity<FileDisplayActivity>()
        lateinit var sut: FileDisplayActivity
        scenario.onActivity { sut = it }

        assertEquals(account1, sut.user.get().toPlatformAccount())

        onView(withId(R.id.switch_account_button)).perform(click())
        onView(anyOf(withText(account2Name), withText(account2DisplayName))).perform(click())

        assertEquals(account2, sut.user.get().toPlatformAccount())

        onView(withId(R.id.switch_account_button)).perform(click())
        onView(withText(account1.name)).perform(click())
    }

    @Test
    fun externalLinkWithIdBeyondLegacyRangeIsOpened() {
        val linkId = insertExternalLink()
        assertTrue(linkId > LEGACY_EXTERNAL_LINK_RANGE)
        val menuItemId = MENU_ITEM_EXTERNAL_LINK + linkId

        Intents.init()
        try {
            val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.Community)
            ActivityScenario.launch<NavigatorActivity>(intent).use { scenario ->
                waitUntil { scenario.hasDrawerMenuItem(menuItemId) }

                onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
                onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(menuItemId))

                val externalSiteIntent = allOf(
                    hasComponent(ExternalSiteWebView::class.java.name),
                    hasExtra(ExternalSiteWebView.EXTRA_URL, EXTERNAL_LINK_URL)
                )
                waitUntil { Intents.getIntents().any(externalSiteIntent::matches) }
            }
        } finally {
            Intents.release()
            deleteExternalLink(linkId)
        }
    }

    private fun ActivityScenario<NavigatorActivity>.hasDrawerMenuItem(menuItemId: Int): Boolean {
        var found = false
        onActivity {
            found = it.findViewById<NavigationView>(R.id.nav_view).menu.findItem(menuItemId) != null
        }
        return found
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MILLIS
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Condition not met within $WAIT_TIMEOUT_MILLIS ms" }
            SystemClock.sleep(WAIT_POLL_INTERVAL_MILLIS)
        }
    }

    private fun insertExternalLink(): Int {
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
        return ContentUris.parseId(requireNotNull(uri) { "External link could not be stored" }).toInt()
    }

    private fun deleteExternalLink(linkId: Int) {
        targetContext.contentResolver.delete(
            ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
            "${ProviderTableMeta._ID} = ?",
            arrayOf(linkId.toString())
        )
    }

    companion object {
        private const val MENU_ITEM_EXTERNAL_LINK = 111
        private const val LEGACY_EXTERNAL_LINK_RANGE = 100

        private const val EXTERNAL_LINK_ID = 501
        private const val EXTERNAL_LINK_NAME = "High ID Test"
        private const val EXTERNAL_LINK_URL = "https://nextcloud.com"

        private const val WAIT_TIMEOUT_MILLIS = 5_000L
        private const val WAIT_POLL_INTERVAL_MILLIS = 100L

        private const val SERVER_VERSION = "14.0.0.0"

        private lateinit var account1: Account
        private lateinit var account2: Account
        private lateinit var account2Name: String
        private lateinit var account2DisplayName: String

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            val baseUrl = Uri.parse(InstrumentationRegistry.getArguments().getString("TEST_SERVER_URL"))
            val platformAccountManager = AccountManager.get(targetContext)

            platformAccountManager.accounts.forEach(platformAccountManager::removeAccountExplicitly)

            account1 = addAccount(platformAccountManager, "user1", baseUrl)
            account2 = addAccount(platformAccountManager, "user2", baseUrl)
            account2Name = account2.name
            account2DisplayName = "User Two@$baseUrl"
        }

        private fun addAccount(platformAccountManager: AccountManager, loginName: String, baseUrl: Uri): Account {
            val accountName = "$loginName@$baseUrl"
            val account = Account(accountName, MainApp.getAccountType(targetContext))

            platformAccountManager.run {
                addAccountExplicitly(account, loginName, null)
                setUserData(
                    account,
                    AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                    UserAccountManager.ACCOUNT_VERSION.toString()
                )
                setUserData(account, AccountUtils.Constants.KEY_OC_VERSION, SERVER_VERSION)
                setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString())
                setUserData(account, AccountUtils.Constants.KEY_USER_ID, loginName)
            }

            return requireNotNull(UserAccountManagerImpl.fromContext(targetContext).getAccountByName(accountName))
        }
    }
}
