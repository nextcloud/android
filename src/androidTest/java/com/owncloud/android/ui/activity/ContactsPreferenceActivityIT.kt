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
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsPreferenceActivityIT : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(ContactsPreferenceActivity::class.java, true, false)

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
        val sut = activityRule.launchActivity(intent)

        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun openContactsPreference() {
        val sut = activityRule.launchActivity(null)

        shortSleep()

        screenshot(sut)
    }
}
