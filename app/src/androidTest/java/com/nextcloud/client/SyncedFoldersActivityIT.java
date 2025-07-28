/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client;

import android.content.Intent;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SyncedFoldersLayoutBinding;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.adapter.SyncedFolderAdapter;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;

import androidx.appcompat.app.AlertDialog;
import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;


public class SyncedFoldersActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<SyncedFoldersActivity> activityRule = new IntentsTestRule<>(SyncedFoldersActivity.class,
                                                                                             true,
                                                                                             false);

    @Test
    @ScreenshotTest
    public void open() {
        SyncedFoldersActivity activity = activityRule.launchActivity(null);
        activity.adapter.clear();
        SyncedFoldersLayoutBinding sut = activity.binding;
        shortSleep();
        screenshot(sut.emptyList.emptyListView);
    }

    @Test
    @ScreenshotTest
    public void testSyncedFolderDialog() {
        SyncedFolderDisplayItem item = makeSyncedFolderDisplayItem(true);
        SyncedFolderPreferencesDialogFragment sut = SyncedFolderPreferencesDialogFragment.newInstance(item, 0);

        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);

        sut.show(activity.getSupportFragmentManager(), "");

        getInstrumentation().waitForIdleSync();
        shortSleep();

        screenshot(Objects.requireNonNull(sut.requireDialog().getWindow()).getDecorView());
    }
    
    @Test
    @ScreenshotTest
    public void showPowerCheckDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        
        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);

        AlertDialog sut = activity.buildPowerCheckDialog();
        
        activity.runOnUiThread(sut::show);
        
        getInstrumentation().waitForIdleSync();
        shortSleep();

        screenshot(Objects.requireNonNull(sut.getWindow()).getDecorView());
    }

    @Test
    public void showForceSyncOption() {
        SyncedFolderDisplayItem enabledItem = makeSyncedFolderDisplayItem(true);

        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);
        activity.runOnUiThread(() -> {
            activity.adapter.clear();
            activity.adapter.addSyncFolderItem(enabledItem);
        });

        getInstrumentation().waitForIdleSync();

        clickOnFolderItem(activity);

        Menu menu = activity.adapter.getPopup$app_genericDebug().getMenu();
        MenuItem forceView = menu.findItem(R.id.action_auto_upload_force_sync);
        Assert.assertTrue(forceView.isEnabled());
        Assert.assertTrue(forceView.isVisible());
    }

    @Test
    public void notShowForceSyncOptionOnDisabledItem() {
        SyncedFolderDisplayItem disabledItem = makeSyncedFolderDisplayItem(false);

        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);
        activity.runOnUiThread(() -> {
            activity.adapter.clear();
            activity.adapter.addSyncFolderItem(disabledItem);
        });

        getInstrumentation().waitForIdleSync();

        clickOnFolderItem(activity);

        Menu menu = activity.adapter.getPopup$app_genericDebug().getMenu();
        MenuItem forceView = menu.findItem(R.id.action_auto_upload_force_sync);
        Assert.assertFalse(forceView.isEnabled());
        Assert.assertFalse(forceView.isVisible());
    }

    @Test
    @ScreenshotTest
    public void showForceSyncDialog() {
        SyncedFolderDisplayItem enabledItem = makeSyncedFolderDisplayItem(true);

        Intent intent = new Intent(targetContext, SyncedFoldersActivity.class);
        SyncedFoldersActivity activity = activityRule.launchActivity(intent);
        activity.runOnUiThread(() -> {
            activity.adapter.clear();
            activity.adapter.addSyncFolderItem(enabledItem);
        });

        getInstrumentation().waitForIdleSync();

        clickOnFolderItem(activity);

        Menu menu = activity.adapter.getPopup$app_genericDebug().getMenu();
        MenuItem forceView = menu.findItem(R.id.action_auto_upload_force_sync);

        activity.runOnUiThread(() -> {
            // I don't really see a nicer way to trigger this through the MenuItem
            // there's no way to simulate a click on it and the interface does not expose its invoke function
            activity.adapter.optionsItemSelected$app_genericDebug(forceView, 0, enabledItem);
        });

        getInstrumentation().waitForIdleSync();

        screenshot(activity.getWindow().getDecorView());
    }

    private void clickOnFolderItem(SyncedFoldersActivity activity) {
        activity.runOnUiThread(() -> {
            SyncedFolderAdapter.HeaderViewHolder holder =
                (SyncedFolderAdapter.HeaderViewHolder)activity.binding.list.findViewHolderForAdapterPosition(0);
            holder
                .getBinding()
                .settingsButton
                .performClick();
        });
        getInstrumentation().waitForIdleSync();
    }

    private SyncedFolderDisplayItem makeSyncedFolderDisplayItem(boolean enabled) {
        return new SyncedFolderDisplayItem(1,
                                           "/sdcard/DCIM/",
                                           "/InstantUpload/",
                                           true,
                                           false,
                                           false,
                                           true,
                                           "test@https://nextcloud.localhost",
                                           0,
                                           0,
                                           enabled,
                                           1000,
                                           "Name",
                                           MediaFolderType.IMAGE,
                                           false,
                                           SubFolderRule.YEAR_MONTH,
                                           false,
                                           SyncedFolder.NOT_SCANNED_YET);
    }
}
