/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
