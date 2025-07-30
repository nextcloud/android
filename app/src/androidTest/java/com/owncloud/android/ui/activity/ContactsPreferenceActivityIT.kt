/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactsPreferenceActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.ContactsPreferenceActivityIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun openVCF() {
        val file = getFile("vcard.vcf")
        val vcfFile = OCFile("/contacts.vcf")
        vcfFile.storagePath = file.absolutePath

        assertTrue(vcfFile.isDown)

        val intent = Intent(targetContext, ContactsPreferenceActivity::class.java).apply {
            putExtra(ContactsPreferenceActivity.EXTRA_FILE, vcfFile)
            putExtra(ContactsPreferenceActivity.EXTRA_USER, user)
        }

        launchActivity<ContactsPreferenceActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "openVCF", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun openContactsPreference() {
        launchActivity<ContactsPreferenceActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "openContactsPreference", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
