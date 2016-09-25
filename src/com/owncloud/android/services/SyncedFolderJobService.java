/**
 *   Nextcloud Android client application
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2016 Tobias Kaminsky
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.services;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.util.Log;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.Date;

/**
 * Created by tobi on 25.09.16.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SyncedFolderJobService extends JobService {
    private static final String TAG = "SyncedFolderJobService";
    private Context mContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Tobi why is this null?
        mContext = MainApp.getAppContext();
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log_OC.d(TAG, "startJob: " + params.getJobId());

        // TODO Tobi just for testing!
        Context context = MainApp.getAppContext();
        Account account = AccountUtils.getCurrentOwnCloudAccount(context);

        PersistableBundle bundle = params.getExtras();
        String filePath = bundle.getString("filePath");
        String remoteFolder = bundle.getString("remoteFolder");
        Long dateTaken = bundle.getLong("dateTaken");
        Boolean subfolderByDate = bundle.getInt("subfolderByDate") == 1;

        File file = new File(filePath);

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                context,
                account,
                filePath,
                FileStorageUtils.getInstantUploadFilePath(remoteFolder, file.getName(), dateTaken, subfolderByDate),
                FileUploader.LOCAL_BEHAVIOUR_FORGET,
                "image/jpg",
                true,           // create parent folder if not existent
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        );
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
