/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
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
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.owncloud.android.utils.EncryptionUtils.encodeStringToBase64Bytes;


/**
 * Operation performing the update in the ownCloud server
 * of a file that was modified locally.
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class UploadEncryptedFileOperation extends SyncOperation {

    private static final String TAG = UploadEncryptedFileOperation.class.getSimpleName();

    public static final int CREATED_BY_USER = 0;
    public static final int CREATED_AS_INSTANT_PICTURE = 1;
    public static final int CREATED_AS_INSTANT_VIDEO = 2;
    private OCFile parentFile;

    /**
     * OCFile which is to be uploaded.
     */
    private OCFile ocFile;
    private int localBehaviour = FileUploader.LOCAL_BEHAVIOUR_COPY;
    private final String originalStoragePath;
    private boolean chunked = false;
    private boolean mRemoteFolderToBeCreated = false;
    private int mCreatedBy = CREATED_BY_USER;

    private long mOCUploadId = -1;
    /**
     * Local path to file which is to be uploaded (before any possible renaming or moving).
     */
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean mUploadStarted = new AtomicBoolean(false);

    private Context context;

    private UploadRemoteFileOperation mUploadOperation;

    protected RequestEntity mEntity = null;

    private Account account;
    private ArbitraryDataProvider arbitraryDataProvider;

    public UploadEncryptedFileOperation(OCFile parent, UploadFileOperation uploadFileOperation) {
        parentFile = parent;
        ocFile = uploadFileOperation.getFile();
        account = uploadFileOperation.getAccount();
        chunked = uploadFileOperation.isChunkedUploadSupported();
        context = uploadFileOperation.getContext();
        localBehaviour = uploadFileOperation.getLocalBehaviour();
        originalStoragePath = uploadFileOperation.getStoragePath();

        arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());
    }

    public Account getAccount() {
        return account;
    }

    public String getFileName() {
        return (ocFile != null) ? ocFile.getFileName() : null;
    }

    public OCFile getFile() {
        return ocFile;
    }

    public String getStoragePath() {
        return ocFile.getStoragePath();
    }

    public String getRemotePath() {
        return ocFile.getParentRemotePath() + ocFile.getEncryptedFileName();
    }

    public String getMimeType() {
        return ocFile.getMimetype();
    }

    public void setRemoteFolderToBeCreated() {
        mRemoteFolderToBeCreated = true;
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

    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
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

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
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

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        boolean metadataExists = false;
        String token = null;
        mCancellationRequested.set(false);
        mUploadStarted.set(true);

        File temporalFile = null;
        File originalFile = new File(ocFile.getStoragePath());
        File expectedFile = null;
        FileLock fileLock = null;

        String privateKey = arbitraryDataProvider.getValue(account.name, EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(account.name, EncryptionUtils.PUBLIC_KEY);

        try {

            /// check if the file continues existing before schedule the operation
            if (!originalFile.exists()) {
                Log_OC.d(TAG, ocFile.getStoragePath() + " not exists anymore");
                throw new FileNotFoundException();
            }

            /// check the existence of the parent folder for the file to upload
            String remoteParentPath = new File(getRemotePath()).getParent();
            remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ?
                    remoteParentPath : remoteParentPath + OCFile.PATH_SEPARATOR;
            result = grantFolderExistence(remoteParentPath, client);

            if (!result.isSuccess()) {
                return result;
            }

            // TODO automatic rename? UploadFileOperation:365

            /// set parent local id in uploading file
//            OCFile parent = getStorageManager().getFileByPath(remoteParentPath);
//            ocFile.setParentId(parent.getFileId());


//        if (mCancellationRequested.get()) {
//            throw new OperationCancelledException();
//        }

            // Get the last modification date of the file from the file system
            Long timeStampLong = originalFile.lastModified() / 1000;
            String timeStamp = timeStampLong.toString();

            // Lock folder
            LockFileOperation lockFileOperation = new LockFileOperation(parentFile.getLocalId());
            RemoteOperationResult lockFileOperationResult = lockFileOperation.execute(client);


            if (lockFileOperationResult.isSuccess()) {
                token = (String) lockFileOperationResult.getData().get(0);
            } else if (lockFileOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                throw new Exception("Forbidden! Please try again later.)");
            } else {
                throw new Exception("Unknown error!");
            }

            // Update metadata
            GetMetadataOperation getMetadataOperation = new GetMetadataOperation(parentFile.getLocalId());
            RemoteOperationResult getMetadataOperationResult = getMetadataOperation.execute(client);

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
                metadata.metadata = new DecryptedFolderMetadata.Metadata();
                metadata.metadata.metadataKeys = new HashMap<>();
                String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
                String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
                metadata.metadata.metadataKeys.put(0, encryptedMetadataKey);
            } else {
                // TODO error
                throw new Exception("something wrong");
            }

            // Key
            byte[] key = null;

            try {
                // TODO change key if file has changed, e.g. when file is updated
                key = encodeStringToBase64Bytes(metadata.files.get(ocFile.getFileName()).encrypted.key);
            } catch (Exception e) {
                // no key found
            }

            if (key == null || key.length == 0) {
                key = EncryptionUtils.generateKey();
            }

            // IV
            byte[] iv = null;

            try {
                iv = encodeStringToBase64Bytes(metadata.files.get(ocFile.getFileName()).initializationVector);
            } catch (Exception e) {
                // no iv found
            }

            if (iv == null || iv.length == 0) {
                iv = EncryptionUtils.generateIV();
            }


            EncryptionUtils.EncryptedFile encryptedFile = EncryptionUtils.encryptFile(ocFile, key, iv);

            // new random file name, check if it exists in metadata
            String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

            while (metadata.files.get(encryptedFileName) != null) {
                encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
            }

            ocFile.setEncryptedFileName(encryptedFileName);

            File encryptedTempFile = File.createTempFile("encFile", encryptedFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(encryptedTempFile);
            fileOutputStream.write(encryptedFile.encryptedBytes);
            fileOutputStream.close();

            /// perform the upload
            if (chunked &&
                    (new File(ocFile.getStoragePath())).length() >
                            ChunkedUploadRemoteFileOperation.CHUNK_SIZE) {
                mUploadOperation = new ChunkedUploadRemoteFileOperation(context, encryptedTempFile.getAbsolutePath(),
                        ocFile.getParentRemotePath() + encryptedFileName, ocFile.getMimetype(),
                        ocFile.getEtagInConflict(), timeStamp);
            } else {
                mUploadOperation = new UploadRemoteFileOperation(encryptedTempFile.getAbsolutePath(),
                        ocFile.getParentRemotePath() + encryptedFileName, ocFile.getMimetype(),
                        ocFile.getEtagInConflict(), timeStamp);
            }

            Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

//            FileChannel channel = null;
//            try {
//                channel = new RandomAccessFile(ocFile.getStoragePath(), "rw").getChannel();
//                fileLock = channel.tryLock();
//            } catch (FileNotFoundException e) {
//                if (temporalFile == null) {
//                    String temporalPath = FileStorageUtils.getTemporalPath(account.name) + ocFile.getRemotePath();
//                    ocFile.setStoragePath(temporalPath);
//                    temporalFile = new File(temporalPath);
//
//                    result = copy(originalFile, temporalFile);
//
//                    if (result != null) {
//                        return result;
//                    } else {
//                        if (temporalFile.length() == originalFile.length()) {
//                            channel = new RandomAccessFile(temporalFile.getAbsolutePath(), "rw").getChannel();
//                            fileLock = channel.tryLock();
//                        } else {
//                            while (temporalFile.length() != originalFile.length()) {
//                                Files.deleteIfExists(Paths.get(temporalPath));
//                                result = copy(originalFile, temporalFile);
//
//                                if (result != null) {
//                                    return result;
//                                } else {
//                                    channel = new RandomAccessFile(temporalFile.getAbsolutePath(), "rw").
//                                            getChannel();
//                                    fileLock = channel.tryLock();
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    channel = new RandomAccessFile(temporalFile.getAbsolutePath(), "rw").getChannel();
//                    fileLock = channel.tryLock();
//                }
//            }

            result = mUploadOperation.execute(client);

            /// move local temporal file or original file to its corresponding
            // location in the ownCloud local folder
            if (!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_PRECONDITION_FAILED) {
                result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
            }

            if (result.isSuccess()) {
                // upload metadata
                DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
                DecryptedFolderMetadata.Data data = new DecryptedFolderMetadata.Data();
                data.filename = ocFile.getFileName();
                data.mimetype = ocFile.getMimetype();
                data.key = EncryptionUtils.encodeBytesToBase64String(key);

                decryptedFile.encrypted = data;
                decryptedFile.initializationVector = EncryptionUtils.encodeBytesToBase64String(iv);
                decryptedFile.authenticationTag = encryptedFile.authenticationTag;

                metadata.files.put(encryptedFileName, decryptedFile);

                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                        privateKey);
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

                // upload metadata
                RemoteOperationResult uploadMetadataOperationResult;
                if (metadataExists) {
                    // update metadata
                    UpdateMetadataOperation storeMetadataOperation = new UpdateMetadataOperation(parentFile.getLocalId(),
                            serializedFolderMetadata, token);
                    uploadMetadataOperationResult = storeMetadataOperation.execute(client);
                } else {
                    // store metadata
                    StoreMetadataOperation storeMetadataOperation = new StoreMetadataOperation(parentFile.getLocalId(),
                            serializedFolderMetadata);
                    uploadMetadataOperationResult = storeMetadataOperation.execute(client);
                }

                if (!uploadMetadataOperationResult.isSuccess()) {
                    throw new Exception();
                }
            }
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, ocFile.getStoragePath() + " not exists anymore");
            result = new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
        } catch (OverlappingFileLockException e) {
            Log_OC.d(TAG, "Overlapping file lock exception");
            result = new RemoteOperationResult(ResultCode.LOCK_FAILED);
        } catch (Exception e) {
            result = new RemoteOperationResult(e);

        } finally {
            mUploadStarted.set(false);

            // unlock file
            if (token != null) {
                UnlockFileOperation unlockFileOperation = new UnlockFileOperation(parentFile.getLocalId(), token);
                RemoteOperationResult unlockFileOperationResult = unlockFileOperation.execute(client);

                if (!unlockFileOperationResult.isSuccess()) {
                    Log_OC.e(TAG, "Failed to unlock " + parentFile.getLocalId());
                }
            }

            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Failed to unlock file with path " + ocFile.getStoragePath());
                }
            }

            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }
            if (result == null) {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }
            if (result.isSuccess()) {
                Log_OC.i(TAG, "Upload of " + ocFile.getStoragePath() + " to " + ocFile.getRemotePath() + ": " +
                        result.getLogMessage());
            } else {
                if (result.getException() != null) {
                    if (result.isCancelled()) {
                        Log_OC.w(TAG, "Upload of " + ocFile.getStoragePath() + " to " + ocFile.getRemotePath() +
                                ": " + result.getLogMessage());
                    } else {
                        Log_OC.e(TAG, "Upload of " + ocFile.getStoragePath() + " to " + ocFile.getRemotePath() +
                                ": " + result.getLogMessage(), result.getException());
                    }

                } else {
                    Log_OC.e(TAG, "Upload of " + ocFile.getStoragePath() + " to " + ocFile.getRemotePath() +
                            ": " + result.getLogMessage());
                }
            }
        }

        switch (localBehaviour) {
            case FileUploader.LOCAL_BEHAVIOUR_FORGET:
                String temporalPath = FileStorageUtils.getTemporalPath(account.name) + ocFile.getRemotePath();
                if (originalStoragePath.equals(temporalPath)) {
                    // delete local file is was pre-copied in temporary folder (see .ui.helpers.UriUploader)
                    temporalFile = new File(temporalPath);
                    temporalFile.delete();
                }
                ocFile.setStoragePath("");
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
                        e.printStackTrace();
                    }
                }
                ocFile.setStoragePath(expectedFile.getAbsolutePath());
                saveUploadedFile(client);
                FileDataStorageManager.triggerMediaScan(expectedFile.getAbsolutePath());
                break;

            case FileUploader.LOCAL_BEHAVIOUR_MOVE:

                String expectedPath = FileStorageUtils.getDefaultSavePathFor(account.name, ocFile);
                expectedFile = new File(expectedPath);

                try {
                    move(originalFile, expectedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getStorageManager().deleteFileInMediaScan(originalFile.getAbsolutePath());
                ocFile.setStoragePath(expectedFile.getAbsolutePath());
                saveUploadedFile(client);
                FileDataStorageManager.triggerMediaScan(expectedFile.getAbsolutePath());
                break;
        }

        return result;
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
        RemoteOperation operation = new ExistenceCheckRemoteOperation(pathToGrant, context, false);
        RemoteOperationResult result = operation.execute(client);
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
     * Checks if remotePath does not exist in the server and returns it, or adds
     * a suffix to it in order to avoid the server file is overwritten.
     *
     * @param wc
     * @param remotePath
     * @return
     */
    private String getAvailableRemotePath(OwnCloudClient wc, String remotePath) {
        boolean check = existsFile(wc, remotePath);
        if (!check) {
            return remotePath;
        }

        int pos = remotePath.lastIndexOf('.');
        String suffix = "";
        String extension = "";
        if (pos >= 0) {
            extension = remotePath.substring(pos + 1);
            remotePath = remotePath.substring(0, pos);
        }
        int count = 2;
        do {
            suffix = " (" + count + ")";
            if (pos >= 0) {
                check = existsFile(wc, remotePath + suffix + "." + extension);
            } else {
                check = existsFile(wc, remotePath + suffix);
            }
            count++;
        } while (check);

        if (pos >= 0) {
            return remotePath + suffix + "." + extension;
        } else {
            return remotePath + suffix;
        }
    }

    private boolean existsFile(OwnCloudClient client, String remotePath) {
        ExistenceCheckRemoteOperation existsOperation =
                new ExistenceCheckRemoteOperation(remotePath, context, false);
        RemoteOperationResult result = existsOperation.execute(client);
        return result.isSuccess();
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

//    /**
//     * TODO rewrite with homogeneous fail handling, remove dependency on {@link RemoteOperationResult},
//     * TODO     use Exceptions instead
//     *
//     * @param sourceFile Source file to copy.
//     * @param targetFile Target location to copy the file.
//     * @return {@link RemoteOperationResult}
//     * @throws IOException
//     */
//    private RemoteOperationResult copy(File sourceFile, File targetFile) throws IOException {
//        Log_OC.d(TAG, "Copying local file");
//
//        RemoteOperationResult result = null;
//
//        if (FileStorageUtils.getUsableSpace(account.name) < sourceFile.length()) {
//            result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_FULL);
//            return result;  // error condition when the file should be copied
//
//        } else {
//            Log_OC.d(TAG, "Creating temporal folder");
//            File temporalParent = targetFile.getParentFile();
//            temporalParent.mkdirs();
//            if (!temporalParent.isDirectory()) {
//                throw new IOException(
//                        "Unexpected error: parent directory could not be created");
//            }
//            Log_OC.d(TAG, "Creating temporal file");
//            targetFile.createNewFile();
//            if (!targetFile.isFile()) {
//                throw new IOException(
//                        "Unexpected error: target file could not be created");
//            }
//
//            Log_OC.d(TAG, "Copying file contents");
//            InputStream in = null;
//            OutputStream out = null;
//
//            try {
//                if (!mOriginalStoragePath.equals(targetFile.getAbsolutePath())) {
//                    // In case document provider schema as 'content://'
//                    if (mOriginalStoragePath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {
//                        Uri uri = Uri.parse(mOriginalStoragePath);
//                        in = context.getContentResolver().openInputStream(uri);
//                    } else {
//                        in = new FileInputStream(sourceFile);
//                    }
//                    out = new FileOutputStream(targetFile);
//                    int nRead;
//                    byte[] buf = new byte[4096];
//                    while (!mCancellationRequested.get() &&
//                            (nRead = in.read(buf)) > -1) {
//                        out.write(buf, 0, nRead);
//                    }
//                    out.flush();
//
//                } // else: weird but possible situation, nothing to copy
//
//                if (mCancellationRequested.get()) {
//                    result = new RemoteOperationResult(new OperationCancelledException());
//                    return result;
//                }
//
//            } catch (Exception e) {
//                result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_COPIED);
//                return result;
//
//            } finally {
//                try {
//                    if (in != null) {
//                        in.close();
//                    }
//                } catch (Exception e) {
//                    Log_OC.d(TAG, "Weird exception while closing input stream for " +
//                            mOriginalStoragePath + " (ignoring)", e);
//                }
//                try {
//                    if (out != null) {
//                        out.close();
//                    }
//                } catch (Exception e) {
//                    Log_OC.d(TAG, "Weird exception while closing output stream for " +
//                            targetFile.getAbsolutePath() + " (ignoring)", e);
//                }
//            }
//        }
//        return result;
//    }


    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on {@link RemoteOperationResult},
     * TODO     use Exceptions instead
     * <p>
     * TODO refactor both this and 'copy' in a single method
     *
     * @param sourceFile Source file to move.
     * @param targetFile Target location to move the file.
     * @return {@link RemoteOperationResult}
     * @throws IOException
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
                        ocFile.setStoragePath(""); // forget the local file
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
                ocFile.setStoragePath("");
            }
        }
    }

    /**
     * Saves a OC File after a successful upload.
     * <p/>
     * A PROPFIND is necessary to keep the props in the local database
     * synchronized with the server, specially the modification time and Etag
     * (where available)
     * <p/>
     */
    private void saveUploadedFile(OwnCloudClient client) {
        OCFile file = ocFile;
        if (file.fileExists()) {
            file = getStorageManager().getFileById(file.getFileId());
        }
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForData(syncDate);

        // new PROPFIND to keep data consistent with server
        // in theory, should return the same we already have
        // TODO from the appropriate OC server version, get data from last PUT response headers, instead
        // TODO     of a new PROPFIND; the latter may fail, specially for chunked uploads
        ReadRemoteFileOperation operation = new ReadRemoteFileOperation(getRemotePath());
        RemoteOperationResult result = operation.execute(client);
        if (result.isSuccess()) {
            updateOCFile(file, (RemoteFile) result.getData().get(0));
            file.setLastSyncDateForProperties(syncDate);
        } else {
            Log_OC.e(TAG, "Error reading properties of file after successful upload; this is gonna hurt...");
        }

        file.setEncrypted(true);
        file.setStoragePath("");
        file.setParentId(parentFile.getFileId());
        getStorageManager().saveFile(file);
        getStorageManager().saveConflict(file, null);

        FileDataStorageManager.triggerMediaScan(file.getStoragePath());
    }

    private void updateOCFile(OCFile file, RemoteFile remoteFile) {
        file.setCreationTimestamp(remoteFile.getCreationTimestamp());
        file.setFileLength(remoteFile.getLength());
//        file.setMimetype(file.getMimetype());
        file.setModificationTimestamp(remoteFile.getModifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(remoteFile.getModifiedTimestamp());
        file.setEtag(remoteFile.getEtag());
        file.setRemoteId(remoteFile.getRemoteId());
    }

    public interface OnRenameListener {

        void onRenameUpload();
    }

}
