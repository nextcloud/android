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
