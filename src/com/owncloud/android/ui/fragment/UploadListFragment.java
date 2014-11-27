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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.ExpandableUploadListAdapter;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 * 
 * @author LukeOwncloud
 * 
 */
public class UploadListFragment extends ExpandableListFragment {
    static private String TAG = "UploadListFragment";

    /**
     * Reference to the Activity which this fragment is attached to. For
     * callbacks
     */
    private UploadListFragment.ContainerActivity mContainerActivity;

    BaseExpandableListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        return v;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + UploadListFragment.ContainerActivity.class.getSimpleName());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated() start");
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ExpandableUploadListAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        boolean handled = false;
        UploadDbObject uploadDbObject = (UploadDbObject) mAdapter.getChild(groupPosition, childPosition);
        if (uploadDbObject != null) {
            // notify the click to container Activity
            handled = mContainerActivity.onUploadItemClick(uploadDbObject);
        } else {
            Log_OC.w(TAG, "Null object in ListAdapter!!");
        }
        return handled;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log_OC.d(TAG, "onItemLongClick() position: " + position + " id: " + id);
        int itemType = ExpandableListView.getPackedPositionType(id);

        if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int childPosition = ExpandableListView.getPackedPositionChild(id);
            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            UploadDbObject uploadDbObject = (UploadDbObject) mAdapter.getChild(groupPosition, childPosition);
            if (uploadDbObject != null) {
                return mContainerActivity.onUploadItemLongClick(uploadDbObject);
            } else {
                Log_OC.w(TAG, "Null object in ListAdapter!!");
            }
        } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            // clicked on group header. ignore.
            return false;
        }

        return false;

    }

    /**
     * Interface to implement by any Activity that includes some instance of
     * UploadListFragment
     * 
     * @author LukeOwncloud
     */
    public interface ContainerActivity {

        /**
         * Callback method invoked when an upload item is clicked by the user on
         * the upload list
         * 
         * @param file
         * @return return true if click was handled.
         */
        public boolean onUploadItemClick(UploadDbObject file);
        
        /**
         * Callback method invoked when an upload item is long clicked by the user on
         * the upload list
         * 
         * @param file
         * @return return true if click was handled.
         */
        public boolean onUploadItemLongClick(UploadDbObject file);

    }

}