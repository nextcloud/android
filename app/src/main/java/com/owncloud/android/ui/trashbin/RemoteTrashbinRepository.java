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

import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudClient;
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

    private final User user;
    private final ClientFactory clientFactory;

    RemoteTrashbinRepository(User user, ClientFactory clientFactory) {
        this.user = user;
        this.clientFactory = clientFactory;
    }

    public void removeTrashbinFile(TrashbinFile file, OperationCallback callback) {
        new RemoveTrashbinFileTask(user, clientFactory, file, callback).execute();
    }

    private static class RemoveTrashbinFileTask extends AsyncTask<Void, Void, Boolean> {

        private User user;
        private ClientFactory clientFactory;
        private TrashbinFile file;
        private OperationCallback callback;

        private RemoveTrashbinFileTask(User user,
                                       ClientFactory clientFactory,
                                       TrashbinFile file,
                                       OperationCallback callback) {
            this.user = user;
            this.clientFactory = clientFactory;
            this.file = file;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OwnCloudClient client = clientFactory.create(user);
                RemoteOperationResult result = new RemoveTrashbinFileRemoteOperation(file.getFullRemotePath())
                    .execute(client);
                return result.isSuccess();
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(this, "Cannot create client", e);
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    public void emptyTrashbin(OperationCallback callback) {
        new EmptyTrashbinTask(user, clientFactory, callback).execute();
    }

    private static class EmptyTrashbinTask extends AsyncTask<Void, Void, Boolean> {

        private User user;
        private ClientFactory clientFactory;
        private OperationCallback callback;

        private EmptyTrashbinTask(User user, ClientFactory clientFactory, OperationCallback callback) {
            this.user = user;
            this.clientFactory = clientFactory;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OwnCloudClient client = clientFactory.create(user);
                EmptyTrashbinRemoteOperation emptyTrashbinFileOperation = new EmptyTrashbinRemoteOperation();
                RemoteOperationResult result = emptyTrashbinFileOperation.execute(client);
                return result.isSuccess();
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(this, "Cannot create client", e);
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    @Override
    public void restoreFile(TrashbinFile file, OperationCallback callback) {
        new RestoreTrashbinFileTask(file, user, clientFactory, callback).execute();
    }

    private static class RestoreTrashbinFileTask extends AsyncTask<Void, Void, Boolean> {

        private TrashbinFile file;
        private User user;
        private ClientFactory clientFactory;
        private TrashbinRepository.OperationCallback callback;

        private RestoreTrashbinFileTask(TrashbinFile file, User user, ClientFactory clientFactory,
                                        TrashbinRepository.OperationCallback callback) {
            this.file = file;
            this.user = user;
            this.clientFactory = clientFactory;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OwnCloudClient client = clientFactory.create(user);
                RemoteOperationResult result = new RestoreTrashbinFileRemoteOperation(file.getFullRemotePath(),
                                                                                      file.getFileName()).execute(client);

                return result.isSuccess();
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(this, "Cannot create client", e);
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            callback.onResult(success);
        }
    }

    @Override
    public void getFolder(String remotePath, @NonNull LoadFolderCallback callback) {
        new ReadRemoteTrashbinFolderTask(remotePath, user, clientFactory, callback).execute();
    }

    private static class ReadRemoteTrashbinFolderTask extends AsyncTask<Void, Void, Boolean> {

        private String remotePath;
        private User user;
        private ClientFactory clientFactory;
        private List<Object> trashbinFiles;
        private LoadFolderCallback callback;

        private ReadRemoteTrashbinFolderTask(String remotePath, User user, ClientFactory clientFactory,
                                             LoadFolderCallback callback) {
            this.remotePath = remotePath;
            this.user = user;
            this.clientFactory = clientFactory;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OwnCloudClient client = clientFactory.create(user);
                RemoteOperationResult result = new ReadTrashbinFolderRemoteOperation(remotePath).execute(client);
                if (result.isSuccess()) {
                    trashbinFiles = result.getData();
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            } catch (ClientFactory.CreationException e) {
                return Boolean.FALSE;
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
