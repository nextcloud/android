/* ownCloud Android client application
 *   Copyright (C) 2012-2013  ownCloud Inc.
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

package com.owncloud.android.ui.fragment;

import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.TransferServiceGetter;


/**
 * Common methods for {@link Fragment}s containing {@link OCFile}s
 * 
 * @author David A. Velasco
 *
 */
public class FileFragment extends SherlockFragment {
    
    private OCFile mFile;


    /**
     * Creates an empty fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileFragment() {
        mFile = null;
    }
    
    /**
     * Creates an instance for a given {@OCFile}.
     * 
     * @param file
     */
    public FileFragment(OCFile file) {
        mFile = file;
    }

    /**
     * Getter for the hold {@link OCFile}
     * 
     * @return The {@link OCFile} hold
     */
    public OCFile getFile() {
        return mFile;
    }
    
    
    protected void setFile(OCFile file) {
        mFile = file;
    }

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
        public void showDetails(OCFile file);

    }
    
}
