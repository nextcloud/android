/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author TSI-mc
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2021 TSI-mc
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
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a new private share for a given file.
 */
public class CreateShareWithShareeOperation extends SyncOperation {

    private String path;
    private String shareeName;
    private ShareType shareType;
    private int permissions;
    private String noteMessage;
    private String sharePassword;
    private boolean hideFileDownload;
    private long expirationDateInMillis;
    private String label;

    private static final List<ShareType> supportedShareTypes = new ArrayList<>(Arrays.asList(ShareType.USER,
                                                                                             ShareType.GROUP,
                                                                                             ShareType.FEDERATED,
                                                                                             ShareType.EMAIL,
                                                                                             ShareType.ROOM,
                                                                                             ShareType.CIRCLE));

    /**
     * Constructor.
     *
     * @param path        Full path of the file/folder being shared.
     * @param shareeName  User or group name of the target sharee.
     * @param shareType   Type of share determines type of sharee; {@link ShareType#USER} and {@link ShareType#GROUP}
     *                    are the only valid values for the moment.
     * @param permissions Share permissions key as detailed in https://doc.owncloud.org/server/8.2/developer_manual/core/ocs-share-api.html
     *                    .
     */
    public CreateShareWithShareeOperation(String path,
                                          String shareeName,
                                          ShareType shareType,
                                          int permissions,
                                          FileDataStorageManager storageManager) {
        super(storageManager);

        if (!supportedShareTypes.contains(shareType)) {
            throw new IllegalArgumentException("Illegal share type " + shareType);
        }
        this.path = path;
        this.shareeName = shareeName;
        this.shareType = shareType;
        this.permissions = permissions;
    }

    /**
     * Constructor.
     *
     * @param path        Full path of the file/folder being shared.
     * @param shareeName  User or group name of the target sharee.
     * @param shareType   Type of share determines type of sharee; {@link ShareType#USER} and {@link ShareType#GROUP}
     *                    are the only valid values for the moment.
     * @param permissions Share permissions key as detailed in https://doc.owncloud.org/server/8.2/developer_manual/core/ocs-share-api.html
     *                    .
     */
    public CreateShareWithShareeOperation(String path,
                                          String shareeName,
                                          ShareType shareType,
                                          int permissions,
                                          String noteMessage,
                                          String sharePassword,
                                          long expirationDateInMillis,
                                          boolean hideFileDownload,
                                          FileDataStorageManager storageManager) {
        super(storageManager);

        if (!supportedShareTypes.contains(shareType)) {
            throw new IllegalArgumentException("Illegal share type " + shareType);
        }
        this.path = path;
        this.shareeName = shareeName;
        this.shareType = shareType;
        this.permissions = permissions;
        this.expirationDateInMillis = expirationDateInMillis;
        this.hideFileDownload = hideFileDownload;
        this.noteMessage = noteMessage;
        this.sharePassword = sharePassword;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        CreateShareRemoteOperation operation = new CreateShareRemoteOperation(
            path,
            shareType,
            shareeName,
            false,
            sharePassword,
            permissions
        );
        operation.setGetShareDetails(true);
        RemoteOperationResult result = operation.execute(client);


        if (result.isSuccess() && result.getData().size() > 0) {
            OCShare share = (OCShare) result.getData().get(0);

            //once creating share link update other information
            UpdateShareInfoOperation updateShareInfoOperation = new UpdateShareInfoOperation(share, getStorageManager());
            updateShareInfoOperation.setExpirationDateInMillis(expirationDateInMillis);
            updateShareInfoOperation.setHideFileDownload(hideFileDownload);
            updateShareInfoOperation.setNote(noteMessage);
            updateShareInfoOperation.setLabel(label);

            //execute and save the result in database
            RemoteOperationResult updateShareInfoResult = updateShareInfoOperation.execute(client);
            if (updateShareInfoResult.isSuccess() && updateShareInfoResult.getData().size() > 0) {
                OCShare shareUpdated = (OCShare) updateShareInfoResult.getData().get(0);
                updateData(shareUpdated);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(path);
        share.setFolder(path.endsWith(FileUtils.PATH_SEPARATOR));
        share.setPasswordProtected(!TextUtils.isEmpty(sharePassword));
        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(path);
        if (file != null) {
            file.setSharedWithSharee(true);    // TODO - this should be done by the FileContentProvider, as part of getStorageManager().saveShare(share)
            getStorageManager().saveFile(file);
        }
    }

    public String getPath() {
        return this.path;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
