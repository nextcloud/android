/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client;

import android.Manifest;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.utils.ScreenshotWithServerTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;


public class SyncedFoldersActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<SyncedFoldersActivity> activityRule = new IntentsTestRule<>(SyncedFoldersActivity.class,
                                                                                             true,
                                                                                             false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotWithServerTest
    @ScreenshotTest
    public void openDrawer() {
        super.openDrawer(activityRule);
    }
}
