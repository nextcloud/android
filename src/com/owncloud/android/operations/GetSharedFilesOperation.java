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
import com.owncloud.android.datamodel.OCShare;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.ShareRemoteFile;
import com.owncloud.android.oc_framework.operations.ShareType;
import com.owncloud.android.oc_framework.operations.remote.GetRemoteSharedFilesOperation;
import com.owncloud.android.oc_framework.utils.FileUtils;
import com.owncloud.android.utils.Log_OC;

/**
 * Access to remote operation to get the share files/folders
 * Save the data in Database
 * 
 * @author masensio
 */

public class GetSharedFilesOperation extends RemoteOperation {
    
    private static final String TAG = GetSharedFilesOperation.class.getSimpleName();

    private String mUrlServer;
    protected FileDataStorageManager mStorageManager;
    

    public GetSharedFilesOperation(String urlServer, FileDataStorageManager storageManager) {
        mUrlServer = urlServer;
        mStorageManager = storageManager;
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        GetRemoteSharedFilesOperation operation = new GetRemoteSharedFilesOperation(mUrlServer);
        RemoteOperationResult result = operation.execute(client);
        
        if (result.isSuccess()) {
            
            // Update DB with the response
            ArrayList<ShareRemoteFile> shareRemoteFiles = operation.getSharedFiles();
            Log_OC.d(TAG, "Share list size = " + shareRemoteFiles.size());
            for (ShareRemoteFile remoteFile: shareRemoteFiles) {
                OCShare shareFile = new OCShare(remoteFile);
                saveShareFileInDB(shareFile);
            }
        }
        
        return result;
    }

    private void saveShareFileInDB(OCShare shareFile) {
        // Save share file
        mStorageManager.saveShareFile(shareFile);
        
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
