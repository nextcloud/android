/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.network

import android.accounts.AccountManager
import android.content.Context
import android.net.ConnectivityManager
import com.nextcloud.client.account.UserAccountManagerImpl
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

        val sut = ConnectivityServiceImpl(
            connectivityManager,
            userAccountManager,
            clientFactory,
            requestBuilder
        )

        assertTrue(sut.connectivity.isConnected)
        assertFalse(sut.isInternetWalled)
    }
}
