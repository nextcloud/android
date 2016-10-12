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
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.util.Date;

/**
 * Created by tobi on 25.09.16.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SyncedFolderJobService extends JobService {
    private static final String TAG = "SyncedFolderJobService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log_OC.d(TAG, "startJob: " + params.getJobId());

        Context context = MainApp.getAppContext();
        PersistableBundle bundle = params.getExtras();
        String filePath = bundle.getString("filePath");
        String remoteFolder = bundle.getString("remotePath");
        Long dateTaken = bundle.getLong("dateTaken");
        Boolean subfolderByDate = bundle.getInt("subfolderByDate") == 1;
        Account account = AccountUtils.getOwnCloudAccountByName(context, bundle.getString("account"));
        Integer uploadBehaviour = bundle.getInt("uploadBehaviour");

        File file = new File(filePath);
        String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(file.getAbsolutePath());

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                context,
                account,
                filePath,
                FileStorageUtils.getInstantUploadFilePath(remoteFolder, file.getName(), dateTaken, subfolderByDate),
                uploadBehaviour,
                mimeType,
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
