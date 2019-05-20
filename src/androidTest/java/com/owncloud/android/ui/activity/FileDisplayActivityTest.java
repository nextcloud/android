package com.owncloud.android.ui.activity;

import com.owncloud.android.AbstractIT;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.rule.GrantPermissionRule;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class FileDisplayActivityTest extends AbstractIT {

    @Rule public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(WRITE_EXTERNAL_STORAGE);

    @Test
    public void testSetupToolbar() {
        try (ActivityScenario<FileDisplayActivity> scenario = ActivityScenario.launch(FileDisplayActivity.class)) {
            scenario.recreate();
        }
    }
}
