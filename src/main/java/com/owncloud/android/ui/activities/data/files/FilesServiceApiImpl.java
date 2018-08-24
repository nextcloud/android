/**
 *   Nextcloud Android client application
 *
 *   Copyright (C) 2018 Edvard Holst
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
package com.owncloud.android.ui.activities.data.files;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.IOException;

/**
 * Implementation of the Files service API that communicates with the NextCloud remote server.
 */
public class FilesServiceApiImpl implements FilesServiceApi {

    private static final String TAG = FilesServiceApiImpl.class.getSimpleName();

    @Override
    public void readRemoteFile(String fileUrl, BaseActivity activity, FilesServiceCallback<OCFile> callback) {
        ReadRemoteFileTask readRemoteFileTask = new ReadRemoteFileTask(fileUrl, activity, callback);
        readRemoteFileTask.execute();
    }

    private static class ReadRemoteFileTask extends AsyncTask<Void, Object, Boolean> {
        private final FilesServiceCallback<OCFile> callback;
        private OCFile remoteOcFile;
        private String errorMessage;
        // TODO: Figure out a better way to do this than passing a BaseActivity reference.
        private final BaseActivity baseActivity;
        private final String fileUrl;

        private ReadRemoteFileTask(String fileUrl, BaseActivity baseActivity, FilesServiceCallback<OCFile> callback) {
            this.callback = callback;
            this.baseActivity = baseActivity;
            this.fileUrl = fileUrl;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            final Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
            final Context context = MainApp.getAppContext();
            OwnCloudAccount ocAccount;
            OwnCloudClient ownCloudClient;
            try {
                ocAccount = new OwnCloudAccount(currentAccount, context);
                ownCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, MainApp.getAppContext());
                ownCloudClient.setOwnCloudVersion(AccountUtils.getServerVersion(currentAccount));
                // always update file as it could be an old state saved in database
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(fileUrl);
                RemoteOperationResult resultRemoteFileOp = operation.execute(ownCloudClient);
                if (resultRemoteFileOp.isSuccess()) {
                    OCFile temp = FileStorageUtils.fillOCFile((RemoteFile) resultRemoteFileOp.getData().get(0));
                    remoteOcFile = baseActivity.getStorageManager().saveFileWithParent(temp, context);

                    if (remoteOcFile.isFolder()) {
                        // perform folder synchronization
                        RemoteOperation synchFolderOp = new RefreshFolderOperation(remoteOcFile,
                                System.currentTimeMillis(),
                                false,
                                true,
                                baseActivity.getStorageManager(),
                                baseActivity.getAccount(),
                                context);
                        synchFolderOp.execute(ownCloudClient);
                    }
                }
                return true;
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Account not found", e);
                errorMessage = "Account not found";
            } catch (IOException e) {
                Log_OC.e(TAG, "IO error", e);
                errorMessage = "IO error";
            } catch (OperationCanceledException e) {
                Log_OC.e(TAG, "Operation has been canceled", e);
                errorMessage = "Operation has been canceled";
            } catch (AuthenticatorException e) {
                Log_OC.e(TAG, "Authentication Exception", e);
                errorMessage = "Authentication Exception";
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                if (remoteOcFile != null) {
                    callback.onLoaded(remoteOcFile);
                    return;
                } else {
                    errorMessage = "File not found";
                }
            }

            callback.onError(errorMessage);
        }
    }
}
