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
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private Context context;
    private Set<OnDatatransferProgressListener> dataTransferListeners = new HashSet<>();
    private long modificationTimestamp;
    private DownloadFileRemoteOperation downloadOperation;

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    public DownloadFileOperation(User user, OCFile file, String behaviour, String activityName,
                                 String packageName, Context context) {
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
        this.context = context;
    }

    public DownloadFileOperation(User user, OCFile file, Context context) {
        this(user, file, null, null, null, context);
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

        RemoteOperationResult result;
        File newFile;
        boolean moved;

        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());

        String tmpFolder =  getTmpFolder();

        downloadOperation = new DownloadFileRemoteOperation(file.getRemotePath(), tmpFolder);
        Iterator<OnDatatransferProgressListener> listener = dataTransferListeners.iterator();
        while (listener.hasNext()) {
            downloadOperation.addDatatransferProgressListener(listener.next());
        }
        result = downloadOperation.execute(client);

        if (result.isSuccess()) {
            modificationTimestamp = downloadOperation.getModificationTimestamp();
            etag = downloadOperation.getEtag();
            newFile = new File(getSavePath());
            if (!newFile.getParentFile().exists() && !newFile.getParentFile().mkdirs()) {
                Log_OC.e(TAG, "Unable to create parent folder " + newFile.getParentFile().getAbsolutePath());
            }

            // decrypt file
            if (file.isEncrypted()) {
                FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(user, context.getContentResolver());

                OCFile parent = fileDataStorageManager.getFileByPath(file.getParentRemotePath());

                DecryptedFolderMetadata metadata = EncryptionUtils.downloadFolderMetadata(parent,
                                                                                          client,
                                                                                          context,
                                                                                          user.toPlatformAccount());

                if (metadata == null) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND);
                }
                byte[] key = EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles()
                        .get(file.getEncryptedFileName()).getEncrypted().getKey());
                byte[] iv = EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles()
                        .get(file.getEncryptedFileName()).getInitializationVector());
                byte[] authenticationTag = EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles()
                        .get(file.getEncryptedFileName()).getAuthenticationTag());

                try {
                    byte[] decryptedBytes = EncryptionUtils.decryptFile(tmpFile, key, iv, authenticationTag);

                    try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)) {
                        fileOutputStream.write(decryptedBytes);
                    }
                } catch (Exception e) {
                    return new RemoteOperationResult(e);
                }
            }
            moved = tmpFile.renameTo(newFile);
            newFile.setLastModified(file.getModificationTimestamp());
            if (!moved) {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED);
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


    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (dataTransferListeners) {
            dataTransferListeners.add(listener);
        }
    }

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (dataTransferListeners) {
            dataTransferListeners.remove(listener);
        }
    }

    @Deprecated
    public Account getAccount() {
        return this.user.toPlatformAccount();
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
}
