/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files;

import android.annotation.SuppressLint;
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

    private final UserAccountManager accountManager;
    private final ClientFactory clientFactory;

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
        @SuppressLint("StaticFieldLeak") private final BaseActivity baseActivity;
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
                                                                                   baseActivity.getUser().orElseThrow(RuntimeException::new),
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
