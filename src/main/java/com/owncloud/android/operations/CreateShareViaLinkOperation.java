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
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Creates a new public share for a given file
 */
@AllArgsConstructor
@Getter
public class CreateShareViaLinkOperation extends SyncOperation {

    private String path;
    private String password;

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        // Check if the share link already exists
        RemoteOperation operation = new GetRemoteSharesForFileOperation(path, false, false);
        RemoteOperationResult result = operation.execute(client);

        // Create public link if doesn't exist yet
        boolean publicShareExists = false;
        if (result.isSuccess()) {
            OCShare share;
            for (int i=0 ; i<result.getData().size(); i++) {
                share = (OCShare) result.getData().get(i);
                if (ShareType.PUBLIC_LINK.equals(share.getShareType())) {
                    publicShareExists = true;
                    break;
                }
            }
        }
        if (!publicShareExists) {
            CreateRemoteShareOperation createOp = new CreateRemoteShareOperation(
                path,
                    ShareType.PUBLIC_LINK,
                    "",
                    false,
                password,
                    OCShare.DEFAULT_PERMISSION
            );
            createOp.setGetShareDetails(true);
            result = createOp.execute(client);
        }

        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                Object item = result.getData().get(0);
                if (item instanceof  OCShare) {
                    updateData((OCShare) item);
                } else {
                    ArrayList<Object> data = result.getData();
                    result = new RemoteOperationResult(
                        RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
                    );
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
        if (path.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setFolder(true);
        } else {
            share.setFolder(false);
        }

        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(path);
        if (file!=null) {
            file.setPublicLink(share.getShareLink());
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }
}
