/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog;

import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

public class SyncFileNotEnoughSpaceDialogFragmentTest extends AbstractIT {
    private ActivityScenario<FileDisplayActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FileDisplayActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    @ScreenshotTest
    public void showNotEnoughSpaceDialogForFolder() {
        scenario.onActivity(test -> {
            OCFile ocFile = new OCFile("/Document/");
            ocFile.setFileLength(5000000);
            ocFile.setFolder();

            SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 1000);
            dialog.show(test.getListOfFilesFragment().getFragmentManager(), "1");

            onIdleSync(() -> screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView()));
        });
    }

    @Test
    @ScreenshotTest
    public void showNotEnoughSpaceDialogForFile() {
        scenario.onActivity(test -> {
            OCFile ocFile = new OCFile("/Video.mp4");
            ocFile.setFileLength(1000000);

            SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 2000);
            dialog.show(test.getListOfFilesFragment().getFragmentManager(), "2");

           onIdleSync(() -> screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView()));
        });
    }
}
