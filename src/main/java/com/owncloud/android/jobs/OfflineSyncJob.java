/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Mario Danic
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.os.Build;
import android.os.PowerManager;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.Set;

import androidx.annotation.NonNull;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

public class OfflineSyncJob extends Job {
    public static final String TAG = "OfflineSyncJob";

    private static final String WAKELOCK_TAG_SEPARATION = ":";
    private final UserAccountManager userAccountManager;
    private final ConnectivityService connectivityService;
    private final PowerManagementService powerManagementService;

    OfflineSyncJob(UserAccountManager userAccountManager, ConnectivityService connectivityService, PowerManagementService powerManagementService) {
        this.userAccountManager = userAccountManager;
        this.connectivityService = connectivityService;
        this.powerManagementService = powerManagementService;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        final Context context = getContext();

        PowerManager.WakeLock wakeLock = null;
        if (!powerManagementService.isPowerSavingEnabled() &&
                connectivityService.getActiveNetworkType() == JobRequest.NetworkType.UNMETERED &&
                !connectivityService.isInternetWalled()) {
            Set<Job> jobs = JobManager.instance().getAllJobsForTag(TAG);
            for (Job job : jobs) {
                if (!job.isFinished() && !job.equals(this)) {
                    return Result.SUCCESS;
                }
            }

            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainApp.getAuthority() +
                        WAKELOCK_TAG_SEPARATION + TAG);
                    wakeLock.acquire(10 * 60 * 1000);
                }
            }

            Account[] accounts = userAccountManager.getAccounts();

            for (Account account : accounts) {
                FileDataStorageManager storageManager = new FileDataStorageManager(account,
                        getContext().getContentResolver());

                OCFile ocRoot = storageManager.getFileByPath(ROOT_PATH);

                if (ocRoot.getStoragePath() == null) {
                    break;
                }

                recursive(new File(ocRoot.getStoragePath()), storageManager, account);
            }

            if (wakeLock != null) {
                wakeLock.release();
            }
        }

        return Result.SUCCESS;
    }

    private void recursive(File folder, FileDataStorageManager storageManager, Account account) {
        String downloadFolder = FileStorageUtils.getSavePath(account.name);
        String folderName = folder.getAbsolutePath().replaceFirst(downloadFolder, "") + PATH_SEPARATOR;
        Log_OC.d(TAG, folderName + ": enter");

        // exit
        if (folder.listFiles() == null) {
            return;
        }

        OCFile ocFolder = storageManager.getFileByPath(folderName);
        Log_OC.d(TAG, folderName + ": currentEtag: " + ocFolder.getEtag());

        // check for etag change, if false, skip
        CheckEtagRemoteOperation checkEtagOperation = new CheckEtagRemoteOperation(ocFolder.getRemotePath(),
                                                                                   ocFolder.getEtagOnServer());
        RemoteOperationResult result = checkEtagOperation.execute(account, getContext());

        // eTag changed, sync file
        switch (result.getCode()) {
            case ETAG_UNCHANGED:
                Log_OC.d(TAG, folderName + ": eTag unchanged");
                return;

            case FILE_NOT_FOUND:
                boolean removalResult = storageManager.removeFolder(ocFolder, true, true);
                if (!removalResult) {
                    Log_OC.e(TAG, "removal of " + ocFolder.getStoragePath() + " failed: file not found");
                }
                return;

            default:
            case ETAG_CHANGED:
                Log_OC.d(TAG, folderName + ": eTag changed");
                break;
        }

        // iterate over downloaded files
        File[] files = folder.listFiles(File::isFile);

        if (files != null) {
            for (File file : files) {
                OCFile ocFile = storageManager.getFileByLocalPath(file.getPath());
                SynchronizeFileOperation synchronizeFileOperation = new SynchronizeFileOperation(ocFile.getRemotePath(),
                        account, true, getContext());

                synchronizeFileOperation.execute(storageManager, getContext());
            }
        }

        // recursive into folder
        File[] subfolders = folder.listFiles(File::isDirectory);

        if (subfolders != null) {
            for (File subfolder : subfolders) {
                recursive(subfolder, storageManager, account);
            }
        }

        // update eTag
        try {
            String updatedEtag = (String) result.getData().get(0);
            ocFolder.setEtagOnServer(updatedEtag);
            storageManager.saveFile(ocFolder);
        } catch (Exception e) {
            Log_OC.e(TAG, "Failed to update etag on " + folder.getAbsolutePath(), e);
        }
    }
}
