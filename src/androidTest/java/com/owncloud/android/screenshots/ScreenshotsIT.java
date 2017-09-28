package com.owncloud.android.screenshots;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AnyOf.anyOf;

@RunWith(JUnit4.class)
public class ScreenshotsIT {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<FileDisplayActivity> fileDisplayRule = new ActivityTestRule<>(FileDisplayActivity.class);

    @BeforeClass
    public static void beforeAll() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @Test
    public void gridViewScreenshot() throws InterruptedException {
        Espresso.openContextualActionModeOverflowMenu();
        onView(anyOf(withText(R.string.action_switch_grid_view), withId(R.id.action_switch_view))).perform(click());

        Screengrab.screenshot("01_grid_view");

        Espresso.openContextualActionModeOverflowMenu();
        onView(anyOf(withText(R.string.action_switch_list_view), withId(R.id.action_switch_view))).perform(click());
    }

    @Test
    public void listViewScreenshot() throws InterruptedException {
        // go into work folder
        onData(anything()).inAdapterView(withId(R.id.list_root)).atPosition(0).perform(click());

        Screengrab.screenshot("02_list_view");

        Espresso.pressBack();
    }

    @Test
    public void drawerScreenshot() throws InterruptedException {

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        Screengrab.screenshot("03_drawer");

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());

    }

    @Test
    public void multipleAccountsScreenshot() throws InterruptedException {

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.drawer_active_user)).perform(click());

        Screengrab.screenshot("04_accounts");

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());
    }

    @Test
    public void autoUploadScreenshot() throws InterruptedException {

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(anyOf(withText(R.string.drawer_synced_folders), withId(R.id.nav_synced_folders))).perform(click());

        Screengrab.screenshot("05_auto_upload");

        Espresso.pressBack();
    }
}