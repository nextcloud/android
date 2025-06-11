/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.network.ClientFactoryImpl;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.extensions.DecryptedUserExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
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

    private final String path;
    private final String shareeName;
    private final ShareType shareType;
    private final int permissions;
    private final String noteMessage;
    private final String sharePassword;
    private final boolean hideFileDownload;
    private final long expirationDateInMillis;
    private String label;
    private final Context context;
    private final User user;

    private ArbitraryDataProvider arbitraryDataProvider;

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
                                          User user,
                                          ArbitraryDataProvider arbitraryDataProvider) {
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
        this.arbitraryDataProvider = arbitraryDataProvider;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCFile folder = getStorageManager().getFileByDecryptedRemotePath(path);

        if (folder == null) {
            throw new IllegalArgumentException("Trying to share on a null folder: " + path);
        }

        boolean isEncrypted = folder.isEncrypted();
        String token = null;
        long newCounter = folder.getE2eCounter() + 1;

        // E2E: lock folder
        if (isEncrypted) {
            try {
                String publicKey = EncryptionUtils.getPublicKey(user, shareeName, arbitraryDataProvider);

                if (publicKey.isEmpty()) {
                    NextcloudClient nextcloudClient = new ClientFactoryImpl(context).createNextcloudClient(user);
                    RemoteOperationResult<String> result = new GetPublicKeyRemoteOperation(shareeName).execute(nextcloudClient);
                    if (result.isSuccess()) {
                        // store it
                        EncryptionUtils.savePublicKey(
                            user,
                            result.getResultData(),
                            shareeName,
                            arbitraryDataProvider
                                                     );
                    } else {
                        RemoteOperationResult e = new RemoteOperationResult(new IllegalStateException());
                        e.setMessage(context.getString(R.string.secure_share_not_set_up));

                        return e;
                    }
                }

                token = EncryptionUtils.lockFolder(folder, client, newCounter);
            } catch (UploadException | ClientFactory.CreationException e) {
                return new RemoteOperationResult(e);
            }
        }

        CreateShareRemoteOperation operation = new CreateShareRemoteOperation(
            path,
            shareType,
            shareeName,
            false,
            sharePassword,
            permissions,
            noteMessage,
            ""
        );
        operation.setGetShareDetails(true);
        RemoteOperationResult shareResult = operation.execute(client);

        if (!shareResult.isSuccess() || shareResult.getData().size() == 0) {
            // something went wrong
            return shareResult;
        }

        // E2E: update metadata
        if (isEncrypted) {
            Object object = EncryptionUtils.downloadFolderMetadata(folder,
                                                                   client,
                                                                   context,
                                                                   user
                                                                  );

            if (object instanceof DecryptedFolderMetadataFileV1) {
                throw new RuntimeException("Trying to share on e2e v1!");
            }

            DecryptedFolderMetadataFile metadata = (DecryptedFolderMetadataFile) object;

            boolean metadataExists;
            if (metadata == null) {
                String cert = EncryptionUtils.retrievePublicKeyForUser(user, context);
                metadata = new EncryptionUtilsV2().createDecryptedFolderMetadataFile();
                metadata.getUsers().add(new DecryptedUser(client.getUserId(), cert, null));

                metadataExists = false;
            } else {
                metadataExists = true;
            }

            EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();

            // add sharee to metadata
            String publicKey = EncryptionUtils.getPublicKey(user, shareeName, arbitraryDataProvider);

            String decryptedMetadataKey = DecryptedUserExtensionsKt.findMetadataKeyByUserId(metadata.getUsers(), shareeName);
            DecryptedFolderMetadataFile newMetadata = encryptionUtilsV2.addShareeToMetadata(metadata,
                                                                                            shareeName,
                                                                                            publicKey,
                                                                                            decryptedMetadataKey);

            // upload metadata
            metadata.getMetadata().setCounter(newCounter);
            try {
                encryptionUtilsV2.serializeAndUploadMetadata(folder,
                                                             newMetadata,
                                                             token,
                                                             client,
                                                             metadataExists,
                                                             context,
                                                             user,
                                                             getStorageManager());
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

        //update permissions for external share (will otherwise default to read-only)
        updateShareInfoOperation.setPermissions(permissions);

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
