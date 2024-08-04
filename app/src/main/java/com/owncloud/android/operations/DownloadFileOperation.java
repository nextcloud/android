/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileExportUtils;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;

import static com.owncloud.android.utils.EncryptionUtils.decodeStringToBase64Bytes;

/**
 * Remote DownloadOperation performing the download of a file to an ownCloud server
 */
public class DownloadFileOperation extends RemoteOperation {
    private static final String TAG = DownloadFileOperation.class.getSimpleName();

    private User user;
    private OCFile file;
    private String behaviour;
    private String etag = "";
    private String activityName;
    private String packageName;
    private DownloadType downloadType;

    private final WeakReference<Context> context;
    private Set<OnDatatransferProgressListener> dataTransferListeners = new HashSet<>();
    private long modificationTimestamp;
    private DownloadFileRemoteOperation downloadOperation;

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    public DownloadFileOperation(User user,
                                 OCFile file,
                                 String behaviour,
                                 String activityName,
                                 String packageName,
                                 Context context,
                                 DownloadType downloadType) {
        if (user == null) {
            throw new IllegalArgumentException("Illegal null user in DownloadFileOperation " +
                                                   "creation");
        }
        if (file == null) {
            throw new IllegalArgumentException("Illegal null file in DownloadFileOperation " +
                                                   "creation");
        }

        this.user = user;
        this.file = file;
        this.behaviour = behaviour;
        this.activityName = activityName;
        this.packageName = packageName;
        this.context = new WeakReference<>(context);
        this.downloadType = downloadType;
    }

    public DownloadFileOperation(User user, OCFile file, Context context) {
        this(user, file, null, null, null, context, DownloadType.DOWNLOAD);
    }

    public boolean isMatching(String accountName, long fileId) {
        return getFile().getFileId() == fileId && getUser().getAccountName().equals(accountName);
    }

    public void cancelMatchingOperation(String accountName, long fileId) {
        if (isMatching(accountName, fileId)) {
            cancel();
        }
    }

