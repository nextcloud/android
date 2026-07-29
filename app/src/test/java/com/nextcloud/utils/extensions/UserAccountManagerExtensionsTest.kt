/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class UserAccountManagerExtensionsTest {

    private val accountName = "test@server.com"

    private lateinit var accountManager: UserAccountManager

    @Before
    fun setUp() {
        accountManager = mock()
        whenever(accountManager.context).thenReturn(mock<Context>())
    }

    @Test
    fun `no client is created for an unknown account`() {
        whenever(accountManager.getUser(accountName)).thenReturn(Optional.empty())

        assertNull(accountManager.createOwncloudClient(accountName))
    }

    @Test
    fun `no client is created while the account has no base url yet`() {
        val anonymousUser = mock<User>()
        whenever(anonymousUser.isAnonymous).thenReturn(true)
        whenever(accountManager.getUser(accountName)).thenReturn(Optional.of(anonymousUser))

        assertNull(accountManager.createOwncloudClient(accountName))
    }
}
