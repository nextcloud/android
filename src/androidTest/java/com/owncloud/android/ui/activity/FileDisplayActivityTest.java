package com.owncloud.android.ui.activity;

import android.app.Activity;

import com.nextcloud.client.onboarding.WhatsNewActivity;
import com.owncloud.android.AbstractIT;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static androidx.test.runner.lifecycle.Stage.RESUMED;

public class FileDisplayActivityTest extends AbstractIT {

    @Rule public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(WRITE_EXTERNAL_STORAGE);

    @Test
    public void testSetupToolbar() {
        try (ActivityScenario<FileDisplayActivity> scenario = ActivityScenario.launch(FileDisplayActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Activity activity =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED).iterator().next();
                if (activity instanceof WhatsNewActivity) {
                    activity.onBackPressed();
                }
            });
            scenario.recreate();
        }
    }
}
