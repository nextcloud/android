package com.nmc.android.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test

class SyncedFoldersActivityIT : AbstractIT() {

    @get:Rule
    val activityRule = ActivityScenarioRule(SyncedFoldersActivity::class.java)

    @Test
    fun syncedFolderDialogWithCameraMediaPath() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()

        onView(withId(android.R.id.list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, object : ViewAction {

                override fun getDescription(): String {
                    return "Click on specific button"
                }

                override fun getConstraints(): Matcher<View>? {
                    return null
                }

                override fun perform(uiController: UiController, view: View) {
                    val button = view.findViewById<View>(R.id.settingsButton)
                    button.performClick()
                }
            }))

        onView(withText("Configure")).perform(click())

        onView(withId(R.id.remote_folder_summary)).check(matches(withText(startsWith(AUTO_UPLOAD_PATH))))
    }

    companion object {
        const val AUTO_UPLOAD_PATH = "/Camera-Media/"
    }
}