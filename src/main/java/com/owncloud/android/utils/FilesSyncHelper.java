/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FilesSyncHelper {

    private static final String LAST_AUTOUPLOAD_JOB_RUN = "last_autoupload_job_run";


    private static void insertAllDBEntries() {
        boolean dryRun = false;

        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

        for (Account account : AccountUtils.getAccounts(context)) {
            if (TextUtils.isEmpty(arbitraryDataProvider.getValue(account.name, LAST_AUTOUPLOAD_JOB_RUN))) {
                dryRun = true;
            } else {
                dryRun = false;
            }

            FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, dryRun,
                    account.name);
            FilesSyncHelper.insertContentIntoDB(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, dryRun, account.name);
            FilesSyncHelper.insertContentIntoDB(android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI, dryRun,
                    account.name);
            FilesSyncHelper.insertContentIntoDB(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, dryRun, account.name);
        }
    }

    public static void prepareSyncStatusForAccounts() {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);

        Set<String> enabledAccounts = new HashSet<>();
        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            enabledAccounts.add(syncedFolder.getAccount());
        }

        for (String enabledAccount : enabledAccounts) {
            arbitraryDataProvider.storeOrUpdateKeyValue(enabledAccount, LAST_AUTOUPLOAD_JOB_RUN,
                    Long.toString(System.currentTimeMillis()));
        }

        ArrayList<String> accountsArrayList = new ArrayList<>();
        accountsArrayList.addAll(enabledAccounts);
        arbitraryDataProvider.deleteForKeyWhereAccountNotIn(accountsArrayList, LAST_AUTOUPLOAD_JOB_RUN);

        insertAllDBEntries();

    }

    public static void insertContentIntoDB(Uri uri, boolean dryRun, String account) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor;
        int column_index_data, column_index_date_modified, column_index_mimetype;

        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);

        String contentPath;
        boolean isFolder;

        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.MIME_TYPE};

        cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_date_modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            column_index_mimetype = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            while (cursor.moveToNext()) {
                contentPath = cursor.getString(column_index_data);
                isFolder = new File(contentPath).isDirectory();
                filesystemDataProvider.storeOrUpdateFileValue(cursor.getString(column_index_data),
                        cursor.getLong(column_index_date_modified), isFolder, account, dryRun,
                        cursor.getString(column_index_mimetype));
            }
            cursor.close();
        }
    }

}
