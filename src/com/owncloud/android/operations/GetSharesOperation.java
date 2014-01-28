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

    private String mUrlServer;
    protected FileDataStorageManager mStorageManager;
    

    public GetSharesOperation(String urlServer, FileDataStorageManager storageManager) {
        mUrlServer = urlServer;
        mStorageManager = storageManager;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetRemoteSharesOperation operation = new GetRemoteSharesOperation(mUrlServer);
        RemoteOperationResult result = operation.execute(client);
        
        if (result.isSuccess()) {
            
            // Clean Share data in filelist table
            mStorageManager.cleanShare();
            
            // Update DB with the response
            Log_OC.d(TAG, "Share list size = " + result.getData().size());
            for(Object obj: result.getData()) {
                saveShareDB((OCShare) obj);
            }
        }
        
        return result;
    }

    private void saveShareDB(OCShare shareFile) {
        // Save share file
        mStorageManager.saveShare(shareFile);
        
        // Get the path
        String path = shareFile.getPath();
        if (shareFile.isDirectory()) {
            path = path + FileUtils.PATH_SEPARATOR;
        }           
            
        // Update OCFile with data from share: ShareByLink  ¿and publicLink?
        OCFile file = mStorageManager.getFileByPath(path);
        if (file != null) {
            if (shareFile.getShareType().equals(ShareType.PUBLIC_LINK)) {
                file.setShareByLink(true);
                mStorageManager.saveFile(file);
            }
        } 
    }

}
