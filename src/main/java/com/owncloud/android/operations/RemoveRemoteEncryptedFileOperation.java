/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.GetMetadataOperation;
import com.owncloud.android.lib.resources.files.LockFileOperation;
import com.owncloud.android.lib.resources.files.UnlockFileOperation;
import com.owncloud.android.lib.resources.files.UpdateMetadataOperation;
import com.owncloud.android.utils.EncryptionUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

/**
 * Remote operation performing the removal of a remote encrypted file or folder
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class RemoveRemoteEncryptedFileOperation extends RemoteOperation {
    private static final String TAG = RemoveRemoteEncryptedFileOperation.class.getSimpleName();

    private static final int REMOVE_READ_TIMEOUT = 30000;
    private static final int REMOVE_CONNECTION_TIMEOUT = 5000;

    private String remotePath;
    private String parentId;
    private Account account;

    private ArbitraryDataProvider arbitraryDataProvider;
    private String fileName;

    /**
     * Constructor
     *
     * @param remotePath RemotePath of the remote file or folder to remove from the server
     * @param parentId   local id of parent folder
     */
    public RemoveRemoteEncryptedFileOperation(String remotePath, String parentId, Account account, Context context,
                                              String fileName) {
        this.remotePath = remotePath;
        this.parentId = parentId;
        this.account = account;
        this.fileName = fileName;

        arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());
    }

    /**
     * Performs the remove operation.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        DeleteMethod delete = null;
        String token = null;
        DecryptedFolderMetadata metadata;

        String privateKey = arbitraryDataProvider.getValue(account.name, EncryptionUtils.PRIVATE_KEY);

        // unlock

        try {
            // Lock folder
            LockFileOperation lockFileOperation = new LockFileOperation(parentId);
            RemoteOperationResult lockFileOperationResult = lockFileOperation.execute(client, true);

            if (lockFileOperationResult.isSuccess()) {
                token = (String) lockFileOperationResult.getData().get(0);
            } else if (lockFileOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                throw new RemoteOperationFailedException("Forbidden! Please try again later.)");
            } else {
                throw new RemoteOperationFailedException("Unknown error!");
            }

            // refresh metadata
            GetMetadataOperation getMetadataOperation = new GetMetadataOperation(parentId);
            RemoteOperationResult getMetadataOperationResult = getMetadataOperation.execute(client, true);

            if (getMetadataOperationResult.isSuccess()) {
                // decrypt metadata
                String serializedEncryptedMetadata = (String) getMetadataOperationResult.getData().get(0);

                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                        serializedEncryptedMetadata, new TypeToken<EncryptedFolderMetadata>() {
                        });

                metadata = EncryptionUtils.decryptFolderMetaData(encryptedFolderMetadata, privateKey);
            } else {
                throw new RemoteOperationFailedException("No Metadata found!");
            }

            // delete file remote
            delete = new DeleteMethod(client.getWebdavUri() + WebdavUtils.encodePath(remotePath));
            int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);

            delete.getResponseBodyAsString();   // exhaust the response, although not interesting
            result = new RemoteOperationResult((delete.succeeded() || status == HttpStatus.SC_NOT_FOUND), delete);
            Log_OC.i(TAG, "Remove " + remotePath + ": " + result.getLogMessage());

            // remove file from metadata
            metadata.getFiles().remove(fileName);

            EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                    privateKey);
            String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

            // upload metadata
            UpdateMetadataOperation storeMetadataOperation = new UpdateMetadataOperation(parentId,
                    serializedFolderMetadata, token);
            RemoteOperationResult uploadMetadataOperationResult = storeMetadataOperation.execute(client, true);

            if (!uploadMetadataOperationResult.isSuccess()) {
                throw new RemoteOperationFailedException("Metadata not uploaded!");
            }

            // return success
            return result;
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Remove " + remotePath + ": " + result.getLogMessage(), e);

        } finally {
            if (delete != null) {
                delete.releaseConnection();
            }

            // unlock file
            if (token != null) {
                UnlockFileOperation unlockFileOperation = new UnlockFileOperation(parentId, token);
                RemoteOperationResult unlockFileOperationResult = unlockFileOperation.execute(client, true);

                if (!unlockFileOperationResult.isSuccess()) {
                    Log_OC.e(TAG, "Failed to unlock " + parentId);
                }
            }
        }

        return result;
    }

}
