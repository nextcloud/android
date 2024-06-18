/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFile;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.ProgressiveDataTransfer;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.operations.e2e.E2EClientData;
import com.owncloud.android.operations.e2e.E2EData;
import com.owncloud.android.operations.e2e.E2EFiles;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.EncryptionUtilsV2;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.FileUtil;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.UriUtils;
import com.owncloud.android.utils.theme.CapabilityUtils;

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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import kotlin.Triple;

import static com.owncloud.android.ui.activity.FileDisplayActivity.REFRESH_FOLDER_EVENT_RECEIVER;

/**
 * Operation performing the update in the ownCloud server of a file that was modified locally.
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
     * Original OCFile which is to be uploaded in case file had to be renamed (if nameCollisionPolicy==RENAME and remote
     * file already exists).
     */
    private OCFile mOldFile;
    private String mRemotePath;
    private String mFolderUnlockToken;
    private boolean mRemoteFolderToBeCreated;
    private NameCollisionPolicy mNameCollisionPolicy;
    private int mLocalBehaviour;
    private int mCreatedBy;
    private boolean mOnWifiOnly;
    private boolean mWhileChargingOnly;
    private boolean mIgnoringPowerSaveMode;
    private final boolean mDisableRetries;

    private boolean mWasRenamed;
    private long mOCUploadId;
    /**
     * Local path to file which is to be uploaded (before any possible renaming or moving).
     */
    private String mOriginalStoragePath;
    private final Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<>();
    private OnRenameListener mRenameUploadListener;

    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean mUploadStarted = new AtomicBoolean(false);

    private Context mContext;

    private UploadFileRemoteOperation mUploadOperation;

    private RequestEntity mEntity;

    private final User user;
    private final OCUpload mUpload;
    private final UploadsStorageManager uploadsStorageManager;
    private final ConnectivityService connectivityService;
    private final PowerManagementService powerManagementService;

    private boolean encryptedAncestor;

    public static OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType) {
        OCFile newFile = new OCFile(remotePath);
        newFile.setStoragePath(localPath);
        newFile.setLastSyncDateForProperties(0);
        newFile.setLastSyncDateForData(0);

        // size
        if (!TextUtils.isEmpty(localPath)) {
            File localFile = new File(localPath);
            newFile.setFileLength(localFile.length());
            newFile.setLastSyncDateForData(localFile.lastModified());
        } // don't worry about not assigning size, the problems with localPath
        // are checked when the UploadFileOperation instance is created

        // MIME type
        if (TextUtils.isEmpty(mimeType)) {
            newFile.setMimeType(MimeTypeUtil.getBestMimeTypeByFilename(localPath));
        } else {
            newFile.setMimeType(mimeType);
        }

        return newFile;
    }

    public UploadFileOperation(UploadsStorageManager uploadsStorageManager,
                               ConnectivityService connectivityService,
                               PowerManagementService powerManagementService,
                               User user,
                               OCFile file,
                               OCUpload upload,
                               NameCollisionPolicy nameCollisionPolicy,
                               int localBehaviour,
                               Context context,
                               boolean onWifiOnly,
                               boolean whileChargingOnly,
                               FileDataStorageManager storageManager) {
        this(uploadsStorageManager,
             connectivityService,
             powerManagementService,
             user,
             file,
             upload,
             nameCollisionPolicy,
             localBehaviour,
             context,
             onWifiOnly,
             whileChargingOnly,
             true,
             storageManager);
    }

    public UploadFileOperation(UploadsStorageManager uploadsStorageManager,
                               ConnectivityService connectivityService,
                               PowerManagementService powerManagementService,
                               User user,
                               OCFile file,
                               OCUpload upload,
                               NameCollisionPolicy nameCollisionPolicy,
                               int localBehaviour,
                               Context context,
                               boolean onWifiOnly,
                               boolean whileChargingOnly,
                               boolean disableRetries,
                               FileDataStorageManager storageManager) {
        super(storageManager);

        if (upload == null) {
            throw new IllegalArgumentException("Illegal NULL file in UploadFileOperation creation");
        }
        if (TextUtils.isEmpty(upload.getLocalPath())) {
            throw new IllegalArgumentException(
                "Illegal file in UploadFileOperation; storage path invalid: "
                    + upload.getLocalPath());
        }

        this.uploadsStorageManager = uploadsStorageManager;
        this.connectivityService = connectivityService;
        this.powerManagementService = powerManagementService;
        this.user = user;
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
        mNameCollisionPolicy = nameCollisionPolicy;
        mLocalBehaviour = localBehaviour;
        mOriginalStoragePath = mFile.getStoragePath();
        mContext = context;
        mOCUploadId = upload.getUploadId();
        mCreatedBy = upload.getCreatedBy();
        mRemoteFolderToBeCreated = upload.isCreateRemoteFolder();
        // Ignore power save mode only if user explicitly created this upload
        mIgnoringPowerSaveMode = mCreatedBy == CREATED_BY_USER;
        mFolderUnlockToken = upload.getFolderUnlockToken();
        mDisableRetries = disableRetries;
    }

    public boolean isWifiRequired() {
        return mOnWifiOnly;
    }

    public boolean isChargingRequired() {
        return mWhileChargingOnly;
    }

    public boolean isIgnoringPowerSaveMode() {
        return mIgnoringPowerSaveMode;
    }

    public User getUser() {
        return user;
    }

    public String getFileName() {
        return (mFile != null) ? mFile.getFileName() : null;
    }

    public OCFile getFile() {
        return mFile;
    }

    /**
     * If remote file was renamed, return original OCFile which was uploaded. Is null is file was not renamed.
     */
    @Nullable
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
        return mFile.getMimeType();
    }

    public int getLocalBehaviour() {
        return mLocalBehaviour;
    }

    public UploadFileOperation setRemoteFolderToBeCreated() {
        mRemoteFolderToBeCreated = true;

        return this;
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
            ((ProgressiveDataTransfer) mEntity).addDataTransferProgressListener(listener);
        }
        if (mUploadOperation != null) {
            mUploadOperation.addDataTransferProgressListener(listener);
        }
    }

    public void removeDataTransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransfer) mEntity).removeDataTransferProgressListener(listener);
        }
        if (mUploadOperation != null) {
            mUploadOperation.removeDataTransferProgressListener(listener);
        }
    }

    public UploadFileOperation addRenameUploadListener(OnRenameListener listener) {
        mRenameUploadListener = listener;

        return this;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected RemoteOperationResult run(OwnCloudClient client) {
        mCancellationRequested.set(false);
        mUploadStarted.set(true);

        updateSize(0);

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

        // check if any parent is encrypted
        encryptedAncestor = FileStorageUtils.checkEncryptionStatus(parent, getStorageManager());
        mFile.setEncrypted(encryptedAncestor);

        if (encryptedAncestor) {
            Log_OC.d(TAG, "encrypted upload");
            return encryptedUpload(client, parent);
        } else {
            Log_OC.d(TAG, "normal upload");
            return normalUpload(client);
        }
    }

    // region E2E Upload
    @SuppressLint("AndroidLintUseSparseArrays") // gson cannot handle sparse arrays easily, therefore use hashmap
    private RemoteOperationResult encryptedUpload(OwnCloudClient client, OCFile parentFile) {
        RemoteOperationResult result = null;
        E2EFiles e2eFiles = new E2EFiles(parentFile, null, new File(mOriginalStoragePath), null, null);
        FileLock fileLock = null;
        long size;

        boolean metadataExists = false;
        String token = null;
        Object object = null;

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(getContext());
        String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        try {
            result = checkConditions(e2eFiles.getOriginalFile());

            if (result != null) {
                return result;
            }

            long counter = getE2ECounter(parentFile);
            token = getFolderUnlockTokenOrLockFolder(client, parentFile, counter);

            // Update metadata
            EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
            object = EncryptionUtils.downloadFolderMetadata(parentFile, client, mContext, user);
            if (object instanceof DecryptedFolderMetadataFileV1 decrypted && decrypted.getMetadata() != null) {
                metadataExists = true;
            }

            if (isEndToEndVersionAtLeastV2()) {
                if (object == null) {
                    return new RemoteOperationResult(new IllegalStateException("Metadata does not exist"));
                }
            } else {
                object = getDecryptedFolderMetadataV1(publicKey, object);
            }

            E2EClientData clientData = new E2EClientData(client, token, publicKey);

            List<String> fileNames = getCollidedFileNames(object);

            RemoteOperationResult collisionResult = checkNameCollision(client, fileNames, parentFile.isEncrypted());
            if (collisionResult != null) {
                result = collisionResult;
                return collisionResult;
            }

            mFile.setDecryptedRemotePath(parentFile.getDecryptedRemotePath() + e2eFiles.getOriginalFile().getName());
            String expectedPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mFile);
            e2eFiles.setExpectedFile(new File(expectedPath));

            result = copyFile(e2eFiles.getOriginalFile(), expectedPath);
            if (!result.isSuccess()) {
                return result;
            }

            long lastModifiedTimestamp = e2eFiles.getOriginalFile().lastModified() / 1000;
            Long creationTimestamp = FileUtil.getCreationTimestamp(e2eFiles.getOriginalFile());
            if (creationTimestamp == null) {
                throw new NullPointerException("creationTimestamp cannot be null");
            }

            E2EData e2eData = getE2EData(object);
            e2eFiles.setEncryptedTempFile(e2eData.getEncryptedFile().getEncryptedFile());
            if (e2eFiles.getEncryptedTempFile() == null) {
                throw new NullPointerException("encryptedTempFile cannot be null");
            }

            Triple<FileLock, RemoteOperationResult, FileChannel> channelResult = initFileChannel(result, fileLock, e2eFiles);
            fileLock = channelResult.getFirst();
            result = channelResult.getSecond();
            FileChannel channel = channelResult.getThird();

            size = getChannelSize(channel);
            updateSize(size);
            setUploadOperationForE2E(token, e2eFiles.getEncryptedTempFile(), e2eData.getEncryptedFileName(), lastModifiedTimestamp, creationTimestamp, size);

            result = performE2EUpload(clientData);

            if (result.isSuccess()) {
                updateMetadataForE2E(object, e2eData, clientData, e2eFiles, arbitraryDataProvider, encryptionUtilsV2, metadataExists);
            }
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, mFile.getStoragePath() + " does not exist anymore");
            result = new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        } catch (OverlappingFileLockException e) {
            Log_OC.d(TAG, "Overlapping file lock exception");
            result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
        } finally {
            result = cleanupE2EUpload(fileLock, e2eFiles, result, object, client, token);
        }

        completeE2EUpload(result, e2eFiles, client);

        return result;
    }

    private boolean isEndToEndVersionAtLeastV2() {
        return getE2EVersion().compareTo(E2EVersion.V2_0) >= 0;
    }

    private E2EVersion getE2EVersion() {
        return CapabilityUtils.getCapability(mContext).getEndToEndEncryptionApiVersion();
    }

    private long getE2ECounter(OCFile parentFile) {
        long counter = -1;

        if (isEndToEndVersionAtLeastV2()) {
            counter = parentFile.getE2eCounter() + 1;
        }

        return counter;
    }

    private String getFolderUnlockTokenOrLockFolder(OwnCloudClient client, OCFile parentFile, long counter) throws UploadException {
        if (mFolderUnlockToken != null && !mFolderUnlockToken.isEmpty()) {
            return mFolderUnlockToken;
        }

        String token = EncryptionUtils.lockFolder(parentFile, client, counter);
        mUpload.setFolderUnlockToken(token);
        uploadsStorageManager.updateUpload(mUpload);

        return token;
    }

    private DecryptedFolderMetadataFileV1 getDecryptedFolderMetadataV1(String publicKey, Object object)
        throws NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        DecryptedFolderMetadataFileV1 metadata = new DecryptedFolderMetadataFileV1();
        metadata.setMetadata(new DecryptedMetadata());
        metadata.getMetadata().setVersion(1.2);
        metadata.getMetadata().setMetadataKeys(new HashMap<>());
        String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
        String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
        metadata.getMetadata().setMetadataKey(encryptedMetadataKey);

        if (object instanceof DecryptedFolderMetadataFileV1) {
            metadata = (DecryptedFolderMetadataFileV1) object;
        }

        return metadata;
    }

    private List<String> getCollidedFileNames(Object object) {
        List<String> result = new ArrayList<>();

        if (object instanceof DecryptedFolderMetadataFileV1 metadata) {
            for (DecryptedFile file : metadata.getFiles().values()) {
                result.add(file.getEncrypted().getFilename());
            }
        } else if (object instanceof DecryptedFolderMetadataFile metadataFile) {
            Map<String, com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile> files = metadataFile.getMetadata().getFiles();
            for (com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile file : files.values()) {
                result.add(file.getFilename());
            }
        }

        return result;
    }

    private String getEncryptedFileName(Object object) {
        String encryptedFileName = EncryptionUtils.generateUid();

        if (object instanceof DecryptedFolderMetadataFileV1 metadata) {
            while (metadata.getFiles().get(encryptedFileName) != null) {
                encryptedFileName = EncryptionUtils.generateUid();
            }
        } else {
            while (((DecryptedFolderMetadataFile) object).getMetadata().getFiles().get(encryptedFileName) != null) {
                encryptedFileName = EncryptionUtils.generateUid();
            }
        }

        return encryptedFileName;
    }

    private void setUploadOperationForE2E(String token,
                                          File encryptedTempFile,
                                          String encryptedFileName,
                                          long lastModifiedTimestamp,
                                          long creationTimestamp,
                                          long size) {

        if (size > ChunkedFileUploadRemoteOperation.CHUNK_SIZE_MOBILE) {
            boolean onWifiConnection = connectivityService.getConnectivity().isWifi();

            mUploadOperation = new ChunkedFileUploadRemoteOperation(encryptedTempFile.getAbsolutePath(),
                                                                    mFile.getParentRemotePath() + encryptedFileName,
                                                                    mFile.getMimeType(),
                                                                    mFile.getEtagInConflict(),
                                                                    lastModifiedTimestamp,
                                                                    onWifiConnection,
                                                                    token,
                                                                    creationTimestamp,
                                                                    mDisableRetries
            );
        } else {
            mUploadOperation = new UploadFileRemoteOperation(encryptedTempFile.getAbsolutePath(),
                                                             mFile.getParentRemotePath() + encryptedFileName,
                                                             mFile.getMimeType(),
                                                             mFile.getEtagInConflict(),
                                                             lastModifiedTimestamp,
                                                             creationTimestamp,
                                                             token,
                                                             mDisableRetries
            );
        }
    }

    private Triple<FileLock, RemoteOperationResult, FileChannel> initFileChannel(RemoteOperationResult result, FileLock fileLock, E2EFiles e2eFiles) throws IOException {
        FileChannel channel = null;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(mFile.getStoragePath(), "rw")) {
            channel = randomAccessFile.getChannel();
            fileLock = channel.tryLock();
        } catch (IOException ioException) {
            Log_OC.d(TAG, "Error caught at getChannelFromFile: " + ioException);

            // this basically means that the file is on SD card
            // try to copy file to temporary dir if it doesn't exist
            String temporalPath = FileStorageUtils.getInternalTemporalPath(user.getAccountName(), mContext) +
                mFile.getRemotePath();
            mFile.setStoragePath(temporalPath);
            e2eFiles.setTemporalFile(new File(temporalPath));

            if (e2eFiles.getTemporalFile() == null) {
                throw new NullPointerException("Original file cannot be null");
            }

            Files.deleteIfExists(Paths.get(temporalPath));
            result = copy(e2eFiles.getOriginalFile(), e2eFiles.getTemporalFile());

            if (result.isSuccess()) {
                if (e2eFiles.getTemporalFile().length() == e2eFiles.getOriginalFile().length()) {
                    try (RandomAccessFile randomAccessFile = new RandomAccessFile(e2eFiles.getTemporalFile().getAbsolutePath(), "rw")) {
                        channel = randomAccessFile.getChannel();
                        fileLock = channel.tryLock();
                    } catch (IOException e) {
                        Log_OC.d(TAG, "Error caught at getChannelFromFile: " + e);
                    }
                } else {
                    result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
                }
            }
        }

        return new Triple<>(fileLock, result, channel);
    }

    private long getChannelSize(FileChannel channel) {
        try {
            return channel.size();
        } catch (IOException e1) {
            return new File(mFile.getStoragePath()).length();
        }
    }

    private RemoteOperationResult performE2EUpload(E2EClientData data) throws OperationCancelledException {
        for (OnDatatransferProgressListener mDataTransferListener : mDataTransferListeners) {
            mUploadOperation.addDataTransferProgressListener(mDataTransferListener);
        }

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        RemoteOperationResult result = mUploadOperation.execute(data.getClient());

        /// move local temporal file or original file to its corresponding
        // location in the Nextcloud local folder
        if (!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_PRECONDITION_FAILED) {
            result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
        }

        return result;
    }

    private E2EData getE2EData(Object object) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException, IOException {
        byte[] key = EncryptionUtils.generateKey();
        byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);
        Cipher cipher = EncryptionUtils.getCipher(Cipher.ENCRYPT_MODE, key, iv);
        File file = new File(mFile.getStoragePath());
        EncryptedFile encryptedFile = EncryptionUtils.encryptFile(user.getAccountName(), file, cipher);
        String encryptedFileName = getEncryptedFileName(object);

        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }

        return new E2EData(key, iv, encryptedFile, encryptedFileName);
    }

    private void updateMetadataForE2E(Object object, E2EData e2eData, E2EClientData clientData, E2EFiles e2eFiles, ArbitraryDataProvider arbitraryDataProvider, EncryptionUtilsV2 encryptionUtilsV2, boolean metadataExists)

        throws InvalidAlgorithmParameterException, UploadException, NoSuchPaddingException, IllegalBlockSizeException, CertificateException,
        NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        mFile.setDecryptedRemotePath(e2eFiles.getParentFile().getDecryptedRemotePath() + e2eFiles.getOriginalFile().getName());
        mFile.setRemotePath(e2eFiles.getParentFile().getRemotePath() + e2eData.getEncryptedFileName());


        if (object instanceof DecryptedFolderMetadataFileV1 metadata) {
            updateMetadataForV1(metadata,
                                e2eData,
                                clientData,
                                e2eFiles.getParentFile(),
                                arbitraryDataProvider,
                                metadataExists);
        } else if (object instanceof DecryptedFolderMetadataFile metadata) {
            updateMetadataForV2(metadata,
                                encryptionUtilsV2,
                                e2eData,
                                clientData,
                                e2eFiles.getParentFile());
        }
    }

    private void updateMetadataForV1(DecryptedFolderMetadataFileV1 metadata, E2EData e2eData, E2EClientData clientData,
                                     OCFile parentFile, ArbitraryDataProvider arbitraryDataProvider, boolean metadataExists)

        throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
        CertificateException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, UploadException {

        DecryptedFile decryptedFile = new DecryptedFile();
        Data data = new Data();
        data.setFilename(mFile.getDecryptedFileName());
        data.setMimetype(mFile.getMimeType());
        data.setKey(EncryptionUtils.encodeBytesToBase64String(e2eData.getKey()));
        decryptedFile.setEncrypted(data);
        decryptedFile.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(e2eData.getIv()));
        decryptedFile.setAuthenticationTag(e2eData.getEncryptedFile().getAuthenticationTag());

        metadata.getFiles().put(e2eData.getEncryptedFileName(), decryptedFile);

        EncryptedFolderMetadataFileV1 encryptedFolderMetadata =
            EncryptionUtils.encryptFolderMetadata(metadata,
                                                  clientData.getPublicKey(),
                                                  parentFile.getLocalId(),
                                                  user,
                                                  arbitraryDataProvider
                                                 );

        String serializedFolderMetadata;

        if (metadata.getMetadata().getMetadataKey() != null) {
            serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata, true);
        } else {
            serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);
        }

        // upload metadata
        EncryptionUtils.uploadMetadata(parentFile,
                                       serializedFolderMetadata,
                                       clientData.getToken(),
                                       clientData.getClient(),
                                       metadataExists,
                                       E2EVersion.V1_2,
                                       "",
                                       arbitraryDataProvider,
                                       user);
    }


    private void updateMetadataForV2(DecryptedFolderMetadataFile metadata, EncryptionUtilsV2 encryptionUtilsV2, E2EData e2eData, E2EClientData clientData, OCFile parentFile) throws UploadException {
        encryptionUtilsV2.addFileToMetadata(
            e2eData.getEncryptedFileName(),
            mFile,
            e2eData.getIv(),
            e2eData.getEncryptedFile().getAuthenticationTag(),
            e2eData.getKey(),
            metadata,
            getStorageManager());

        // upload metadata
        encryptionUtilsV2.serializeAndUploadMetadata(parentFile,
                                                     metadata,
                                                     clientData.getToken(),
                                                     clientData.getClient(),
                                                     true,
                                                     mContext,
                                                     user,
                                                     getStorageManager());
    }

    private void completeE2EUpload(RemoteOperationResult result, E2EFiles e2eFiles, OwnCloudClient client) {
        if (result.isSuccess()) {
            handleSuccessfulUpload(e2eFiles.getTemporalFile(), e2eFiles.getExpectedFile(), e2eFiles.getOriginalFile(), client);
        } else if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            getStorageManager().saveConflict(mFile, mFile.getEtagInConflict());
        }

        e2eFiles.deleteTemporalFile();
    }

    private RemoteOperationResult cleanupE2EUpload(FileLock fileLock, E2EFiles e2eFiles, RemoteOperationResult result, Object object, OwnCloudClient client, String token) {
        mUploadStarted.set(false);
        sendRefreshFolderEventBroadcast();

        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException e) {
                Log_OC.e(TAG, "Failed to unlock file with path " + mFile.getStoragePath());
            }
        }

        e2eFiles.deleteTemporalFileWithOriginalFileComparison();

        if (result == null) {
            result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
        }

        logResult(result, mFile.getStoragePath(), mFile.getRemotePath());

        // Unlock must be done otherwise folder stays locked and user can't upload any file
        RemoteOperationResult<Void> unlockFolderResult;
        if (object instanceof DecryptedFolderMetadataFileV1) {
            unlockFolderResult = EncryptionUtils.unlockFolderV1(e2eFiles.getParentFile(), client, token);
        } else {
            unlockFolderResult = EncryptionUtils.unlockFolder(e2eFiles.getParentFile(), client, token);
        }

        if (unlockFolderResult != null && !unlockFolderResult.isSuccess()) {
            result = unlockFolderResult;
        }

        e2eFiles.deleteEncryptedTempFile();

        return result;
    }
    // endregion

    private void sendRefreshFolderEventBroadcast() {
        Intent intent = new Intent(REFRESH_FOLDER_EVENT_RECEIVER);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private RemoteOperationResult checkConditions(File originalFile) {
        RemoteOperationResult remoteOperationResult = null;

        // check that connectivity conditions are met and delays the upload otherwise
        Connectivity connectivity = connectivityService.getConnectivity();
        if (mOnWifiOnly && (!connectivity.isWifi() || connectivity.isMetered())) {
            Log_OC.d(TAG, "Upload delayed until WiFi is available: " + getRemotePath());
            remoteOperationResult = new RemoteOperationResult(ResultCode.DELAYED_FOR_WIFI);
        }

        // check if charging conditions are met and delays the upload otherwise
        final BatteryStatus battery = powerManagementService.getBattery();
        if (mWhileChargingOnly && !battery.isCharging()) {
            Log_OC.d(TAG, "Upload delayed until the device is charging: " + getRemotePath());
            remoteOperationResult = new RemoteOperationResult(ResultCode.DELAYED_FOR_CHARGING);
        }

        // check that device is not in power save mode
        if (!mIgnoringPowerSaveMode && powerManagementService.isPowerSavingEnabled()) {
            Log_OC.d(TAG, "Upload delayed because device is in power save mode: " + getRemotePath());
            remoteOperationResult = new RemoteOperationResult(ResultCode.DELAYED_IN_POWER_SAVE_MODE);
        }

        // check if the file continues existing before schedule the operation
        if (!originalFile.exists()) {
            Log_OC.d(TAG, mOriginalStoragePath + " does not exist anymore");
            remoteOperationResult = new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        }

        // check that internet is not behind walled garden
        if (!connectivityService.getConnectivity().isConnected() || connectivityService.isInternetWalled()) {
            remoteOperationResult = new RemoteOperationResult(ResultCode.NO_NETWORK_CONNECTION);
        }

        return remoteOperationResult;
    }

    private RemoteOperationResult normalUpload(OwnCloudClient client) {
        RemoteOperationResult result = null;
        File temporalFile = null;
        File originalFile = new File(mOriginalStoragePath);
        File expectedFile = null;
        FileLock fileLock = null;
        FileChannel channel = null;

        long size;

        try {
            // check conditions
            result = checkConditions(originalFile);

            if (result != null) {
                return result;
            }

            // check name collision
            RemoteOperationResult collisionResult = checkNameCollision(client, null, false);
            if (collisionResult != null) {
                result = collisionResult;
                return collisionResult;
            }

            String expectedPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mFile);
            expectedFile = new File(expectedPath);

            result = copyFile(originalFile, expectedPath);
            if (!result.isSuccess()) {
                return result;
            }

            // Get the last modification date of the file from the file system
            long lastModifiedTimestamp = originalFile.lastModified() / 1000;

            final Long creationTimestamp = FileUtil.getCreationTimestamp(originalFile);

            try {
                channel = new RandomAccessFile(mFile.getStoragePath(), "rw").getChannel();
                fileLock = channel.tryLock();
            } catch (FileNotFoundException e) {
                // this basically means that the file is on SD card
                // try to copy file to temporary dir if it doesn't exist
                String temporalPath = FileStorageUtils.getInternalTemporalPath(user.getAccountName(), mContext) +
                    mFile.getRemotePath();
                mFile.setStoragePath(temporalPath);
                temporalFile = new File(temporalPath);

                Files.deleteIfExists(Paths.get(temporalPath));
                result = copy(originalFile, temporalFile);

                if (result.isSuccess()) {
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
            } catch (Exception e1) {
                size = new File(mFile.getStoragePath()).length();
            }

            updateSize(size);

            // perform the upload
            if (size > ChunkedFileUploadRemoteOperation.CHUNK_SIZE_MOBILE) {
                boolean onWifiConnection = connectivityService.getConnectivity().isWifi();

                mUploadOperation = new ChunkedFileUploadRemoteOperation(mFile.getStoragePath(),
                                                                        mFile.getRemotePath(),
                                                                        mFile.getMimeType(),
                                                                        mFile.getEtagInConflict(),
                                                                        lastModifiedTimestamp,
                                                                        creationTimestamp,
                                                                        onWifiConnection,
                                                                        mDisableRetries);
            } else {
                mUploadOperation = new UploadFileRemoteOperation(mFile.getStoragePath(),
                                                                 mFile.getRemotePath(),
                                                                 mFile.getMimeType(),
                                                                 mFile.getEtagInConflict(),
                                                                 lastModifiedTimestamp,
                                                                 creationTimestamp,
                                                                 mDisableRetries);
            }

            for (OnDatatransferProgressListener mDataTransferListener : mDataTransferListeners) {
                mUploadOperation.addDataTransferProgressListener(mDataTransferListener);
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            if (result.isSuccess() && mUploadOperation != null) {
                result = mUploadOperation.execute(client);

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

            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    Log_OC.w(TAG, "Failed to close file channel");
                }
            }

            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }

            if (result == null) {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }

            logResult(result, mOriginalStoragePath, mRemotePath);
        }

        if (result.isSuccess()) {
            handleSuccessfulUpload(temporalFile, expectedFile, originalFile, client);
        } else if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            getStorageManager().saveConflict(mFile, mFile.getEtagInConflict());
        }

        // delete temporal file
        if (temporalFile != null && temporalFile.exists() && !temporalFile.delete()) {
            Log_OC.e(TAG, "Could not delete temporal file " + temporalFile.getAbsolutePath());
        }

        return result;
    }

    private void updateSize(long size) {
        OCUpload ocUpload = uploadsStorageManager.getUploadById(getOCUploadId());
        if (ocUpload != null) {
            ocUpload.setFileSize(size);
            uploadsStorageManager.updateUpload(ocUpload);
        }
    }

    private void logResult(RemoteOperationResult result, String sourcePath, String targetPath) {
        if (result.isSuccess()) {
            Log_OC.i(TAG, "Upload of " + sourcePath + " to " + targetPath + ": " + result.getLogMessage());
        } else {
            if (result.getException() != null) {
                if (result.isCancelled()) {
                    Log_OC.w(TAG, "Upload of " + sourcePath + " to " + targetPath + ": "
                        + result.getLogMessage());
                } else {
                    Log_OC.e(TAG, "Upload of " + sourcePath + " to " + targetPath + ": "
                        + result.getLogMessage(), result.getException());
                }
            } else {
                Log_OC.e(TAG, "Upload of " + sourcePath + " to " + targetPath + ": " + result.getLogMessage());
            }
        }
    }

    private RemoteOperationResult copyFile(File originalFile, String expectedPath) throws OperationCancelledException,
        IOException {
        if (mLocalBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_COPY && !mOriginalStoragePath.equals(expectedPath)) {
            String temporalPath = FileStorageUtils.getInternalTemporalPath(user.getAccountName(), mContext) +
                mFile.getRemotePath();
            mFile.setStoragePath(temporalPath);
            File temporalFile = new File(temporalPath);

            return copy(originalFile, temporalFile);
        }

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        return new RemoteOperationResult(ResultCode.OK);
    }

    @CheckResult
    private RemoteOperationResult checkNameCollision(OwnCloudClient client,
                                                     List<String> fileNames,
                                                     boolean encrypted)
        throws OperationCancelledException {
        Log_OC.d(TAG, "Checking name collision in server");

        if (existsFile(client, mRemotePath, fileNames, encrypted)) {
            switch (mNameCollisionPolicy) {
                case CANCEL:
                    Log_OC.d(TAG, "File exists; canceling");
                    throw new OperationCancelledException();
                case RENAME:
                    mRemotePath = getNewAvailableRemotePath(client, mRemotePath, fileNames, encrypted);
                    mWasRenamed = true;
                    createNewOCFile(mRemotePath);
                    Log_OC.d(TAG, "File renamed as " + mRemotePath);
                    if (mRenameUploadListener != null) {
                        mRenameUploadListener.onRenameUpload();
                    }
                    break;
                case OVERWRITE:
                    Log_OC.d(TAG, "Overwriting file");
                    break;
                case ASK_USER:
                    Log_OC.d(TAG, "Name collision; asking the user what to do");
                    return new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
            }
        }

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        return null;
    }

    private void handleSuccessfulUpload(File temporalFile,
                                        File expectedFile,
                                        File originalFile,
                                        OwnCloudClient client) {
        switch (mLocalBehaviour) {
            case FileUploadWorker.LOCAL_BEHAVIOUR_FORGET:
            default:
                mFile.setStoragePath("");
                saveUploadedFile(client);
                break;

            case FileUploadWorker.LOCAL_BEHAVIOUR_DELETE:
                originalFile.delete();
                mFile.setStoragePath("");
                getStorageManager().deleteFileInMediaScan(originalFile.getAbsolutePath());
                saveUploadedFile(client);
                break;

            case FileUploadWorker.LOCAL_BEHAVIOUR_COPY:
                if (temporalFile != null) {
                    try {
                        move(temporalFile, expectedFile);
                    } catch (IOException e) {
                        Log_OC.e(TAG, e.getMessage());
                    }
                } else if (originalFile != null) {
                    try {
                        copy(originalFile, expectedFile);
                    } catch (IOException e) {
                        Log_OC.e(TAG, e.getMessage());
                    }
                }
                mFile.setStoragePath(expectedFile.getAbsolutePath());
                saveUploadedFile(client);
                if (MimeTypeUtil.isMedia(mFile.getMimeType())) {
                    FileDataStorageManager.triggerMediaScan(expectedFile.getAbsolutePath());
                }
                break;

            case FileUploadWorker.LOCAL_BEHAVIOUR_MOVE:
                String expectedPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mFile);
                File newFile = new File(expectedPath);

                try {
                    move(originalFile, newFile);
                } catch (IOException e) {
                    Log_OC.e(TAG, "Error moving file", e);
                }
                getStorageManager().deleteFileInMediaScan(originalFile.getAbsolutePath());
                mFile.setStoragePath(newFile.getAbsolutePath());
                saveUploadedFile(client);
                if (MimeTypeUtil.isMedia(mFile.getMimeType())) {
                    FileDataStorageManager.triggerMediaScan(newFile.getAbsolutePath());
                }
                break;
        }
    }

    /**
     * Checks the existence of the folder where the current file will be uploaded both in the remote server and in the
     * local database.
     * <p/>
     * If the upload is set to enforce the creation of the folder, the method tries to create it both remote and
     * locally.
     *
     * @param pathToGrant Full remote path whose existence will be granted.
     * @return An {@link OCFile} instance corresponding to the folder where the file will be uploaded.
     */
    private RemoteOperationResult grantFolderExistence(String pathToGrant, OwnCloudClient client) {
        RemoteOperation operation = new ExistenceCheckRemoteOperation(pathToGrant, false);
        RemoteOperationResult result = operation.execute(client);
        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND && mRemoteFolderToBeCreated) {
            SyncOperation syncOp = new CreateFolderOperation(pathToGrant, user, getContext(), getStorageManager());
            result = syncOp.execute(client);
        }
        if (result.isSuccess()) {
            OCFile parentDir = getStorageManager().getFileByPath(pathToGrant);
            if (parentDir == null) {
                parentDir = createLocalFolder(pathToGrant);
            }
            if (parentDir != null) {
                result = new RemoteOperationResult(ResultCode.OK);
            } else {
                result = new RemoteOperationResult(ResultCode.CANNOT_CREATE_FILE);
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
            createdFolder.setMimeType(MimeType.DIRECTORY);
            createdFolder.setParentId(parent.getFileId());
            getStorageManager().saveFile(createdFolder);
            return createdFolder;
        }
        return null;
    }


    /**
     * Create a new OCFile mFile with new remote path. This is required if nameCollisionPolicy==RENAME. New file is
     * stored as mFile, original as mOldFile.
     *
     * @param newRemotePath new remote path
     */
    private void createNewOCFile(String newRemotePath) {
        // a new OCFile instance must be created for a new remote path
        OCFile newFile = new OCFile(newRemotePath);
        newFile.setCreationTimestamp(mFile.getCreationTimestamp());
        newFile.setFileLength(mFile.getFileLength());
        newFile.setMimeType(mFile.getMimeType());
        newFile.setModificationTimestamp(mFile.getModificationTimestamp());
        newFile.setModificationTimestampAtLastSyncForData(
            mFile.getModificationTimestampAtLastSyncForData()
                                                         );
        newFile.setEtag(mFile.getEtag());
        newFile.setLastSyncDateForProperties(mFile.getLastSyncDateForProperties());
        newFile.setLastSyncDateForData(mFile.getLastSyncDateForData());
        newFile.setStoragePath(mFile.getStoragePath());
        newFile.setParentId(mFile.getParentId());
        mOldFile = mFile;
        mFile = newFile;
    }

    /**
     * Returns a new and available (does not exists on the server) remotePath. This adds an incremental suffix.
     *
     * @param client     OwnCloud client
     * @param remotePath remote path of the file
     * @param fileNames  list of decrypted file names
     * @return new remote path
     */
    private String getNewAvailableRemotePath(OwnCloudClient client,
                                             String remotePath,
                                             List<String> fileNames,
                                             boolean encrypted) {
        int extPos = remotePath.lastIndexOf('.');
        String suffix;
        String extension = "";
        String remotePathWithoutExtension = "";
        if (extPos >= 0) {
            extension = remotePath.substring(extPos + 1);
            remotePathWithoutExtension = remotePath.substring(0, extPos);
        }

        int count = 2;
        boolean exists;
        String newPath;
        do {
            suffix = " (" + count + ")";
            newPath = extPos >= 0 ? remotePathWithoutExtension + suffix + "." + extension : remotePath + suffix;
            exists = existsFile(client, newPath, fileNames, encrypted);
            count++;
        } while (exists);

        return newPath;
    }

    private boolean existsFile(OwnCloudClient client,
                               String remotePath,
                               List<String> fileNames,
                               boolean encrypted) {
        if (encrypted) {
            String fileName = new File(remotePath).getName();

            for (String name : fileNames) {
                if (name.equalsIgnoreCase(fileName)) {
                    return true;
                }
            }

            return false;
        } else {
            ExistenceCheckRemoteOperation existsOperation = new ExistenceCheckRemoteOperation(remotePath, false);
            RemoteOperationResult result = existsOperation.execute(client);
            return result.isSuccess();
        }
    }

    /**
     * Allows to cancel the actual upload operation. If actual upload operating is in progress it is cancelled, if
     * upload preparation is being performed upload will not take place.
     */
    public void cancel(ResultCode cancellationReason) {
        if (mUploadOperation == null) {
            if (mUploadStarted.get()) {
                Log_OC.d(TAG, "Cancelling upload during upload preparations.");
                mCancellationRequested.set(true);
            } else {
                mCancellationRequested.set(true);
                Log_OC.e(TAG, "No upload in progress. This should not happen.");
            }
        } else {
            Log_OC.d(TAG, "Cancelling upload during actual upload operation.");
            mUploadOperation.cancel(cancellationReason);
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

        if (FileStorageUtils.getUsableSpace() < sourceFile.length()) {
            return new RemoteOperationResult(ResultCode.LOCAL_STORAGE_FULL); // error when the file should be copied
        } else {
            Log_OC.d(TAG, "Creating temporal folder");
            File temporalParent = targetFile.getParentFile();

            if (!temporalParent.mkdirs() && !temporalParent.isDirectory()) {
                return new RemoteOperationResult(ResultCode.CANNOT_CREATE_FILE);
            }

            Log_OC.d(TAG, "Creating temporal file");
            if (!targetFile.createNewFile() && !targetFile.isFile()) {
                return new RemoteOperationResult(ResultCode.CANNOT_CREATE_FILE);
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
                    return new RemoteOperationResult(new OperationCancelledException());
                }
            } catch (Exception e) {
                return new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_COPIED);
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
        return new RemoteOperationResult(ResultCode.OK);
    }


    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on {@link RemoteOperationResult},
     * TODO     use Exceptions instead
     * TODO refactor both this and 'copy' in a single method
     *
     * @param sourceFile Source file to move.
     * @param targetFile Target location to move the file.
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
     * A PROPFIND is necessary to keep the props in the local database synchronized with the server, specially the
     * modification time and Etag (where available)
     */
    private void saveUploadedFile(OwnCloudClient client) {
        OCFile file = mFile;
        if (file.fileExists()) {
            file = getStorageManager().getFileById(file.getFileId());
        }
        if (file == null) {
            // this can happen e.g. when the file gets deleted during upload
            return;
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

        ReadFileRemoteOperation operation = new ReadFileRemoteOperation(path);
        RemoteOperationResult result = operation.execute(client);
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
        file.setUpdateThumbnailNeeded(true);
        getStorageManager().saveFile(file);
        getStorageManager().saveConflict(file, null);

        if (MimeTypeUtil.isMedia(file.getMimeType())) {
            FileDataStorageManager.triggerMediaScan(file.getStoragePath(), file);
        }

        // generate new Thumbnail
        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
            new ThumbnailsCacheManager.ThumbnailGenerationTask(getStorageManager(), user);
        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, file.getRemoteId()));
    }

    private void updateOCFile(OCFile file, RemoteFile remoteFile) {
        file.setCreationTimestamp(remoteFile.getCreationTimestamp());
        file.setFileLength(remoteFile.getLength());
        file.setMimeType(remoteFile.getMimeType());
        file.setModificationTimestamp(remoteFile.getModifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(remoteFile.getModifiedTimestamp());
        file.setEtag(remoteFile.getEtag());
        file.setRemoteId(remoteFile.getRemoteId());
        file.setPermissions(remoteFile.getPermissions());
    }

    public interface OnRenameListener {

        void onRenameUpload();
    }

}
