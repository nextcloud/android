package com.owncloud.android.ui.dialog;

import com.nextcloud.client.TestActivity;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class DurationPickerDialogFragmentIT extends AbstractIT {
    @Rule public IntentsTestRule<TestActivity> testActivityRule = new IntentsTestRule<>(TestActivity.class,
                                                                                               true, false);

    @Test
    public void showSyncDelayDurationDialog() {
        TestActivity test = testActivityRule.launchActivity(null);
        FragmentManager fm = test.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        DurationPickerDialogFragment dialog = DurationPickerDialogFragment.newInstance(TimeUnit.HOURS.toMillis(5),
                                                                                       "Dialog title", "Hint message");
        dialog.show(ft, "DURATION_DIALOG");

        getInstrumentation().waitForIdleSync();

        screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView());
    }

}
