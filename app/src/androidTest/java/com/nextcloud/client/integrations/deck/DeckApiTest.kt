/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.integrations.deck

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.User
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.lib.resources.notifications.models.Notification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
    DeckApiTest.DeckIsInstalled::class,
    DeckApiTest.DeckIsNotInstalled::class
)
class DeckApiTest {

    abstract class Fixture {
        @Mock
        lateinit var packageManager: PackageManager

        lateinit var context: Context

        @Mock
        lateinit var user: User

        lateinit var deck: DeckApiImpl

        @Before
        fun setUpFixture() {
            MockitoAnnotations.initMocks(this)
            context = InstrumentationRegistry.getInstrumentation().targetContext
            deck = DeckApiImpl(context, packageManager)
        }
    }

    @RunWith(Parameterized::class)
    class DeckIsInstalled : Fixture() {

        @Parameterized.Parameter(0)
        lateinit var installedDeckPackage: String

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun initParametrs(): Array<String> {
                return DeckApiImpl.DECK_APP_PACKAGES
            }
        }

        @Before
        fun setUp() {
            whenever(packageManager.resolveActivity(any(), any())).thenAnswer {
                val intent = it.getArgument<Intent>(0)
                return@thenAnswer if (intent.component?.packageName == installedDeckPackage) {
                    ResolveInfo()
                } else {
                    null
                }
            }
        }

        @Test
        fun can_forward_deck_notification() {
            // GIVEN
            //      notification to deck arrives
            val notification = Notification().apply { app = "deck" }

            // WHEN
            //      deck action is created
            val forwardActionIntent = deck.createForwardToDeckActionIntent(notification, user)

            // THEN
            //      open action is created
            assertTrue("Failed for $installedDeckPackage", forwardActionIntent.isPresent)
        }

        @Test
        fun notifications_from_other_apps_are_ignored() {
            // GIVEN
            //      notification from other app arrives
            val deckNotification = Notification().apply {
                app = "some_other_app"
            }

            // WHEN
            //      deck action is created
            val openDeckActionIntent = deck.createForwardToDeckActionIntent(deckNotification, user)

            // THEN
            //      deck application is not being resolved
            //      open action is not created
            verify(packageManager, never()).resolveActivity(anyOrNull(), anyOrNull())
            assertFalse(openDeckActionIntent.isPresent)
        }
    }

    class DeckIsNotInstalled : Fixture() {

        @Before
        fun setUp() {
            whenever(packageManager.resolveActivity(any(), any())).thenReturn(null)
        }

        @Test
        fun cannot_forward_deck_notification() {
            // GIVEN
            //      notification is coming from deck app
            val notification = Notification().apply {
                app = DeckApiImpl.APP_NAME
            }

            // WHEN
            //      creating open in deck action
            val openDeckActionIntent = deck.createForwardToDeckActionIntent(notification, user)

            // THEN
            //      deck application is being resolved using all known packages
            //      open action is not created
            verify(packageManager, times(DeckApiImpl.DECK_APP_PACKAGES.size)).resolveActivity(anyOrNull(), anyOrNull())
            assertFalse(openDeckActionIntent.isPresent)
        }

        @Test
        fun notifications_from_other_apps_are_ignored() {
            // GIVEN
            //      notification is coming from other app
            val notification = Notification().apply {
                app = "some_other_app"
            }

            // WHEN
            //      creating open in deck action
            val openDeckActionIntent = deck.createForwardToDeckActionIntent(notification, user)

            // THEN
            //      deck application is not being resolved
            //      open action is not created
            verify(packageManager, never()).resolveActivity(anyOrNull(), anyOrNull())
            assertFalse(openDeckActionIntent.isPresent)
        }
    }
}
