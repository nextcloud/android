/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2016 Tobias Kaminsky
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.accounts.Account;
import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class AutoUploadJob extends Job {
    public static final String TAG = "AutoUploadJob";

    public static final String LOCAL_PATH = "filePath";
    public static final String REMOTE_PATH = "remotePath";
    public static final String ACCOUNT = "account";
    public static final String UPLOAD_BEHAVIOUR = "uploadBehaviour";
    public static final String REQUIRES_WIFI = "requiresWifi";
    public static final String REQUIRES_CHARGING = "requiresCharging";


    @NonNull
    @Override
    protected Result onRunJob(Params params) {

        final Context context = MainApp.getAppContext();

        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG);
        wakeLock.acquire();

        PersistableBundleCompat bundle = params.getExtras();
        final String filePath = bundle.getString(LOCAL_PATH, "");
        final String remotePath = bundle.getString(REMOTE_PATH, "");
        final Account account = AccountUtils.getOwnCloudAccountByName(context, bundle.getString(ACCOUNT, ""));
        final Integer uploadBehaviour = bundle.getInt(UPLOAD_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_FORGET);

        final boolean requiresWifi = bundle.getBoolean(REQUIRES_WIFI, false);
        final boolean requiresCharging = bundle.getBoolean(REQUIRES_CHARGING, false);

        File file = new File(filePath);

        // File can be deleted between job generation and job execution. If file does not exist, just ignore it
        if (file.exists()) {
            final String mimeType = MimeTypeUtil.getBestMimeTypeByFilename(file.getAbsolutePath());

            final FileUploader.UploadRequester requester = new FileUploader.UploadRequester();

            FileChannel channel = null;
            FileLock lock = null;
            try {
                channel = new RandomAccessFile(file, "rw").getChannel();
                lock = channel.tryLock();

                requester.uploadFileWithOverwrite(
                        context,
                        account,
                        filePath,
                        remotePath,
                        uploadBehaviour,
                        mimeType,
                        true,           // create parent folder if not existent
                        UploadFileOperation.CREATED_AS_INSTANT_PICTURE,
                        requiresWifi,
                        requiresCharging,
                        true
                );

                lock.release();
                wakeLock.release();
                return Result.SUCCESS;

            } catch (FileNotFoundException e) {
                Log_OC.d(TAG, "Something went wrong while trying to access file");
            } catch (OverlappingFileLockException e) {
                Log_OC.d(TAG, "Overlapping file lock exception");
            } catch (IOException e) {
                Log_OC.d(TAG, "IO exception");
            }
        }

        wakeLock.release();
        return Result.RESCHEDULE;
    }
}
