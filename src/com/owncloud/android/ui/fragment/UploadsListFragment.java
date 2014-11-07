/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.fragment;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.UploadsListAdapter;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 * 
 * @author LukeOwncloud
 * 
 */
public class UploadsListFragment extends ExtendedListFragment {
    private static final String TAG = "LocalFileListFragment";

    /**
     * Reference to the Activity which this fragment is attached to. For
     * callbacks
     */
    private UploadsListFragment.ContainerActivity mContainerActivity;

    /** Adapter to connect the data from the directory with the View object */
    private UploadsListAdapter mAdapter = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + UploadsListFragment.ContainerActivity.class.getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        disableSwipe(); // Disable pull refresh
//        setMessageForEmptyList(getString(R.string.local_file_list_empty));
        setMessageForEmptyList("No uploads available.");
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
        mAdapter = new UploadsListAdapter(getActivity());
        setListAdapter(mAdapter);

        Log_OC.i(TAG, "onActivityCreated() stop");
    }

    public void selectAll() {
        int numberOfFiles = mAdapter.getCount();
        for (int i = 0; i < numberOfFiles; i++) {
            File file = (File) mAdapter.getItem(i);
            if (file != null) {
                if (!file.isDirectory()) {
                    // / Click on a file
                    getListView().setItemChecked(i, true);
                    // notify the change to the container Activity
                    mContainerActivity.onUploadItemClick(file);
                }
            }
        }
    }

    public void deselectAll() {
        mAdapter = new UploadsListAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    /**
     * Checks the file clicked over. Browses inside if it is a directory.
     * Notifies the container activity in any case.
     */
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        File file = (File) mAdapter.getItem(position);

        if (file != null) {

            // notify the click to container Activity
            mContainerActivity.onUploadItemClick(file);

            ImageView checkBoxV = (ImageView) v.findViewById(R.id.custom_checkbox);
            if (checkBoxV != null) {
                if (getListView().isItemChecked(position)) {
                    checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                } else {
                    checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                }
            }

        } else {
            Log_OC.w(TAG, "Null object in ListAdapter!!");
        }
    }

    /**
     * Returns the fule paths to the files checked by the user
     * 
     * @return File paths to the files checked by the user.
     */
    public String[] getCheckedFilePaths() {
        ArrayList<String> result = new ArrayList<String>();
        SparseBooleanArray positions = mList.getCheckedItemPositions();
        if (positions.size() > 0) {
            for (int i = 0; i < positions.size(); i++) {
                if (positions.get(positions.keyAt(i)) == true) {
                    result.add(((File) mList.getItemAtPosition(positions.keyAt(i))).getAbsolutePath());
                }
            }

            Log_OC.d(TAG, "Returning " + result.size() + " selected files");
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Interface to implement by any Activity that includes some instance of
     * UploadsListFragment
     * 
     * @author LukeOwncloud
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when an upload item is clicked by the user on
         * the upload list
         * 
         * @param file
         */
        public void onUploadItemClick(File file);

        /**
         * Callback method invoked when the parent activity is fully created to
         * get the filter which is to be applied to the upload list.
         * 
         * @return Filter to be applied. Can be null, then all uploads are
         *         shown.
         */
        public File getInitialFilter();

    }

}
