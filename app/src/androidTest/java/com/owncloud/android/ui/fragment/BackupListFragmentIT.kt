/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupListFragmentIT : AbstractIT() {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR)

    private val testClassName = "com.owncloud.android.ui.fragment.BackupListFragmentIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @ScreenshotTest
    fun showLoading() {
        launchActivity<ContactsPreferenceActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val file = OCFile("/")
                val transaction = sut.supportFragmentManager.beginTransaction()

                onIdleSync {
                    EspressoIdlingResource.increment()

                    transaction.replace(R.id.frame_container, BackupListFragment.newInstance(file, user))
                    transaction.commit()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showLoading", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showContactList() {
        launchActivity<ContactsPreferenceActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val transaction = sut.supportFragmentManager.beginTransaction()
                val file = getFile("vcard.vcf")
                val ocFile = OCFile("/vcard.vcf").apply {
                    storagePath = file.absolutePath
                    mimeType = "text/vcard"
                }

                onIdleSync {
                    EspressoIdlingResource.increment()

                    transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
                    transaction.commit()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showContactList", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showCalendarList() {
        launchActivity<ContactsPreferenceActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val transaction = sut.supportFragmentManager.beginTransaction()
                val file = getFile("calendar.ics")
                val ocFile = OCFile("/Private calender_2020-09-01_10-45-20.ics.ics").apply {
                    storagePath = file.absolutePath
                    mimeType = "text/calendar"
                }

                onIdleSync {
                    EspressoIdlingResource.increment()

                    transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
                    transaction.commit()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showCalendarList", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showCalendarAndContactsList() {
        launchActivity<ContactsPreferenceActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val transaction = sut.supportFragmentManager.beginTransaction()
                val calendarFile = getFile("calendar.ics")
                val calendarOcFile = OCFile("/Private calender_2020-09-01_10-45-20.ics.ics").apply {
                    storagePath = calendarFile.absolutePath
                    mimeType = "text/calendar"
                }

                val contactFile = getFile("vcard.vcf")
                val contactOcFile = OCFile("/vcard.vcf").apply {
                    storagePath = contactFile.absolutePath
                    mimeType = "text/vcard"
                }

                val files = arrayOf(calendarOcFile, contactOcFile)

                onIdleSync {
                    EspressoIdlingResource.increment()

                    transaction.replace(R.id.frame_container, BackupListFragment.newInstance(files, user))
                    transaction.commit()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showCalendarAndContactsList", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
