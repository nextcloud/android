/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.FragmentListView;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.ui.adapter.FileListListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

/**
 * A Fragment that lists all files and folders in a given path.
 * 
 * @author Bartek Przybylski
 * 
 */
public class OCFileListFragment extends FragmentListView {
    private static final String TAG = "FileListFragment";
    private static final String SAVED_LIST_POSITION = "LIST_POSITION"; 
    
    private OCFileListFragment.ContainerActivity mContainerActivity;
    
    private OCFile mFile = null;
    private FileListListAdapter mAdapter;

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + OCFileListFragment.ContainerActivity.class.getCanonicalName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated() start");
        
        super.onActivityCreated(savedInstanceState);
        mAdapter = new FileListListAdapter(mContainerActivity.getInitialDirectory(), mContainerActivity.getStorageManager(), getActivity(), mContainerActivity);
        setListAdapter(mAdapter);
        
        if (savedInstanceState != null) {
            Log.i(TAG, "savedInstanceState is not null");
            int position = savedInstanceState.getInt(SAVED_LIST_POSITION);
            setReferencePosition(position);
        }
        
        Log.i(TAG, "onActivityCreated() stop");
    }
    
    

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState() start");
        
        savedInstanceState.putInt(SAVED_LIST_POSITION, getReferencePosition());
        
        Log.i(TAG, "onSaveInstanceState() stop");
    }
    
    
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        OCFile file = (OCFile) mAdapter.getItem(position);
        if (file != null) {
            /// Click on a directory
            if (file.getMimetype().equals("DIR")) {
                // just local updates
                mFile = file;
                listDirectory(file);
                // any other updates are let to the container Activity
                mContainerActivity.onDirectoryClick(file);
            
            } else {    /// Click on a file
                mContainerActivity.onFileClick(file);
            }
            
        } else {
            Log.d(TAG, "Null object in ListAdapter!!");
        }
        
    }

    /**
     * Call this, when the user presses the up button
     */
    public void onNavigateUp() {
        OCFile parentDir = null;
        
        if(mFile != null){
            DataStorageManager storageManager = mContainerActivity.getStorageManager();
            parentDir = storageManager.getFileById(mFile.getParentId());
            mFile = parentDir;
        }
        listDirectory(parentDir);
    }

    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile(){
        return mFile;
    }
    
    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile)} with a null parameter
     */
    public void listDirectory(){
        listDirectory(null);
    }
    
    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory, or list the root
     * if there never was a directory.
     * 
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory) {
        DataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {

            // Check input parameters for null
            if(directory == null){
                if(mFile != null){
                    directory = mFile;
                } else {
                    directory = storageManager.getFileByPath("/");
                    if (directory == null) return; // no files, wait for sync
                }
            }
        
        
            // If that's not a directory -> List its parent
            if(!directory.isDirectory()){
                Log.w(TAG, "You see, that is not a directory -> " + directory.toString());
                directory = storageManager.getFileById(directory.getParentId());
            }

            mAdapter.swapDirectory(directory, storageManager);
            if (mFile == null || !mFile.equals(directory)) {
                mList.setSelectionFromTop(0, 0);
            }
            mFile = directory;
        }
    }
    
    
    
    /**
     * Interface to implement by any Activity that includes some instance of FileListFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity extends TransferServiceGetter {

        /**
         * Callback method invoked when a directory is clicked by the user on the files list
         *  
         * @param file
         */
        public void onDirectoryClick(OCFile file);
        
        /**
         * Callback method invoked when a file (non directory) is clicked by the user on the files list
         *  
         * @param file
         */
        public void onFileClick(OCFile file);

        /**
         * Getter for the current DataStorageManager in the container activity
         */
        public DataStorageManager getStorageManager();
        
        
        /**
         * Callback method invoked when the parent activity is fully created to get the directory to list firstly.
         * 
         * @return  Directory to list firstly. Can be NULL.
         */
        public OCFile getInitialDirectory();
        
        
    }

}
