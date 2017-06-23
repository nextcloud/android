/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

public interface ComponentsGetter {

    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the FileDownloader service API.
     */
    public FileDownloaderBinder getFileDownloaderBinder();

    
    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the FileUploader service API.
     */
    public FileUploaderBinder getFileUploaderBinder();

    
    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the OperationsSerivce service API.
     */
    public OperationsServiceBinder getOperationsServiceBinder();

    
    public FileDataStorageManager getStorageManager();
    
    public FileOperationsHelper getFileOperationsHelper();


}
