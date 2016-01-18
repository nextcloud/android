/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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
import android.net.Uri;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploadService;
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
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.UriUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Remote operation performing the upload of a file to an ownCloud server
 */
public class UploadFileOperation extends RemoteOperation {

    private static final String TAG = UploadFileOperation.class.getSimpleName();

    private Account mAccount;
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
    private boolean mChunked = false;
    private boolean mRemoteFolderToBeCreated = false;
    private boolean mForceOverwrite = false;
    private int mLocalBehaviour = FileUploadService.LOCAL_BEHAVIOUR_COPY;
    private boolean mWasRenamed = false;
    private String mOriginalFileName = null;
    /**
     * Local path to file which is to be uploaded (before any possible renaming or moving).
     */
    private String mOriginalStoragePath = null;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean mUploadStarted = new AtomicBoolean(false);

    private Context mContext;
    
    private UploadRemoteFileOperation mUploadOperation;

    protected RequestEntity mEntity = null;

    public UploadFileOperation( Account account,
                                OCFile file,
                                boolean chunked,
                                boolean forceOverwrite,
                                int localBehaviour,
                                Context context) {
        if (account == null)
            throw new IllegalArgumentException("Illegal NULL account in UploadFileOperation " +
                    "creation");
        if (file == null)
            throw new IllegalArgumentException("Illegal NULL file in UploadFileOperation creation");
        if (file.getStoragePath() == null || file.getStoragePath().length() <= 0) {
            throw new IllegalArgumentException(
                    "Illegal file in UploadFileOperation; storage path invalid: "
                            + file.getStoragePath());
        }

        mAccount = account;
        mFile = file;
        mRemotePath = file.getRemotePath();
        mChunked = chunked;
        mForceOverwrite = forceOverwrite;
        mLocalBehaviour = localBehaviour;
        mOriginalStoragePath = mFile.getStoragePath();
        mOriginalFileName = mFile.getFileName();
        mContext = context;
    }

    public Account getAccount() {
        return mAccount;
    }

