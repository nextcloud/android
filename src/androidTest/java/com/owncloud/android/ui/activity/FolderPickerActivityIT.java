package com.owncloud.android.ui.activity;

/*
 * Nextcloud Android client application
 *
 * @author Kilian Périsset
 * Copyright (C) 2019 Kilian Périsset (Infomaniak Network SA)
 * Copyright (C) 2019 Nextcloud GmbH
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

import com.owncloud.android.AbstractIT;
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

        screenshot(sut);
    }
}
