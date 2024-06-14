/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.mixins

import android.app.Activity
import com.nextcloud.client.account.UserAccountManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.same
import org.mockito.Mockito.spy
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class SessionMixinTest {

    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var userAccountManager: UserAccountManager

    private lateinit var session: SessionMixin

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        session = spy(
            SessionMixin(
                activity,
                userAccountManager
            )
        )
    }

    @Test
    fun `start account creation`() {
        // WHEN
        //      start account creation flow
        session.startAccountCreation()

        // THEN
        //      start is delegated to account manager
        //      account manager receives parent activity
        verify(userAccountManager).startAccountCreation(same(activity))
    }
}
