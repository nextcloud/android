/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.http.HttpStatus;

import android.accounts.Account;
import android.util.Log;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.network.ProgressiveDataTransferer;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.FileRequestEntity;
import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * Remote operation performing the upload of a file to an ownCloud server
 * 
 * @author David A. Velasco
 */
public class UploadFileOperation extends RemoteOperation {

    private static final String TAG = UploadFileOperation.class.getSimpleName();

    private Account mAccount;
    private OCFile mFile;
    private OCFile mOldFile;
    private String mRemotePath = null;
    private boolean mIsInstant = false;
    private boolean mRemoteFolderToBeCreated = false;
    private boolean mForceOverwrite = false;
    private int mLocalBehaviour = FileUploader.LOCAL_BEHAVIOUR_COPY;
    private boolean mWasRenamed = false;
    private String mOriginalFileName = null;
    private String mOriginalStoragePath = null;
    PutMethod mPutMethod = null;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);

    protected RequestEntity mEntity = null;

    
    public UploadFileOperation( Account account,
                                OCFile file,
                                boolean isInstant, 
                                boolean forceOverwrite,
                                int localBehaviour) {
        if (account == null)
            throw new IllegalArgumentException("Illegal NULL account in UploadFileOperation creation");
        if (file == null)
            throw new IllegalArgumentException("Illegal NULL file in UploadFileOperation creation");
        if (file.getStoragePath() == null || file.getStoragePath().length() <= 0
                || !(new File(file.getStoragePath()).exists())) {
            throw new IllegalArgumentException(
                    "Illegal file in UploadFileOperation; storage path invalid or file not found: "
                            + file.getStoragePath());
        }

        mAccount = account;
        mFile = file;
        mRemotePath = file.getRemotePath();
        mIsInstant = isInstant;
        mForceOverwrite = forceOverwrite;
        mLocalBehaviour = localBehaviour;
        mOriginalStoragePath = mFile.getStoragePath();
        mOriginalFileName = mFile.getFileName();
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

    public boolean isInstant() {
        return mIsInstant;
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
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).removeDatatransferProgressListener(listener);
        }
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
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

            // / check location of local file; if not the expected, copy to a
            // temporal file before upload (if COPY is the expected behaviour)
            if (!mOriginalStoragePath.equals(expectedPath) && mLocalBehaviour == FileUploader.LOCAL_BEHAVIOUR_COPY) {

                if (FileStorageUtils.getUsableSpace(mAccount.name) < originalFile.length()) {
                    result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_FULL);
                    return result; // error condition when the file should be
                                   // copied

                } else {
                    String temporalPath = FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
                    mFile.setStoragePath(temporalPath);
                    temporalFile = new File(temporalPath);
                    if (!mOriginalStoragePath.equals(temporalPath)) { // preventing
                                                                      // weird
                                                                      // but
                                                                      // possible
                                                                      // situation
                        InputStream in = null;
                        OutputStream out = null;
                        try {
                            File temporalParent = temporalFile.getParentFile();
                            temporalParent.mkdirs();
                            if (!temporalParent.isDirectory()) {
                                throw new IOException("Unexpected error: parent directory could not be created");
                            }
                            temporalFile.createNewFile();
                            if (!temporalFile.isFile()) {
                                throw new IOException("Unexpected error: target file could not be created");
                            }
                            in = new FileInputStream(originalFile);
                            out = new FileOutputStream(temporalFile);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }

                        } catch (Exception e) {
                            result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_COPIED);
                            return result;

                        } finally {
                            try {
                                if (in != null)
                                    in.close();
                            } catch (Exception e) {
                                Log.d(TAG, "Weird exception while closing input stream for " + mOriginalStoragePath
                                        + " (ignoring)", e);
                            }
                            try {
                                if (out != null)
                                    out.close();
                            } catch (Exception e) {
                                Log.d(TAG, "Weird exception while closing output stream for " + expectedPath
                                        + " (ignoring)", e);
                            }
                        }
                    }
                }
            }
            localCopyPassed = true;

            // / perform the upload
            synchronized (mCancellationRequested) {
                if (mCancellationRequested.get()) {
                    throw new OperationCancelledException();
                } else {
                    mPutMethod = new PutMethod(client.getBaseUri() + WebdavUtils.encodePath(mFile.getRemotePath()));
                }
            }
            int status = uploadFile(client);

            // / move local temporal file or original file to its corresponding
            // location in the ownCloud local folder
            if (isSuccess(status)) {
                if (mLocalBehaviour == FileUploader.LOCAL_BEHAVIOUR_FORGET) {
                    mFile.setStoragePath(null);

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
                        if (!expectedFolder.isDirectory() || !fileToMove.renameTo(expectedFile)) {
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
                    }
                }
            }

            result = new RemoteOperationResult(isSuccess(status), status);

        } catch (Exception e) {
            // TODO something cleaner with cancellations
            if (mCancellationRequested.get()) {
                result = new RemoteOperationResult(new OperationCancelledException());
            } else {
                result = new RemoteOperationResult(e);
            }

        } finally {
            if (temporalFile != null && !originalFile.equals(temporalFile)) {
                temporalFile.delete();
            }
            if (result.isSuccess()) {
                Log.i(TAG, "Upload of " + mOriginalStoragePath + " to " + mRemotePath + ": " + result.getLogMessage());

            } else {
                if (result.getException() != null) {
                    String complement = "";
                    if (!nameCheckPassed) {
                        complement = " (while checking file existence in server)";
                    } else if (!localCopyPassed) {
                        complement = " (while copying local file to " + FileStorageUtils.getSavePath(mAccount.name)
                                + ")";
                    }
                    Log.e(TAG,
                            "Upload of " + mOriginalStoragePath + " to " + mRemotePath + ": " + result.getLogMessage()
                                    + complement, result.getException());
                } else {
                    Log.e(TAG,
                            "Upload of " + mOriginalStoragePath + " to " + mRemotePath + ": " + result.getLogMessage());
                }
            }
        }

        return result;
    }

    private void createNewOCFile(String newRemotePath) {
        // a new OCFile instance must be created for a new remote path
        OCFile newFile = new OCFile(newRemotePath);
        newFile.setCreationTimestamp(mFile.getCreationTimestamp());
        newFile.setFileLength(mFile.getFileLength());
        newFile.setMimetype(mFile.getMimetype());
        newFile.setModificationTimestamp(mFile.getModificationTimestamp());
        newFile.setModificationTimestampAtLastSyncForData(mFile.getModificationTimestampAtLastSyncForData());
        // newFile.setEtag(mFile.getEtag())
        newFile.setKeepInSync(mFile.keepInSync());
        newFile.setLastSyncDateForProperties(mFile.getLastSyncDateForProperties());
        newFile.setLastSyncDateForData(mFile.getLastSyncDateForData());
        newFile.setStoragePath(mFile.getStoragePath());
        newFile.setParentId(mFile.getParentId());
        mOldFile = mFile;
        mFile = newFile;
    }

    public boolean isSuccess(int status) {
        return ((status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT));
    }

    protected int uploadFile(WebdavClient client) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        try {
            File f = new File(mFile.getStoragePath());
            mEntity  = new FileRequestEntity(f, getMimeType());
            synchronized (mDataTransferListeners) {
                ((ProgressiveDataTransferer)mEntity).addDatatransferProgressListeners(mDataTransferListeners);
            }
            mPutMethod.setRequestEntity(mEntity);
            status = client.executeMethod(mPutMethod);
            client.exhaustResponse(mPutMethod.getResponseBodyAsStream());

        } finally {
            mPutMethod.releaseConnection(); // let the connection available for
                                            // other methods
        }
        return status;
    }

    /**
     * Checks if remotePath does not exist in the server and returns it, or adds
     * a suffix to it in order to avoid the server file is overwritten.
     * 
     * @param string
     * @return
     */
    private String getAvailableRemotePath(WebdavClient wc, String remotePath) throws Exception {
        boolean check = wc.existsFile(remotePath);
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
            if (pos >= 0)
                check = wc.existsFile(remotePath + suffix + "." + extension);
            else
                check = wc.existsFile(remotePath + suffix);
            count++;
        } while (check);

        if (pos >= 0) {
            return remotePath + suffix + "." + extension;
        } else {
            return remotePath + suffix;
        }
    }

    public void cancel() {
        synchronized (mCancellationRequested) {
            mCancellationRequested.set(true);
            if (mPutMethod != null)
                mPutMethod.abort();
        }
    }

}
