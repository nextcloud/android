/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.ProgressiveDataTransferer;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ChunkedUploadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.GetMetadataOperation;
import com.owncloud.android.lib.resources.files.LockFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.StoreMetadataOperation;
import com.owncloud.android.lib.resources.files.UnlockFileOperation;
import com.owncloud.android.lib.resources.files.UpdateMetadataOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.ConnectivityUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PowerUtils;
import com.owncloud.android.utils.UriUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Paths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Operation performing the update in the ownCloud server
 * of a file that was modified locally.
 */
public class UploadFileOperation extends SyncOperation {

    private static final String TAG = UploadFileOperation.class.getSimpleName();

    public static final int CREATED_BY_USER = 0;
    public static final int CREATED_AS_INSTANT_PICTURE = 1;
    public static final int CREATED_AS_INSTANT_VIDEO = 2;

    /**
     * OCFile which is to be uploaded.
     */
    private OCFile mFile;

    /**
     * Original OCFile which is to be uploaded in case file had to be renamed
     * (if forceOverwrite==false and remote file already exists).
     */
    private OCFile mOldFile;
    private String mRemotePath = null;
    private String mFolderUnlockToken;
    private boolean mChunked = false;
    private boolean mRemoteFolderToBeCreated = false;
    private boolean mForceOverwrite = false;
    private int mLocalBehaviour = FileUploader.LOCAL_BEHAVIOUR_COPY;
    private int mCreatedBy = CREATED_BY_USER;
    private boolean mOnWifiOnly = false;
    private boolean mWhileChargingOnly = false;
    private boolean mIgnoringPowerSaveMode = false;

    private boolean mWasRenamed = false;
    private long mOCUploadId = -1;
    /**
     * Local path to file which is to be uploaded (before any possible renaming or moving).
     */
    private String mOriginalStoragePath = null;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<>();
    private OnRenameListener mRenameUploadListener;

    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean mUploadStarted = new AtomicBoolean(false);

    private Context mContext;

    private UploadRemoteFileOperation mUploadOperation;

    protected RequestEntity mEntity = null;

    private Account mAccount;
    private OCUpload mUpload;
    private UploadsStorageManager uploadsStorageManager;

    private boolean encryptedAncestor;

