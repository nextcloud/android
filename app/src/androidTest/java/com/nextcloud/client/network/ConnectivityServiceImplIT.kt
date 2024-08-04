/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

import android.accounts.AccountManager
import android.content.Context
import android.net.ConnectivityManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.network.ConnectivityServiceImpl.GetRequestBuilder
import com.owncloud.android.AbstractOnServerIT
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityServiceImplIT : AbstractOnServerIT() {
    @Test
    fun testInternetWalled() {
        val connectivityManager = targetContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val accountManager = targetContext.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        val userAccountManager = UserAccountManagerImpl(targetContext, accountManager)
        val clientFactory = ClientFactoryImpl(targetContext)
        val requestBuilder = GetRequestBuilder()
        val walledCheckCache = WalledCheckCache(ClockImpl())

        val sut = ConnectivityServiceImpl(
            connectivityManager,
            userAccountManager,
            clientFactory,
            requestBuilder,
            walledCheckCache
        )

        assertTrue(sut.connectivity.isConnected)
        assertFalse(sut.isInternetWalled)
    }
}
