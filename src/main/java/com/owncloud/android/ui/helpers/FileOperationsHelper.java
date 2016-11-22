/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author Juan Carlos González Cabrero
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

package com.owncloud.android.ui.helpers;

import android.accounts.Account;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;
import com.owncloud.android.ui.events.FavoriteEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class FileOperationsHelper {

    private static final String TAG = FileOperationsHelper.class.getSimpleName();
    
    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";

    protected FileActivity mFileActivity = null;

    /// Identifier of operation in progress which result shouldn't be lost 
    private long mWaitingForOpId = Long.MAX_VALUE;

	private static final Pattern mPatternUrl = Pattern.compile("^URL=(.+)$");
	private static final Pattern mPatternString = Pattern.compile("<string>(.+)</string>");

    public FileOperationsHelper(FileActivity fileActivity) {
        mFileActivity = fileActivity;
    }

	@Nullable
    private String getUrlFromFile(String storagePath, Pattern pattern) {
        String url = null;

        InputStreamReader fr = null;
        BufferedReader br = null;
        try {
            fr =  new InputStreamReader(new FileInputStream(storagePath), "UTF8");
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    url = m.group(1);
                    break;
                }
            }
        } catch (IOException e) {
			Log_OC.d(TAG, e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log_OC.d(TAG, "Error closing buffered reader for URL file", e);
                }
            }

            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Log_OC.d(TAG, "Error closing file reader for URL file", e);
                }
            }
        }
        return url;
	}
	
    @Nullable
    private Intent createIntentFromFile(String storagePath) {
        String url = null;
        int lastIndexOfDot = storagePath.lastIndexOf('.');
        if (lastIndexOfDot >= 0) {
            String fileExt = storagePath.substring(lastIndexOfDot + 1);
            if (fileExt.equalsIgnoreCase("url") ||fileExt.equalsIgnoreCase("desktop")) {
				// Windows internet shortcut file .url
				// Ubuntu internet shortcut file .desktop
                url = getUrlFromFile(storagePath, mPatternUrl);
            } else if (fileExt.equalsIgnoreCase("webloc")) {
				// mac internet shortcut file .webloc
                url = getUrlFromFile(storagePath, mPatternString);
            }
        }
        if (url == null) {
            return null;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public void openFile(OCFile file) {
        if (file != null) {
            String storagePath = file.getStoragePath();

            Intent openFileWithIntent = null;
            int lastIndexOfDot = storagePath.lastIndexOf('.');
            if (lastIndexOfDot >= 0) {
                String fileExt = storagePath.substring(lastIndexOfDot + 1);
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt);
                if (guessedMimeType != null) {
                    openFileWithIntent = new Intent(Intent.ACTION_VIEW);
                    openFileWithIntent.setDataAndType(
                            file.getExposedFileUri(mFileActivity),
                            guessedMimeType
                    );
                }
            }

            if(openFileWithIntent == null) {
                openFileWithIntent = createIntentFromFile(storagePath);
            }

            if (openFileWithIntent == null) {
                openFileWithIntent = new Intent(Intent.ACTION_VIEW);
                openFileWithIntent.setDataAndType(
                        file.getExposedFileUri(mFileActivity),
                        file.getMimetype()
                );
            }

            openFileWithIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            List<ResolveInfo> launchables = mFileActivity.getPackageManager().
                    queryIntentActivities(openFileWithIntent, PackageManager.GET_INTENT_FILTERS);

            if(launchables != null && launchables.size() > 0) {
                try {
                    mFileActivity.startActivity(
                            Intent.createChooser(
                                    openFileWithIntent, mFileActivity.getString(R.string.actionbar_open_with)
                            )
                    );
                } catch (ActivityNotFoundException anfe) {
                    showNoAppForFileTypeToast(mFileActivity.getApplicationContext());
                }
            } else {
                showNoAppForFileTypeToast(mFileActivity.getApplicationContext());
            }

        } else {
            Log_OC.e(TAG, "Trying to open a NULL OCFile");
        }
    }

    /**
     * Displays a toast stating that no application could be found to open the file.
     *
     * @param context the context to be able to show a toast.
     */
    private void showNoAppForFileTypeToast(Context context) {
        Toast.makeText(context,
                R.string.file_list_no_app_for_file_type, Toast.LENGTH_SHORT)
                .show();
    }


    /**
     * Helper method to share a file via a public link. Starts a request to do it in {@link OperationsService}
     *
     * @param file          The file to share.
     * @param password      Optional password to protect the public share.
     */
    public void shareFileViaLink(OCFile file, String password) {
        if (isSharedSupported()) {
            if (file != null) {
                mFileActivity.showLoadingDialog(
                        mFileActivity.getApplicationContext().
                                getString(R.string.wait_a_moment)
                );
                Intent service = new Intent(mFileActivity, OperationsService.class);
                service.setAction(OperationsService.ACTION_CREATE_SHARE_VIA_LINK);
                service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
                if (password != null && password.length() > 0) {
                    service.putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password);
                }
                service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
                mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

            } else {
                Log_OC.e(TAG, "Trying to share a NULL OCFile");
                // TODO user-level error?
            }

        } else {
            // Show a Message
            Toast t = Toast.makeText(
                    mFileActivity, mFileActivity.getString(R.string.share_link_no_support_share_api),
                    Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    public void getFileWithLink(OCFile file){
        if (isSharedSupported()) {
            if (file != null) {
                mFileActivity.showLoadingDialog(mFileActivity.getApplicationContext().
                        getString(R.string.wait_a_moment));

                Intent service = new Intent(mFileActivity, OperationsService.class);
                service.setAction(OperationsService.ACTION_CREATE_SHARE_VIA_LINK);
                service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
                service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
                mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

            } else {
                Log_OC.e(TAG, "Trying to share a NULL OCFile");
            }
        } else {
            // Show a Message
            Toast t = Toast.makeText(
                    mFileActivity, mFileActivity.getString(R.string.share_link_no_support_share_api),
                    Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    /**
     * Helper method to share a file with a known sharee. Starts a request to do it in {@link OperationsService}
     *
     * @param file          The file to share.
     * @param shareeName    Name (user name or group name) of the target sharee.
     * @param shareType     The share type determines the sharee type.
     * @param permissions   Permissions to grant to sharee on the shared file.
     */
    public void shareFileWithSharee(OCFile file, String shareeName, ShareType shareType, int permissions) {
        if (file != null) {
            // TODO check capability?
            mFileActivity.showLoadingDialog(mFileActivity.getApplicationContext().
                getString(R.string.wait_a_moment));

            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_CREATE_SHARE_WITH_SHAREE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_SHARE_WITH, shareeName);
            service.putExtra(OperationsService.EXTRA_SHARE_TYPE, shareType);
            service.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        } else {
            Log_OC.e(TAG, "Trying to share a NULL OCFile");
        }
    }


    /**
     * @return 'True' if the server supports the Share API
     */
    public boolean isSharedSupported() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isSharedSupported());
        }
        return false;
    }


    /**
     * Helper method to unshare a file publicly shared via link.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param file      The file to unshare.
     */
    public void unshareFileViaLink(OCFile file) {

        // Unshare the file: Create the intent
        Intent unshareService = new Intent(mFileActivity, OperationsService.class);
        unshareService.setAction(OperationsService.ACTION_UNSHARE);
        unshareService.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        unshareService.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        unshareService.putExtra(OperationsService.EXTRA_SHARE_TYPE, ShareType.PUBLIC_LINK);
        unshareService.putExtra(OperationsService.EXTRA_SHARE_WITH, "");

        queueShareIntent(unshareService);
    }

    public void unshareFileWithUserOrGroup(OCFile file, ShareType shareType, String userOrGroup){

        // Unshare the file: Create the intent
        Intent unshareService = new Intent(mFileActivity, OperationsService.class);
        unshareService.setAction(OperationsService.ACTION_UNSHARE);
        unshareService.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        unshareService.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        unshareService.putExtra(OperationsService.EXTRA_SHARE_TYPE, shareType);
        unshareService.putExtra(OperationsService.EXTRA_SHARE_WITH, userOrGroup);

        queueShareIntent(unshareService);
    }


    private void queueShareIntent(Intent shareIntent){
        if (isSharedSupported()) {
            // Unshare the file
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().
                    queueNewOperation(shareIntent);

            mFileActivity.showLoadingDialog(mFileActivity.getApplicationContext().
                    getString(R.string.wait_a_moment));

        } else {
            // Show a Message
            Toast t = Toast.makeText(mFileActivity,
                    mFileActivity.getString(R.string.share_link_no_support_share_api),
                    Toast.LENGTH_LONG);
            t.show();

        }
    }

    /**
     * Show an instance of {@link ShareType} for sharing or unsharing the {@OCFile} received as parameter.
     *
     * @param file  File to share or unshare.
     */
    public void showShareFile(OCFile file){
        Intent intent = new Intent(mFileActivity, ShareActivity.class);
        intent.putExtra(mFileActivity.EXTRA_FILE, (Parcelable) file);
        intent.putExtra(mFileActivity.EXTRA_ACCOUNT, mFileActivity.getAccount());
        mFileActivity.startActivity(intent);

    }


    /**
     * Updates a public share on a file to set its password.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param file          File which public share will be protected with a password.
     * @param password      Password to set for the public link; null or empty string to clear
     *                      the current password
     */
    public void setPasswordToShareViaLink(OCFile file, String password) {
        // Set password updating share
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PASSWORD,
                (password == null) ? "" : password
        );

        queueShareIntent(updateShareIntent);
    }


    /**
     * Updates a public share on a file to set its expiration date.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param file                      File which public share will be constrained with an expiration date.
     * @param expirationTimeInMillis    Expiration date to set. A negative value clears the current expiration
     *                                  date, leaving the link unrestricted. Zero makes no change.
     */
    public void setExpirationDateToShareViaLink(OCFile file, long expirationTimeInMillis) {
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS,
                expirationTimeInMillis
        );
        queueShareIntent(updateShareIntent);
    }


    /**
     * Updates a share on a file to set its access permissions.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share                     {@link OCShare} instance which permissions will be updated.
     * @param permissions               New permissions to set. A value <= 0 makes no update.
     */
    public void setPermissionsToShare(OCShare share, int permissions) {
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PERMISSIONS,
                permissions
        );
        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates a public share on a folder to set its editing permission.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param folder                     Folder which editing permission of his public share will be modified.
     * @param uploadPermission          New state of the permission for editing the folder shared via link.
     */
    public void setUploadPermissionsToShare(OCFile folder, boolean uploadPermission) {
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, folder.getRemotePath());
        updateShareIntent.putExtra(
                OperationsService.EXTRA_SHARE_PUBLIC_UPLOAD,
                uploadPermission
        );
        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates a public share on a folder to set its hide file listing permission.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share                    {@link OCShare} instance which permissions will be updated.
     * @param hideFileListing          New state of the permission for editing the folder shared via link.
     */
    public void setHideFileListingPermissionsToShare(OCShare share, boolean hideFileListing) {
        Intent updateShareIntent = new Intent(mFileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());

        if (hideFileListing) {
            updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, OCShare.CREATE_PERMISSION_FLAG);
        } else {
            updateShareIntent.
                    putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS,
                            OCShare.FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9);
        }

        queueShareIntent(updateShareIntent);
    }

    /**
     * @return 'True' if the server supports the Search Users API
     */
    public boolean isSearchUserSupportedSupported() {
        if (mFileActivity.getAccount() != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isSearchUsersSupported());
        }
        return false;
    }

    public void sendDownloadedFile(OCFile file) {
        if (file != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            // set MimeType
            sendIntent.setType(file.getMimetype());
            sendIntent.putExtra(
                Intent.EXTRA_STREAM,
                file.getExposedFileUri(mFileActivity)
            );
            sendIntent.putExtra(Intent.ACTION_SEND, true);      // Send Action

            // Show dialog, without the own app
            String[] packagesToExclude = new String[]{mFileActivity.getPackageName()};
            DialogFragment chooserDialog = ShareLinkToDialog.newInstance(sendIntent, packagesToExclude);
            chooserDialog.show(mFileActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);

        } else {
            Log_OC.e(TAG, "Trying to send a NULL OCFile");
        }
    }

    public void syncFiles(Collection<OCFile> files) {
        for (OCFile file: files) {
            syncFile(file);
        }
    }

    /**
     * Request the synchronization of a file or folder with the OC server, including its contents.
     *
     * @param file          The file or folder to synchronize
     */
    public void syncFile(OCFile file) {
        if (!file.isFolder()){
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FILE);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            intent.putExtra(OperationsService.EXTRA_SYNC_FILE_CONTENTS, true);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(intent);
            mFileActivity.showLoadingDialog(mFileActivity.getApplicationContext().
                getString(R.string.wait_a_moment));
            
        } else {
            Intent intent = new Intent(mFileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            mFileActivity.startService(intent);

        }
    }

    public void toggleFavoriteFiles(Collection<OCFile> files, boolean shouldBeFavorite) {
        List<OCFile> alreadyRightStateList = new ArrayList<>();
        for(OCFile file : files) {
            if(file.getIsFavorite() == shouldBeFavorite) {
                alreadyRightStateList.add(file);
            }
        }

        files.removeAll(alreadyRightStateList);

        for (OCFile file: files) {
            toggleFavoriteFile(file, shouldBeFavorite);
        }
    }

    public void toggleFavoriteFile(OCFile file, boolean shouldBeFavorite) {
        if(file.getIsFavorite() != shouldBeFavorite) {
            EventBus.getDefault().post(new FavoriteEvent(file.getRemotePath(), shouldBeFavorite, file.getRemoteId()));
        }
    }


    public void toogleOfflineFiles(Collection<OCFile> files, boolean isAvailableOffline){
        List<OCFile> alreadyRightStateList = new ArrayList<>();
        for(OCFile file : files) {
            if(file.isAvailableOffline() == isAvailableOffline) {
                alreadyRightStateList.add(file);
            }
        }

        files.removeAll(alreadyRightStateList);

        for (OCFile file: files) {
            toggleOfflineFile(file, isAvailableOffline);
        }
    }


    public void toggleOfflineFile(OCFile file, boolean isAvailableOffline) {
        if (file.isAvailableOffline() != isAvailableOffline) {
            file.setAvailableOffline(isAvailableOffline);
            mFileActivity.getStorageManager().saveFile(file);

            /// register the OCFile instance in the observer service to monitor local updates
            Intent observedFileIntent = FileObserverService.makeObservedFileIntent(
                    mFileActivity,
                    file,
                    mFileActivity.getAccount(),
                    isAvailableOffline);
            mFileActivity.startService(observedFileIntent);

            /// immediate content synchronization
            if (file.isAvailableOffline()) {
                syncFile(file);
            }
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
        
        mFileActivity.showLoadingDialog(mFileActivity.getString(R.string.wait_a_moment));
    }


    /**
     * Start operations to delete one or several files
     *
     * @param files             Files to delete
     * @param onlyLocalCopy     When 'true' only local copy of the files is removed; otherwise files are also deleted
     *                          in the server.
     */
    public void removeFiles(Collection<OCFile> files, boolean onlyLocalCopy) {
        for (OCFile file : files) {
            // RemoveFile
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_REMOVE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy);
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        
        mFileActivity.showLoadingDialog(mFileActivity.getString(R.string.wait_a_moment));
    }


    public void createFolder(String remotePath, boolean createFullPath) {
        // Create Folder
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CREATE_FOLDER);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath);
        service.putExtra(OperationsService.EXTRA_CREATE_FULL_PATH, createFullPath);
        mWaitingForOpId =  mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        
        mFileActivity.showLoadingDialog(mFileActivity.getString(R.string.wait_a_moment));
    }

    /**
     * Cancel the transference in downloads (files/folders) and file uploads
     * @param file OCFile
     */
    public void cancelTransference(OCFile file) {
        Account account = mFileActivity.getAccount();
        if (file.isFolder()) {
            OperationsService.OperationsServiceBinder opsBinder =
                    mFileActivity.getOperationsServiceBinder();
            if (opsBinder != null) {
                opsBinder.cancel(account, file);
            }
        }

        // for both files and folders
        FileDownloaderBinder downloaderBinder = mFileActivity.getFileDownloaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(account, file)) {
            downloaderBinder.cancel(account, file);
        }
        FileUploaderBinder uploaderBinder = mFileActivity.getFileUploaderBinder();
        if (uploaderBinder != null && uploaderBinder.isUploading(account, file)) {
            uploaderBinder.cancel(account, file);
        }
    }

    /**
     * Start operations to move one or several files
     *
     * @param files            Files to move
     * @param targetFolder     Folder where the files while be moved into
     */
    public void moveFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_MOVE_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        mFileActivity.showLoadingDialog(mFileActivity.getString(R.string.wait_a_moment));
    }

    /**
     * Start operations to copy one or several files
     *
     * @param files            Files to copy
     * @param targetFolder     Folder where the files while be copied into
     */
    public void copyFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(mFileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_COPY_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, mFileActivity.getAccount());
            mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        mFileActivity.showLoadingDialog(mFileActivity.getString(R.string.wait_a_moment));
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
            OwnCloudVersion serverVersion =
                    AccountUtils.getServerVersion(mFileActivity.getAccount());
            return (serverVersion != null && serverVersion.isVersionWithForbiddenCharacters());
        }
        return false;
    }

    /**
     * Starts a check of the currenlty stored credentials for the given account.
     *
     * @param account       OC account which credentials will be checked.
     */
    public void checkCurrentCredentials(Account account) {
        Intent service = new Intent(mFileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CHECK_CURRENT_CREDENTIALS);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, account);
        mWaitingForOpId = mFileActivity.getOperationsServiceBinder().queueNewOperation(service);

        mFileActivity.showLoadingDialog(
            mFileActivity.getString(R.string.wait_checking_credentials)
        );
    }
}
