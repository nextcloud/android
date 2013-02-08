/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
package com.owncloud.android.files.services;

import java.io.File;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.content.Context;
import eu.alefzero.webdav.WebdavClient;

public class FileOperation {

    Context mContext;
    
    public FileOperation(Context contex){
        this.mContext = contex;
    }
    
    /**
     * Deletes a file from ownCloud - locally and remote.
     * @param file The file to delete
     * @return True on success, otherwise false
     */
    public boolean delete(OCFile file){
        
        Account account = AccountUtils.getCurrentOwnCloudAccount(mContext);
        WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(account, mContext);
        if(client.deleteFile(file.getRemotePath())){
            File localFile = new File(file.getStoragePath());
            return localFile.delete();
        }
        
        return false;
    }
    
}
