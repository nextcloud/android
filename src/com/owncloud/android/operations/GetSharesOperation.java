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

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.ShareType;
import com.owncloud.android.lib.operations.remote.GetRemoteSharesOperation;
import com.owncloud.android.lib.utils.FileUtils;
import com.owncloud.android.utils.Log_OC;

/**
 * Access to remote operation to get the share files/folders
 * Save the data in Database
 * 
 * @author masensio
 */

public class GetSharesOperation extends RemoteOperation {

    private static final String TAG = GetSharesOperation.class.getSimpleName();

    protected FileDataStorageManager mStorageManager;


    public GetSharesOperation(FileDataStorageManager storageManager) {
        mStorageManager = storageManager;
    }

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

        if (shares.size() > 0) {
            // Save share file
            mStorageManager.saveShares(shares);

            ArrayList<OCFile> sharedFiles = new ArrayList<OCFile>();

            for (OCShare share : shares) {
                // Get the path
                String path = share.getPath();
                if (share.isDirectory()) {
                    path = path + FileUtils.PATH_SEPARATOR;
                }           

                // Update OCFile with data from share: ShareByLink  ¿and publicLink?
                OCFile file = mStorageManager.getFileByPath(path);
                if (file != null) {
                    if (share.getShareType().equals(ShareType.PUBLIC_LINK)) {
                        file.setShareByLink(true);
                        sharedFiles.add(file);
                    }
                } 
            }
            
            if (sharedFiles.size() > 0) {
                mStorageManager.updateSharedFiles(sharedFiles);
            }
        }
    }

}