    public static OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType) {

        // MIME type
        if (mimeType == null || mimeType.length() <= 0) {
            mimeType = MimeTypeUtil.getBestMimeTypeByFilename(localPath);
        }

        OCFile newFile = new OCFile(remotePath);
        newFile.setStoragePath(localPath);
        newFile.setLastSyncDateForProperties(0);
        newFile.setLastSyncDateForData(0);

        // size
        if (localPath != null && localPath.length() > 0) {
            File localFile = new File(localPath);
            newFile.setFileLength(localFile.length());
            newFile.setLastSyncDateForData(localFile.lastModified());
        } // don't worry about not assigning size, the problems with localPath
        // are checked when the UploadFileOperation instance is created


        newFile.setMimetype(mimeType);

        return newFile;
    }

    public UploadFileOperation(Account account,
                               OCFile file,
                               OCUpload upload,
                               boolean chunked,
                               boolean forceOverwrite,
                               int localBehaviour,
                               Context context,
                               boolean onWifiOnly,
                               boolean whileChargingOnly
    ) {
        if (account == null) {
            throw new IllegalArgumentException("Illegal NULL account in UploadFileOperation " + "creation");
        }
        if (upload == null) {
            throw new IllegalArgumentException("Illegal NULL file in UploadFileOperation creation");
        }
        if (upload.getLocalPath() == null || upload.getLocalPath().length() <= 0) {
            throw new IllegalArgumentException(
                    "Illegal file in UploadFileOperation; storage path invalid: "
                            + upload.getLocalPath());
        }

        mAccount = account;
        mUpload = upload;
        if (file == null) {
            mFile = obtainNewOCFileToUpload(
                    upload.getRemotePath(),
                    upload.getLocalPath(),
                    upload.getMimeType()
            );
        } else {
            mFile = file;
        }
        mOnWifiOnly = onWifiOnly;
        mWhileChargingOnly = whileChargingOnly;
        mRemotePath = upload.getRemotePath();
        mChunked = chunked;
        mForceOverwrite = forceOverwrite;
        mLocalBehaviour = localBehaviour;
        mOriginalStoragePath = mFile.getStoragePath();
        mContext = context;
        mOCUploadId = upload.getUploadId();
        mCreatedBy = upload.getCreadtedBy();
        mRemoteFolderToBeCreated = upload.isCreateRemoteFolder();
        // Ignore power save mode only if user explicitly created this upload
        mIgnoringPowerSaveMode = (mCreatedBy == CREATED_BY_USER);
        mFolderUnlockToken = upload.getFolderUnlockToken();
    }

    public boolean getIsWifiRequired() {
        return mOnWifiOnly;
    }

    public boolean getIsChargingRequired() {
        return mWhileChargingOnly;
    }

    public boolean getIsIgnoringPowerSaveMode() { return mIgnoringPowerSaveMode; }

    public Account getAccount() {
        return mAccount;
    }

    public String getFileName() {
        return (mFile != null) ? mFile.getFileName() : null;
    }

    public OCFile getFile() {
        return mFile;
    }

    /**
     * If remote file was renamed, return original OCFile which was uploaded. Is
     * null is file was not renamed.
     */
    public OCFile getOldFile() {
        return mOldFile;
    }

    public String getOriginalStoragePath() {
        return mOriginalStoragePath;
    }

    public String getStoragePath() {
        return mFile.getStoragePath();
    }

    public String getRemotePath() {
        return mFile.getRemotePath();
    }

    public String getDecryptedRemotePath() {
        return mFile.getDecryptedRemotePath();
    }

    public String getMimeType() {
        return mFile.getMimetype();
    }

    public int getLocalBehaviour() {
        return mLocalBehaviour;
    }

    public void setRemoteFolderToBeCreated() {
        mRemoteFolderToBeCreated = true;
    }

    public boolean wasRenamed() {
        return mWasRenamed;
    }

    public void setCreatedBy(int createdBy) {
        mCreatedBy = createdBy;
        if (createdBy < CREATED_BY_USER || CREATED_AS_INSTANT_VIDEO < createdBy) {
            mCreatedBy = CREATED_BY_USER;
        }
    }

    public int getCreatedBy() {
        return mCreatedBy;
    }

    public boolean isInstantPicture() {
        return mCreatedBy == CREATED_AS_INSTANT_PICTURE;
    }

    public boolean isInstantVideo() {
        return mCreatedBy == CREATED_AS_INSTANT_VIDEO;
    }

    public void setOCUploadId(long id) {
        mOCUploadId = id;
    }

    public long getOCUploadId() {
        return mOCUploadId;
    }

    public Set<OnDatatransferProgressListener> getDataTransferListeners() {
        return mDataTransferListeners;
    }

    public void addDataTransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer) mEntity).addDatatransferProgressListener(listener);
        }
        if (mUploadOperation != null) {
            mUploadOperation.addDatatransferProgressListener(listener);
        }
    }

    public void removeDataTransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer) mEntity).removeDatatransferProgressListener(listener);
        }
        if (mUploadOperation != null) {
            mUploadOperation.removeDatatransferProgressListener(listener);
        }
    }

    public void addRenameUploadListener(OnRenameListener listener) {
        mRenameUploadListener = listener;
    }

    public boolean isChunkedUploadSupported() {
        return mChunked;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected RemoteOperationResult run(OwnCloudClient client) {
        mCancellationRequested.set(false);
        mUploadStarted.set(true);

        uploadsStorageManager = new UploadsStorageManager(mContext.getContentResolver(), mContext);

        for (OCUpload ocUpload : uploadsStorageManager.getAllStoredUploads()) {
            if (ocUpload.getUploadId() == getOCUploadId()) {
                ocUpload.setFileSize(0);
                uploadsStorageManager.updateUpload(ocUpload);
                break;
            }
        }

        String remoteParentPath = new File(getRemotePath()).getParent();
        remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ?
                remoteParentPath : remoteParentPath + OCFile.PATH_SEPARATOR;

        OCFile parent = getStorageManager().getFileByPath(remoteParentPath);

        // in case of a fresh upload with subfolder, where parent does not exist yet
        if (parent == null && (mFolderUnlockToken == null || mFolderUnlockToken.isEmpty())) {
            // try to create folder
            RemoteOperationResult result = grantFolderExistence(remoteParentPath, client);

            if (!result.isSuccess()) {
                return result;
            }

            parent = getStorageManager().getFileByPath(remoteParentPath);

            if (parent == null) {
                return new RemoteOperationResult(false, "Parent folder not found", HttpStatus.SC_NOT_FOUND);
            }
        }

        // parent file is not null anymore:
        // - it was created on fresh upload or
        // - resume of encrypted upload, then parent file exists already as unlock is only for direct parent
        
        mFile.setParentId(parent.getFileId());

        // try to unlock folder with stored token, e.g. when upload needs to be resumed or app crashed
        // the parent folder should exist as it is a resume of a broken upload
        if (mFolderUnlockToken != null && !mFolderUnlockToken.isEmpty()) {
            UnlockFileOperation unlockFileOperation = new UnlockFileOperation(parent.getLocalId(), mFolderUnlockToken);
            RemoteOperationResult unlockFileOperationResult = unlockFileOperation.execute(client, true);

            if (!unlockFileOperationResult.isSuccess()) {
                return unlockFileOperationResult;
            }
        }

        // check if any parent is encrypted
        encryptedAncestor = FileStorageUtils.checkEncryptionStatus(parent, getStorageManager());
        mFile.setEncrypted(encryptedAncestor);

        if (encryptedAncestor) {
            Log_OC.d(TAG, "encrypted upload");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return encryptedUpload(client, parent);
            } else {
                Log_OC.e(TAG, "Encrypted upload on old Android API");
                return new RemoteOperationResult(ResultCode.OLD_ANDROID_API);
            }
        } else {
            Log_OC.d(TAG, "normal upload");
            return normalUpload(client);
        }
    }

    @SuppressLint("AndroidLintUseSparseArrays") // gson cannot handle sparse arrays easily, therefore use hashmap
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private RemoteOperationResult encryptedUpload(OwnCloudClient client, OCFile parentFile) {
        RemoteOperationResult result = null;
        File temporalFile = null;
        File originalFile = new File(mOriginalStoragePath);
        File expectedFile = null;
        FileLock fileLock = null;
        long size;

        boolean metadataExists = false;
        String token = null;

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        String privateKey = arbitraryDataProvider.getValue(getAccount().name, EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(getAccount().name, EncryptionUtils.PUBLIC_KEY);

        try {
            // check conditions 
            result = checkConditions(originalFile);

            if (result != null) {
                return result;
            }
            /***** E2E *****/

            // Lock folder
            LockFileOperation lockFileOperation = new LockFileOperation(parentFile.getLocalId());
            RemoteOperationResult lockFileOperationResult = lockFileOperation.execute(client, true);

            if (lockFileOperationResult.isSuccess()) {
                token = (String) lockFileOperationResult.getData().get(0);
                // immediately store it 
                mUpload.setFolderUnlockToken(token);
                uploadsStorageManager.updateUpload(mUpload);
            } else if (lockFileOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                throw new Exception("Forbidden! Please try again later.)");
            } else {
                throw new Exception("Unknown error!");
            }

            // Update metadata
            GetMetadataOperation getMetadataOperation = new GetMetadataOperation(parentFile.getLocalId());
            RemoteOperationResult getMetadataOperationResult = getMetadataOperation.execute(client, true);

            DecryptedFolderMetadata metadata;

            if (getMetadataOperationResult.isSuccess()) {
                metadataExists = true;

                // decrypt metadata
                String serializedEncryptedMetadata = (String) getMetadataOperationResult.getData().get(0);


                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                        serializedEncryptedMetadata, new TypeToken<EncryptedFolderMetadata>() {
                        });

                metadata = EncryptionUtils.decryptFolderMetaData(encryptedFolderMetadata, privateKey);

            } else if (getMetadataOperationResult.getHttpCode() == HttpStatus.SC_NOT_FOUND) {
                // new metadata
                metadata = new DecryptedFolderMetadata();
                metadata.setMetadata(new DecryptedFolderMetadata.Metadata());
                metadata.getMetadata().setMetadataKeys(new HashMap<>());
                String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
                String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
                metadata.getMetadata().getMetadataKeys().put(0, encryptedMetadataKey);
            } else {
                // TODO error
                throw new Exception("something wrong");
            }

            /***** E2E *****/

            // check name collision
            checkNameCollision(client, metadata, parentFile.isEncrypted());

            String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
            expectedFile = new File(expectedPath);

            result = copyFile(originalFile, expectedPath);
            if (result != null) {
                return result;
            }

            // Get the last modification date of the file from the file system
            Long timeStampLong = originalFile.lastModified() / 1000;
            String timeStamp = timeStampLong.toString();

            /***** E2E *****/

            // Key, always generate new one
            byte[] key = EncryptionUtils.generateKey();

            // IV, always generate new one
            byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);

            EncryptionUtils.EncryptedFile encryptedFile = EncryptionUtils.encryptFile(mFile, key, iv);

            // new random file name, check if it exists in metadata
            String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

            while (metadata.getFiles().get(encryptedFileName) != null) {
                encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
            }

            mFile.setEncryptedFileName(encryptedFileName);

            File encryptedTempFile = File.createTempFile("encFile", encryptedFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(encryptedTempFile);
            fileOutputStream.write(encryptedFile.encryptedBytes);
            fileOutputStream.close();

            /***** E2E *****/

            FileChannel channel = null;
            try {
                channel = new RandomAccessFile(mFile.getStoragePath(), "rw").getChannel();
                fileLock = channel.tryLock();
            } catch (FileNotFoundException e) {
                // this basically means that the file is on SD card
                // try to copy file to temporary dir if it doesn't exist
                String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
                mFile.setStoragePath(temporalPath);
                temporalFile = new File(temporalPath);

                Files.deleteIfExists(Paths.get(temporalPath));
                result = copy(originalFile, temporalFile);

                if (result == null) {
                    if (temporalFile.length() == originalFile.length()) {
                        channel = new RandomAccessFile(temporalFile.getAbsolutePath(), "rw").getChannel();
                        fileLock = channel.tryLock();
                    } else {
                        result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
                    }
                }
            }

            try {
                size = channel.size();
            } catch (IOException e1) {
                size = new File(mFile.getStoragePath()).length();
            }

            for (OCUpload ocUpload : uploadsStorageManager.getAllStoredUploads()) {
                if (ocUpload.getUploadId() == getOCUploadId()) {
                    ocUpload.setFileSize(size);
                    uploadsStorageManager.updateUpload(ocUpload);
                    break;
                }
            }

            /// perform the upload
            if (mChunked && (size > ChunkedUploadRemoteFileOperation.CHUNK_SIZE)) {
                mUploadOperation = new ChunkedUploadRemoteFileOperation(mContext, encryptedTempFile.getAbsolutePath(),
                        mFile.getParentRemotePath() + encryptedFileName, mFile.getMimetype(),
                        mFile.getEtagInConflict(), timeStamp);
            } else {
                mUploadOperation = new UploadRemoteFileOperation(encryptedTempFile.getAbsolutePath(),
                        mFile.getParentRemotePath() + encryptedFileName, mFile.getMimetype(),
                        mFile.getEtagInConflict(), timeStamp);
            }

            Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            result = mUploadOperation.execute(client, true);

            /// move local temporal file or original file to its corresponding
            // location in the Nextcloud local folder
            if (!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_PRECONDITION_FAILED) {
                result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
            }

            if (result.isSuccess()) {
                // upload metadata
                DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
                DecryptedFolderMetadata.Data data = new DecryptedFolderMetadata.Data();
                data.setFilename(mFile.getFileName());
                data.setMimetype(mFile.getMimetype());
                data.setKey(EncryptionUtils.encodeBytesToBase64String(key));

                decryptedFile.setEncrypted(data);
                decryptedFile.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(iv));
                decryptedFile.setAuthenticationTag(encryptedFile.authenticationTag);

                metadata.getFiles().put(encryptedFileName, decryptedFile);

                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                        privateKey);
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

                // upload metadata
                RemoteOperationResult uploadMetadataOperationResult;
                if (metadataExists) {
                    // update metadata
                    UpdateMetadataOperation storeMetadataOperation = new UpdateMetadataOperation(parentFile.getLocalId(),
                            serializedFolderMetadata, token);
                    uploadMetadataOperationResult = storeMetadataOperation.execute(client, true);
                } else {
                    // store metadata
                    StoreMetadataOperation storeMetadataOperation = new StoreMetadataOperation(parentFile.getLocalId(),
                            serializedFolderMetadata);
                    uploadMetadataOperationResult = storeMetadataOperation.execute(client, true);
                }

                if (!uploadMetadataOperationResult.isSuccess()) {
                    throw new Exception();
                }
            }
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, mFile.getStoragePath() + " not exists anymore");
            result = new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        } catch (OverlappingFileLockException e) {
            Log_OC.d(TAG, "Overlapping file lock exception");
            result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
        } finally {
            mUploadStarted.set(false);

            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Failed to unlock file with path " + mFile.getStoragePath());
                }
            }

            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }
            if (result == null) {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }

            if (result.isSuccess()) {
                Log_OC.i(TAG, "Upload of " + mFile.getStoragePath() + " to " + mFile.getRemotePath() + ": " +
                        result.getLogMessage());
            } else {
                if (result.getException() != null) {
                    if (result.isCancelled()) {
                        Log_OC.w(TAG, "Upload of " + mFile.getStoragePath() + " to " + mFile.getRemotePath() +
                                ": " + result.getLogMessage());
                    } else {
                        Log_OC.e(TAG, "Upload of " + mFile.getStoragePath() + " to " + mFile.getRemotePath() +
                                ": " + result.getLogMessage(), result.getException());
                    }

                } else {
                    Log_OC.e(TAG, "Upload of " + mFile.getStoragePath() + " to " + mFile.getRemotePath() +
                            ": " + result.getLogMessage());
                }
            }
        }

        if (result.isSuccess()) {
            handleSuccessfulUpload(temporalFile, expectedFile, originalFile, client);
            RemoteOperationResult unlockFolderResult = unlockFolder(parentFile, client, token);

            if (!unlockFolderResult.isSuccess()) {
                return unlockFolderResult;
            }

        } else if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            getStorageManager().saveConflict(mFile, mFile.getEtagInConflict());
        }

        return result;
    }

    private RemoteOperationResult unlockFolder(OCFile parentFolder, OwnCloudClient client, String token) {
        if (token != null) {
            UnlockFileOperation unlockFileOperation = new UnlockFileOperation(parentFolder.getLocalId(), token);
            RemoteOperationResult unlockFileOperationResult = unlockFileOperation.execute(client, true);

            return unlockFileOperationResult;
        } else
            return new RemoteOperationResult(new Exception("No token available"));
    }

    private RemoteOperationResult checkConditions(File originalFile) {
        RemoteOperationResult remoteOperationResult = null;

        // check that internet is not behind walled garden
        if (Device.getNetworkType(mContext).equals(JobRequest.NetworkType.ANY) ||
                ConnectivityUtils.isInternetWalled(mContext)) {
            remoteOperationResult =  new RemoteOperationResult(ResultCode.NO_NETWORK_CONNECTION);
        }

        // check that connectivity conditions are met and delays the upload otherwise
        if (mOnWifiOnly && !Device.getNetworkType(mContext).equals(JobRequest.NetworkType.UNMETERED)) {
            Log_OC.d(TAG, "Upload delayed until WiFi is available: " + getRemotePath());
            remoteOperationResult = new RemoteOperationResult(ResultCode.DELAYED_FOR_WIFI);
        }

        // check if charging conditions are met and delays the upload otherwise
        if (mWhileChargingOnly && (!Device.getBatteryStatus(mContext).isCharging() && Device.getBatteryStatus(mContext)
                .getBatteryPercent() < 1)) {
            Log_OC.d(TAG, "Upload delayed until the device is charging: " + getRemotePath());
            remoteOperationResult =  new RemoteOperationResult(ResultCode.DELAYED_FOR_CHARGING);
        }

        // check that device is not in power save mode
        if (!mIgnoringPowerSaveMode && PowerUtils.isPowerSaveMode(mContext)) {
            Log_OC.d(TAG, "Upload delayed because device is in power save mode: " + getRemotePath());
            remoteOperationResult =  new RemoteOperationResult(ResultCode.DELAYED_IN_POWER_SAVE_MODE);
        }

        // check if the file continues existing before schedule the operation
        if (!originalFile.exists()) {
            Log_OC.d(TAG, mOriginalStoragePath + " not exists anymore");
            remoteOperationResult =  new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        }

        return remoteOperationResult;
    }

    private RemoteOperationResult normalUpload(OwnCloudClient client) {
        RemoteOperationResult result = null;
        File temporalFile = null;
        File originalFile = new File(mOriginalStoragePath);
        File expectedFile = null;
        FileLock fileLock = null;
        long size = 0;

        try {
            // check conditions
            result = checkConditions(originalFile);

            if (result != null) {
                return result;
            }

            // check name collision
            checkNameCollision(client, null, false);

            String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
            expectedFile = new File(expectedPath);

            result = copyFile(originalFile, expectedPath);
            if (result != null) {
                return result;
            }

            // Get the last modification date of the file from the file system
            Long timeStampLong = originalFile.lastModified() / 1000;
            String timeStamp = timeStampLong.toString();

            FileChannel channel = null;
            try {
                channel = new RandomAccessFile(mFile.getStoragePath(), "rw").getChannel();
                fileLock = channel.tryLock();
            } catch (FileNotFoundException e) {
                // this basically means that the file is on SD card
                // try to copy file to temporary dir if it doesn't exist
                String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
                mFile.setStoragePath(temporalPath);
                temporalFile = new File(temporalPath);

                Files.deleteIfExists(Paths.get(temporalPath));
                result = copy(originalFile, temporalFile);

                if (result == null) {
                    if (temporalFile.length() == originalFile.length()) {
                        channel = new RandomAccessFile(temporalFile.getAbsolutePath(), "rw").getChannel();
                        fileLock = channel.tryLock();
                    } else {
                        result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
                    }
                }
            }

            try {
                size = channel.size();
            } catch (IOException e1) {
                size = new File(mFile.getStoragePath()).length();
            }

            for (OCUpload ocUpload : uploadsStorageManager.getAllStoredUploads()) {
                if (ocUpload.getUploadId() == getOCUploadId()) {
                    ocUpload.setFileSize(size);
                    uploadsStorageManager.updateUpload(ocUpload);
                    break;
                }
            }

            // perform the upload
            if (mChunked && (size > ChunkedUploadRemoteFileOperation.CHUNK_SIZE)) {
                mUploadOperation = new ChunkedUploadRemoteFileOperation(mContext, mFile.getStoragePath(),
                        mFile.getRemotePath(), mFile.getMimetype(), mFile.getEtagInConflict(), timeStamp);
            } else {
                mUploadOperation = new UploadRemoteFileOperation(mFile.getStoragePath(),
                        mFile.getRemotePath(), mFile.getMimetype(), mFile.getEtagInConflict(), timeStamp);
            }

            Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            if (result == null || result.isSuccess() && mUploadOperation != null) {
                result = mUploadOperation.execute(client, mFile.isEncrypted());

                /// move local temporal file or original file to its corresponding
                // location in the Nextcloud local folder
                if (!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_PRECONDITION_FAILED) {
                    result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
                }
            }
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, mOriginalStoragePath + " not exists anymore");
            result = new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        } catch (OverlappingFileLockException e) {
            Log_OC.d(TAG, "Overlapping file lock exception");
            result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
        } finally {
            mUploadStarted.set(false);

            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Failed to unlock file with path " + mOriginalStoragePath);
                }
            }

            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }

            if (result == null) {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }

            if (result.isSuccess()) {
                Log_OC.i(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath + ": " +
                        result.getLogMessage());
            } else {
                if (result.getException() != null) {
                    if (result.isCancelled()) {
                        Log_OC.w(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                                ": " + result.getLogMessage());
                    } else {
                        Log_OC.e(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                                ": " + result.getLogMessage(), result.getException());
                    }
                } else {
                    Log_OC.e(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                            ": " + result.getLogMessage());
                }
            }
        }

        if (result.isSuccess()) {
            handleSuccessfulUpload(temporalFile, expectedFile, originalFile, client);
        } else if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            getStorageManager().saveConflict(mFile, mFile.getEtagInConflict());
        }

        return result;
    }

    private RemoteOperationResult copyFile(File originalFile, String expectedPath) throws OperationCancelledException,
            IOException {
        RemoteOperationResult result = null;

        if (mLocalBehaviour == FileUploader.LOCAL_BEHAVIOUR_COPY && !mOriginalStoragePath.equals(expectedPath)) {
            String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
            mFile.setStoragePath(temporalPath);
            File temporalFile = new File(temporalPath);

            result = copy(originalFile, temporalFile);
        }

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        return result;
    }

    private void checkNameCollision(OwnCloudClient client, DecryptedFolderMetadata metadata, boolean encrypted)
            throws OperationCancelledException {
        /// automatic rename of file to upload in case of name collision in server
        Log_OC.d(TAG, "Checking name collision in server");
        if (!mForceOverwrite) {
            String remotePath = getAvailableRemotePath(client, mRemotePath, metadata, encrypted);
            mWasRenamed = !remotePath.equals(mRemotePath);
            if (mWasRenamed) {
                createNewOCFile(remotePath);
                Log_OC.d(TAG, "File renamed as " + remotePath);
            }
            mRemotePath = remotePath;
            mRenameUploadListener.onRenameUpload();
        }

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }
    }

    private void handleSuccessfulUpload(File temporalFile, File expectedFile, File originalFile,
                                        OwnCloudClient client) {
        switch (mLocalBehaviour) {
            case FileUploader.LOCAL_BEHAVIOUR_FORGET:
            default:
                String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
                if (mOriginalStoragePath.equals(temporalPath)) {
                    // delete local file is was pre-copied in temporary folder (see .ui.helpers.UriUploader)
                    temporalFile = new File(temporalPath);
                    temporalFile.delete();
                }
                mFile.setStoragePath("");
                saveUploadedFile(client);
                break;

            case FileUploader.LOCAL_BEHAVIOUR_DELETE:
                Log_OC.d(TAG, "Delete source file");

                originalFile.delete();
                getStorageManager().deleteFileInMediaScan(originalFile.getAbsolutePath());
                saveUploadedFile(client);
                break;

            case FileUploader.LOCAL_BEHAVIOUR_COPY:
                if (temporalFile != null) {
                    try {
                        move(temporalFile, expectedFile);
                    } catch (IOException e) {
                        Log_OC.e(TAG, e.getMessage());
                    }
                }
                mFile.setStoragePath(expectedFile.getAbsolutePath());
                saveUploadedFile(client);
                FileDataStorageManager.triggerMediaScan(expectedFile.getAbsolutePath());
                break;

            case FileUploader.LOCAL_BEHAVIOUR_MOVE:
                String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
                File newFile = new File(expectedPath);

                try {
                    move(originalFile, newFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getStorageManager().deleteFileInMediaScan(originalFile.getAbsolutePath());
                mFile.setStoragePath(newFile.getAbsolutePath());
                saveUploadedFile(client);
                FileDataStorageManager.triggerMediaScan(newFile.getAbsolutePath());
                break;
        }
    }

    /**
     * Checks the existence of the folder where the current file will be uploaded both
     * in the remote server and in the local database.
     * <p/>
     * If the upload is set to enforce the creation of the folder, the method tries to
     * create it both remote and locally.
     *
     * @param pathToGrant Full remote path whose existence will be granted.
     * @return An {@link OCFile} instance corresponding to the folder where the file
     * will be uploaded.
     */
    private RemoteOperationResult grantFolderExistence(String pathToGrant, OwnCloudClient client) {
        RemoteOperation operation = new ExistenceCheckRemoteOperation(pathToGrant, mContext, false);
        RemoteOperationResult result = operation.execute(client, true);
        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND && mRemoteFolderToBeCreated) {
            SyncOperation syncOp = new CreateFolderOperation(pathToGrant, true);
            result = syncOp.execute(client, getStorageManager());
        }
        if (result.isSuccess()) {
            OCFile parentDir = getStorageManager().getFileByPath(pathToGrant);
            if (parentDir == null) {
                parentDir = createLocalFolder(pathToGrant);
            }
            if (parentDir != null) {
                result = new RemoteOperationResult(ResultCode.OK);
            } else {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }
        }
        return result;
    }

    private OCFile createLocalFolder(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ?
                parentPath : parentPath + OCFile.PATH_SEPARATOR;
        OCFile parent = getStorageManager().getFileByPath(parentPath);
        if (parent == null) {
            parent = createLocalFolder(parentPath);
        }
        if (parent != null) {
            OCFile createdFolder = new OCFile(remotePath);
            createdFolder.setMimetype(MimeType.DIRECTORY);
            createdFolder.setParentId(parent.getFileId());
            getStorageManager().saveFile(createdFolder);
            return createdFolder;
        }
        return null;
    }


    /**
     * Create a new OCFile mFile with new remote path. This is required if forceOverwrite==false.
     * New file is stored as mFile, original as mOldFile.
     *
     * @param newRemotePath new remote path
     */
    private void createNewOCFile(String newRemotePath) {
        // a new OCFile instance must be created for a new remote path
        OCFile newFile = new OCFile(newRemotePath);
        newFile.setCreationTimestamp(mFile.getCreationTimestamp());
        newFile.setFileLength(mFile.getFileLength());
        newFile.setMimetype(mFile.getMimetype());
        newFile.setModificationTimestamp(mFile.getModificationTimestamp());
        newFile.setModificationTimestampAtLastSyncForData(
                mFile.getModificationTimestampAtLastSyncForData()
        );
        newFile.setEtag(mFile.getEtag());
        newFile.setAvailableOffline(mFile.isAvailableOffline());
        newFile.setLastSyncDateForProperties(mFile.getLastSyncDateForProperties());
        newFile.setLastSyncDateForData(mFile.getLastSyncDateForData());
        newFile.setStoragePath(mFile.getStoragePath());
        newFile.setParentId(mFile.getParentId());
        mOldFile = mFile;
        mFile = newFile;
    }

    /**
     * Checks if remotePath does not exist in the server and returns it, or adds
     * a suffix to it in order to avoid the server file is overwritten.
     *
     * @param client     OwnCloud client
     * @param remotePath remote path of the file
     * @param metadata   metadata of encrypted folder
     * @return new remote path
     */
    private String getAvailableRemotePath(OwnCloudClient client, String remotePath, DecryptedFolderMetadata metadata,
                                          boolean encrypted) {
        boolean check = existsFile(client, remotePath, metadata, encrypted);
        if (!check) {
            return remotePath;
        }

        int pos = remotePath.lastIndexOf('.');
        String suffix;
        String extension = "";
        String remotePathWithoutExtension = "";
        if (pos >= 0) {
            extension = remotePath.substring(pos + 1);
            remotePathWithoutExtension = remotePath.substring(0, pos);
        }
        int count = 2;
        do {
            suffix = " (" + count + ")";
            if (pos >= 0) {
                check = existsFile(client, remotePathWithoutExtension + suffix + "." + extension, metadata, encrypted);
            } else {
                check = existsFile(client, remotePath + suffix, metadata, encrypted);
            }
            count++;
        } while (check);

        if (pos >= 0) {
            return remotePathWithoutExtension + suffix + "." + extension;
        } else {
            return remotePath + suffix;
        }
    }

    private boolean existsFile(OwnCloudClient client, String remotePath, DecryptedFolderMetadata metadata,
                               boolean encrypted) {
        if (encrypted) {
            String fileName = new File(remotePath).getName();

            for (DecryptedFolderMetadata.DecryptedFile file : metadata.getFiles().values()) {
                if (file.getEncrypted().getFilename().equalsIgnoreCase(fileName)) {
                    return true;
                }
            }

            return false;
        } else {
            ExistenceCheckRemoteOperation existsOperation = new ExistenceCheckRemoteOperation(remotePath, mContext,
                    false);
            RemoteOperationResult result = existsOperation.execute(client);
            return result.isSuccess();
        }
    }

    /**
     * Allows to cancel the actual upload operation. If actual upload operating
     * is in progress it is cancelled, if upload preparation is being performed
     * upload will not take place.
     */
    public void cancel() {
        if (mUploadOperation == null) {
            if (mUploadStarted.get()) {
                Log_OC.d(TAG, "Cancelling upload during upload preparations.");
                mCancellationRequested.set(true);
            } else {
                Log_OC.e(TAG, "No upload in progress. This should not happen.");
            }
        } else {
            Log_OC.d(TAG, "Cancelling upload during actual upload operation.");
            mUploadOperation.cancel();
        }
    }

    /**
     * As soon as this method return true, upload can be cancel via cancel().
     */
    public boolean isUploadInProgress() {
        return mUploadStarted.get();

    }

    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on {@link RemoteOperationResult},
     * TODO     use Exceptions instead
     *
     * @param sourceFile Source file to copy.
     * @param targetFile Target location to copy the file.
     * @return {@link RemoteOperationResult}
     * @throws IOException exception if file cannot be accessed
     */
    private RemoteOperationResult copy(File sourceFile, File targetFile) throws IOException {
        Log_OC.d(TAG, "Copying local file");

        RemoteOperationResult result = null;

        if (FileStorageUtils.getUsableSpace(mAccount.name) < sourceFile.length()) {
            result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_FULL);
            return result;  // error condition when the file should be copied

        } else {
            Log_OC.d(TAG, "Creating temporal folder");
            File temporalParent = targetFile.getParentFile();
            temporalParent.mkdirs();
            if (!temporalParent.isDirectory()) {
                throw new IOException(
                        "Unexpected error: parent directory could not be created");
            }
            Log_OC.d(TAG, "Creating temporal file");
            targetFile.createNewFile();
            if (!targetFile.isFile()) {
                throw new IOException(
                        "Unexpected error: target file could not be created");
            }

            Log_OC.d(TAG, "Copying file contents");
            InputStream in = null;
            OutputStream out = null;

            try {
                if (!mOriginalStoragePath.equals(targetFile.getAbsolutePath())) {
                    // In case document provider schema as 'content://'
                    if (mOriginalStoragePath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {
                        Uri uri = Uri.parse(mOriginalStoragePath);
                        in = mContext.getContentResolver().openInputStream(uri);
                    } else {
                        in = new FileInputStream(sourceFile);
                    }
                    out = new FileOutputStream(targetFile);
                    int nRead;
                    byte[] buf = new byte[4096];
                    while (!mCancellationRequested.get() &&
                            (nRead = in.read(buf)) > -1) {
                        out.write(buf, 0, nRead);
                    }
                    out.flush();

                } // else: weird but possible situation, nothing to copy

                if (mCancellationRequested.get()) {
                    result = new RemoteOperationResult(new OperationCancelledException());
                    return result;
                }

            } catch (Exception e) {
                result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_COPIED);
                return result;

            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    Log_OC.d(TAG, "Weird exception while closing input stream for " +
                            mOriginalStoragePath + " (ignoring)", e);
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    Log_OC.d(TAG, "Weird exception while closing output stream for " +
                            targetFile.getAbsolutePath() + " (ignoring)", e);
                }
            }
        }
        return result;
    }


    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on {@link RemoteOperationResult},
     * TODO     use Exceptions instead
     * <p>
     * TODO refactor both this and 'copy' in a single method
     *
     * @param sourceFile Source file to move.
     * @param targetFile Target location to move the file.
     * @return {@link RemoteOperationResult} result from remote operation
     * @throws IOException exception if file cannot be read/wrote
     */
    private void move(File sourceFile, File targetFile) throws IOException {

        if (!targetFile.equals(sourceFile)) {
            File expectedFolder = targetFile.getParentFile();
            expectedFolder.mkdirs();

            if (expectedFolder.isDirectory()) {
                if (!sourceFile.renameTo(targetFile)) {
                    // try to copy and then delete
                    targetFile.createNewFile();
                    FileChannel inChannel = new FileInputStream(sourceFile).getChannel();
                    FileChannel outChannel = new FileOutputStream(targetFile).getChannel();
                    try {
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                        sourceFile.delete();
                    } catch (Exception e) {
                        mFile.setStoragePath(""); // forget the local file
                        // by now, treat this as a success; the file was uploaded
                        // the best option could be show a warning message
                    } finally {
                        if (inChannel != null) {
                            inChannel.close();
                        }
                        if (outChannel != null) {
                            outChannel.close();
                        }
                    }
                }

            } else {
                mFile.setStoragePath("");
            }
        }
    }

    /**
     * Saves a OC File after a successful upload.
     * <p>
     * A PROPFIND is necessary to keep the props in the local database
     * synchronized with the server, specially the modification time and Etag
     * (where available)
     */
    private void saveUploadedFile(OwnCloudClient client) {
        OCFile file = mFile;
        if (file.fileExists()) {
            file = getStorageManager().getFileById(file.getFileId());
        }
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForData(syncDate);

        // new PROPFIND to keep data consistent with server
        // in theory, should return the same we already have
        // TODO from the appropriate OC server version, get data from last PUT response headers, instead
        // TODO     of a new PROPFIND; the latter may fail, specially for chunked uploads
        String path;
        if (encryptedAncestor) {
            path = file.getParentRemotePath() + mFile.getEncryptedFileName();
        } else {
            path = getRemotePath();
        }

        ReadRemoteFileOperation operation = new ReadRemoteFileOperation(path);
        RemoteOperationResult result = operation.execute(client, mFile.isEncrypted());
        if (result.isSuccess()) {
            updateOCFile(file, (RemoteFile) result.getData().get(0));
            file.setLastSyncDateForProperties(syncDate);
        } else {
            Log_OC.e(TAG, "Error reading properties of file after successful upload; this is gonna hurt...");
        }

        if (mWasRenamed) {
            OCFile oldFile = getStorageManager().getFileByPath(mOldFile.getRemotePath());
            if (oldFile != null) {
                oldFile.setStoragePath(null);
                getStorageManager().saveFile(oldFile);
                getStorageManager().saveConflict(oldFile, null);
            }
            // else: it was just an automatic renaming due to a name
            // coincidence; nothing else is needed, the storagePath is right
            // in the instance returned by mCurrentUpload.getFile()
        }
        file.setNeedsUpdateThumbnail(true);
        getStorageManager().saveFile(file);
        getStorageManager().saveConflict(file, null);

        FileDataStorageManager.triggerMediaScan(file.getStoragePath());

        // generate new Thumbnail
        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                new ThumbnailsCacheManager.ThumbnailGenerationTask(getStorageManager(), mAccount);
        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, file.getRemoteId()));
    }

    private void updateOCFile(OCFile file, RemoteFile remoteFile) {
        file.setCreationTimestamp(remoteFile.getCreationTimestamp());
        file.setFileLength(remoteFile.getLength());
        file.setMimetype(remoteFile.getMimeType());
        file.setModificationTimestamp(remoteFile.getModifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(remoteFile.getModifiedTimestamp());
        file.setEtag(remoteFile.getEtag());
        file.setRemoteId(remoteFile.getRemoteId());
    }

    public interface OnRenameListener {

        void onRenameUpload();
    }

}
