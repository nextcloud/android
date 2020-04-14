/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
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

import android.accounts.Account;
import android.content.Context;
import android.os.Build;
import android.util.Pair;

import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import java.io.File;
import java.util.UUID;

import androidx.annotation.RequiresApi;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;


/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server. Save the new folder in
 * Database
 */
public class CreateFolderOperation extends SyncOperation implements OnRemoteOperationListener {

    private static final String TAG = CreateFolderOperation.class.getSimpleName();

    protected String remotePath;
    private RemoteFile createdRemoteFolder;
    private Account account;
    private Context context;

    /**
     * Constructor
     */
    public CreateFolderOperation(String remotePath, Account account, Context context) {
        this.remotePath = remotePath;
        this.account = account;
        this.context = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        String remoteParentPath = new File(getRemotePath()).getParent();
        remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ?
            remoteParentPath : remoteParentPath + OCFile.PATH_SEPARATOR;

        OCFile parent = getStorageManager().getFileByPath(remoteParentPath);

        // check if any parent is encrypted
        boolean encryptedAncestor = FileStorageUtils.checkEncryptionStatus(parent, getStorageManager());

        if (encryptedAncestor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return encryptedCreate(parent, remoteParentPath, client);
            } else {
                Log_OC.e(TAG, "Encrypted upload on old Android API");
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OLD_ANDROID_API);
            }
        } else {
            return normalCreate(client);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private RemoteOperationResult encryptedCreate(OCFile parent, String remoteParentPath, OwnCloudClient client) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());
        String privateKey = arbitraryDataProvider.getValue(account.name, EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(account.name, EncryptionUtils.PUBLIC_KEY);

        String token;
        Boolean metadataExists;
        DecryptedFolderMetadata metadata;

        String filename = new File(remotePath).getName();

        try {
            // lock folder
            token = EncryptionUtils.lockFolder(parent, client);

            // get metadata
            Pair<Boolean, DecryptedFolderMetadata> metadataPair = EncryptionUtils.retrieveMetadata(parent,
                                                                                                   client,
                                                                                                   privateKey,
                                                                                                   publicKey);

            metadataExists = metadataPair.first;
            metadata = metadataPair.second;

            // check if filename already exists
            for (String key : metadata.getFiles().keySet()) {
                DecryptedFolderMetadata.DecryptedFile file = metadata.getFiles().get(key);

                if (file != null && filename.equalsIgnoreCase(file.getEncrypted().getFilename())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS);
                }
            }

            // generate new random file name, check if it exists in metadata
            String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

            while (metadata.getFiles().get(encryptedFileName) != null) {
                encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
            }
            String encryptedRemotePath = remoteParentPath + encryptedFileName;

            RemoteOperationResult result = new CreateFolderRemoteOperation(encryptedRemotePath, true).execute(client);

            if (result.isSuccess()) {
                RemoteOperationResult remoteFolderOperationResult = new ReadFolderRemoteOperation(encryptedRemotePath)
                    .execute(client);

                createdRemoteFolder = (RemoteFile) remoteFolderOperationResult.getData().get(0);
                OCFile newDir = new OCFile(remoteParentPath + filename);
                newDir.setMimeType(MimeType.DIRECTORY);
                long parentId = getStorageManager().getFileByPath(FileStorageUtils.getParentPath(remotePath)).getFileId();
                newDir.setParentId(parentId);
                newDir.setRemoteId(createdRemoteFolder.getRemoteId());
                newDir.setModificationTimestamp(System.currentTimeMillis());
                newDir.setEncrypted(true);
                newDir.setEncryptedFileName(encryptedFileName);
                newDir.setPermissions(createdRemoteFolder.getPermissions());
                getStorageManager().saveFile(newDir);

                RemoteOperationResult encryptionOperationResult = new ToggleEncryptionRemoteOperation(
                    newDir.getLocalId(),
                    newDir.getRemotePath(),
                    true)
                    .execute(client);

                if (!encryptionOperationResult.isSuccess()) {
                    // TODO ERROR and clean up
                }

                // Key, always generate new one
                byte[] key = EncryptionUtils.generateKey();

                // IV, always generate new one
                byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);

                // update metadata
                DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
                DecryptedFolderMetadata.Data data = new DecryptedFolderMetadata.Data();
                data.setFilename(filename);
                data.setMimetype(MimeType.WEBDAV_FOLDER);
                data.setKey(EncryptionUtils.encodeBytesToBase64String(key));

                decryptedFile.setEncrypted(data);
                decryptedFile.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(iv));
                // TODO not needed? decryptedFile.setAuthenticationTag(encryptedFile.authenticationTag);

                metadata.getFiles().put(encryptedFileName, decryptedFile);

                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                                                                                                        privateKey);
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);
                // upload metadata
                EncryptionUtils.uploadMetadata(parent,
                                               serializedFolderMetadata,
                                               token,
                                               client,
                                               metadataExists);
            } else {
                // revert to sane state in case of any error
                Log_OC.e(TAG, remotePath + " hasn't been created");
            }

            // unlock folder
            RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(parent, client, token);

            if (!unlockFolderResult.isSuccess()) {
                return unlockFolderResult;
            }

            return result;
        } catch (Exception e) {
            // TODO do better
            return new RemoteOperationResult(e);
        }
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
        } else { // Create directory on DB
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
