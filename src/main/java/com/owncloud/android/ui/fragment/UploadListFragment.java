/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
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
package com.owncloud.android.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.owncloud.android.R;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ExpandableUploadListAdapter;
import com.owncloud.android.utils.AnalyticsUtils;

/**
 * A Fragment that lists all files and folders in a given LOCAL path.
 */
public class UploadListFragment extends ExpandableListFragment {
    private static final String TAG = UploadListFragment.class.getSimpleName();

    private static final String SCREEN_NAME = "Uploads";

    /**
     * Reference to the Activity which this fragment is attached to.
     * For callbacks.
     */
    private UploadListFragment.ContainerActivity mContainerActivity;

    private ExpandableUploadListAdapter mAdapter;

    /** Is binder ready in the Activity? */
    private boolean mBinderReady = false;

    public void setBinderReady(boolean ready) {
        mBinderReady = ready;
    }
    public boolean isBinderReady(){
        return mBinderReady;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setMessageForEmptyList(
                R.string.upload_list_empty_headline, R.string.upload_list_empty_text_auto_upload,
                R.drawable.ic_list_empty_upload, true
        );
        setOnRefreshListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
    }

    @Override
    public void onRefresh() {
        // remove the progress circle as soon as pull is triggered, like in the list of files
        mRefreshEmptyLayout.setRefreshing(false);
        mRefreshListLayout.setRefreshing(false);

        mAdapter.notifyDataSetChanged();
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
        Log_OC.d(TAG, "onActivityCreated() start");
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onStart() {
        Log_OC.d(TAG, "onStart() start");
        super.onStart();
        mAdapter = new ExpandableUploadListAdapter((FileActivity)getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        boolean handled = false;
        OCUpload OCUpload = (OCUpload) mAdapter.getChild(groupPosition, childPosition);
        if (OCUpload != null) {
            // notify the click to container Activity
            handled = mContainerActivity.onUploadItemClick(OCUpload);
        } else {
            Log_OC.w(TAG, "Null object in ListAdapter!!");
        }
        return handled;
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
         * @param file the file that has been clicked on.
         * @return return true if click was handled.
         */
        public boolean onUploadItemClick(OCUpload file);
    }

    public void binderReady(){
        setBinderReady(true);

        if (mAdapter != null) {
            mAdapter.addBinder();
        }
    }

    public void updateUploads(){
        if (mAdapter != null) {
            mAdapter.refreshView();
        }
    }
}