/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview;

import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

public class PreviewTextFileFragmentTest extends AbstractIT {
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
    public void displaySimpleTextFile() {
        scenario.onActivity(sut -> {
            shortSleep();

            File file;
            try {
                file = getDummyFile("nonEmpty.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            OCFile test = new OCFile("/text.md");
            test.setMimeType(MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN);
            test.setStoragePath(file.getAbsolutePath());
            sut.startTextPreview(test, false);

            onIdleSync(() -> {
                shortSleep();
                screenshot(sut);
            });
        });
    }

    @Test
    @ScreenshotTest
    public void displayJavaSnippetFile() {
        scenario.onActivity(sut -> {

            shortSleep();

            File file = null;
            try {
                file = getFile("java.md");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            OCFile test = new OCFile("/java.md");
            test.setMimeType(MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN);
            test.setStoragePath(file.getAbsolutePath());
            sut.startTextPreview(test, false);

            onIdleSync(() -> {
                shortSleep();
                screenshot(sut);
            });
        });
    }
}
