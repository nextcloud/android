/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author TSI-mc
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2021 TSI-mc
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
 *
 */

package com.owncloud.android.operations;

import android.content.Context;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.users.GetPublicKeyRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates a new private share for a given file.
 */
public class CreateShareWithShareeOperation extends SyncOperation {

    private String path;
    private String shareeName;
    private ShareType shareType;
    private int permissions;
    private String noteMessage;
    private String sharePassword;
    private boolean hideFileDownload;
    private long expirationDateInMillis;
    private String label;
    private Context context;
    private User user;

    private static final Set<ShareType> supportedShareTypes = new HashSet<>(Arrays.asList(ShareType.USER,
                                                                                          ShareType.GROUP,
                                                                                          ShareType.FEDERATED,
                                                                                          ShareType.EMAIL,
                                                                                          ShareType.ROOM,
                                                                                          ShareType.CIRCLE));

    /**
     * Constructor.
     *
     * @param path        Full path of the file/folder being shared.
     * @param shareeName  User or group name of the target sharee.
     * @param shareType   Type of share determines type of sharee; {@link ShareType#USER} and {@link ShareType#GROUP}
     *                    are the only valid values for the moment.
     * @param permissions Share permissions key as detailed in <a
     *                    href="https://docs.nextcloud.com/server/latest/developer_manual/client_apis/OCS/ocs-share-api.html">OCS
     *                    Share API</a>.
     */
    public CreateShareWithShareeOperation(String path,
                                          String shareeName,
                                          ShareType shareType,
                                          int permissions,
                                          String noteMessage,
                                          String sharePassword,
                                          long expirationDateInMillis,
                                          boolean hideFileDownload,
                                          FileDataStorageManager storageManager,
                                          Context context,
                                          User user) {
        super(storageManager);

        if (!supportedShareTypes.contains(shareType)) {
            throw new IllegalArgumentException("Illegal share type " + shareType);
        }
        this.path = path;
        this.shareeName = shareeName;
        this.shareType = shareType;
        this.permissions = permissions;
        this.expirationDateInMillis = expirationDateInMillis;
        this.hideFileDownload = hideFileDownload;
        this.noteMessage = noteMessage;
        this.sharePassword = sharePassword;
        this.context = context;
        this.user = user;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCFile folder = getStorageManager().getFileByDecryptedRemotePath(path);
        boolean isEncrypted = folder != null && folder.isEncrypted();
        String token = null;
        RemoteOperationResult<String> keyResult = null;

        // first check if sharee is using E2E
        if (isEncrypted) {
            keyResult = new GetPublicKeyRemoteOperation(shareeName).executeNextcloudClient(user, context);

            if (!keyResult.isSuccess()) {
                RemoteOperationResult errorResult = new RemoteOperationResult<>(new RuntimeException());
                errorResult.setMessage(context.getString(R.string.user_not_using_e2e));

                return errorResult;
            }
        }


        // E2E: lock folder
        if (isEncrypted) {
            try {
                token = EncryptionUtils.lockFolder(folder, client);
            } catch (UploadException e) {
                return new RemoteOperationResult(e);
            }
        }

        CreateShareRemoteOperation operation = new CreateShareRemoteOperation(
            path,
            shareType,
            shareeName,
            false,
            sharePassword,
            permissions
        );
        operation.setGetShareDetails(true);
        RemoteOperationResult shareResult = operation.execute(client);

        if (!shareResult.isSuccess() || shareResult.getData().size() == 0) {
            // something went wrong
            return shareResult;
        }

        // E2E: update metadata
        if (isEncrypted) {
            DecryptedFolderMetadataFile metadata = EncryptionUtils.downloadFolderMetadata(folder,
                                                                                          client,
                                                                                          context,
                                                                                          user,
                                                                                          token);

            boolean metadataExists;
            if (metadata == null) {
                String cert = EncryptionUtils.retrievePublicKeyForUser(user, context);
                metadata = new DecryptedFolderMetadataFile();
                metadata.getUsers().add(new DecryptedUser(client.getUserId(), cert));

                metadataExists = false;
            } else {
                metadataExists = true;
            }

            EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();

            // add sharee to metadata
            DecryptedFolderMetadataFile newMetadata = encryptionUtilsV2.addShareeToMetadata(metadata,
                                                                                            shareeName,
                                                                                            keyResult.getResultData());

            // upload metadata
            OCFile parent = getStorageManager().getFileByDecryptedRemotePath(path);
            try {
                encryptionUtilsV2.serializeAndUploadMetadata(parent,
                                                             newMetadata,
                                                             token,
                                                             client,
                                                             metadataExists);
            } catch (UploadException e) {
                return new RemoteOperationResult<>(new RuntimeException("Uploading metadata failed"));
            }

            // E2E: unlock folder
            RemoteOperationResult<Void> unlockResult = EncryptionUtils.unlockFolder(folder, client, token);
            if (!unlockResult.isSuccess()) {
                return new RemoteOperationResult<>(new RuntimeException("Unlock failed"));
            }
        }

        OCShare share = (OCShare) shareResult.getData().get(0);

        // once creating share link update other information
        UpdateShareInfoOperation updateShareInfoOperation = new UpdateShareInfoOperation(share, getStorageManager());
        updateShareInfoOperation.setExpirationDateInMillis(expirationDateInMillis);
        updateShareInfoOperation.setHideFileDownload(hideFileDownload);
        updateShareInfoOperation.setNote(noteMessage);
        updateShareInfoOperation.setLabel(label);

        // execute and save the result in database
        RemoteOperationResult updateShareInfoResult = updateShareInfoOperation.execute(client);
        if (updateShareInfoResult.isSuccess() && updateShareInfoResult.getData().size() > 0) {
            OCShare shareUpdated = (OCShare) updateShareInfoResult.getData().get(0);
            updateData(shareUpdated);
        }

        return shareResult;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(path);
        share.setFolder(path.endsWith(FileUtils.PATH_SEPARATOR));
        share.setPasswordProtected(!TextUtils.isEmpty(sharePassword));
        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(path);
        if (file != null) {
            file.setSharedWithSharee(true);    // TODO - this should be done by the FileContentProvider, as part of getStorageManager().saveShare(share)
            getStorageManager().saveFile(file);
        }
    }

    public String getPath() {
        return this.path;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