    public String getSavePath() {
        if (file.getStoragePath() != null) {
            File parentFile = new File(file.getStoragePath()).getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            File path = new File(file.getStoragePath());  // re-downloads should be done over the original file
            if (path.canWrite() || parentFile != null && parentFile.canWrite()) {
                return path.getAbsolutePath();
            }
        }
        return FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), file);
    }

    public String getTmpPath() {
        return FileStorageUtils.getTemporalPath(user.getAccountName()) + file.getRemotePath();
    }

    public String getTmpFolder() {
        return FileStorageUtils.getTemporalPath(user.getAccountName());
    }

    public String getRemotePath() {
        return file.getRemotePath();
    }

    public String getMimeType() {
        String mimeType = file.getMimeType();
        if (TextUtils.isEmpty(mimeType)) {
            try {
                mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                        file.getRemotePath().substring(
                            file.getRemotePath().lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " +
                    file.getRemotePath());
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public long getSize() {
        return file.getFileLength();
    }

    public long getModificationTimestamp() {
        return modificationTimestamp > 0 ? modificationTimestamp : file.getModificationTimestamp();
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        /// perform the download
        synchronized(cancellationRequested) {
            if (cancellationRequested.get()) {
                return new RemoteOperationResult(new OperationCancelledException());
            }
        }

        Context operationContext = context.get();
        if (operationContext == null) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
        }

        RemoteOperationResult result;
        File newFile = null;
        boolean moved;

        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());

        String tmpFolder = getTmpFolder();

        downloadOperation = new DownloadFileRemoteOperation(file.getRemotePath(), tmpFolder);

        if (downloadType == DownloadType.DOWNLOAD) {
            Iterator<OnDatatransferProgressListener> listener = dataTransferListeners.iterator();
            while (listener.hasNext()) {
                downloadOperation.addDatatransferProgressListener(listener.next());
            }
        }

        result = downloadOperation.execute(client);



        if (result.isSuccess()) {
            modificationTimestamp = downloadOperation.getModificationTimestamp();
            etag = downloadOperation.getEtag();

            if (downloadType == DownloadType.DOWNLOAD) {
                newFile = new File(getSavePath());

                if (!newFile.getParentFile().exists() && !newFile.getParentFile().mkdirs()) {
                    Log_OC.e(TAG, "Unable to create parent folder " + newFile.getParentFile().getAbsolutePath());
                }
            }

            // decrypt file
            if (file.isEncrypted()) {
                FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(user, operationContext.getContentResolver());

                OCFile parent = fileDataStorageManager.getFileByEncryptedRemotePath(file.getParentRemotePath());

                Object object = EncryptionUtils.downloadFolderMetadata(parent,
                                                                       client,
                                                                       operationContext,
                                                                       user);

                if (object == null) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND);
                }

                String keyString;
                String nonceString;
                String authenticationTagString;
                if (object instanceof DecryptedFolderMetadataFile) {
                    DecryptedFile decryptedFile = ((DecryptedFolderMetadataFile) object)
                        .getMetadata()
                        .getFiles()
                        .get(file.getEncryptedFileName());

                    if (decryptedFile == null) {
                        return new RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND);
                    }

                    keyString = decryptedFile.getKey();
                    nonceString = decryptedFile.getNonce();
                    authenticationTagString = decryptedFile.getAuthenticationTag();
                } else {
                    com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile decryptedFile =
                        ((DecryptedFolderMetadataFileV1) object)
                            .getFiles()
                            .get(file.getEncryptedFileName());

                    if (decryptedFile == null) {
                        return new RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND);
                    }

                    keyString = decryptedFile.getEncrypted().getKey();
                    nonceString = decryptedFile.getInitializationVector();
                    authenticationTagString = decryptedFile.getAuthenticationTag();
                }

                byte[] key = decodeStringToBase64Bytes(keyString);
                byte[] iv = decodeStringToBase64Bytes(nonceString);

                try {
                    Cipher cipher = EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, key, iv);
                    EncryptionUtils.decryptFile(cipher, tmpFile, newFile, authenticationTagString, new ArbitraryDataProviderImpl(operationContext), user);
                } catch (Exception e) {
                    return new RemoteOperationResult(e);
                }
            }

            if (downloadType == DownloadType.DOWNLOAD && !file.isEncrypted()) {
                moved = tmpFile.renameTo(newFile);
                boolean isLastModifiedSet = newFile.setLastModified(file.getModificationTimestamp());
                Log_OC.d(TAG, "Last modified set: " + isLastModifiedSet);
                if (!moved) {
                    result = new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED);
                }
            } else if (downloadType == DownloadType.EXPORT) {
                new FileExportUtils().exportFile(file.getFileName(),
                                                 file.getMimeType(),
                                                 operationContext.getContentResolver(),
                                                 null,
                                                 tmpFile);
                if (!tmpFile.delete()) {
                    Log_OC.e(TAG, "Deletion of " + tmpFile.getAbsolutePath() + " failed!");
                }
            }
        }

        Log_OC.i(TAG, "Download of " + file.getRemotePath() + " to " + getSavePath() + ": " +
                result.getLogMessage());

        return result;
    }

    public void cancel() {
        cancellationRequested.set(true);   // atomic set; there is no need of synchronizing it
        if (downloadOperation != null) {
            downloadOperation.cancel();
        }
    }


    public void addDownloadDataTransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (dataTransferListeners) {
            dataTransferListeners.add(listener);
        }
    }

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (dataTransferListeners) {
            dataTransferListeners.remove(listener);
        }
    }

    public User getUser() {
        return this.user;
    }

    public OCFile getFile() {
        return this.file;
    }

    public String getBehaviour() {
        return this.behaviour;
    }

    public String getEtag() {
        return this.etag;
    }

    public String getActivityName() {
        return this.activityName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public DownloadType getDownloadType() {
        return downloadType;
    }
}
