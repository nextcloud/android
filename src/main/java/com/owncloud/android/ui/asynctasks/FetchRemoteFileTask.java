/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.os.AsyncTask;
import android.text.TextUtils;

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
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;

import java.util.List;

import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.FILE_ID_SEARCH;

public class FetchRemoteFileTask extends AsyncTask<Void, Void, String> {
    private final Account account;
    private final String fileId;
    private final FileDataStorageManager storageManager;
    private final FileDisplayActivity fileDisplayActivity;

    public FetchRemoteFileTask(Account account,
                               String fileId,
                               FileDataStorageManager storageManager,
                               FileDisplayActivity fileDisplayActivity) {
        this.account = account;
        this.fileId = fileId;
        this.storageManager = storageManager;
        this.fileDisplayActivity = fileDisplayActivity;
    }

    @Override
    protected String doInBackground(Void... voids) {


        RemoteOperationResult<List<RemoteFile>> remoteOperationResult = new SearchRemoteOperation(fileId,
                                                                                                  FILE_ID_SEARCH,
                                                                                                  false)
            .execute(account, fileDisplayActivity);

        if (remoteOperationResult.isSuccess() && remoteOperationResult.getResultData() != null) {
            if (remoteOperationResult.getResultData().isEmpty()) {
                return fileDisplayActivity.getString(R.string.remote_file_fetch_failed);
            }
            String remotePath = remoteOperationResult.getResultData().get(0).getRemotePath();

            RemoteOperationResult<RemoteFile> result = new ReadFileRemoteOperation(remotePath)
                .execute(account, fileDisplayActivity);

            if (!result.isSuccess()) {
                Exception exception = result.getException();
                String message = "Fetching file " + remotePath + " fails with: " + result.getLogMessage();

                if (exception != null) {
                    return exception.getMessage();
                } else {
                    return message;
                }
            }

            RemoteFile remoteFile = result.getResultData();

            OCFile ocFile = FileStorageUtils.fillOCFile(remoteFile);
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, account);
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
                                                                                account,
                                                                                fileDisplayActivity);
            refreshFolderOperation.execute(account, fileDisplayActivity);

            fileDisplayActivity.setFile(ocFile);
        } else {
            return remoteOperationResult.getLogMessage();
        }

        return "";
    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);

        fileDisplayActivity.dismissLoadingDialog();

        OCFileListFragment listOfFiles = fileDisplayActivity.getListOfFilesFragment();
        if (listOfFiles != null) {
            if (TextUtils.isEmpty(message)) {
                OCFile temp = fileDisplayActivity.getFile();
                fileDisplayActivity.setFile(fileDisplayActivity.getCurrentDir());
                listOfFiles.listDirectory(fileDisplayActivity.getCurrentDir(), temp, MainApp.isOnlyOnDevice(), false);
                fileDisplayActivity.updateActionBarTitleAndHomeButton(null);
            } else {
                DisplayUtils.showSnackMessage(listOfFiles.getView(), message);
            }
        }
    }
}
