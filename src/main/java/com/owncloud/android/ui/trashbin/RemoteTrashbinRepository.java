/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
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
package com.owncloud.android.ui.trashbin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteTrashbinFolderOperation;
import com.owncloud.android.lib.resources.files.RemoveTrashbinFileOperation;
import com.owncloud.android.lib.resources.files.TrashbinFile;
import com.owncloud.android.operations.EmptyTrashbinFileOperation;
import com.owncloud.android.operations.RestoreTrashbinFileOperation;

import java.util.List;

public class RemoteTrashbinRepository implements TrashbinRepository {

    private static final String TAG = RemoteTrashbinRepository.class.getSimpleName();

    private String userId;
    private OwnCloudClient client;

    public RemoteTrashbinRepository(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = AccountUtils.getCurrentOwnCloudAccount(context);

        try {
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
            client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context);
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
        }

        userId = accountManager.getUserData(account,
                com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);
    }

    public void removeTrashbinFile(TrashbinFile file, OperationCallback callback) {
        new RemoveTrashbinFileTask(client, file, callback).execute();
    }

    private static class RemoveTrashbinFileTask extends AsyncTask<Void, Void, Boolean> {

        private OwnCloudClient client;
        private TrashbinFile file;
        private OperationCallback callback;

        private RemoveTrashbinFileTask(OwnCloudClient client, TrashbinFile file, OperationCallback callback) {
            this.client = client;
            this.file = file;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            RemoveTrashbinFileOperation removeTrashbinFileOperation = new RemoveTrashbinFileOperation(
                    file.getFullRemotePath());
            RemoteOperationResult result = removeTrashbinFileOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    public void emptyTrashbin(OperationCallback callback) {
        new EmptyTrashbinTask(client, userId, callback).execute();
    }

    private static class EmptyTrashbinTask extends AsyncTask<Void, Void, Boolean> {

        private OwnCloudClient client;
        private String userId;
        private OperationCallback callback;

        private EmptyTrashbinTask(OwnCloudClient client, String userId, OperationCallback callback) {
            this.client = client;
            this.userId = userId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            EmptyTrashbinFileOperation emptyTrashbinFileOperation = new EmptyTrashbinFileOperation(userId);
            RemoteOperationResult result = emptyTrashbinFileOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    @Override
    public void restoreFile(TrashbinFile file, OperationCallback callback) {
        new RestoreTrashbinFileTask(file, userId, client, callback).execute();
    }

    private static class RestoreTrashbinFileTask extends AsyncTask<Void, Void, Boolean> {

        private TrashbinFile file;
        private String userId;
        private OwnCloudClient client;
        private TrashbinRepository.OperationCallback callback;

        private RestoreTrashbinFileTask(TrashbinFile file, String userId, OwnCloudClient client,
                                        TrashbinRepository.OperationCallback callback) {
            this.file = file;
            this.userId = userId;
            this.client = client;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            RestoreTrashbinFileOperation restoreTrashbinFileOperation = new RestoreTrashbinFileOperation(
                    file.getFullRemotePath(), file.getFileName(), userId);

            RemoteOperationResult result = restoreTrashbinFileOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    @Override
    public void getFolder(String remotePath, @NonNull LoadFolderCallback callback) {
        new ReadRemoteTrashbinFolderTask(remotePath, userId, client, callback).execute();
    }

    private static class ReadRemoteTrashbinFolderTask extends AsyncTask<Void, Void, Boolean> {

        private String remotePath;
        private String userId;
        private OwnCloudClient client;
        private List<Object> trashbinFiles;
        private LoadFolderCallback callback;

        private ReadRemoteTrashbinFolderTask(String remotePath, String userId, OwnCloudClient client,
                                             LoadFolderCallback callback) {
            this.remotePath = remotePath;
            this.userId = userId;
            this.client = client;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            ReadRemoteTrashbinFolderOperation readRemoteTrashbinFolderOperation =
                    new ReadRemoteTrashbinFolderOperation(remotePath, userId);

            RemoteOperationResult result = readRemoteTrashbinFolderOperation.execute(client);

            if (result.isSuccess()) {
                trashbinFiles = result.getData();
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                callback.onSuccess(trashbinFiles);
            } else {
                callback.onError(R.string.trashbin_loading_failed);
            }
        }
    }
}