    public String getFileName() {
        return mOriginalFileName;
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

    public String getMimeType() {
        return mFile.getMimetype();
    }

    public boolean isRemoteFolderToBeCreated() {
        return mRemoteFolderToBeCreated;
    }

    public void setRemoteFolderToBeCreated() {
        mRemoteFolderToBeCreated = true;
    }

    public boolean getForceOverwrite() {
        return mForceOverwrite;
    }

    public boolean wasRenamed() {
        return mWasRenamed;
    }

    public Set<OnDatatransferProgressListener> getDataTransferListeners() {
        return mDataTransferListeners;
    }
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).addDatatransferProgressListener(listener);
        }
        if(mUploadOperation != null){
            mUploadOperation.addDatatransferProgressListener(listener);
        }
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).removeDatatransferProgressListener(listener);
        }
        if(mUploadOperation != null){
            mUploadOperation.removeDatatransferProgressListener(listener);
        }
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        mCancellationRequested.set(false);
        mUploadStarted.set(true);
        RemoteOperationResult result = null;
        boolean localCopyPassed = false, nameCheckPassed = false;
        File temporalFile = null, originalFile = new File(mOriginalStoragePath), expectedFile = null;
        try {
            // / rename the file to upload, if necessary
            if (!mForceOverwrite) {
                String remotePath = getAvailableRemotePath(client, mRemotePath);
                mWasRenamed = !remotePath.equals(mRemotePath);
                if (mWasRenamed) {
                    createNewOCFile(remotePath);
                }
            }
            nameCheckPassed = true;

            String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile); // /
                                                                                                // not
                                                                                                // before
                                                                                                // getAvailableRemotePath()
                                                                                                // !!!
            expectedFile = new File(expectedPath);
            
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            // check location of local file; if not the expected, copy to a
            // temporal file before upload (if COPY is the expected behaviour)
            if (!mOriginalStoragePath.equals(expectedPath) &&
                    mLocalBehaviour == FileUploadService.LOCAL_BEHAVIOUR_COPY) {


                if (FileStorageUtils.getUsableSpace(mAccount.name) < originalFile.length()) {
                    result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_FULL);
                    return result; // error condition when the file should be
                                   // copied

                } else {

                    String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) +
                            mFile.getRemotePath();
                    mFile.setStoragePath(temporalPath);
                    temporalFile = new File(temporalPath);

                    File temporalParent = temporalFile.getParentFile();
                    temporalParent.mkdirs();
                    if (!temporalParent.isDirectory()) {
                        throw new IOException(
                                "Unexpected error: parent directory could not be created");
                    }
                    temporalFile.createNewFile();
                    if (!temporalFile.isFile()) {
                        throw new IOException(
                                "Unexpected error: target file could not be created");
                    }

                    InputStream in = null;
                    OutputStream out = null;

                    try {

                        // In case document provider schema as 'content://'
                        if (mOriginalStoragePath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {

                            Uri uri = Uri.parse(mOriginalStoragePath);

                            in = MainApp.getAppContext().getContentResolver().openInputStream(uri);
                            out = new FileOutputStream(temporalFile);

                            int nRead;
                            byte[] data = new byte[16384];

                            while (!mCancellationRequested.get() &&
                                    (nRead = in.read(data, 0, data.length)) != -1) {
                                out.write(data, 0, nRead);
                            }
                            out.flush();

                        } else {
                            if (!mOriginalStoragePath.equals(temporalPath)) { // preventing
                                                                          // weird
                                                                          // but
                                                                          // possible
                                                                          // situation

                                in = new FileInputStream(originalFile);
                                out = new FileOutputStream(temporalFile);
                                byte[] buf = new byte[1024];
                                int len;
                                while (!mCancellationRequested.get() && (len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                            }
                        }

                        if (mCancellationRequested.get()) {
                            result = new RemoteOperationResult(new OperationCancelledException());
                        }


                    } catch (Exception e) {
                        result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_COPIED);
                        return result;

                    } finally {
                        try {
                            if (in != null)
                                in.close();
                        } catch (Exception e) {
                            Log_OC.d(TAG, "Weird exception while closing input stream for " +
                                    mOriginalStoragePath + " (ignoring)", e);
                        }
                        try {
                            if (out != null)
                                out.close();
                        } catch (Exception e) {
                            Log_OC.d(TAG, "Weird exception while closing output stream for " +
                                    expectedPath + " (ignoring)", e);
                        }
                    }
                }
            }
            localCopyPassed = (result == null);
            
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            /// perform the upload
            if ( mChunked &&
                    (new File(mFile.getStoragePath())).length() >
                            ChunkedUploadRemoteFileOperation.CHUNK_SIZE ) {
                mUploadOperation = new ChunkedUploadRemoteFileOperation(mFile.getStoragePath(),
                        mFile.getRemotePath(), mFile.getMimetype(), mFile.getEtagInConflict());
            } else {
                mUploadOperation = new UploadRemoteFileOperation(mFile.getStoragePath(),
                        mFile.getRemotePath(), mFile.getMimetype(), mFile.getEtagInConflict());
            }
            Iterator <OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            result = mUploadOperation.execute(client);

            /// move local temporal file or original file to its corresponding
            // location in the ownCloud local folder
            if (result.isSuccess()) {
                if (mLocalBehaviour == FileUploadService.LOCAL_BEHAVIOUR_FORGET) {
                    mFile.setStoragePath("");
                } else {
                    mFile.setStoragePath(expectedPath);
                    File fileToMove = null;
                    if (temporalFile != null) { // FileUploader.LOCAL_BEHAVIOUR_COPY
                                                // ; see where temporalFile was
                                                // set
                        fileToMove = temporalFile;
                    } else { // FileUploader.LOCAL_BEHAVIOUR_MOVE
                        fileToMove = originalFile;
                    }
                    if (!expectedFile.equals(fileToMove)) {
                        File expectedFolder = expectedFile.getParentFile();
                        expectedFolder.mkdirs();

                        if (expectedFolder.isDirectory()){
                            if (!fileToMove.renameTo(expectedFile)){
                                // try to copy and then delete
                                expectedFile.createNewFile();
                                FileChannel inChannel = new FileInputStream(fileToMove).getChannel();
                                FileChannel outChannel = new FileOutputStream(expectedFile).getChannel();

                                try {
                                    inChannel.transferTo(0, inChannel.size(), outChannel);
                                    fileToMove.delete();
                                } catch (Exception e){
                                    mFile.setStoragePath(null); // forget the local file
                                    // by now, treat this as a success; the file was
                                    // uploaded; the user won't like that the local file
                                    // is not linked, but this should be a very rare
                                    // fail;
                                    // the best option could be show a warning message
                                    // (but not a fail)
                                    // result = new
                                    // RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_MOVED);
                                    // return result;
                                }
                                finally {
                                    if (inChannel != null) inChannel.close();
                                    if (outChannel != null) outChannel.close();
                                }
                            }

                        } else {
                            mFile.setStoragePath(null);
                        }
                    }
                }
                FileDataStorageManager.triggerMediaScan(originalFile.getAbsolutePath());
                FileDataStorageManager.triggerMediaScan(expectedFile.getAbsolutePath());
            } else if (result.getHttpCode() == HttpStatus.SC_PRECONDITION_FAILED ) {
                result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);

        } finally {
            mUploadStarted.set(false);
            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }
            if (result.isSuccess()) {
                Log_OC.i(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath + ": " +
                        result.getLogMessage());
            } else {
                if (result.getException() != null) {
                    String complement = "";
                    if (!nameCheckPassed) {
                        complement = " (while checking file existence in server)";
                    } else if (!localCopyPassed) {
                        complement = " (while copying local file to " +
                                FileStorageUtils.getSavePath(mAccount.name)
                                + ")";
                    }
                    if(result.isCancelled()){
                        Log_OC.w(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                                ": " + result.getLogMessage());
                    } else {
                        Log_OC.e(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                                ": " + result.getLogMessage() + complement, result.getException());
                    }                    

                } else {
                    Log_OC.e(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath +
                            ": " + result.getLogMessage());
                }
            }
        }

        return result;
    }

    /**
     * Create a new OCFile mFile with new remote path. This is required if forceOverwrite==false.
     * New file is stored as mFile, original as mOldFile. 
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
                mFile.getModificationTimestampAtLastSyncForData());
        newFile.setEtag(mFile.getEtag());
        newFile.setFavorite(mFile.isFavorite());
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
     * @param wc
     * @param remotePath
     * @return
     */
    private String getAvailableRemotePath(OwnCloudClient wc, String remotePath) throws Exception {
        boolean check = existsFile(wc, remotePath);
        if (!check) {
            return remotePath;
        }

        int pos = remotePath.lastIndexOf(".");
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
            }
            else {
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

    private boolean existsFile(OwnCloudClient client, String remotePath){
        ExistenceCheckRemoteOperation existsOperation =
                new ExistenceCheckRemoteOperation(remotePath, mContext, false);
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
}
