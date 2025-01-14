/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.integrations.deck

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.User
import com.owncloud.android.lib.resources.notifications.models.Notification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
            fun initParametrs(): Array<String> = DeckApiImpl.DECK_APP_PACKAGES
        }

        @Before
        fun setUp() {
            whenever(packageManager.resolveActivity(any(), anyInt())).thenAnswer {
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
            verify(packageManager, never()).resolveActivity(anyOrNull(), anyOrNull<Int>())
            assertFalse(openDeckActionIntent.isPresent)
        }
    }

    class DeckIsNotInstalled : Fixture() {

        @Before
        fun setUp() {
            whenever(packageManager.resolveActivity(any(), anyInt())).thenReturn(null)
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
            verify(packageManager, times(DeckApiImpl.DECK_APP_PACKAGES.size))
                .resolveActivity(anyOrNull(), anyOrNull<Int>())
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
            verify(packageManager, never()).resolveActivity(anyOrNull(), anyOrNull<Int>())
            assertFalse(openDeckActionIntent.isPresent)
        }
    }
}
