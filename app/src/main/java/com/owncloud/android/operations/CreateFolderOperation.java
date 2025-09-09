/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.util.Pair;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import java.io.File;
import java.util.UUID;

import androidx.annotation.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server. Save the new folder in
 * Database.
 */
public class CreateFolderOperation extends SyncOperation implements OnRemoteOperationListener {

    private static final String TAG = CreateFolderOperation.class.getSimpleName();

    protected String remotePath;
    private RemoteFile createdRemoteFolder;
    private User user;
    private Context context;

    /**
     * Constructor
     */
    public CreateFolderOperation(String remotePath, User user, Context context, FileDataStorageManager storageManager) {
        super(storageManager);

        this.remotePath = remotePath;
        this.user = user;
        this.context = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        String remoteParentPath = new File(getRemotePath()).getParent();
        remoteParentPath = remoteParentPath.endsWith(PATH_SEPARATOR) ?
            remoteParentPath : remoteParentPath + PATH_SEPARATOR;

        OCFile parent = getStorageManager().getFileByDecryptedRemotePath(remoteParentPath);

        String tempRemoteParentPath = remoteParentPath;
        while (parent == null) {
            tempRemoteParentPath = new File(tempRemoteParentPath).getParent();

            if (!tempRemoteParentPath.endsWith(PATH_SEPARATOR)) {
                tempRemoteParentPath = tempRemoteParentPath + PATH_SEPARATOR;
            }

            parent = getStorageManager().getFileByDecryptedRemotePath(tempRemoteParentPath);
        }

        // check if any parent is encrypted
        boolean encryptedAncestor = FileStorageUtils.checkEncryptionStatus(parent, getStorageManager());

        if (encryptedAncestor) {
            E2EVersion e2EVersion = getStorageManager().getCapability(user).getEndToEndEncryptionApiVersion();
            if (e2EVersion == E2EVersion.V1_0 ||
                e2EVersion == E2EVersion.V1_1 ||
                e2EVersion == E2EVersion.V1_2) {
                return encryptedCreateV1(parent, client);
            } else if (e2EVersion == E2EVersion.V2_0) {
                return encryptedCreateV2(parent, client);
            }
            return new RemoteOperationResult(new IllegalStateException("E2E not supported"));
        } else {
            return normalCreate(client);
        }
    }

    @SuppressFBWarnings(
        value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
        justification = "Converting checked exception to runtime is acceptable in this context"
    )
    private RemoteOperationResult encryptedCreateV1(OCFile parent, OwnCloudClient client) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        String privateKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        String token = null;
        Boolean metadataExists;
        DecryptedFolderMetadataFileV1 metadata;
        String encryptedRemotePath = null;

        String filename = new File(remotePath).getName();

