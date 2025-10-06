/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Unpublished <unpublished@gmx.net>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.app.Activity;

import com.nextcloud.client.onboarding.WhatsNewActivity;
import com.owncloud.android.AbstractIT;

import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import static androidx.test.runner.lifecycle.Stage.RESUMED;

public class FileDisplayActivityTest extends AbstractIT {
    @Test
    public void testSetupToolbar() {
        try (ActivityScenario<FileDisplayActivity> scenario = ActivityScenario.launch(FileDisplayActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Activity activity =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED).iterator().next();
                if (activity instanceof WhatsNewActivity whatsNewActivity) {
                    whatsNewActivity.getOnBackPressedDispatcher().onBackPressed();
                }
            });
            scenario.recreate();
        }
    }
}
