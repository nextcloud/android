/*
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

package com.owncloud.android.operations;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.List;

/**
 * Creates a new public share for a given file
 */
public class CreateShareViaLinkOperation extends SyncOperation<List<OCShare>> {

    private final String path;
    private final String password;

    public CreateShareViaLinkOperation(String path, String password) {
        this.path = path;
        this.password = password;
    }

    @Override
    protected RemoteOperationResult<List<OCShare>> run(OwnCloudClient client) {
        CreateShareRemoteOperation createOp = new CreateShareRemoteOperation(path,
                                                                             ShareType.PUBLIC_LINK,
                                                                             "",
                                                                             false,
                                                                             password,
                                                                             OCShare.NO_PERMISSION);
        createOp.setGetShareDetails(true);
        RemoteOperationResult<List<OCShare>> result = createOp.execute(client);

        if (result.isSuccess()) {
            if (result.getResultData().size() > 0) {
                OCShare item = result.getResultData().get(0);
                if (item != null) {
                    updateData(item);
                } else {
                    List<OCShare> data = result.getResultData();
                    result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
                    result.setResultData(data);
                }
            } else {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
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
