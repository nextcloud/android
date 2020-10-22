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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nextcloud.client.account.RegisteredUser;
import com.nextcloud.client.account.Server;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;

import androidx.fragment.app.DialogFragment;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class DialogFragmentIT extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule =
        new IntentsTestRule<>(FileDisplayActivity.class, true, false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotTest
    public void testRenameFileDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(new OCFile("/Test/"));
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testLoadingDialog() {
        LoadingDialog dialog = LoadingDialog.newInstance("Wait…");
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testRemoveFileDialog() {
        RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(new OCFile("/Test.md"));
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testRemoveFilesDialog() {
        ArrayList<OCFile> toDelete = new ArrayList<>();
        toDelete.add(new OCFile("/Test.md"));
        toDelete.add(new OCFile("/Document.odt"));

        RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(toDelete);
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testRemoveFolderDialog() {
        RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(new OCFile("/Folder/"));
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testRemoveFoldersDialog() {
        ArrayList<OCFile> toDelete = new ArrayList<>();
        toDelete.add(new OCFile("/Folder/"));
        toDelete.add(new OCFile("/Documents/"));

        RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(toDelete);
        showDialog(dialog);
    }

    @Test
    @ScreenshotTest
    public void testNewFolderDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        CreateFolderDialogFragment sut = CreateFolderDialogFragment.newInstance(new OCFile("/"));
        showDialog(sut);
    }

    @Test
    @ScreenshotTest
    public void testAccountChooserDialog() throws AccountUtils.AccountNotFoundException {
        AccountManager accountManager = AccountManager.get(targetContext);
        for (Account account : accountManager.getAccountsByType(MainApp.getAccountType(targetContext))) {
            accountManager.removeAccountExplicitly(account);
        }

        Account newAccount = new Account("test@https://server.com", MainApp.getAccountType(targetContext));
        accountManager.addAccountExplicitly(newAccount, "password", null);
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, "https://server.com");
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, "test");


        Account newAccount2 = new Account("user1@server.com", MainApp.getAccountType(targetContext));
        accountManager.addAccountExplicitly(newAccount2, "password", null);
        accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_OC_BASE_URL, "https://server.com");
        accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_USER_ID, "user1");
        accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_OC_VERSION, "20.0.0");

        FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(newAccount,
                                                                                   targetContext.getContentResolver());

        OCCapability capability = new OCCapability();
        capability.setUserStatus(CapabilityBooleanType.TRUE);
        capability.setUserStatusSupportsEmoji(CapabilityBooleanType.TRUE);
        fileDataStorageManager.saveCapabilities(capability);

        ChooseAccountDialogFragment sut =
            ChooseAccountDialogFragment.newInstance(new RegisteredUser(newAccount,
                                                                       new OwnCloudAccount(newAccount, targetContext),
                                                                       new Server(URI.create("https://server.com"),
                                                                                  OwnCloudVersion.nextcloud_20)));
        showDialog(sut);
    }

    @Test
    @ScreenshotTest
    public void testAccountChooserDialogWithStatusDisabled() throws AccountUtils.AccountNotFoundException {
        AccountManager accountManager = AccountManager.get(targetContext);
        for (Account account : accountManager.getAccounts()) {
            accountManager.removeAccountExplicitly(account);
        }

        Account newAccount = new Account("test@https://server.com", MainApp.getAccountType(targetContext));
        accountManager.addAccountExplicitly(newAccount, "password", null);
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, "https://server.com");
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, "test");

        FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(newAccount,
                                                                                   targetContext.getContentResolver());

        OCCapability capability = new OCCapability();
        capability.setUserStatus(CapabilityBooleanType.FALSE);

        fileDataStorageManager.saveCapabilities(capability);

        ChooseAccountDialogFragment sut =
            ChooseAccountDialogFragment.newInstance(new RegisteredUser(newAccount,
                                                                       new OwnCloudAccount(newAccount, targetContext),
                                                                       new Server(URI.create("https://server.com"),
                                                                                  OwnCloudVersion.nextcloud_20)));
        showDialog(sut);
    }

    private void showDialog(DialogFragment dialog) {
        Intent intent = new Intent(targetContext, FileDisplayActivity.class);
        FileDisplayActivity sut = activityRule.launchActivity(intent);

        dialog.show(sut.getSupportFragmentManager(), "");

        getInstrumentation().waitForIdleSync();
        shortSleep();

        ViewGroup viewGroup = dialog.requireDialog().getWindow().findViewById(android.R.id.content);
        hideCursors(viewGroup);

        screenshot(Objects.requireNonNull(dialog.requireDialog().getWindow()).getDecorView());
    }

    private void hideCursors(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof ViewGroup) {
                hideCursors((ViewGroup) child);
            }

            if (child instanceof TextView) {
                ((TextView) child).setCursorVisible(false);
            }
        }
    }
}
