/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class BackupListFragmentIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<ContactsPreferenceActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), ContactsPreferenceActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<ContactsPreferenceActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR)

    @Test
    @ScreenshotTest
    fun showLoading() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val file = OCFile("/")
            val transaction = sut.supportFragmentManager.beginTransaction()

            transaction.replace(R.id.frame_container, BackupListFragment.newInstance(file, user))
            transaction.commit()

            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showContactList() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val transaction = sut.supportFragmentManager.beginTransaction()
            val file = getFile("vcard.vcf")
            val ocFile = OCFile("/vcard.vcf")
            ocFile.storagePath = file.absolutePath
            ocFile.mimeType = "text/vcard"

            transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
            transaction.commit()

            onIdleSync {
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showCalendarList() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val transaction = sut.supportFragmentManager.beginTransaction()
            val file = getFile("calendar.ics")
            val ocFile = OCFile("/Private calender_2020-09-01_10-45-20.ics.ics")
            ocFile.storagePath = file.absolutePath
            ocFile.mimeType = "text/calendar"

            transaction.replace(R.id.frame_container, BackupListFragment.newInstance(ocFile, user))
            transaction.commit()

            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showCalendarAndContactsList() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val transaction = sut.supportFragmentManager.beginTransaction()

            val calendarFile = getFile("calendar.ics")
            val calendarOcFile = OCFile("/Private calender_2020-09-01_10-45-20.ics")
            calendarOcFile.storagePath = calendarFile.absolutePath
            calendarOcFile.mimeType = "text/calendar"

            val contactFile = getFile("vcard.vcf")
            val contactOcFile = OCFile("/vcard.vcf")
            contactOcFile.storagePath = contactFile.absolutePath
            contactOcFile.mimeType = "text/vcard"

            val files = arrayOf(calendarOcFile, contactOcFile)
            transaction.replace(R.id.frame_container, BackupListFragment.newInstance(files, user))
            transaction.commit()

            onIdleSync {
                screenshot(sut)
            }
        }
    }
}
