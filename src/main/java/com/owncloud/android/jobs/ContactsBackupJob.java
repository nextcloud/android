/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Job that backup contacts to /Contacts-Backup and deletes files older than x days
 */

public class ContactsBackupJob extends Job {
    public static final String TAG = "ContactsBackupJob";
    public static final String ACCOUNT = "account";
    public static final String FORCE = "force";
    private OperationsServiceConnection operationsServiceConnection;
    private OperationsService.OperationsServiceBinder operationsServiceBinder;


    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        final Context context = MainApp.getAppContext();
        PersistableBundleCompat bundle = params.getExtras();

        boolean force = bundle.getBoolean(FORCE, false);

        final Account account = AccountUtils.getOwnCloudAccountByName(context, bundle.getString(ACCOUNT, ""));

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());
        Long lastExecution = arbitraryDataProvider.getLongValue(account,
                ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP);

        if (force || (lastExecution + 24 * 60 * 60 * 1000) < Calendar.getInstance().getTimeInMillis()) {
            Log_OC.d(TAG, "start contacts backup job");

            String backupFolder = getContext().getResources().getString(R.string.contacts_backup_folder) +
                    OCFile.PATH_SEPARATOR;
            Integer daysToExpire = getContext().getResources().getInteger(R.integer.contacts_backup_expire);

            backupContact(account, backupFolder);

            // bind to Operations Service
            operationsServiceConnection = new OperationsServiceConnection(daysToExpire, backupFolder, account);

            getContext().bindService(new Intent(getContext(), OperationsService.class), operationsServiceConnection,
                    OperationsService.BIND_AUTO_CREATE);

            // store execution date
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                    ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP,
                    String.valueOf(Calendar.getInstance().getTimeInMillis()));
        } else {
            Log_OC.d(TAG, "last execution less than 24h ago");
        }

        return Result.SUCCESS;
    }

    private void backupContact(Account account, String backupFolder) {
        ArrayList<String> vCard = new ArrayList<>();
        try {

            Cursor cursor = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
                    null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {

                    vCard.add(getContactFromCursor(cursor));
                    cursor.moveToNext();
                }
            }

            String filename = DateFormat.format("yyyy-MM-dd_HH-mm-ss", Calendar.getInstance()).toString() + ".vcf";
            Log_OC.d(TAG, "Storing: " + filename);
            File file = new File(getContext().getCacheDir(), filename);

            FileWriter fw = null;
            try {
                fw = new FileWriter(file);

                for (String card : vCard) {
                    fw.write(card);
                }

            } catch (IOException e) {
                Log_OC.d(TAG, "Error ", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        Log_OC.d(TAG, "Error closing file writer ", e);
                    }
                }
            }

            FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
            requester.uploadNewFile(
                    getContext(),
                    account,
                    file.getAbsolutePath(),
                    backupFolder + filename,
                    FileUploader.LOCAL_BEHAVIOUR_MOVE,
                    null,
                    true,
                    UploadFileOperation.CREATED_BY_USER,
                    false,
                    false
            );

        } catch (Exception e) {
            Log_OC.d(TAG, e.getMessage());
        }
    }

    private void expireFiles(Integer daysToExpire, String backupFolderString, Account account) {
        // -1 disables expiration
        if (daysToExpire > -1) {
            FileDataStorageManager storageManager = new FileDataStorageManager(account, getContext().getContentResolver());
            OCFile backupFolder = storageManager.getFileByPath(backupFolderString);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -daysToExpire);
            Long timestampToExpire = cal.getTimeInMillis();

            if (backupFolder != null) {
                Log_OC.d(TAG, "expire: " + daysToExpire + " " + backupFolder.getFileName());
            }

            ArrayList<OCFile> backups = storageManager.getFolderContent(backupFolder, false);

            for (OCFile backup : backups) {
                if (timestampToExpire > backup.getModificationTimestamp()) {
                    Log_OC.d(TAG, "delete " + backup.getRemotePath());

                    // delete backups
                    Intent service = new Intent(getContext(), OperationsService.class);
                    service.setAction(OperationsService.ACTION_REMOVE);
                    service.putExtra(OperationsService.EXTRA_ACCOUNT, account);
                    service.putExtra(OperationsService.EXTRA_REMOTE_PATH, backup.getRemotePath());
                    service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, false);
                    operationsServiceBinder.queueNewOperation(service);
                }
            }
        }

        getContext().unbindService(operationsServiceConnection);
    }

    private String getContactFromCursor(Cursor cursor) {
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);

        String vCard = "";
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            InputStreamReader inputStreamReader;
            char[] buffer = new char[1024];
            StringBuilder stringBuilder = new StringBuilder();

            if (inputStream != null) {
                inputStreamReader = new InputStreamReader(inputStream);

                while (true) {
                    int byteCount = inputStreamReader.read(buffer, 0, buffer.length);

                    if (byteCount > 0) {
                        stringBuilder.append(buffer, 0, byteCount);
                    } else {
                        break;
                    }
                }
            }

            vCard = stringBuilder.toString();

            return vCard;

        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage());
        }
        return vCard;
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection implements ServiceConnection {
        private Integer daysToExpire;
        private String backupFolder;
        private Account account;

        OperationsServiceConnection(Integer daysToExpire, String backupFolder, Account account) {
            this.daysToExpire = daysToExpire;
            this.backupFolder = backupFolder;
            this.account = account;
        }

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            Log_OC.d(TAG, "service connected");


            if (component.equals(new ComponentName(getContext(), OperationsService.class))) {
                operationsServiceBinder = (OperationsService.OperationsServiceBinder) service;
                expireFiles(daysToExpire, backupFolder, account);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            Log_OC.d(TAG, "service disconnected");

            if (component.equals(new ComponentName(getContext(), OperationsService.class))) {
             operationsServiceBinder = null;
            }
        }
    }
}
