/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2018 Mario Danic
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
package com.owncloud.android.jobs;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.utils.ConnectivityUtils;
import com.owncloud.android.utils.PowerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OfflineSyncJob extends Job {
    public static final String TAG = "OfflineSyncJob";

    private List<OfflineFile> offlineFileList = new ArrayList<>();

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        final Context context = MainApp.getAppContext();

        PowerManager.WakeLock wakeLock = null;
        if (!PowerUtils.isPowerSaveMode(context) &&
                Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED) &&
                !ConnectivityUtils.isInternetWalled(context)) {
            Set<Job> jobs = JobManager.instance().getAllJobsForTag(TAG);
            for (Job job : jobs) {
                if (!job.isFinished() && !job.equals(this)) {
                    return Result.SUCCESS;
                }
            }

            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                wakeLock.acquire();
            }

            Cursor cursorOnKeptInSync = context.getContentResolver().query(
                    ProviderMeta.ProviderTableMeta.CONTENT_URI,
                    null,
                    ProviderMeta.ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?",
                    new String[]{String.valueOf(1)},
                    null
            );

            if (cursorOnKeptInSync != null) {
                if (cursorOnKeptInSync.moveToFirst()) {

                    String localPath = "";
                    String accountName = "";
                    Account account = null;
                    do {
                        localPath = cursorOnKeptInSync.getString(cursorOnKeptInSync
                                .getColumnIndex(ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH));
                        accountName = cursorOnKeptInSync.getString(cursorOnKeptInSync
                                .getColumnIndex(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER));

                        account = new Account(accountName, MainApp.getAccountType());
                        if (!AccountUtils.exists(account, context) || localPath == null || localPath.length() <= 0) {
                            continue;
                        }

                        offlineFileList.add(new OfflineFile(localPath, account));

                    } while (cursorOnKeptInSync.moveToNext());

                }
                cursorOnKeptInSync.close();
            }

            FileDataStorageManager storageManager;
            for (OfflineFile offlineFile : offlineFileList) {
                storageManager = new FileDataStorageManager(offlineFile.getAccount(), context.getContentResolver());
                OCFile file = storageManager.getFileByLocalPath(offlineFile.getLocalPath());
                SynchronizeFileOperation sfo =
                        new SynchronizeFileOperation(file, null, offlineFile.getAccount(), true, context);
                RemoteOperationResult result = sfo.execute(storageManager, context);
                if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                    Intent i = new Intent(context, ConflictsResolveActivity.class);
                    i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
                    i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, offlineFile.getAccount());
                    context.startActivity(i);
                }
            }

            if (wakeLock != null) {
                wakeLock.release();
            }
        }

        return Result.SUCCESS;
    }


    private class OfflineFile {
        private String localPath;
        private Account account;

        private OfflineFile(String localPath, Account account) {
            this.localPath = localPath;
            this.account = account;
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }

        public Account getAccount() {
            return account;
        }

        public void setAccount(Account account) {
            this.account = account;
        }
    }
}
