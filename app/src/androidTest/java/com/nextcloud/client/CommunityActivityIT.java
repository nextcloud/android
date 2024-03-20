/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client;

import android.content.Intent;

import com.nextcloud.test.GrantStoragePermissionRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.ui.activity.CommunityActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

public class CommunityActivityIT extends AbstractIT {
    private ActivityScenario<CommunityActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CommunityActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Rule
    public final TestRule permissionRule = GrantStoragePermissionRule.grant();

    @Test
    @ScreenshotTest
    public void open() {
        scenario.onActivity(sut -> onIdleSync(() -> screenshot(sut)));
    }
}
