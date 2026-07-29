/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.accounts.Account
import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class UserAccountManagerExtensionsTest {

    private val accountName = "test@server.com"

    private lateinit var accountManager: UserAccountManager
    private lateinit var context: Context
    private lateinit var platformAccount: Account
    private lateinit var client: OwnCloudClient
    private lateinit var clientFactory: MockedStatic<OwnCloudClientFactory>

    @Before
    fun setUp() {
        context = mock()
        platformAccount = mock()
        client = mock()

        accountManager = mock()
        whenever(accountManager.context).thenReturn(context)

        clientFactory = Mockito.mockStatic(OwnCloudClientFactory::class.java)
    }

    @After
    fun tearDown() {
        clientFactory.close()
    }

    @Test
    fun `client is created for a registered account`() {
        givenRegisteredAccount()
        givenClientIsCreatedFor(platformAccount, context)

        assertSame(client, accountManager.createOwncloudClient(accountName))
    }

    @Test
    fun `client is created for the current account when no account name is given`() {
        whenever(accountManager.currentAccount).thenReturn(accountNamed(accountName))
        givenRegisteredAccount()
        givenClientIsCreatedFor(platformAccount, context)

        assertSame(client, accountManager.createOwncloudClient())
    }

    @Test
    fun `no client is created for an unknown account`() {
        whenever(accountManager.getUser(accountName)).thenReturn(Optional.empty())

        assertNull(accountManager.createOwncloudClient(accountName))

        clientFactory.verifyNoInteractions()
    }

    @Test
    fun `no client is created while the account has no base url yet`() {
        val anonymousUser = mock<User>()
        whenever(anonymousUser.isAnonymous).thenReturn(true)
        whenever(accountManager.getUser(accountName)).thenReturn(Optional.of(anonymousUser))

        assertNull(accountManager.createOwncloudClient(accountName))

        clientFactory.verifyNoInteractions()
    }

    @Test
    fun `no client is created when the account is removed while the client is created`() {
        givenRegisteredAccount()
        clientFactory.`when`<OwnCloudClient> {
            OwnCloudClientFactory.createOwnCloudClient(platformAccount, context)
        }.thenThrow(AccountUtils.AccountNotFoundException(platformAccount, "Account not found", null))

        assertNull(accountManager.createOwncloudClient(accountName))
    }

    private fun givenClientIsCreatedFor(account: Account, appContext: Context) {
        clientFactory.`when`<OwnCloudClient> {
            OwnCloudClientFactory.createOwnCloudClient(account, appContext)
        }.thenReturn(client)
    }

    // Account.name is a public final field, so it cannot be stubbed and has to be written directly
    private fun accountNamed(name: String): Account = mock<Account>().also { account ->
        Account::class.java.getField("name").apply { isAccessible = true }.set(account, name)
    }

    @Suppress("DEPRECATION")
    private fun givenRegisteredAccount() {
        val user = mock<User>()
        whenever(user.isAnonymous).thenReturn(false)
        whenever(user.toPlatformAccount()).thenReturn(platformAccount)
        whenever(accountManager.getUser(accountName)).thenReturn(Optional.of(user))
    }
}
