/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Unpublished <unpublished@gmx.net>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Intent;

import com.nextcloud.client.onboarding.WhatsNewActivity;
import com.owncloud.android.AbstractIT;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import static androidx.test.runner.lifecycle.Stage.RESUMED;

public class FileDisplayActivityTest extends AbstractIT {
    private ActivityScenario<FileDisplayActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FileDisplayActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    public void testSetupToolbar() {
        scenario.onActivity(sut -> onIdleSync(() -> {
            Activity activity =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED).iterator().next();
            if (activity instanceof WhatsNewActivity) {
                activity.onBackPressed();
            }

            scenario.recreate();
        }));
    }
}