        try {
            // lock folder
            token = EncryptionUtils.lockFolder(parent, client);

            // get metadata
            Pair<Boolean, DecryptedFolderMetadataFileV1> metadataPair = EncryptionUtils.retrieveMetadataV1(parent,
                                                                                                           client,
                                                                                                           privateKey,
                                                                                                           publicKey,
                                                                                                           arbitraryDataProvider,
                                                                                                           user
                                                                                                          );

            metadataExists = metadataPair.first;
            metadata = metadataPair.second;

            // check if filename already exists
            if (isFileExisting(metadata, filename)) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS);
            }

            // generate new random file name, check if it exists in metadata
            String encryptedFileName = createRandomFileName(metadata);
            encryptedRemotePath = parent.getRemotePath() + encryptedFileName;

            RemoteOperationResult<String> result = new CreateFolderRemoteOperation(encryptedRemotePath,
                                                                                   true,
                                                                                   token)
                .execute(client);

            if (result.isSuccess()) {
                // update metadata
                metadata.getFiles().put(encryptedFileName, createDecryptedFile(filename));

                EncryptedFolderMetadataFileV1 encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                                                                                                              publicKey,
                                                                                                              parent.getLocalId(),
                                                                                                              user,
                                                                                                              arbitraryDataProvider
                                                                                                             );
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

                // upload metadata
                EncryptionUtils.uploadMetadata(parent,
                                               serializedFolderMetadata,
                                               token,
                                               client,
                                               metadataExists,
                                               E2EVersion.V1_2,
                                               "",
                                               arbitraryDataProvider,
                                               user);

                // unlock folder
                if (token != null) {
                    RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolderV1(parent, client, token);

                    if (unlockFolderResult.isSuccess()) {
                        token = null;
                    } else {
                        // TODO E2E: do better
                        throw new RuntimeException("Could not unlock folder!");
                    }
                }

                final var remoteFolderOperationResult = new ReadFolderRemoteOperation(encryptedRemotePath)
                    .execute(client);

                if (remoteFolderOperationResult.isSuccess() && remoteFolderOperationResult.getData().get(0) instanceof RemoteFile remoteFile) {
                    createdRemoteFolder = remoteFile;
                    OCFile newDir = createRemoteFolderOcFile(parent, filename, createdRemoteFolder);
                    getStorageManager().saveFile(newDir);

                    final var encryptionOperationResult = new ToggleEncryptionRemoteOperation(
                        newDir.getLocalId(),
                        newDir.getRemotePath(),
                        true)
                        .execute(client);

                    if (!encryptionOperationResult.isSuccess()) {
                        throw new RuntimeException("Error creating encrypted subfolder!");
                    }
                } else {
                    throw new RuntimeException("Error creating encrypted subfolder!");
                }
            } else {
                // revert to sane state in case of any error
                Log_OC.e(TAG, remotePath + " hasn't been created");
            }

            return result;
        } catch (Exception e) {
            if (!EncryptionUtils.unlockFolderV1(parent, client, token).isSuccess()) {
                throw new RuntimeException("Could not clean up after failing folder creation!", e);
            }

            // remove folder
            if (encryptedRemotePath != null) {
                RemoteOperationResult removeResult = new RemoveRemoteEncryptedFileOperation(encryptedRemotePath,
                                                                                            user,
                                                                                            context,
                                                                                            filename,
                                                                                            parent,
                                                                                            true
                ).execute(client);

                if (!removeResult.isSuccess()) {
                    throw new RuntimeException("Could not clean up after failing folder creation!");
                }
            }

            // TODO E2E: do better
            return new RemoteOperationResult(e);
        } finally {
            // unlock folder
            if (token != null) {
                RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolderV1(parent, client, token);

                if (!unlockFolderResult.isSuccess()) {
                    // TODO E2E: do better
                    throw new RuntimeException("Could not unlock folder!");
                }
            }
        }
    }

    @SuppressFBWarnings(
        value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
        justification = "Converting checked exception to runtime is acceptable in this context"
    )
    private RemoteOperationResult encryptedCreateV2(OCFile parent, OwnCloudClient client) {
        String token = null;
        Boolean metadataExists;
        DecryptedFolderMetadataFile metadata;
        String encryptedRemotePath = null;

        String filename = new File(remotePath).getName();

        try {
            // lock folder
            token = EncryptionUtils.lockFolder(parent, client);

            // get metadata
            EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
            kotlin.Pair<Boolean, DecryptedFolderMetadataFile> metadataPair = encryptionUtilsV2.retrieveMetadata(parent,
                                                                                                                client,
                                                                                                                user,
                                                                                                                context);

            metadataExists = metadataPair.getFirst();
            metadata = metadataPair.getSecond();

            // check if filename already exists
            if (isFileExisting(metadata, filename)) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS);
            }

            // generate new random file name, check if it exists in metadata
            String encryptedFileName = createRandomFileName(metadata);
            encryptedRemotePath = parent.getRemotePath() + encryptedFileName;

            RemoteOperationResult<String> result = new CreateFolderRemoteOperation(encryptedRemotePath,
                                                                                   true,
                                                                                   token)
                .execute(client);

            String remoteId = result.getResultData();

            if (result.isSuccess()) {
                DecryptedFolderMetadataFile subFolderMetadata = encryptionUtilsV2.createDecryptedFolderMetadataFile();

                // upload metadata
                encryptionUtilsV2.serializeAndUploadMetadata(remoteId,
                                                             subFolderMetadata,
                                                             token,
                                                             client,
                                                             false,
                                                             context,
                                                             user,
                                                             parent,
                                                             getStorageManager());
            }

            if (result.isSuccess()) {
                // update metadata
                DecryptedFolderMetadataFile updatedMetadataFile = encryptionUtilsV2.addFolderToMetadata(encryptedFileName,
                                                                                                        filename,
                                                                                                        metadata,
                                                                                                        parent,
                                                                                                        getStorageManager());

                // upload metadata
                encryptionUtilsV2.serializeAndUploadMetadata(parent,
                                                             updatedMetadataFile,
                                                             token,
                                                             client,
                                                             metadataExists,
                                                             context,
                                                             user,
                                                             getStorageManager());

                // unlock folder
                RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(parent, client, token);

                if (unlockFolderResult.isSuccess()) {
                    token = null;
                } else {
                    // TODO E2E: do better
                    throw new RuntimeException("Could not unlock folder!");
                }

                final var remoteFolderOperationResult = new ReadFolderRemoteOperation(encryptedRemotePath)
                    .execute(client);

                if (remoteFolderOperationResult.isSuccess() && remoteFolderOperationResult.getData().get(0) instanceof RemoteFile remoteFile) {
                    createdRemoteFolder = remoteFile;
                    OCFile newDir = createRemoteFolderOcFile(parent, filename, createdRemoteFolder);
                    getStorageManager().saveFile(newDir);

                    final var encryptionOperationResult = new ToggleEncryptionRemoteOperation(
                        newDir.getLocalId(),
                        newDir.getRemotePath(),
                        true)
                        .execute(client);

                    if (!encryptionOperationResult.isSuccess()) {
                        throw new RuntimeException("Error creating encrypted subfolder!");
                    }
                } else {
                    throw new RuntimeException("Error creating encrypted subfolder!");
                }
            } else {
                // revert to sane state in case of any error
                Log_OC.e(TAG, remotePath + " hasn't been created");
            }

            return result;
        } catch (Exception e) {
            // TODO remove folder

            if (!EncryptionUtils.unlockFolder(parent, client, token).isSuccess()) {
                throw new RuntimeException("Could not clean up after failing folder creation!", e);
            }

            // remove folder
            if (encryptedRemotePath != null) {
                RemoteOperationResult removeResult = new RemoveRemoteEncryptedFileOperation(encryptedRemotePath,
                                                                                            user,
                                                                                            context,
                                                                                            filename,
                                                                                            parent,
                                                                                            true).execute(client);

                if (!removeResult.isSuccess()) {
                    throw new RuntimeException("Could not clean up after failing folder creation!");
                }
            }

            // TODO E2E: do better
            return new RemoteOperationResult(e);
        } finally {
            // unlock folder
            if (token != null) {
                RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(parent, client, token);

                if (!unlockFolderResult.isSuccess()) {
                    // TODO E2E: do better
                    throw new RuntimeException("Could not unlock folder!");
                }
            }
        }
    }

    private boolean isFileExisting(DecryptedFolderMetadataFileV1 metadata, String filename) {
        for (com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile file : metadata.getFiles().values()) {
            if (filename.equalsIgnoreCase(file.getEncrypted().getFilename())) {
                return true;
            }
        }

        return false;
    }

    private boolean isFileExisting(DecryptedFolderMetadataFile metadata, String filename) {
        for (DecryptedFile file : metadata.getMetadata().getFiles().values()) {
            if (filename.equalsIgnoreCase(file.getFilename())) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private OCFile createRemoteFolderOcFile(OCFile parent, String filename, RemoteFile remoteFolder) {
        OCFile newDir = new OCFile(remoteFolder.getRemotePath());

        newDir.setMimeType(MimeType.DIRECTORY);
        newDir.setParentId(parent.getFileId());
        newDir.setRemoteId(remoteFolder.getRemoteId());
        newDir.setModificationTimestamp(System.currentTimeMillis());
        newDir.setEncrypted(true);
        newDir.setPermissions(remoteFolder.getPermissions());
        newDir.setDecryptedRemotePath(parent.getDecryptedRemotePath() + filename + "/");

        return newDir;
    }

    @NonNull
    private com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile createDecryptedFile(String filename) {
        // Key, always generate new one
        byte[] key = EncryptionUtils.generateKey();

        // IV, always generate new one
        byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);

        com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile decryptedFile =
            new com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile();
        Data data = new Data();
        data.setFilename(filename);
        data.setMimetype(MimeType.WEBDAV_FOLDER);
        data.setKey(EncryptionUtils.encodeBytesToBase64String(key));

        decryptedFile.setEncrypted(data);
        decryptedFile.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(iv));

        return decryptedFile;
    }

    @NonNull
    private DecryptedFile createDecryptedFolder(String filename) {
        // Key, always generate new one
        byte[] key = EncryptionUtils.generateKey();

        // IV, always generate new one
        byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);

        return new DecryptedFile(filename,
                                 MimeType.WEBDAV_FOLDER,
                                 EncryptionUtils.encodeBytesToBase64String(iv),
                                 "",
                                 EncryptionUtils.encodeBytesToBase64String(key));
    }

    @NonNull
    private String createRandomFileName(DecryptedFolderMetadataFile metadata) {
        String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

        while (metadata.getMetadata().getFiles().get(encryptedFileName) != null) {
            encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
        }
        return encryptedFileName;
    }

    @NonNull
    private String createRandomFileName(DecryptedFolderMetadataFileV1 metadata) {
        String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

        while (metadata.getFiles().get(encryptedFileName) != null) {
            encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
        }
        return encryptedFileName;
    }

    private RemoteOperationResult normalCreate(OwnCloudClient client) {
        RemoteOperationResult result = new CreateFolderRemoteOperation(remotePath, true).execute(client);

        if (result.isSuccess()) {
            RemoteOperationResult remoteFolderOperationResult = new ReadFolderRemoteOperation(remotePath)
                .execute(client);

            createdRemoteFolder = (RemoteFile) remoteFolderOperationResult.getData().get(0);
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, remotePath + " hasn't been created");
        }

        return result;
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CreateFolderRemoteOperation) {
            onCreateRemoteFolderOperationFinish(result);
        }
    }

    private void onCreateRemoteFolderOperationFinish(RemoteOperationResult result) {
        if (result.isSuccess()) {
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, remotePath + " hasn't been created");
        }
    }

    /**
     * Save new directory in local database.
     */
    private void saveFolderInDB() {
        if (getStorageManager().getFileByPath(FileStorageUtils.getParentPath(remotePath)) == null) {
            // When parent of remote path is not created
            String[] subFolders = remotePath.split(PATH_SEPARATOR);
            String composedRemotePath = ROOT_PATH;

            // For each ancestor folders create them recursively
            for (String subFolder : subFolders) {
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + PATH_SEPARATOR;
                    remotePath = composedRemotePath;
                    saveFolderInDB();
                }
            }
        } else {
            // Create directory on DB
            OCFile newDir = new OCFile(remotePath);
            newDir.setMimeType(MimeType.DIRECTORY);
            long parentId = getStorageManager().getFileByPath(FileStorageUtils.getParentPath(remotePath)).getFileId();
            newDir.setParentId(parentId);
            newDir.setRemoteId(createdRemoteFolder.getRemoteId());
            newDir.setModificationTimestamp(System.currentTimeMillis());
            newDir.setEncrypted(FileStorageUtils.checkEncryptionStatus(newDir, getStorageManager()));
            newDir.setPermissions(createdRemoteFolder.getPermissions());
            getStorageManager().saveFile(newDir);

            Log_OC.d(TAG, "Create directory " + remotePath + " in Database");
        }
    }

    public String getRemotePath() {
        return remotePath;
    }
}
