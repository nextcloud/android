/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.nextcloud.ui.ImageDetailFragment
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.activities.models.PreviewObject
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test
import java.io.File
import java.util.GregorianCalendar

class FileDetailFragmentStaticServerIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.FileDetailFragmentStaticServerIT"

    private var file: File = getFile("gps.jpg")
    private val oCFile: OCFile = OCFile("/").apply {
        storagePath = file.absolutePath
        fileId = 12
        fileDataStorageManager.saveFile(this)
    }

    @Test
    @ScreenshotTest
    fun showFileDetailActivitiesFragment() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                sut.addFragment(FileDetailActivitiesFragment.newInstance(oCFile, user))
                sut.supportFragmentManager.executePendingTransactions()
            }

            val screenShotName = createName(testClassName + "_" + "showFileDetailActivitiesFragment", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showFileDetailSharingFragment() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                sut.addFragment(FileDetailSharingFragment.newInstance(oCFile, user))
                sut.supportFragmentManager.executePendingTransactions()
            }

            val screenShotName = createName(testClassName + "_" + "showFileDetailSharingFragment", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showFileDetailDetailsFragment() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = ImageDetailFragment.newInstance(oCFile, user).apply {
                    hideMap()
                }
                sut.addFragment(fragment)
                sut.supportFragmentManager.executePendingTransactions()
            }

            val screenShotName = createName(testClassName + "_" + "showFileDetailDetailsFragment", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showDetailsActivities() {
        val date = GregorianCalendar().apply {
            set(2005, 4, 17, 10, 35, 30)
        }

        val richObjectList = ArrayList<RichObject>().apply {
            add(RichObject("file", "abc", "text.txt", "/text.txt", "link", "tag"))
            add(RichObject("file", "1", "text.txt", "/text.txt", "link", "tag"))
        }

        val previewObjectList1 = ArrayList<PreviewObject>().apply {
            add(PreviewObject(1, "source", "link", true, "text/plain", "view", "text.txt"))
        }

        val activities = mutableListOf(
            Activity(
                1,
                date.time,
                date.time,
                "files",
                "file_changed",
                "user1",
                "user1",
                "You changed text.txt",
                "",
                "icon",
                "link",
                "files",
                "1",
                "/text.txt",
                previewObjectList1,
                RichElement("", richObjectList)
            ),
            Activity(
                2,
                date.time,
                date.time,
                "comments",
                "comments",
                "user1",
                "user1",
                "admin commented",
                "test2",
                "icon",
                "link",
                "files",
                "1",
                "/text.txt",
                emptyList(),
                RichElement()
            )
        )

        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = FileDetailFragment.newInstance(oCFile, user, 0)
                sut.addFragment(fragment)
                sut.supportFragmentManager.executePendingTransactions()
                fragment.fileDetailActivitiesFragment.populateList(activities as List<Any>?, true)
            }

            val screenShotName = createName(testClassName + "_" + "showDetailsActivities", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesNone() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = FileDetailFragment.newInstance(oCFile, user, 0)
                sut.addFragment(fragment)
                activity.supportFragmentManager.executePendingTransactions()
                fragment.fileDetailActivitiesFragment.populateList(emptyList(), true)
            }

            val screenShotName = createName(testClassName + "_" + "showDetailsActivitiesNone", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showDetailsActivitiesError() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = FileDetailFragment.newInstance(oCFile, user, 0)
                sut.addFragment(fragment)
                sut.supportFragmentManager.executePendingTransactions()
                fragment.fileDetailActivitiesFragment.disableLoadingActivities()
                fragment.fileDetailActivitiesFragment.setErrorContent(
                    targetContext.resources.getString(R.string.file_detail_activity_error)
                )
            }

            val screenShotName = createName(testClassName + "_" + "showDetailsActivitiesError", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showDetailsSharing() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = FileDetailFragment.newInstance(oCFile, user, 1)
                sut.addFragment(fragment)
                sut.supportFragmentManager.executePendingTransactions()
            }

            val screenShotName = createName(testClassName + "_" + "showDetailsSharing", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }
}
