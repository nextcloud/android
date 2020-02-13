package com.owncloud.android.ui.activity;

import com.owncloud.android.datamodel.OCFile;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FolderPickerActivityTest {
    @Rule
    public ActivityTestRule<FolderPickerActivity> activityRule =
        new ActivityTestRule<>(FolderPickerActivity.class);

    @Test
    public void getFile_NoDifferenceTest() {
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
    public void getParentFolder_isNotRootFolderTest() {
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
    public void getParentFolder_isRootFolderTest() {
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
}
