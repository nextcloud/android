/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.trashbin.EmptyTrashbinRemoteOperation;
import com.owncloud.android.lib.resources.trashbin.ReadTrashbinFolderRemoteOperation;
import com.owncloud.android.lib.resources.trashbin.RemoveTrashbinFileRemoteOperation;
import com.owncloud.android.lib.resources.trashbin.RestoreTrashbinFileRemoteOperation;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;

import java.util.List;

import androidx.annotation.NonNull;

public class RemoteTrashbinRepository implements TrashbinRepository {

    private static final String TAG = RemoteTrashbinRepository.class.getSimpleName();

    private OwnCloudClient client;

    RemoteTrashbinRepository(final Context context, final Account account) {
        try {
            OwnCloudAccount nextcloudAccount = new OwnCloudAccount(account, context);
            client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(nextcloudAccount, context);
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
        }
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
            RemoteOperationResult result = new RemoveTrashbinFileRemoteOperation(file.getFullRemotePath())
                .execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    public void emptyTrashbin(OperationCallback callback) {
        new EmptyTrashbinTask(client, callback).execute();
    }

    private static class EmptyTrashbinTask extends AsyncTask<Void, Void, Boolean> {

        private OwnCloudClient client;
        private OperationCallback callback;

        private EmptyTrashbinTask(OwnCloudClient client, OperationCallback callback) {
            this.client = client;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            EmptyTrashbinRemoteOperation emptyTrashbinFileOperation = new EmptyTrashbinRemoteOperation();
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
        new RestoreTrashbinFileTask(file, client, callback).execute();
    }

    private static class RestoreTrashbinFileTask extends AsyncTask<Void, Void, Boolean> {

        private TrashbinFile file;
        private OwnCloudClient client;
        private TrashbinRepository.OperationCallback callback;

        private RestoreTrashbinFileTask(TrashbinFile file, OwnCloudClient client,
                                        TrashbinRepository.OperationCallback callback) {
            this.file = file;
            this.client = client;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            RemoteOperationResult result = new RestoreTrashbinFileRemoteOperation(file.getFullRemotePath(),
                                                                                  file.getFileName()).execute(client);

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
        new ReadRemoteTrashbinFolderTask(remotePath, client, callback).execute();
    }

    private static class ReadRemoteTrashbinFolderTask extends AsyncTask<Void, Void, Boolean> {

        private String remotePath;
        private OwnCloudClient client;
        private List<Object> trashbinFiles;
        private LoadFolderCallback callback;

        private ReadRemoteTrashbinFolderTask(String remotePath, OwnCloudClient client, LoadFolderCallback callback) {
            this.remotePath = remotePath;
            this.client = client;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            RemoteOperationResult result = new ReadTrashbinFolderRemoteOperation(remotePath).execute(client);

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
