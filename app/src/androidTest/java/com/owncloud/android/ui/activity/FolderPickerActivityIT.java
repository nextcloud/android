/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Kilian Périsset <kilian.perisset@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

@RunWith(AndroidJUnit4.class)
//@LargeTest
public class FolderPickerActivityIT extends AbstractIT {
    @Rule
    public ActivityTestRule<FolderPickerActivity> activityRule =
        new ActivityTestRule<>(FolderPickerActivity.class);

    @Test
    public void getActivityFile() {
        // Arrange
        FolderPickerActivity targetActivity = activityRule.getActivity();
        OCFile origin = new OCFile("/test/file.test");
        origin.setRemotePath("/remotePath/test");

        // Act
        targetActivity.setFile(origin);
        OCFile target = targetActivity.getFile();

        // Assert
        Assert.assertEquals(origin, target);
    }

    @Test
    public void getParentFolder_isNotRootFolder() {
        // Arrange
        FolderPickerActivity targetActivity = activityRule.getActivity();
        OCFile origin = new OCFile("/test/");
        origin.setFileId(1);
        origin.setRemotePath("/test/");
        origin.setStoragePath("/test/");
        origin.setFolder();

        // Act
        targetActivity.setFile(origin);
        OCFile target = targetActivity.getCurrentFolder();

        // Assert
        Assert.assertEquals(origin, target);
    }

    @Test
    public void getParentFolder_isRootFolder() {
        // Arrange
        FolderPickerActivity targetActivity = activityRule.getActivity();
        OCFile origin = new OCFile("/");
        origin.setFileId(1);
        origin.setRemotePath("/");
        origin.setStoragePath("/");
        origin.setFolder();

        // Act
        targetActivity.setFile(origin);
        OCFile target = targetActivity.getCurrentFolder();

        // Assert
        Assert.assertEquals(origin, target);
    }

    @Test
    public void nullFile() {
        // Arrange
        FolderPickerActivity targetActivity = activityRule.getActivity();
        OCFile rootFolder = targetActivity.getStorageManager().getFileByPath(OCFile.ROOT_PATH);

        // Act
        targetActivity.setFile(null);
        OCFile target = targetActivity.getCurrentFolder();

        // Assert
        Assert.assertEquals(rootFolder, target);
    }

    @Test
    public void getParentFolder() {
        // Arrange
        FolderPickerActivity targetActivity = activityRule.getActivity();
        OCFile origin = new OCFile("/test/file.test");
        origin.setRemotePath("/test/file.test");

        OCFile target = new OCFile("/test/");

        // Act
        targetActivity.setFile(origin);

        // Assert
        Assert.assertEquals(origin, target);
    }

    @Test
    @ScreenshotTest
    public void open() {
        FolderPickerActivity sut = activityRule.getActivity();
        OCFile origin = new OCFile("/test/file.txt");
        sut.setFile(origin);

        sut.runOnUiThread(() -> {
            sut.findViewById(R.id.folder_picker_btn_copy).requestFocus();
        });
        waitForIdleSync();
        screenshot(sut);
    }

    @Test
    @ScreenshotTest
    public void testMoveOrCopy() {
        Intent intent = new Intent();
        FolderPickerActivity targetActivity = activityRule.launchActivity(intent);

        waitForIdleSync();
        screenshot(targetActivity);
    }

    @Test
    @ScreenshotTest
    public void testChooseLocationAction() {
        Intent intent = new Intent();
        intent.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION);
        FolderPickerActivity targetActivity = activityRule.launchActivity(intent);

        waitForIdleSync();
        screenshot(targetActivity);
    }
}
