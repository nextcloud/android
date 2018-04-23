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
    public void readRemoteFile(String fileUrl, BaseActivity activity,
                               boolean isSharingSupported, FilesServiceCallback<OCFile> callback) {
        ReadRemoteFileTask readRemoteFileTask = new ReadRemoteFileTask(fileUrl, activity,
                isSharingSupported, callback);
        readRemoteFileTask.execute();
    }

    private static class ReadRemoteFileTask extends AsyncTask<Void, Object, Boolean> {
        private final FilesServiceCallback<OCFile> mCallback;
        private OCFile remoteOcFile;
        private String errorMessage;
        // TODO: Figure out a better way to do this than passing a BaseActivity reference.
        private final BaseActivity mBaseActivity;
        private final boolean mIsSharingSupported;
        private final String mFileUrl;

        private ReadRemoteFileTask(String fileUrl, BaseActivity baseActivity,
                                   boolean isSharingSupported,
                                   FilesServiceCallback<OCFile> callback) {
            mCallback = callback;
            mBaseActivity = baseActivity;
            mIsSharingSupported = isSharingSupported;
            mFileUrl = fileUrl;
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
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(mFileUrl);
                RemoteOperationResult resultRemoteFileOp = operation.execute(ownCloudClient);
                if (resultRemoteFileOp.isSuccess()) {
                    OCFile temp = FileStorageUtils.fillOCFile((RemoteFile) resultRemoteFileOp.getData().get(0));
                    remoteOcFile = mBaseActivity.getStorageManager().saveFileWithParent(temp, context);

                    if (remoteOcFile.isFolder()) {
                        // perform folder synchronization
                        RemoteOperation synchFolderOp = new RefreshFolderOperation(remoteOcFile,
                                System.currentTimeMillis(),
                                false,
                                mIsSharingSupported,
                                true,
                                mBaseActivity.getStorageManager(),
                                mBaseActivity.getAccount(),
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
            if (success && remoteOcFile != null) {
                mCallback.onLoaded(remoteOcFile);
                return;
            } else if (success) {
                errorMessage = "File not found";
            }

            mCallback.onError(errorMessage);
        }
    }

}
