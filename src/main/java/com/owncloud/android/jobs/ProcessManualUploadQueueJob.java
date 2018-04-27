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
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.operations.UploadFileOperation;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class ProcessManualUploadQueueJob extends Job {
    public static final String TAG = "ProcessManualUploadQueueJob";

    public static final String KEY_UPLOAD_ACCOUNT = "account";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        PersistableBundleCompat persistableBundleCompat = getParams().getExtras();
        String accountString = persistableBundleCompat.getString(KEY_UPLOAD_ACCOUNT, "");
        Account account = AccountUtils.getOwnCloudAccountByName(getContext(), accountString);

        HashMap<String, Set<String>> pathsToUploadNothing = arbitraryDataProvider.getValues(accountString,
                "upload_queue_nothing%");
        HashMap<String, Set<String>> pathsToUploadDelete = arbitraryDataProvider.getValues(accountString,
                "upload_queue_delete%");
        HashMap<String, Set<String>> pathsToUploadMove = arbitraryDataProvider.getValues(accountString,
                "upload_queue_move%");

        String remotePathBase;

        String[] remotePaths;
        Iterable<String> keys = pathsToUploadNothing.keySet();
        int i;
        for (String currentDir : keys) {
            i = 0;
            remotePathBase = currentDir;
            remotePaths = new String[pathsToUploadNothing.get(currentDir).size()];
            for (String path : pathsToUploadNothing.get(currentDir)) {
                remotePaths[i] = remotePathBase + (new File(path).getName());
                i++;
            }
            uploadFiles(account, FileUploader.LOCAL_BEHAVIOUR_FORGET, pathsToUploadNothing.get(currentDir).toArray
                    (new String[pathsToUploadNothing.get(currentDir).size()]), remotePaths);
        }

        arbitraryDataProvider.deleteKeyForAccountLike(accountString, "upload_queue_nothing%");

        keys = pathsToUploadMove.keySet();
        for (String currentDir : keys) {
            i = 0;
            remotePathBase = currentDir;
            remotePaths = new String[pathsToUploadMove.get(currentDir).size()];
            for (String path : pathsToUploadMove.get(currentDir)) {
                remotePaths[i] = remotePathBase + (new File(path).getName());
                i++;
            }
            uploadFiles(account, FileUploader.LOCAL_BEHAVIOUR_MOVE, pathsToUploadMove.get(currentDir).toArray
                    (new String[pathsToUploadMove.get(currentDir).size()]), remotePaths);
        }

        arbitraryDataProvider.deleteKeyForAccount(accountString, "upload_queue_move%");

        keys = pathsToUploadDelete.keySet();
        for (String currentDir : keys) {
            i = 0;
            remotePathBase = currentDir;
            remotePaths = new String[pathsToUploadDelete.get(currentDir).size()];
            for (String path : pathsToUploadDelete.get(currentDir)) {
                remotePaths[i] = remotePathBase + (new File(path).getName());
                i++;
            }
            uploadFiles(account, FileUploader.LOCAL_BEHAVIOUR_DELETE, pathsToUploadDelete.get(currentDir).toArray
                    (new String[pathsToUploadDelete.get(currentDir).size()]), remotePaths);
        }

        arbitraryDataProvider.deleteKeyForAccount(accountString, "upload_queue_delete%");

        return Result.SUCCESS;
    }

    private void uploadFiles(Account account, int behaviour, String[] filePaths, String[] remotePaths) {
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                getContext(),
                account,
                filePaths,
                remotePaths,
                null,           // MIME type will be detected from file name
                behaviour,
                true,          // do not create parent folder if not existent
                UploadFileOperation.CREATED_BY_USER,
                false,
                false
        );
    }
}
