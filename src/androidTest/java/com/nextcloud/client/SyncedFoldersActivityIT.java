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
import android.app.Activity;
import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;


public class SyncedFoldersActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<SyncedFoldersActivity> activityRule = new IntentsTestRule<>(SyncedFoldersActivity.class,
                                                                                             true,
                                                                                             false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotTest
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        screenshot(sut);
    }

    @Test
    @ScreenshotTest
    public void testSyncedFolderDialog() {
        SyncedFolderDisplayItem item = new SyncedFolderDisplayItem(1,
                                                                   "/sdcard/DCIM/",
                                                                   "/InstantUpload/",
                                                                   true,
                                                                   false,
                                                                   false,
                                                                   true,
                                                                   "test@https://nextcloud.localhost",
                                                                   0,
                                                                   0,
                                                                   true,
                                                                   1000,
                                                                   "Name",
                                                                   MediaFolderType.IMAGE,
                                                                   false);
        SyncedFolderPreferencesDialogFragment sut = SyncedFolderPreferencesDialogFragment.newInstance(item, 0);

        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);

        sut.show(activity.getSupportFragmentManager(), "");

        getInstrumentation().waitForIdleSync();
        shortSleep();

        screenshot(Objects.requireNonNull(sut.requireDialog().getWindow()).getDecorView());
    }
}
