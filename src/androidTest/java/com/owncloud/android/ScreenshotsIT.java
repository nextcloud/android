package com.owncloud.android;

import android.Manifest;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import androidx.test.core.app.ActivityScenario;
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
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(JUnit4.class)
public class ScreenshotsIT extends AbstractOnServerIT {
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
    public void gridViewScreenshot() {
        ActivityScenario.launch(FileDisplayActivity.class);

        onView(anyOf(withText(R.string.action_switch_grid_view), withId(R.id.switch_grid_view_button))).perform(click());

        shortSleep();

        Screengrab.screenshot("01_gridView");

        onView(anyOf(withText(R.string.action_switch_list_view), withId(R.id.switch_grid_view_button))).perform(click());

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void listViewScreenshot() {
        String path = "/Camera/";

        // folder does not exist yet
        if (getStorageManager().getFileByEncryptedRemotePath(path) == null) {
            SyncOperation syncOp = new CreateFolderOperation(path, user, targetContext, getStorageManager());
            RemoteOperationResult result = syncOp.execute(client);

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

        onView(withId(R.id.switch_account_button)).perform(click());

        Screengrab.screenshot("04_accounts");

        pressBack();

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void autoUploadScreenshot() {
        ActivityScenario.launch(SyncedFoldersActivity.class);

        Screengrab.screenshot("05_autoUpload");

        Assert.assertTrue(true); // if we reach this, everything is ok
    }

    @Test
    public void davdroidScreenshot() {
        ActivityScenario.launch(SettingsActivity.class);

        onData(PreferenceMatchers.withTitle(R.string.prefs_category_more)).perform(ViewActions.scrollTo());

        shortSleep();

        Screengrab.screenshot("06_davdroid");

        Assert.assertTrue(true); // if we reach this, everything is ok
    }
}
