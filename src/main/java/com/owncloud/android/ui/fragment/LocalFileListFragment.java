/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.FileSortOrder;

import java.io.File;


/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 */
public class LocalFileListFragment extends ExtendedListFragment implements LocalFileListFragmentInterface {
    private static final String TAG = LocalFileListFragment.class.getSimpleName();

    /** Reference to the Activity which this fragment is attached to. For callbacks */
    private LocalFileListFragment.ContainerActivity mContainerActivity;

    /** Directory to show */
    private File mDirectory = null;

    /** Adapter to connect the data from the directory with the View object */
    private LocalFileListAdapter mAdapter = null;

    private static final String SCREEN_NAME = "Local file browser";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
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

        if (!mContainerActivity.isFolderPickerMode()) {
            setMessageForEmptyList(R.string.file_list_empty_headline, R.string.local_file_list_empty,
                    R.drawable.ic_list_empty_folder, true);
        } else {
            setMessageForEmptyList(R.string.folder_list_empty_headline, R.string.local_folder_list_empty,
                    R.drawable.ic_list_empty_folder, true);
        }

        setSwipeEnabled(false); // Disable pull-to-refresh
        setFabEnabled(false); // Disable FAB

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

        mAdapter = new LocalFileListAdapter(mContainerActivity.isFolderPickerMode(),
                mContainerActivity.getInitialDirectory(), this, getActivity());
        setRecyclerViewAdapter(mAdapter);

        Log_OC.i(TAG, "onActivityCreated() stop");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mContainerActivity.isFolderPickerMode()) {
            menu.removeItem(R.id.action_select_all_action_menu);
            menu.removeItem(R.id.action_search);
        } else {
            super.onCreateOptionsMenu(menu, inflater);
        }
    }


    /**
     * Checks the file clicked over. Browses inside if it is a directory.
     * Notifies the container activity in any case.
     */
    @Override
    public void onItemClicked(File file) {
        if (file != null) {
            /// Click on a directory
            if (file.isDirectory()) {
                // just local updates
                listDirectory(file);
                // notify the click to container Activity
                mContainerActivity.onDirectoryClick(file);

                // save index and top position
                saveIndexAndTopPosition(mAdapter.getItemPosition(file));

            } else {    /// Click on a file
                if (mAdapter.isCheckedFile(file)) {
                    // uncheck
                    mAdapter.removeCheckedFile(file);
                } else {
                    // check
                    mAdapter.addCheckedFile(file);
                }

                mAdapter.notifyItemChanged(mAdapter.getItemPosition(file));

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
        if (mDirectory != null) {
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
    public File getCurrentDirectory() {
        return mDirectory;
    }


    /**
     * Calls {@link LocalFileListFragment#listDirectory(File)} with a null parameter
     * to refresh the current directory.
     */
    public void listDirectory() {
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
        if (directory == null) {
            if (mDirectory != null) {
                directory = mDirectory;
            } else {
                directory = Environment.getExternalStorageDirectory();
                // TODO be careful with the state of the storage; could not be available
                if (directory == null) {
                    return; // no files to show
                }
            }
        }


        // if that's not a directory -> List its parent
        if (!directory.isDirectory()) {
            Log_OC.w(TAG, "You see, that is not a directory -> " + directory.toString());
            directory = directory.getParentFile();
        }

        // by now, only files in the same directory will be kept as selected
        mAdapter.removeAllFilesFromCheckedFiles();
        mAdapter.swapDirectory(directory);

        mDirectory = directory;
    }


    /**
     * Returns the full paths to the files checked by the user
     *
     * @return File paths to the files checked by the user.
     */
    public String[] getCheckedFilePaths() {
        return mAdapter.getCheckedFilesPath();
    }

    public void sortFiles(FileSortOrder sortOrder) {
        mAdapter.setSortOrder(sortOrder);
    }

    /**
     * De-/select all elements in the local file list.
     *
     * @param select <code>true</code> to select all, <code>false</code> to deselect all
     */
    public void selectAllFiles(boolean select) {
        LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) getRecyclerView().getAdapter();

        if (select) {
            localFileListAdapter.addAllFilesToCheckedFiles();
        } else {
            localFileListAdapter.removeAllFilesFromCheckedFiles();
        }

        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            mAdapter.notifyItemChanged(i);
        }
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
        void onDirectoryClick(File directory);

        /**
         * Callback method invoked when a file (non directory)
         * is clicked by the user on the files list
         *
         * @param file
         */
        void onFileClick(File file);

        /**
         * Callback method invoked when the parent activity
         * is fully created to get the directory to list firstly.
         *
         * @return Directory to list firstly. Can be NULL.
         */
        File getInitialDirectory();

        /**
         * config check if the list should behave in
         * folder picker mode only displaying folders but no files.
         *
         * @return true if folder picker mode, else false
         */
        boolean isFolderPickerMode();
    }


}
