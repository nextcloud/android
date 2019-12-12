package com.owncloud.android;

import android.Manifest;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.SettingsActivity;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.PreferenceMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(JUnit4.class)
public class ScreenshotsIT extends AbstractIT {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @BeforeClass
    public static void beforeScreenshot() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @Test
    public void gridViewScreenshot() throws InterruptedException {
        ActivityScenario.launch(FileDisplayActivity.class);

        openOverflowMenu();
        onView(anyOf(withText(R.string.action_switch_grid_view), withId(R.id.action_switch_view))).perform(click());

        Thread.sleep(1000);

        Screengrab.screenshot("01_gridView");

        openOverflowMenu();
        onView(anyOf(withText(R.string.action_switch_list_view), withId(R.id.action_switch_view))).perform(click());

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    private void openOverflowMenu() throws InterruptedException {
        try {
            Espresso.openContextualActionModeOverflowMenu();
        } catch (NoActivityResumedException e) {
            ActivityScenario.launch(FileDisplayActivity.class);
            Thread.sleep(1000);
            Espresso.openContextualActionModeOverflowMenu();
        }
    }

    @Test
    public void listViewScreenshot() {
        String path = "/Camera/";

        // folder does not exist yet
        if (getStorageManager().getFileByPath(path) == null) {
            SyncOperation syncOp = new CreateFolderOperation(path, true);
            RemoteOperationResult result = syncOp.execute(client, getStorageManager());

            assertTrue(result.isSuccess());
        }

        ActivityScenario.launch(FileDisplayActivity.class);

        // go into work folder
        onView(withId(R.id.list_root)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        Screengrab.screenshot("02_listView");

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void drawerScreenshot() {
        ActivityScenario.launch(FileDisplayActivity.class);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        Screengrab.screenshot("03_drawer");

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void multipleAccountsScreenshot() {
        ActivityScenario.launch(FileDisplayActivity.class);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.drawer_active_user)).perform(click());

        Screengrab.screenshot("04_accounts");

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void autoUploadScreenshot() {
        ActivityScenario.launch(FileDisplayActivity.class);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(swipeUp());
        onView(anyOf(withText(R.string.drawer_synced_folders), withId(R.id.nav_synced_folders))).perform(click());

        Screengrab.screenshot("05_autoUpload");

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void davdroidScreenshot() throws InterruptedException {
        ActivityScenario.launch(SettingsActivity.class);

        onData(PreferenceMatchers.withTitle(R.string.prefs_category_more)).perform(ViewActions.scrollTo());

        Thread.sleep(1000);

        Screengrab.screenshot("06_davdroid");

        Assert.assertTrue(true); // if we reach this, everything is ok
    }
}
