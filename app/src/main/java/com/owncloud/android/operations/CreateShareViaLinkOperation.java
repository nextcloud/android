/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

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

/**
 * Creates a new public share for a given file
 */
public class CreateShareViaLinkOperation extends SyncOperation {

    private String path;
    private String password;
    private int permissions = OCShare.NO_PERMISSION;

    public CreateShareViaLinkOperation(String path, String password, FileDataStorageManager storageManager) {
        super(storageManager);

        this.path = path;
        this.password = password;
    }

    public CreateShareViaLinkOperation(String path, FileDataStorageManager storageManager, int permissions) {
        this(path, null, storageManager);
        this.permissions = permissions;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateShareRemoteOperation createOp = new CreateShareRemoteOperation(path,
                                                                             ShareType.PUBLIC_LINK,
                                                                             "",
                                                                             false,
                                                                             password,
                                                                             permissions);
        createOp.setGetShareDetails(true);
        RemoteOperationResult result = createOp.execute(client);

        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                Object item = result.getData().get(0);
                if (item instanceof OCShare) {
                    updateData((OCShare) item);
                } else {
                    ArrayList<Object> data = result.getData();
                    result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
                    result.setData(data);
                }
            } else {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(path);
        share.setFolder(path.endsWith(FileUtils.PATH_SEPARATOR));

        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByEncryptedRemotePath(path);
        if (file != null) {
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getPassword() {
        return this.password;
    }
}
