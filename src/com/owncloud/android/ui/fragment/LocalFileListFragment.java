/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;


/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 */
public class LocalFileListFragment extends ExtendedListFragment {
    private static final String TAG = "LocalFileListFragment";
    
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
            throw new ClassCastException(activity.toString() + " must implement " +
                    LocalFileListFragment.ContainerActivity.class.getSimpleName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setSwipeEnabled(false); // Disable pull-to-refresh
        setFabEnabled(false); // Disable FAB
        setMessageForEmptyList(getString(R.string.local_file_list_empty));
        Log_OC.i(TAG, "onCreateView() end");
        return v;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log_OC.i(TAG, "onActivityCreated() start");
        
        super.onActivityCreated(savedInstanceState);
        mAdapter = new LocalFileListAdapter(mContainerActivity.getInitialDirectory(), getActivity());
        setListAdapter(mAdapter);
        
        Log_OC.i(TAG, "onActivityCreated() stop");
    }
    
    /**
     * Checks the file clicked over. Browses inside if it is a directory.
     * Notifies the container activity in any case.
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
                // save index and top position
                saveIndexAndTopPosition(position);
            
            } else {    /// Click on a file
                ImageView checkBoxV = (ImageView) v.findViewById(R.id.custom_checkbox);
                if (checkBoxV != null) {
                    if (((ListView)getListView()).isItemChecked(position)) {
                        checkBoxV.setImageResource(R.drawable.ic_checkbox_marked);
                    } else {
                        checkBoxV.setImageResource(R.drawable.ic_checkbox_blank_outline);
                    }
                }
                // notify the change to the container Activity
                mContainerActivity.onFileClick(file);
            }
            
        } else {
            Log_OC.w(TAG, "Null object in ListAdapter!!");
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

        // restore index and top position
        restoreIndexAndTopPosition();
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
     * it will either refresh the last known directory. list the root
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
                directory = Environment.getExternalStorageDirectory();
                // TODO be careful with the state of the storage; could not be available
                if (directory == null) return; // no files to show
            }
        }
        
        
        // if that's not a directory -> List its parent
        if(!directory.isDirectory()){
            Log_OC.w(TAG, "You see, that is not a directory -> " + directory.toString());
            directory = directory.getParentFile();
        }

        // by now, only files in the same directory will be kept as selected
        ((ListView)mCurrentListView).clearChoices();
        mAdapter.swapDirectory(directory);
        if (mDirectory == null || !mDirectory.equals(directory)) {
            mCurrentListView.setSelection(0);
        }
        mDirectory = directory;
    }
    

    /**
     * Returns the fule paths to the files checked by the user
     * 
     * @return      File paths to the files checked by the user.
     */
    public String[] getCheckedFilePaths() {
        ArrayList<String> result = new ArrayList<String>();
        SparseBooleanArray positions = ((ListView)mCurrentListView).getCheckedItemPositions();
        if (positions.size() > 0) {
            for (int i = 0; i < positions.size(); i++) {
                if (positions.get(positions.keyAt(i)) == true) {
                    result.add(((File) mCurrentListView.getItemAtPosition(
                            positions.keyAt(i))).getAbsolutePath());
                }
            }

            Log_OC.d(TAG, "Returning " + result.size() + " selected files");
        }
        return result.toArray(new String[result.size()]);
    }

    
    /**
     * Interface to implement by any Activity that includes some instance of LocalFileListFragment
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when a directory is clicked by the user on the files list
         *  
         * @param directory
         */
        public void onDirectoryClick(File directory);
        
        /**
         * Callback method invoked when a file (non directory)
         * is clicked by the user on the files list
         *  
         * @param file
         */
        public void onFileClick(File file);
        
        
        /**
         * Callback method invoked when the parent activity
         * is fully created to get the directory to list firstly.
         * 
         * @return  Directory to list firstly. Can be NULL.
         */
        public File getInitialDirectory();

    }


}
