/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import java.util.Objects;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class SyncFileNotEnoughSpaceDialogFragmentTest extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Test
    @ScreenshotTest
    public void showNotEnoughSpaceDialogForFolder() {
        FileDisplayActivity test = activityRule.launchActivity(null);
        OCFile ocFile = new OCFile("/Document/");
        ocFile.setFileLength(5000000);
        ocFile.setFolder();

        SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 1000);
        dialog.show(test.getListOfFilesFragment().getFragmentManager(), "1");

        getInstrumentation().waitForIdleSync();

        screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView());
    }

    @Test
    @ScreenshotTest
    public void showNotEnoughSpaceDialogForFile() {
        FileDisplayActivity test = activityRule.launchActivity(null);
        OCFile ocFile = new OCFile("/Video.mp4");
        ocFile.setFileLength(1000000);

        SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 2000);
        dialog.show(test.getListOfFilesFragment().getFragmentManager(), "2");

        getInstrumentation().waitForIdleSync();

        screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView());
    }
}
