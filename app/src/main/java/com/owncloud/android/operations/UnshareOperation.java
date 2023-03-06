/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author Andy Scherzinger
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2018 Andy Scherzinger
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.RemoveShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;

import java.util.List;

/**
 * Unshare file/folder Save the data in Database
 */
public class UnshareOperation extends SyncOperation {

    private static final String TAG = UnshareOperation.class.getSimpleName();
    private static final int SINGLY_SHARED = 1;

    private final String remotePath;
    private final long shareId;
    private final Context context;
    private final User user;

    public UnshareOperation(String remotePath,
                            long shareId,
                            FileDataStorageManager storageManager,
                            User user,
                            Context context) {
        super(storageManager);

        this.remotePath = remotePath;
        this.shareId = shareId;
        this.user = user;
        this.context = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        String token = null;

        // Get Share for a file
        OCShare share = getStorageManager().getShareById(shareId);

        if (share != null) {
            OCFile file = getStorageManager().getFileByEncryptedRemotePath(remotePath);

            if (file.isEncrypted()) {
                // E2E: lock folder
                try {
                    token = EncryptionUtils.lockFolder(file, client);
                } catch (UploadException e) {
                    return new RemoteOperationResult(e);
                }

                // download metadata
                DecryptedFolderMetadataFile metadata = EncryptionUtils.downloadFolderMetadata(file,
                                                                                              client,
                                                                                              context,
                                                                                              user,
                                                                                              token);

                if (metadata == null) {
                    return new RemoteOperationResult(new RuntimeException("No metadata!"));
                }

                // remove sharee from metadata
                EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
                DecryptedFolderMetadataFile newMetadata = encryptionUtilsV2.removeShareeFromMetadata(metadata,
                                                                                                     share.getShareWith());

                EncryptedFolderMetadataFile encryptedFolderMetadata = encryptionUtilsV2.encryptFolderMetadataFile(newMetadata);
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

                // upload metadata
                try {
                    EncryptionUtils.uploadMetadata(file,
                                                   serializedFolderMetadata,
                                                   token,
                                                   client,
                                                   true);
                } catch (UploadException e) {
                    return new RemoteOperationResult(new RuntimeException("Upload of metadata failed!"));
                }
            }

            RemoveShareRemoteOperation operation = new RemoveShareRemoteOperation(share.getRemoteId());
            result = operation.execute(client);

            if (result.isSuccess()) {
                // E2E: unlock folder
                RemoteOperationResult<Void> unlockResult = EncryptionUtils.unlockFolder(file, client, token);
                if (!unlockResult.isSuccess()) {
                    return new RemoteOperationResult<>(new RuntimeException("Unlock failed"));
                }

                Log_OC.d(TAG, "Share id = " + share.getRemoteId() + " deleted");

                if (ShareType.PUBLIC_LINK == share.getShareType()) {
                    file.setSharedViaLink(false);
                } else if (ShareType.USER == share.getShareType() || ShareType.GROUP == share.getShareType()
                    || ShareType.FEDERATED == share.getShareType()) {
                    // Check if it is the last share
                    List<OCShare> sharesWith = getStorageManager().
                        getSharesWithForAFile(remotePath,
                                              getStorageManager().getUser().getAccountName());
                    if (sharesWith.size() == SINGLY_SHARED) {
                        file.setSharedWithSharee(false);
                    }
                }

                getStorageManager().saveFile(file);
                getStorageManager().removeShare(share);

            } else if (result.getCode() != ResultCode.MAINTENANCE_MODE && !existsFile(client, file.getRemotePath())) {
                // unshare failed because file was deleted before
                getStorageManager().removeFile(file, true, true);
            }

        } else {
            result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
        }

        return result;
    }

    private boolean existsFile(OwnCloudClient client, String remotePath) {
        return new ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess();
    }
}
