/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.mixins

import android.app.Activity
import android.content.ContentResolver
import com.nextcloud.client.account.UserAccountManager
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.same
import org.mockito.MockitoAnnotations

class SessionMixinTest {

    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var userAccountManager: UserAccountManager

    private lateinit var session: SessionMixin

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        session = SessionMixin(
            activity,
            contentResolver,
            userAccountManager
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
