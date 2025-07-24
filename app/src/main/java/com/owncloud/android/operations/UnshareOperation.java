/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
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

            if (file.isEncrypted() && share.getShareType() != ShareType.PUBLIC_LINK) {
                // E2E: lock folder
                try {
                    token = EncryptionUtils.lockFolder(file, client, file.getE2eCounter() + 1);
                } catch (UploadException e) {
                    return new RemoteOperationResult(e);
                }

                // download metadata
                Object object = EncryptionUtils.downloadFolderMetadata(file,
                                                                       client,
                                                                       context,
                                                                       user);

                if (object == null) {
                    return new RemoteOperationResult(new RuntimeException("No metadata!"));
                }

                if (object instanceof DecryptedFolderMetadataFileV1) {
                    throw new RuntimeException("Trying to unshare on e2e v1!");
                }

                DecryptedFolderMetadataFile metadata = (DecryptedFolderMetadataFile) object;

                // remove sharee from metadata
                EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
                DecryptedFolderMetadataFile newMetadata = encryptionUtilsV2.removeShareeFromMetadata(metadata,
                                                                                                     share.getShareWith());

                // upload metadata
                try {
                    encryptionUtilsV2.serializeAndUploadMetadata(file,
                                                                 newMetadata,
                                                                 token,
                                                                 client,
                                                                 true,
                                                                 context,
                                                                 user,
                                                                 getStorageManager());
                } catch (UploadException e) {
                    return new RemoteOperationResult(new RuntimeException("Upload of metadata failed!"));
                }
            }

            RemoveShareRemoteOperation operation = new RemoveShareRemoteOperation(share.getRemoteId());
            result = operation.execute(client);
            boolean isFileExists = existsFile(client, file.getRemotePath());
            boolean isShareExists = getStorageManager().getShareById(shareId) != null;

            if (result.isSuccess()) {
                // E2E: unlock folder
                if (file.isEncrypted() && share.getShareType() != ShareType.PUBLIC_LINK) {
                    RemoteOperationResult<Void> unlockResult = EncryptionUtils.unlockFolder(file, client, token);
                    if (!unlockResult.isSuccess()) {
                        return new RemoteOperationResult<>(new RuntimeException("Unlock failed"));
                    }
                }

                Log_OC.d(TAG, "Share id = " + share.getRemoteId() + " deleted");

                if (ShareType.PUBLIC_LINK == share.getShareType()) {
                    file.setSharedViaLink(false);
                } else if (ShareType.USER == share.getShareType() || ShareType.GROUP == share.getShareType()
                    || ShareType.FEDERATED == share.getShareType() || ShareType.FEDERATED_GROUP == share.getShareType()) {
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
            } else if (result.getCode() != ResultCode.MAINTENANCE_MODE && !isFileExists) {
                // UnShare failed because file was deleted before
                getStorageManager().removeFile(file, true, true);
            } else if (isShareExists && result.getCode() == ResultCode.FILE_NOT_FOUND) {
                // UnShare failed because share was deleted before
                getStorageManager().removeShare(share);
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
