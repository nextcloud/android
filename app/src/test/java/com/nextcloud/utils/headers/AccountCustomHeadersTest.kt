/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.nextcloud.common.CustomHeaders
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountCustomHeadersTest {
    private val context = mockk<Context>(relaxed = true)
    private val accountManager = mockk<AccountManager>()
    private val userData = mutableMapOf<Pair<Account, String>, String?>()
    private val accounts = mutableListOf<Account>()

    @Before
    fun setUp() {
        UriParsingMock.install()
        mockkStatic(AccountManager::class, MainApp::class)
        every { AccountManager.get(any()) } returns accountManager
        every { MainApp.getAccountType(any()) } returns ACCOUNT_TYPE
        every { accountManager.getAccountsByType(ACCOUNT_TYPE) } answers { accounts.toTypedArray() }
        every { accountManager.getUserData(any(), any()) } answers { userData[firstArg<Account>() to secondArg()] }
        every { accountManager.setUserData(any(), any(), any()) } answers {
            userData[firstArg<Account>() to secondArg()] = thirdArg()
        }
    }

    @After
    fun tearDown() {
        CustomHeaders.origins.forEach { CustomHeaders.unregister(it) }
        unmockkAll()
    }

    @Test
    fun `headers of https accounts are registered for their origin`() {
        addAccount("https://Cloud.Example.com:443/nextcloud", mapOf("X-A" to "1"))

        AccountCustomHeaders.registerAll(context)

        assertEquals(mapOf("X-A" to "1"), CustomHeaders.forOrigin("https://cloud.example.com"))
    }

    @Test
    fun `headers of accounts that are not https are ignored`() {
        addAccount("http://cloud.example.com", mapOf("X-A" to "1"))

        AccountCustomHeaders.registerAll(context)

        assertTrue(CustomHeaders.origins.isEmpty())
    }

    @Test
    fun `reconcile drops the headers of removed accounts only`() {
        val removed = addAccount("https://one.example.com", mapOf("X-A" to "1"))
        addAccount("https://two.example.com", mapOf("X-B" to "2"))
        AccountCustomHeaders.registerAll(context)

        accounts.remove(removed)
        AccountCustomHeaders.reconcile(context)

        assertEquals(setOf("https://two.example.com"), CustomHeaders.origins)
    }

    @Test
    fun `replaced headers are stored and take effect`() {
        val account = addAccount("https://cloud.example.com", mapOf("X-A" to "1"))
        AccountCustomHeaders.registerAll(context)

        AccountCustomHeaders.replace(context, account, mapOf("X-B" to "2"))

        assertEquals(mapOf("X-B" to "2"), AccountCustomHeaders.read(context, account))
        assertEquals(mapOf("X-B" to "2"), CustomHeaders.forOrigin("https://cloud.example.com"))
    }

    @Test
    fun `replacing with no headers removes them`() {
        val account = addAccount("https://cloud.example.com", mapOf("X-A" to "1"))
        AccountCustomHeaders.registerAll(context)

        AccountCustomHeaders.replace(context, account, emptyMap())

        assertTrue(AccountCustomHeaders.read(context, account).isEmpty())
        assertTrue(CustomHeaders.origins.isEmpty())
    }

    private fun addAccount(baseUrl: String, headers: Map<String, String>): Account {
        val account = mockk<Account>()
        accounts.add(account)
        userData[account to AccountUtils.Constants.KEY_OC_BASE_URL] = baseUrl
        userData[account to KEY_CUSTOM_HEADERS] = CustomHeaders.toJson(headers)
        return account
    }

    companion object {
        private const val ACCOUNT_TYPE = "nextcloud"
        private const val KEY_CUSTOM_HEADERS = "oc_custom_headers"
    }
}
