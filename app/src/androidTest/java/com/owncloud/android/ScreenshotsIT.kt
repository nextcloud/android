package com.owncloud.android

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import org.hamcrest.core.AnyOf
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
@RunWith(JUnit4::class)
class ScreenshotsIT : AbstractOnServerIT() {
    @Test
    fun gridViewScreenshot() {
        ActivityScenario.launch(FileDisplayActivity::class.java)
        Espresso.onView(
            AnyOf.anyOf(
                ViewMatchers.withText(R.string.action_switch_grid_view),
                ViewMatchers.withId(R.id.switch_grid_view_button)
            )
        ).perform(ViewActions.click())
        shortSleep()
        Screengrab.screenshot("01_gridView")
        Espresso.onView(
            AnyOf.anyOf(
                ViewMatchers.withText(R.string.action_switch_list_view),
                ViewMatchers.withId(R.id.switch_grid_view_button)
            )
        ).perform(ViewActions.click())
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    @Test
    fun listViewScreenshot() {
        val path = "/Camera/"

        // folder does not exist yet
        if (storageManager.getFileByEncryptedRemotePath(path) == null) {
            val syncOp: SyncOperation = CreateFolderOperation(path, user, targetContext, storageManager)
            val result = syncOp.execute(client)
            Assert.assertTrue(result.isSuccess)
        }
        ActivityScenario.launch(FileDisplayActivity::class.java)

        // go into work folder
        Espresso.onView(ViewMatchers.withId(R.id.list_root))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ViewActions.click()))
        Screengrab.screenshot("02_listView")
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    @Test
    fun drawerScreenshot() {
        ActivityScenario.launch(FileDisplayActivity::class.java)
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
        Screengrab.screenshot("03_drawer")
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.close())
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    @Test
    fun multipleAccountsScreenshot() {
        ActivityScenario.launch(FileDisplayActivity::class.java)
        Espresso.onView(ViewMatchers.withId(R.id.switch_account_button)).perform(ViewActions.click())
        Screengrab.screenshot("04_accounts")
        Espresso.pressBack()
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    @Test
    fun autoUploadScreenshot() {
        ActivityScenario.launch(SyncedFoldersActivity::class.java)
        Screengrab.screenshot("05_autoUpload")
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    @Test
    fun davdroidScreenshot() {
        ActivityScenario.launch(SettingsActivity::class.java)
        Espresso.onData(PreferenceMatchers.withTitle(R.string.prefs_category_more)).perform(ViewActions.scrollTo())
        shortSleep()
        Screengrab.screenshot("06_davdroid")
        Assert.assertTrue(true) // if we reach this, everything is ok
    }

    companion object {
        @ClassRule
        val localeTestRule = LocaleTestRule()
        @BeforeClass
        fun beforeScreenshot() {
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        }
    }
}