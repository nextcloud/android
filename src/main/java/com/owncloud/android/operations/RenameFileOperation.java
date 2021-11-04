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

import android.text.TextUtils;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RenameFileRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.io.IOException;


/**
 * Remote operation performing the rename of a remote file (or folder?) in the ownCloud server.
 */
public class RenameFileOperation extends SyncOperation {

    private static final String TAG = RenameFileOperation.class.getSimpleName();

    private OCFile file;
    private String remotePath;
    private String newName;

    /**
     * Constructor
     *
     * @param remotePath RemotePath of the OCFile instance describing the remote file or folder to rename
     * @param newName    New name to set as the name of file.
     */
    public RenameFileOperation(String remotePath, String newName, FileDataStorageManager storageManager) {
        super(storageManager);

        this.remotePath = remotePath;
        this.newName = newName;
    }

    /**
     * Performs the rename operation.
     *
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        String newRemotePath = null;

        file = getStorageManager().getFileByPath(remotePath);

        // check if the new name is valid in the local file system
        try {
            if (!isValidNewName()) {
                return new RemoteOperationResult(ResultCode.INVALID_LOCAL_FILE_NAME);
            }
            String parent = new File(file.getRemotePath()).getParent();
            parent = parent.endsWith(OCFile.PATH_SEPARATOR) ? parent : parent + OCFile.PATH_SEPARATOR;
            newRemotePath =  parent + newName;
            if (file.isFolder()) {
                newRemotePath += OCFile.PATH_SEPARATOR;
            }

            // check local overwrite
            if (getStorageManager().getFileByPath(newRemotePath) != null) {
                return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }

            result = new RenameFileRemoteOperation(file.getFileName(),
                                                   file.getRemotePath(),
                                                   newName,
                                                   file.isFolder())
                .execute(client);

            if (result.isSuccess()) {
                if (file.isFolder()) {
                    getStorageManager().moveLocalFile(file, newRemotePath, parent);
                    //saveLocalDirectory();

                } else {
                    saveLocalFile();
                }
            }

        } catch (IOException e) {
            Log_OC.e(TAG, "Rename " + file.getRemotePath() + " to " + ((newRemotePath == null) ?
                newName : newRemotePath) + ": " +
                    (result!= null ? result.getLogMessage() : ""), e);
        }

        return result;
    }

    private void saveLocalFile() {
        file.setFileName(newName);

        // try to rename the local copy of the file
        if (file.isDown()) {
            String oldPath = file.getStoragePath();
            File f = new File(oldPath);
            String parentStoragePath = f.getParent();
            if (!parentStoragePath.endsWith(File.separator)) {
                parentStoragePath += File.separator;
            }
            if (f.renameTo(new File(parentStoragePath + newName))) {
                String newPath = parentStoragePath + newName;
                file.setStoragePath(newPath);

                // notify MediaScanner about removed file
                getStorageManager().deleteFileInMediaScan(oldPath);
                // notify to scan about new file, if it is a media file
                if (MimeTypeUtil.isMedia(file.getMimeType())) {
                    FileDataStorageManager.triggerMediaScan(newPath, file);
                }
            }
            // else - NOTHING: the link to the local file is kept although the local name
            // can't be updated
            // TODO - study conditions when this could be a problem
        }

        getStorageManager().saveFile(file);
    }

    /**
     * Checks if the new name to set is valid in the file system
     *
     * The only way to be sure is trying to create a file with that name. It's made in the
     * temporal directory for downloads, out of any account, and then removed.
     *
     * IMPORTANT: The test must be made in the same file system where files are download.
     * The internal storage could be formatted with a different file system.
     *
     * TODO move this method, and maybe FileDownload.get***Path(), to a class with utilities
     * specific for the interactions with the file system
     *
     * @return              'True' if a temporal file named with the name to set could be
     *                      created in the file system where local files are stored.
     * @throws IOException  When the temporal folder can not be created.
     */
    private boolean isValidNewName() throws IOException {
        // check tricky names
        if (TextUtils.isEmpty(newName) || newName.contains(File.separator)) {
            return false;
        }
        // create a test file
        String tmpFolderName = FileStorageUtils.getTemporalPath("");
        File testFile = new File(tmpFolderName + newName);
        File tmpFolder = testFile.getParentFile();
        if (! tmpFolder.mkdirs()) {
            Log_OC.e(TAG, "Unable to create parent folder " + tmpFolder.getAbsolutePath());
        }
        if (!tmpFolder.isDirectory()) {
            throw new IOException("Unexpected error: temporal directory could not be created");
        }
        try {
            testFile.createNewFile();   // return value is ignored; it could be 'false' because
            // the file already existed, that doesn't invalidate the name
        } catch (IOException e) {
            Log_OC.i(TAG, "Test for validity of name " + newName + " in the file system failed");
            return false;
        }
        boolean result = testFile.exists() && testFile.isFile();

        // cleaning ; result is ignored, since there is not much we could do in case of failure,
        // but repeat and repeat...
        testFile.delete();

        return result;
    }

    public OCFile getFile() {
        return this.file;
    }
}
