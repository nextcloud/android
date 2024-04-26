/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;
import com.owncloud.android.utils.theme.CapabilityUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import kotlin.Pair;

/**
 * Remote operation performing the removal of a remote encrypted file or folder
 */
public class RemoveRemoteEncryptedFileOperation extends RemoteOperation<Void> {
    private static final String TAG = RemoveRemoteEncryptedFileOperation.class.getSimpleName();
    private static final int REMOVE_READ_TIMEOUT = 30000;
    private static final int REMOVE_CONNECTION_TIMEOUT = 5000;
    private final String remotePath;
    private final OCFile parentFolder;
    private final User user;
    private final String fileName;
    private final Context context;
    private final boolean isFolder;

    /**
     * Constructor
     *
     * @param remotePath   RemotePath of the remote file or folder to remove from the server
     * @param parentFolder parent folder
     */
    RemoveRemoteEncryptedFileOperation(String remotePath,
                                       User user,
                                       Context context,
                                       String fileName,
                                       OCFile parentFolder,
                                       boolean isFolder) {
        this.remotePath = remotePath;
        this.user = user;
        this.fileName = fileName;
        this.context = context;
        this.parentFolder = parentFolder;
        this.isFolder = isFolder;
    }

    /**
     * Performs the remove operation.
     */
    @Override
    protected RemoteOperationResult<Void> run(OwnCloudClient client) {
        RemoteOperationResult<Void> result;
        DeleteMethod delete = null;
        String token = null;

        E2EVersion e2eVersion = CapabilityUtils.getCapability(context).getEndToEndEncryptionApiVersion();
        boolean isE2EVersionAtLeast2 = e2eVersion.compareTo(E2EVersion.V2_0) >= 0;

        try {
            token = EncryptionUtils.lockFolder(parentFolder, client);

            if (isE2EVersionAtLeast2) {
                Pair<RemoteOperationResult<Void>, DeleteMethod> deleteResult = deleteForV2(client, token);
                result = deleteResult.getFirst();
                delete = deleteResult.getSecond();
                return result;
            } else {
                return deleteForV1(client, token);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Remove " + remotePath + ": " + result.getLogMessage(), e);

        } finally {
            if (delete != null) {
                delete.releaseConnection();
            }

            if (token != null) {
                unlockFile(client, token,isE2EVersionAtLeast2);
            }
        }

        return result;
    }

    private void unlockFile(OwnCloudClient client, String token, boolean isE2EVersionAtLeast2) {
        RemoteOperationResult<Void> unlockFileOperationResult;

        if (isE2EVersionAtLeast2) {
            unlockFileOperationResult = EncryptionUtils.unlockFolder(parentFolder, client, token);
        } else {
            unlockFileOperationResult = EncryptionUtils.unlockFolderV1(parentFolder, client, token);
        }

        if (!unlockFileOperationResult.isSuccess()) {
            Log_OC.e(TAG, "Failed to unlock " + parentFolder.getLocalId());
        }
    }

    private Pair<RemoteOperationResult<Void>, DeleteMethod> deleteRemoteFile(OwnCloudClient client, String token) throws IOException {
        DeleteMethod delete = new DeleteMethod(client.getFilesDavUri(remotePath));
        delete.setQueryString(new NameValuePair[]{new NameValuePair(E2E_TOKEN, token)});
        int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);

        delete.getResponseBodyAsString();   // exhaust the response, although not interesting
        RemoteOperationResult<Void> result = new RemoteOperationResult<>(delete.succeeded() || status == HttpStatus.SC_NOT_FOUND, delete);
        Log_OC.i(TAG, "Remove " + remotePath + ": " + result.getLogMessage());

        return new Pair<>(result, delete);
    }

    private DecryptedFolderMetadataFileV1 getMetadataV1(ArbitraryDataProvider arbitraryDataProvider) throws NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        DecryptedFolderMetadataFileV1 metadata = new DecryptedFolderMetadataFileV1();
        metadata.setMetadata(new DecryptedMetadata());
        metadata.getMetadata().setVersion(1.2);
        metadata.getMetadata().setMetadataKeys(new HashMap<>());
        String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
        String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
        metadata.getMetadata().setMetadataKey(encryptedMetadataKey);
        return metadata;
    }

    private Pair<RemoteOperationResult<Void>, DeleteMethod> deleteForV2(OwnCloudClient client, String token) throws UploadException, IOException {
        EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
        Pair<Boolean, DecryptedFolderMetadataFile> pair = encryptionUtilsV2.retrieveMetadata(parentFolder, client, user, context);
        boolean metadataExists = pair.getFirst();
        DecryptedFolderMetadataFile metadata = pair.getSecond();

        Pair<RemoteOperationResult<Void>, DeleteMethod> deleteResult = deleteRemoteFile(client, token);
        RemoteOperationResult<Void> result = deleteResult.getFirst();
        DeleteMethod delete = deleteResult.getSecond();

        if (isFolder) {
            encryptionUtilsV2.removeFolderFromMetadata(fileName, metadata);
        } else {
            encryptionUtilsV2.removeFileFromMetadata(fileName, metadata);
        }

        encryptionUtilsV2.serializeAndUploadMetadata(parentFolder,
                                                     metadata,
                                                     token,
                                                     client,
                                                     metadataExists,
                                                     context,
                                                     user,
                                                     new FileDataStorageManager(user, context.getContentResolver()));

        return new Pair<>(result, delete);
    }

    private RemoteOperationResult<Void> deleteForV1(OwnCloudClient client, String token) throws NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, UploadException {
        //noinspection deprecation
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        DecryptedFolderMetadataFileV1 metadata = getMetadataV1(arbitraryDataProvider);
        Pair<RemoteOperationResult<Void>, DeleteMethod> deleteResult = deleteRemoteFile(client, token);

        String serializedMetadata;

        // check if we need metadataKeys
        if (metadata.getMetadata().getMetadataKey() != null) {
            serializedMetadata = EncryptionUtils.serializeJSON(metadata, true);
        } else {
            serializedMetadata = EncryptionUtils.serializeJSON(metadata);
        }

        EncryptionUtils.uploadMetadata(parentFolder,
                                       serializedMetadata,
                                       token,
                                       client,
                                       true,
                                       E2EVersion.V1_2,
                                       "",
                                       arbitraryDataProvider,
                                       user);

        return deleteResult.getFirst();
    }
}
