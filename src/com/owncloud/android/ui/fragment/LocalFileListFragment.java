/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.fragment;

import java.io.File;

import com.owncloud.android.ui.FragmentListView;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.owncloud.android.R;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 * 
 * @author David A. Velasco
 * 
 */
public class LocalFileListFragment extends FragmentListView {
    private static final String TAG = "LocalFileListFragment";
    private static final String SAVED_LIST_POSITION = "LIST_POSITION"; 
    
    /** Reference to the Activity which this fragment is attached to. For callbacks */
    private LocalFileListFragment.ContainerActivity mContainerActivity;
    
    /** Directory to show */
    private File mDirectory = null;
    
    /** Adapter to connect the data from the directory with the View object */
    private LocalFileListAdapter mAdapter = null;

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + LocalFileListFragment.ContainerActivity.class.getSimpleName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        Log.i(TAG, "onCreateView() end");
        return v;
    }    


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated() start");
        
        super.onCreate(savedInstanceState);
        mAdapter = new LocalFileListAdapter(mContainerActivity.getInitialDirectory(), getActivity());
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
    
    
    /**
     * Checks the file clicked over. Browses inside if it is a directory. Notifies the container activity in any case.
     */
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        File file = (File) mAdapter.getItem(position); 
        if (file != null) {
            /// Click on a directory
            if (file.isDirectory()) {
                // just local updates
                listDirectory(file);
                // notify the click to container Activity
                mContainerActivity.onDirectoryClick(file);
            
            } else {    /// Click on a file
                ImageView checkBoxV = (ImageView) v.findViewById(R.id.custom_checkbox);
                if (checkBoxV != null) {
                    if (getListView().isItemChecked(position)) {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                    }
                }
                // notify the change to the container Activity
                mContainerActivity.onFileClick(file);
            }
            
        } else {
            Log.w(TAG, "Null object in ListAdapter!!");
        }
    }

    
    /**
     * Call this, when the user presses the up button
     */
    public void onNavigateUp() {
        File parentDir = null;
        if(mDirectory != null) {
            parentDir = mDirectory.getParentFile();  // can be null
        }
        listDirectory(parentDir);
    }

    
    /**
     * Use this to query the {@link File} object for the directory
     * that is currently being displayed by this fragment
     * 
     * @return File     The currently displayed directory
     */
    public File getCurrentDirectory(){
        return mDirectory;
    }
    
    
    /**
     * Calls {@link LocalFileListFragment#listDirectory(File)} with a null parameter
     * to refresh the current directory.
     */
    public void listDirectory(){
        listDirectory(null);
    }
    
    
    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory, or list the root
     * if there never was a directory.
     * 
     * @param directory     Directory to be listed
     */
    public void listDirectory(File directory) {
        
        // Check input parameters for null
        if(directory == null) {
            if(mDirectory != null){
                directory = mDirectory;
            } else {
                directory = Environment.getExternalStorageDirectory();  // TODO be careful with the state of the storage; could not be available
                if (directory == null) return; // no files to show
            }
        }
        
        
        // if that's not a directory -> List its parent
        if(!directory.isDirectory()){
            Log.w(TAG, "You see, that is not a directory -> " + directory.toString());
            directory = directory.getParentFile();
        }

        mList.clearChoices();   // by now, only files in the same directory will be kept as selected
        mAdapter.swapDirectory(directory);
        if (mDirectory == null || !mDirectory.equals(directory)) {
            mList.setSelectionFromTop(0, 0);
        }
        mDirectory = directory;
    }
    

    /**
     * Returns the fule paths to the files checked by the user
     * 
     * @return      File paths to the files checked by the user.
     */
    public String[] getCheckedFilePaths() {
        String [] result = null;
        SparseBooleanArray positions = mList.getCheckedItemPositions();
        if (positions.size() > 0) {
            Log.d(TAG, "Returning " + positions.size() + " selected files");
            result = new String[positions.size()];
            for (int i=0; i<positions.size(); i++) {
                result[i] = ((File) mList.getItemAtPosition(positions.keyAt(i))).getAbsolutePath();
            }
        }
        return result;
    }

    
    /**
     * Interface to implement by any Activity that includes some instance of LocalFileListFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when a directory is clicked by the user on the files list
         *  
         * @param file
         */
        public void onDirectoryClick(File directory);
        
        /**
         * Callback method invoked when a file (non directory) is clicked by the user on the files list
         *  
         * @param file
         */
        public void onFileClick(File file);
        
        
        /**
         * Callback method invoked when the parent activity is fully created to get the directory to list firstly.
         * 
         * @return  Directory to list firstly. Can be NULL.
         */
        public File getInitialDirectory();

    }


}
