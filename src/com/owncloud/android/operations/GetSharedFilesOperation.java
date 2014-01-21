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

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.GetRemoteSharedFilesOperation;

/**
 * Access to remote operation to get the share files/folders
 * Save the data in Database
 * 
 * @author masensio
 */

public class GetSharedFilesOperation extends RemoteOperation {

    public GetSharedFilesOperation() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        GetRemoteSharedFilesOperation operation = new GetRemoteSharedFilesOperation();
        RemoteOperationResult result = operation.execute(client);
        
        if (result.isSuccess()) {
            
        } else {
            
        }
        
        return null;
    }

}
