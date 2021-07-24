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

import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.LockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UnlockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataRemoteOperation;
import com.owncloud.android.utils.EncryptionUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Remote operation performing the removal of a remote encrypted file or folder
 */
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
    RemoveRemoteEncryptedFileOperation(String remotePath,
                                       String parentId,
                                       Account account,
                                       Context context,
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

        try {
            // Lock folder
            RemoteOperationResult lockFileOperationResult = new LockFileRemoteOperation(parentId).execute(client);

            if (lockFileOperationResult.isSuccess()) {
                token = (String) lockFileOperationResult.getData().get(0);
            } else if (lockFileOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                throw new RemoteOperationFailedException("Forbidden! Please try again later.)");
            } else {
                throw new RemoteOperationFailedException("Unknown error!");
            }

            // refresh metadata
            RemoteOperationResult getMetadataOperationResult = new GetMetadataRemoteOperation(parentId).execute(client);

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
            delete = new DeleteMethod(client.getFilesDavUri(remotePath));
            delete.setQueryString(new NameValuePair[]{new NameValuePair(E2E_TOKEN, token)});
            int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);

            delete.getResponseBodyAsString();   // exhaust the response, although not interesting
            result = new RemoteOperationResult(delete.succeeded() || status == HttpStatus.SC_NOT_FOUND, delete);
            Log_OC.i(TAG, "Remove " + remotePath + ": " + result.getLogMessage());

            // remove file from metadata
            metadata.getFiles().remove(fileName);

            EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                    privateKey);
            String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

            // upload metadata
            RemoteOperationResult uploadMetadataOperationResult =
                new UpdateMetadataRemoteOperation(parentId,
                                                  serializedFolderMetadata, token).execute(client);

            if (!uploadMetadataOperationResult.isSuccess()) {
                throw new RemoteOperationFailedException("Metadata not uploaded!");
            }

            // return success
            return result;
        } catch (NoSuchAlgorithmException |
            IOException |
            InvalidKeyException |
            InvalidAlgorithmParameterException |
            NoSuchPaddingException |
            BadPaddingException |
            IllegalBlockSizeException |
            InvalidKeySpecException e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Remove " + remotePath + ": " + result.getLogMessage(), e);

        } finally {
            if (delete != null) {
                delete.releaseConnection();
            }

            // unlock file
            if (token != null) {
                RemoteOperationResult unlockFileOperationResult = new UnlockFileRemoteOperation(parentId, token)
                    .execute(client);

                if (!unlockFileOperationResult.isSuccess()) {
                    Log_OC.e(TAG, "Failed to unlock " + parentId);
                }
            }
        }

        return result;
    }
}
