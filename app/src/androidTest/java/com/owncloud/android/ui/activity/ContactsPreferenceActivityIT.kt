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
            onIdleSync {
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun openContactsPreference() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            onIdleSync {
                shortSleep()
                screenshot(sut)
            }
        }
    }
}
