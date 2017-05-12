/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015  ownCloud Inc.
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

import android.accounts.Account;
import android.app.Activity;
import android.support.v4.app.Fragment;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;


/**
 * Common methods for {@link Fragment}s containing {@link OCFile}s
 */
public class FileFragment extends Fragment {
    
    private OCFile mFile;
    
    protected ContainerActivity mContainerActivity;


    /**
     * Creates an empty fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when
     * tries to reinstantiate a fragment automatically.
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
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
            
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " +
                    ContainerActivity.class.getSimpleName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        mContainerActivity = null;
        super.onDetach();
    }    
    
    
    /**
     * Interface to implement by any Activity that includes some instance of FileListFragment
     * Interface to implement by any Activity that includes some instance of FileFragment
     */
    public interface ContainerActivity extends ComponentsGetter {

        /**
         * Request the parent activity to show the details of an {@link OCFile}.
         * 
         * @param file      File to show details
         */
        public void showDetails(OCFile file);

        
        ///// TO UNIFY IN A SINGLE CALLBACK METHOD - EVENT NOTIFICATIONs  -> something happened
        // inside the fragment, MAYBE activity is interested --> unify in notification method
        /**
         * Callback method invoked when a the user browsed into a different folder through the
         * list of files
         *  
         * @param folder
         */
        public void onBrowsedDownTo(OCFile folder);                 

        /**
         * Callback method invoked when a the 'transfer state' of a file changes.
         * 
         * This happens when a download or upload is started or ended for a file.
         * 
         * This method is necessary by now to update the user interface of the double-pane layout
         * in tablets because methods {@link FileDownloaderBinder#isDownloading(Account, OCFile)}
         * and {@link FileUploaderBinder#isUploading(Account, OCFile)}
         * won't provide the needed response before the method where this is called finishes. 
         * 
         * TODO Remove this when the transfer state of a file is kept in the database
         * (other thing TODO)
         * 
         * @param file          OCFile which state changed.
         * @param downloading   Flag signaling if the file is now downloading.
         * @param uploading     Flag signaling if the file is now uploading.
         */
        public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading);

    }

}
