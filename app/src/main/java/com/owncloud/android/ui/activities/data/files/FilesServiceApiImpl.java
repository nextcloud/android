/*
 *   Nextcloud Android client application
 *
 *   @author Chris Narkiewicz
 *   @author Edvard Holst
 *
 *   Copyright (C) 2018 Edvard Holst
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.content.Context;
import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.FileStorageUtils;

/**
 * Implementation of the Files service API that communicates with the NextCloud remote server.
 */
public class FilesServiceApiImpl implements FilesServiceApi {

    private static final String TAG = FilesServiceApiImpl.class.getSimpleName();

    private UserAccountManager accountManager;
    private ClientFactory clientFactory;

    public FilesServiceApiImpl(UserAccountManager accountManager, ClientFactory clientFactory) {
        this.accountManager = accountManager;
        this.clientFactory = clientFactory;
    }

    @Override
    public void readRemoteFile(String fileUrl, BaseActivity activity, FilesServiceCallback<OCFile> callback) {
        ReadRemoteFileTask readRemoteFileTask = new ReadRemoteFileTask(
            accountManager,
            clientFactory,
            fileUrl,
            activity,
            callback
        );
        readRemoteFileTask.execute();
    }

    private static class ReadRemoteFileTask extends AsyncTask<Void, Object, Boolean> {
        private final FilesServiceCallback<OCFile> callback;
        private OCFile remoteOcFile;
        private String errorMessage;
        // TODO: Figure out a better way to do this than passing a BaseActivity reference.
        private final BaseActivity baseActivity;
        private final String fileUrl;
        private final User user;
        private final UserAccountManager accountManager;
        private final ClientFactory clientFactory;

        private ReadRemoteFileTask(UserAccountManager accountManager,
                                   ClientFactory clientFactory,
                                   String fileUrl,
                                   BaseActivity baseActivity,
                                   FilesServiceCallback<OCFile> callback) {
            this.callback = callback;
            this.baseActivity = baseActivity;
            this.fileUrl = fileUrl;
            this.user = accountManager.getUser();
            this.accountManager = accountManager;
            this.clientFactory = clientFactory;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            final Context context = MainApp.getAppContext();
            try {
                OwnCloudClient ownCloudClient = clientFactory.create(user);
                // always update file as it could be an old state saved in database
                RemoteOperationResult resultRemoteFileOp = new ReadFileRemoteOperation(fileUrl).execute(ownCloudClient);

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
                return Boolean.TRUE;
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Account not found", e);
                errorMessage = baseActivity.getString(R.string.account_not_found);
            }

            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                if (remoteOcFile != null) {
                    callback.onLoaded(remoteOcFile);
                    return;
                } else {
                    errorMessage = baseActivity.getString(R.string.file_not_found);
                }
            }

            callback.onError(errorMessage);
        }
    }
}
