/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsPreferenceActivityIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<ContactsPreferenceActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), ContactsPreferenceActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<ContactsPreferenceActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun openVCF() {
        val file = getFile("vcard.vcf")
        val vcfFile = OCFile("/contacts.vcf")
        vcfFile.storagePath = file.absolutePath

        assertTrue(vcfFile.isDown)

        val intent = Intent()
        intent.putExtra(ContactsPreferenceActivity.EXTRA_FILE, vcfFile)
        intent.putExtra(ContactsPreferenceActivity.EXTRA_USER, user)

        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            shortSleep()
            screenshot(sut)
        }
    }

    @Test
    @ScreenshotTest
    fun openContactsPreference() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            shortSleep()
            screenshot(sut)
        }
    }
}
