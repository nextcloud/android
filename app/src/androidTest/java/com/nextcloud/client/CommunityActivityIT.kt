/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client;

import android.app.Activity;

import com.nextcloud.test.GrantStoragePermissionRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.ui.activity.CommunityActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import androidx.test.espresso.intent.rule.IntentsTestRule;


public class CommunityActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<CommunityActivity> activityRule = new IntentsTestRule<>(CommunityActivity.class,
                                                                                         true,
                                                                                         false);

    @Rule
    public final TestRule permissionRule = GrantStoragePermissionRule.grant();

    @Test
    @ScreenshotTest
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        screenshot(sut);
    }
}
