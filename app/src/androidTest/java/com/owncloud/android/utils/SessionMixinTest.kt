/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.mixins.SessionMixin
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionMixinTest : AbstractIT() {

    private lateinit var userAccountManager: UserAccountManager
    private lateinit var session: SessionMixin

    private var scenario: ActivityScenario<FileDisplayActivity>? = null
    val intent = Intent(ApplicationProvider.getApplicationContext(), FileDisplayActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<FileDisplayActivity>(intent)

    @Before
    fun setUp() {
        userAccountManager = UserAccountManagerImpl.fromContext(targetContext)

        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            session = SessionMixin(
                sut,
                userAccountManager
            )
        }
    }

    @Test
    fun startAccountCreation() {
        session.startAccountCreation()

        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            assert(sut.account.name == userAccountManager.accounts.first().name)
        }
    }
}
