/* ownCloud Android client application
 *   Copyright (C) 2012-2013  ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

package com.owncloud.android.ui.fragment;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.TransferServiceGetter;

/**
 * Common methods for {@link Fragment}s containing {@link OCFile}s
 * 
 * @author David A. Velasco
 *
 */
public interface FileFragment {
    
    /**
     * Getter for the hold {@link OCFile}
     * 
     * @return The {@link OCFile} hold
     */
    public OCFile getFile();
    
    
    /**
     * Interface to implement by any Activity that includes some instance of FileFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity extends TransferServiceGetter {

        /**
         * Callback method invoked when the detail fragment wants to notice its container 
         * activity about a relevant state the file shown by the fragment.
         * 
         * Added to notify to FileDisplayActivity about the need of refresh the files list. 
         * 
         * Currently called when:
         *  - a download is started;
         *  - a rename is completed;
         *  - a deletion is completed;
         *  - the 'inSync' flag is changed;
         */
        public void onFileStateChanged();

        /**
         * Request the parent activity to show the details of an {@link OCFile}.
         * 
         * @param file      File to show details
         */
        public void showFragmentWithDetails(OCFile file);
        
        
    }
    
}
