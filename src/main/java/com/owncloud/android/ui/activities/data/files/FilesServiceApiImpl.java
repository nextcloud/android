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

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.IOException;

/**
 * Implementation of the Files service API that communicates with the NextCloud remote server.
 */
public class FilesServiceApiImpl implements FilesServiceApi {

    private static final String TAG = FilesServiceApiImpl.class.getSimpleName();

    private UserAccountManager accountManager;

    public FilesServiceApiImpl(UserAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    public void readRemoteFile(String fileUrl, BaseActivity activity, FilesServiceCallback<OCFile> callback) {
        ReadRemoteFileTask readRemoteFileTask = new ReadRemoteFileTask(
            accountManager,
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
        private final Account account;
        private final UserAccountManager accountManager;

        private ReadRemoteFileTask(UserAccountManager accountManager,
                                   String fileUrl,
                                   BaseActivity baseActivity,
                                   FilesServiceCallback<OCFile> callback) {
            this.callback = callback;
            this.baseActivity = baseActivity;
            this.fileUrl = fileUrl;
            this.account = accountManager.getCurrentAccount();
            this.accountManager = accountManager;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            final Context context = MainApp.getAppContext();
            OwnCloudAccount ocAccount;
            OwnCloudClient ownCloudClient;
            try {
                ocAccount = new OwnCloudAccount(account, context);
                ownCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, MainApp.getAppContext());
                ownCloudClient.setOwnCloudVersion(accountManager.getServerVersion(account));
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
                return true;
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Account not found", e);
                errorMessage = baseActivity.getString(R.string.account_not_found);
            } catch (IOException e) {
                Log_OC.e(TAG, "IO error", e);
                errorMessage = baseActivity.getString(R.string.io_error);
            } catch (OperationCanceledException e) {
                Log_OC.e(TAG, "Operation has been canceled", e);
                errorMessage = baseActivity.getString(R.string.operation_canceled);
            } catch (AuthenticatorException e) {
                Log_OC.e(TAG, "Authentication Exception", e);
                errorMessage = baseActivity.getString(R.string.authentication_exception);
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
                    errorMessage = baseActivity.getString(R.string.file_not_found);
                }
            }

            callback.onError(errorMessage);
        }
    }
}
