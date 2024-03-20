/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client;

import android.content.Intent;

import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.databinding.SyncedFoldersLayoutBinding;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;


public class SyncedFoldersActivityIT extends AbstractIT {
    private ActivityScenario<SyncedFoldersActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SyncedFoldersActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    @ScreenshotTest
    public void open() {
        scenario.onActivity(activity -> {
            activity.adapter.clear();
            SyncedFoldersLayoutBinding sut = activity.binding;
            onIdleSync(() -> {
                shortSleep();
                screenshot(sut.emptyList.emptyListView);
            });
        });
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
                                                                   false,
                                                                   SubFolderRule.YEAR_MONTH,
                                                                   false);
        SyncedFolderPreferencesDialogFragment sut = SyncedFolderPreferencesDialogFragment.newInstance(item, 0);

        scenario.onActivity(activity -> {
            sut.show(activity.getSupportFragmentManager(), "");

            onIdleSync(() -> {
                shortSleep();
                screenshot(Objects.requireNonNull(sut.requireDialog().getWindow()).getDecorView());
            });
        });
    }
}
