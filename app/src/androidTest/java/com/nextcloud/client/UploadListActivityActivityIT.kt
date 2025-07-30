/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;


public class UploadListActivityActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<UploadListActivity> activityRule = new IntentsTestRule<>(UploadListActivity.class,
                                                                                          true,
                                                                                          false);

    @Test
    @ScreenshotTest
    public void openDrawer() {
        super.openDrawer(activityRule);
    }
}
