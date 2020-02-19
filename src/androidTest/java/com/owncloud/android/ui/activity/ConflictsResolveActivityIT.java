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

package com.owncloud.android.ui.activity;

import android.content.Intent;

import com.facebook.testing.screenshot.Screenshot;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ConflictsResolveActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<ConflictsResolveActivity> activityRule =
        new IntentsTestRule<>(ConflictsResolveActivity.class, true, false);
    private boolean returnCode;

    @Test
    public void screenshotTextFiles() throws InterruptedException {
        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          account.name);

        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        FileDataStorageManager storageManager = new FileDataStorageManager(account, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {

        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        Thread.sleep(2000);

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();
    }

    @Test
    public void screenshotImages() throws InterruptedException, IOException {
        FileDataStorageManager storageManager = new FileDataStorageManager(account,
                                                                           targetContext.getContentResolver());

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt", account.name);

        File image = getFile("image.jpg");

        assertTrue(new UploadFileRemoteOperation(image.getAbsolutePath(),
                                                 "/image.jpg",
                                                 "image/jpg",
                                                 "10000000").execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(storageManager.getFileByPath("/"),
                                              System.currentTimeMillis(),
                                              false,
                                              true,
                                              storageManager,
                                              account,
                                              targetContext
        ).execute(client).isSuccess());

        OCFile existingFile = storageManager.getFileByPath("/image.jpg");

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {

        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        Thread.sleep(10000);

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();
    }

    @Test
    public void cancel() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          account.name);
        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        FileDataStorageManager storageManager = new FileDataStorageManager(account, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.CANCEL);
            returnCode = true;
        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        onView(withText("Cancel")).perform(click());

        assertTrue(returnCode);
    }

    @Test
    public void keepExisting() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          account.name);
        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        FileDataStorageManager storageManager = new FileDataStorageManager(account, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_SERVER);
            returnCode = true;
        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.existing_checkbox)).perform(click());

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();

        onView(withText("OK")).perform(click());

        assertTrue(returnCode);
    }

    @Test
    public void keepNew() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          account.name);
        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        FileDataStorageManager storageManager = new FileDataStorageManager(account, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_SERVER);
            returnCode = true;
        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.new_checkbox)).perform(click());

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();

        onView(withText("OK")).perform(click());

        assertTrue(returnCode);
    }

    @Test
    public void keepBoth() {
        returnCode = false;

        OCUpload newUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                          "/newFile.txt",
                                          account.name);
        OCFile existingFile = new OCFile("/newFile.txt");
        existingFile.setFileLength(1024000);
        existingFile.setModificationTimestamp(1582019340);

        FileDataStorageManager storageManager = new FileDataStorageManager(account, targetContext.getContentResolver());
        storageManager.saveNewFile(existingFile);

        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, existingFile);
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD, newUpload);

        ConflictsResolveActivity sut = activityRule.launchActivity(intent);

        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {
            assertEquals(decision, ConflictsResolveDialog.Decision.KEEP_SERVER);
            returnCode = true;
        };

        ConflictsResolveDialog dialog = new ConflictsResolveDialog(listener,
                                                                   existingFile,
                                                                   newUpload,
                                                                   Optional.of(UserAccountManagerImpl
                                                                                   .fromContext(targetContext)
                                                                                   .getUser()
                                                                   ));
        dialog.showDialog(sut);

        getInstrumentation().waitForIdleSync();

        onView(withId(R.id.existing_checkbox)).perform(click());
        onView(withId(R.id.new_checkbox)).perform(click());

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();

        onView(withText("OK")).perform(click());

        assertTrue(returnCode);
    }
}
