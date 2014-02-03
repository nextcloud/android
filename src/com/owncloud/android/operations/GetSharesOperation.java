/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import java.util.ArrayList;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.ShareType;
import com.owncloud.android.lib.operations.remote.GetRemoteSharesOperation;
import com.owncloud.android.lib.utils.FileUtils;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.Log_OC;

/**
 * Access to remote operation to get the share files/folders
 * Save the data in Database
 * 
 * @author masensio
 * @author David A. Velasco
 */

public class GetSharesOperation extends SyncOperation {

    private static final String TAG = GetSharesOperation.class.getSimpleName();

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetRemoteSharesOperation operation = new GetRemoteSharesOperation();
        RemoteOperationResult result = operation.execute(client);

        if (result.isSuccess()) {

            // Update DB with the response
            Log_OC.d(TAG, "Share list size = " + result.getData().size());
            ArrayList<OCShare> shares = new ArrayList<OCShare>();
            for(Object obj: result.getData()) {
                shares.add((OCShare) obj);
            }

            saveSharesDB(shares);
        }

        return result;
    }

    private void saveSharesDB(ArrayList<OCShare> shares) {
        // Save share file
        getStorageManager().saveShares(shares);

        ArrayList<OCFile> sharedFiles = new ArrayList<OCFile>();

        for (OCShare share : shares) {
            // Get the path
            String path = share.getPath();
            if (share.isDirectory()) {
                path = path + FileUtils.PATH_SEPARATOR;
            }           

            // Update OCFile with data from share: ShareByLink  ¿and publicLink?
            OCFile file = getStorageManager().getFileByPath(path);
            if (file != null) {
                if (share.getShareType().equals(ShareType.PUBLIC_LINK)) {
                    file.setShareByLink(true);
                    sharedFiles.add(file);
                }
            } 
        }
        
        getStorageManager().updateSharedFiles(sharedFiles);
    }

}
