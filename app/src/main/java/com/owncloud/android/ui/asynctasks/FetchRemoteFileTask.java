/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.FileStorageUtils;

import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.FILE_ID_SEARCH;

public class FetchRemoteFileTask extends AsyncTask<Void, Void, String> {
    private final User user;
    private final String fileId;
    private final FileDataStorageManager storageManager;
    private final FileDisplayActivity fileDisplayActivity;
    private OCFile ocFile;

    public FetchRemoteFileTask(User user,
                               String fileId,
                               FileDataStorageManager storageManager,
                               FileDisplayActivity fileDisplayActivity) {
        this.user = user;
        this.fileId = fileId;
        this.storageManager = storageManager;
        this.fileDisplayActivity = fileDisplayActivity;
    }

    @Override
    protected String doInBackground(Void... voids) {
        SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation(fileId,
                                                                                FILE_ID_SEARCH,
                                                                                false,
                                                                                fileDisplayActivity.getCapabilities());
        RemoteOperationResult remoteOperationResult = searchRemoteOperation.execute(user, fileDisplayActivity);

        if (remoteOperationResult.isSuccess() && remoteOperationResult.getData() != null) {
            if (remoteOperationResult.getData().isEmpty()) {
                return fileDisplayActivity.getString(R.string.remote_file_fetch_failed);
            }
            String remotePath = ((RemoteFile) remoteOperationResult.getData().get(0)).getRemotePath();

            ReadFileRemoteOperation operation = new ReadFileRemoteOperation(remotePath);
            RemoteOperationResult result = operation.execute(user, fileDisplayActivity);

            if (!result.isSuccess()) {
                Exception exception = result.getException();
                String message = "Fetching file " + remotePath + " fails with: " + result.getLogMessage(MainApp.getAppContext());

                if (exception != null) {
                    return exception.getMessage();
                } else {
                    return message;
                }
            }

            RemoteFile remoteFile = (RemoteFile) result.getData().get(0);

            ocFile = FileStorageUtils.fillOCFile(remoteFile);
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.getAccountName());
            ocFile = storageManager.saveFileWithParent(ocFile, fileDisplayActivity);

            // also sync folder content
            OCFile toSync;
            if (ocFile.isFolder()) {
                toSync = ocFile;
            } else {
                toSync = storageManager.getFileById(ocFile.getParentId());
            }

            long currentSyncTime = System.currentTimeMillis();
            RemoteOperation refreshFolderOperation = new RefreshFolderOperation(toSync,
                                                                                currentSyncTime,
                                                                                true,
                                                                                true,
                                                                                storageManager,
                                                                                user,
                                                                                fileDisplayActivity);
            refreshFolderOperation.execute(user, fileDisplayActivity);

            fileDisplayActivity.setFile(ocFile);
        } else {
            return remoteOperationResult.getLogMessage(MainApp.getAppContext());
        }

        return "";
    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);

        fileDisplayActivity.showFile(ocFile, message);
    }
}
