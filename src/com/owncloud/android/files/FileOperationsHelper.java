/**
 *   ownCloud Android client application
 *
 *   @author masensio
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

package com.owncloud.android.files;

import org.apache.http.protocol.HTTP;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;

import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;

/**
 *
 */
public class FileOperationsHelper {

    private static final String TAG = FileOperationsHelper.class.getName();
    
    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG"; 

    protected FileActivity mFileActivity = null;

    /// Identifier of operation in progress which result shouldn't be lost 
    private long mWaitingForOpId = Long.MAX_VALUE;
    
    public FileOperationsHelper(FileActivity fileActivity) {
        mFileActivity = fileActivity;
    }


    public void openFile(OCFile file) {
        if (file != null) {
            String storagePath = file.getStoragePath();
            String encodedStoragePath = WebdavUtils.encodePath(storagePath);
            
            Intent intentForSavedMimeType = new Intent(Intent.ACTION_VIEW);
            intentForSavedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), file.getMimetype());
            intentForSavedMimeType.setFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            
            Intent intentForGuessedMimeType = null;
            if (storagePath.lastIndexOf('.') >= 0) {
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        storagePath.substring(storagePath.lastIndexOf('.') + 1)
                );
                if (guessedMimeType != null && !guessedMimeType.equals(file.getMimetype())) {
                    intentForGuessedMimeType = new Intent(Intent.ACTION_VIEW);
                    intentForGuessedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), guessedMimeType);
                    intentForGuessedMimeType.setFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                }
            }
            
            Intent chooserIntent;
            if (intentForGuessedMimeType != null) {
                chooserIntent = Intent.createChooser(intentForGuessedMimeType, mFileActivity.getString(R.string.actionbar_open_with));
            } else {
                chooserIntent = Intent.createChooser(intentForSavedMimeType, mFileActivity.getString(R.string.actionbar_open_with));
            }
            
            mFileActivity.startActivity(chooserIntent);
            
        } else {
            Log_OC.wtf(TAG, "Trying to open a NULL OCFile");
        }
    }
    
    
    public void shareFileWithLink(OCFile file) {
        
        if (isSharedSupported()) {
            if (file != null) {
                String link = "https://fake.url";
                Intent intent = createShareWithLinkIntent(link);
                String[] packagesToExclude = new String[] { mFileActivity.getPackageName() };
                DialogFragment chooserDialog = ShareLinkToDialog.newInstance(intent, packagesToExclude, file);
                chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);
                
            } else {
                Log_OC.wtf(TAG, "Trying to share a NULL OCFile");
            }
            
        } else {
            // Show a Message
            Toast t = Toast.makeText(
                    mFileActivity, mFileActivity.getString(R.string.share_link_no_support_share_api), Toast.LENGTH_LONG
            );
            t.show();
        }
    }
    
    
    public void shareFileWithLinkToApp(OCFile file, String password, Intent sendIntent) {
        
        if (file != null) {
            mFileActivity.showLoadingDialog();
            
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_CREATE_SHARE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_PASSWORD_SHARE, password);
            service.putExtra(OperationsService.EXTRA_SEND_INTENT, sendIntent);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
            
        } else {
            Log_OC.wtf(TAG, "Trying to open a NULL OCFile");
        }
    }
    
    
    private Intent createShareWithLinkIntent(String link) {
        Intent intentToShareLink = new Intent(Intent.ACTION_SEND);
        intentToShareLink.putExtra(Intent.EXTRA_TEXT, link);
        intentToShareLink.setType(HTTP.PLAIN_TEXT_TYPE);
        return intentToShareLink; 
    }
    
    
    /**
     *  @return 'True' if the server supports the Share API
     */
    public boolean isSharedSupported() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isSharedSupported());
        }
        return false;
    }
    
    
    public void unshareFileWithLink(OCFile file) {
        
        if (isSharedSupported()) {
            // Unshare the file
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_UNSHARE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
            
            mFileActivity.showLoadingDialog();
            
        } else {
            // Show a Message
            Toast t = Toast.makeText(mFileActivity, mFileActivity.getString(R.string.share_link_no_support_share_api), Toast.LENGTH_LONG);
            t.show();
            
        }
    }
    
    public void sendDownloadedFile(OCFile file) {
        if (file != null) {
            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            // set MimeType
            sendIntent.setType(file.getMimetype());
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getStoragePath()));
            sendIntent.putExtra(Intent.ACTION_SEND, true);      // Send Action
            
            // Show dialog, without the own app
            String[] packagesToExclude = new String[] { mFileActivity.getPackageName() };
            DialogFragment chooserDialog = ShareLinkToDialog.newInstance(sendIntent, packagesToExclude, file);
            chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);

        } else {
            Log_OC.wtf(TAG, "Trying to send a NULL OCFile");
        }
    }
    
    
    public void syncFile(OCFile file) {
        
        if (!file.isFolder()){
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FILE);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            intent.putExtra(OperationsService.EXTRA_SYNC_FILE_CONTENTS, true);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(intent);
            mFileActivity.showLoadingDialog();
            
        } else {
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            mFileActivity.startService(intent);
        }
    }
    
    public void renameFile(OCFile file, String newFilename) {
        // RenameFile
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_RENAME);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        service.putExtra(OperationsService.EXTRA_NEWNAME, newFilename);
        mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        
        mFileActivity.showLoadingDialog();
    }


    public void removeFile(OCFile file, boolean onlyLocalCopy) {
        // RemoveFile
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_REMOVE);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy);
        mWaitingForOpId =  mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        
        mFileActivity.showLoadingDialog();
    }
    
    
    public void createFolder(String remotePath, boolean createFullPath) {
        // Create Folder
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CREATE_FOLDER);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath);
        service.putExtra(OperationsService.EXTRA_CREATE_FULL_PATH, createFullPath);
        mWaitingForOpId =  mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        
        mFileActivity.showLoadingDialog();
    }

    /**
     * Cancel the transference in downloads (files/folders) and file uploads
     * @param file OCFile
     */
    public void cancelTransference(OCFile file) {
        Account account = mFileActivity.getAccount();
        if (file.isFolder()) {
            OperationsService.OperationsServiceBinder opsBinder = mFileActivity.getOperationsServiceBinder();
            if (opsBinder != null) {
                opsBinder.cancel(account, file);
            }
        }

        // for both files and folders
        FileDownloaderBinder downloaderBinder = mFileActivity.getFileDownloaderBinder();
        FileUploaderBinder uploaderBinder = mFileActivity.getFileUploaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(account, file)) {
            downloaderBinder.cancel(account, file);

            // TODO - review why is this here, and solve in a better way
            // Remove etag for parent, if file is a keep_in_sync
            if (file.keepInSync()) {
                OCFile parent = mFileActivity.getStorageManager().getFileById(file.getParentId());
                parent.setEtag("");
                mFileActivity.getStorageManager().saveFile(parent);
            }

        } else if (uploaderBinder != null && uploaderBinder.isUploading(account, file)) {
            uploaderBinder.cancel(account, file);
        }
    }

    /**
     * Start move file operation
     * @param newfile           File where it is going to be moved
     * @param currentFile       File with the previous info
     */
    public void moveFile(OCFile newfile, OCFile currentFile) {
        // Move files
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_MOVE_FILE);
        service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, newfile.getRemotePath());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, currentFile.getRemotePath());
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        mWaitingForOpId =  mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        mFileActivity.showLoadingDialog();
    }


    public long getOpIdWaitingFor() {
        return mWaitingForOpId;
    }


    public void setOpIdWaitingFor(long waitingForOpId) {
        mWaitingForOpId = waitingForOpId;
    }
    
    /**
     *  @return 'True' if the server doesn't need to check forbidden characters
     */
    public boolean isVersionWithForbiddenCharacters() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isVersionWithForbiddenCharacters());
        }
        return false;
    }
}
