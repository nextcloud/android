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
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.fragment.app.DialogFragment
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.ScreenshotTest
import junit.framework.TestCase
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.Objects

class ConflictsResolveActivityIT : AbstractIT() {
    @Rule
    var activityRule = IntentsTestRule(
        ConflictsResolveActivity::class.java, true, false
    )
    private var returnCode = false
    @Test
    @ScreenshotTest
    fun screenshotTextFiles() {
        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"
        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)
        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        val sut = activityRule.launchActivity(intent)
        val dialog = ConflictsResolveDialog.newInstance(
            existingFile,
            newFile,
            UserAccountManagerImpl
                .fromContext(targetContext)
                .user
        )
        dialog.showDialog(sut)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()
        shortSleep()
        shortSleep()
        shortSleep()
        screenshot(Objects.requireNonNull(dialog.requireDialog().window)!!.decorView)
    }

    //    @Test
    // @ScreenshotTest // todo run without real server
    //    public void screenshotImages() throws IOException {
    //        FileDataStorageManager storageManager = new FileDataStorageManager(user,
    //                                                                           targetContext.getContentResolver());
    //
    //        OCFile newFile = new OCFile("/newFile.txt");
    //        newFile.setFileLength(56000);
    //        newFile.setModificationTimestamp(1522019340);
    //        newFile.setStoragePath(FileStorageUtils.getSavePath(user.getAccountName()) + "/nonEmpty.txt");
    //
    //        File image = getFile("image.jpg");
    //
    //        assertTrue(new UploadFileRemoteOperation(image.getAbsolutePath(),
    //                                                 "/image.jpg",
    //                                                 "image/jpg",
    //                                                 "10000000").execute(client).isSuccess());
    //
    //        assertTrue(new RefreshFolderOperation(storageManager.getFileByPath("/"),
    //                                              System.currentTimeMillis(),
    //                                              false,
    //                                              true,
    //                                              storageManager,
    //                                              user.toPlatformAccount(),
    //                                              targetContext
    //        ).execute(client).isSuccess());
    //
    //        OCFile existingFile = storageManager.getFileByPath("/image.jpg");
    //
    //        Intent intent = new Intent(targetContext, ConflictsResolveActivity.class);
    //        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile);
    //        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile);
    //
    //        ConflictsResolveActivity sut = activityRule.launchActivity(intent);
    //
    //        ConflictsResolveDialog.OnConflictDecisionMadeListener listener = decision -> {
    //
    //        };
    //
    //        ConflictsResolveDialog dialog = ConflictsResolveDialog.newInstance(existingFile,
    //                                                                           newFile,
    //                                                                           UserAccountManagerImpl
    //                                                                               .fromContext(targetContext)
    //                                                                               .getUser()
    //                                                                          );
    //        dialog.showDialog(sut);
    //        dialog.listener = listener;
    //
    //        getInstrumentation().waitForIdleSync();
    //        shortSleep();
    //
    //        screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView());
    //    }
    @Test
    fun cancel() {
        returnCode = false
        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )
        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"
        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)
        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        val sut = activityRule.launchActivity(intent)
        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.CANCEL)
            returnCode = true
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shortSleep()
        Espresso.onView(ViewMatchers.withText("Cancel")).perform(ViewActions.click())
        TestCase.assertTrue(returnCode)
    }

    @Test
    @ScreenshotTest
    fun keepExisting() {
        returnCode = false
        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )
        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"
        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)
        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        val sut = activityRule.launchActivity(intent)
        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_SERVER)
            returnCode = true
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.onView(ViewMatchers.withId(R.id.existing_checkbox)).perform(ViewActions.click())
        val dialog = sut.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
        screenshot(Objects.requireNonNull(dialog!!.requireDialog().window)!!.decorView)
        Espresso.onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
        TestCase.assertTrue(returnCode)
    }

    @Test
    @ScreenshotTest
    fun keepNew() {
        returnCode = false
        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )
        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        existingFile.remoteId = "00000123abc"
        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"
        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)
        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        val sut = activityRule.launchActivity(intent)
        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_LOCAL)
            returnCode = true
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.onView(ViewMatchers.withId(R.id.new_checkbox)).perform(ViewActions.click())
        val dialog = sut.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
        screenshot(Objects.requireNonNull(dialog!!.requireDialog().window)!!.decorView)
        Espresso.onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
        TestCase.assertTrue(returnCode)
    }

    @Test
    @ScreenshotTest
    fun keepBoth() {
        returnCode = false
        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )
        val existingFile = OCFile("/newFile.txt")
        existingFile.fileLength = 1024000
        existingFile.modificationTimestamp = 1582019340
        val newFile = OCFile("/newFile.txt")
        newFile.fileLength = 56000
        newFile.modificationTimestamp = 1522019340
        newFile.storagePath = FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt"
        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)
        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        val sut = activityRule.launchActivity(intent)
        sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
            Assert.assertEquals(decision, Decision.KEEP_BOTH)
            returnCode = true
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Espresso.onView(ViewMatchers.withId(R.id.existing_checkbox)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.new_checkbox)).perform(ViewActions.click())
        val dialog = sut.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
        screenshot(Objects.requireNonNull(dialog!!.requireDialog().window)!!.decorView)
        Espresso.onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
        TestCase.assertTrue(returnCode)
    }

    @After
    override fun after() {
        storageManager.deleteAllFiles()
    }
}