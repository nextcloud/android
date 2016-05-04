/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2016 ownCloud Inc.
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

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.widgets.OCRecyclerView;


public class ExtendedListFragment extends Fragment
        implements OnEnforceableRefreshListener {

    protected static final String TAG = ExtendedListFragment.class.getSimpleName();

    protected static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    protected static final int NUMBER_OF_GRID_COLUMNS = 2;
    protected static final int NUMBER_OF_GRID_COLUMNS_LANDSCAPE = 4;

    protected SwipeRefreshLayout mRefreshListLayout;
    protected TextView mEmptyListMessage;

    private FloatingActionsMenu mFabMain;
    private FloatingActionButton mFabUpload;
    private FloatingActionButton mFabMkdir;
    private FloatingActionButton mFabUploadFromApp;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = null;

    protected SharedPreferences mAppPreferences;
    protected OCRecyclerView mCurrentRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    private FileListListAdapter mAdapter;

    /**
     * Set adapter for current recylcerView list
     * @param mAdapter
     */
    protected void setListAdapter(RecyclerView.Adapter mAdapter) {
        if (mAdapter instanceof FileListListAdapter) {
            // set LayoutManager for Remote list of files
            buildLayoutManager((FileListListAdapter) mAdapter);
        } else
        {
            mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            // set LayoutManager to RecyclerView for Local list of files
            getRecyclerView().setLayoutManager(mLayoutManager);
        }

        mCurrentRecyclerView.setAdapter(mAdapter);
        mCurrentRecyclerView.invalidate();
    }

    /**
     * Returns instance of current RecyclerView
     * @return current recylcer view
     */
    protected OCRecyclerView getRecyclerView() {
        return mCurrentRecyclerView;
    }

    /**
     * Returns FabUpload instance
     * @return mFabUpload
     */
    public FloatingActionButton getFabUpload() {
        return mFabUpload;
    }

    /**
     * Returns FabUploadFromApp insance
     * @return mFabUploadFromApp
     */
    public FloatingActionButton getFabUploadFromApp() {
        return mFabUploadFromApp;
    }

    /**
     * Returns FabMkDir instance
     * @return mFabMkdir
     */
    public FloatingActionButton getFabMkdir() {
        return mFabMkdir;
    }

    public FloatingActionsMenu getFabMain() {
        return mFabMain;
    }

    public void switchToGridView(FileListListAdapter mAdapter) {
        mAdapter.setGridMode(true);
        setListAdapter(mAdapter);
    }

    public void switchToListView(FileListListAdapter mAdapter) {
        mAdapter.setGridMode(false);
        setListAdapter(mAdapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE));
            //mCurrentRecyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(KEY_SAVED_LIST_POSITION));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, container, false);

        // shared preferences
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // assign recycler view
        mCurrentRecyclerView = (OCRecyclerView)(v.findViewById(R.id.list_root));

        // set empty recylcer view message
        mEmptyListMessage = (TextView) v.findViewById(R.id.empty_list_view);
        mCurrentRecyclerView.setEmptyView(mEmptyListMessage);

        // set default recycler view animator
        mCurrentRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mCurrentRecyclerView.setHasFixedSize(true);

        // Pull-down to refresh layout
        mRefreshListLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_list);
        onCreateSwipeToRefresh(mRefreshListLayout);

        // Assign FAB buttons and menu
        mFabMain = (FloatingActionsMenu) v.findViewById(R.id.fab_main);
        mFabUpload = (FloatingActionButton) v.findViewById(R.id.fab_upload);
        mFabMkdir = (FloatingActionButton) v.findViewById(R.id.fab_mkdir);
        mFabUploadFromApp = (FloatingActionButton) v.findViewById(R.id.fab_upload_from_app);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log_OC.d(TAG, "onSaveInstanceState()");
        // Save recycler view instance state
        savedInstanceState.putParcelable(KEY_SAVED_LIST_POSITION, mCurrentRecyclerView.getLayoutManager().onSaveInstanceState());
        // Save empty list message
        savedInstanceState.putString(KEY_EMPTY_LIST_MESSAGE, getEmptyViewText());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Sets aLayoutManager based on device orientation and selected view (grid, list),
     * and saves the selected view into the application preferences
     */
    public void buildLayoutManager(FileListListAdapter mAdapter)
    {
        if (mAdapter.isGridMode())
        {
            if (getActivity().getResources().getBoolean(R.bool.is_landscape)){
                mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS_LANDSCAPE, GridLayoutManager.VERTICAL, false);
            } else {
                mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS, GridLayoutManager.VERTICAL, false);
            }
        } else
        {
            mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        }

        // This may occur on library updates
        if (mLayoutManager == null)
        {
            if (mAppPreferences != null) {
                mAppPreferences.edit().clear().apply();
            }
            mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        }

        // set LayoutManager to RecyclerView
        getRecyclerView().setLayoutManager(mLayoutManager);
    }

    @Override
    public void onRefresh() {
        mRefreshListLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
    public void setOnRefreshListener(OnEnforceableRefreshListener listener) {
        mOnRefreshListener = listener;
    }


    /**
     * Disables swipe gesture.
     *
     * Sets the 'enabled' state of the refresh layouts contained in the fragment.
     *
     * When 'false' is set, prevents user gestures but keeps the option to refresh programatically,
     *
     * @param   enabled     Desired state for capturing swipe gesture.
     */
    public void setSwipeEnabled(boolean enabled) {
        mRefreshListLayout.setEnabled(enabled);
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     *
     * When 'false' is set, FAB visibility is set to View.GONE programatically,
     *
     * @param   enabled     Desired visibility for the FAB.
     */
    public void setFabEnabled(boolean enabled) {
        if(enabled) {
            mFabMain.setVisibility(View.VISIBLE);
        } else {
            mFabMain.setVisibility(View.GONE);
        }
    }

    /**
     * Set message for empty list view
     */
    public void setMessageForEmptyList(String message) {
        if (mEmptyListMessage != null) {
            mEmptyListMessage.setText(message);
        }
    }

    /**
     * Get the text of EmptyListMessage TextView
     *
     * @return String
     */
    public String getEmptyViewText() {
        return (mEmptyListMessage != null) ? mEmptyListMessage.getText().toString() : "";
    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        // Colors in animations
        refreshLayout.setColorSchemeResources(R.color.color_accent, R.color.primary,
                R.color.primary_dark);

        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshListLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
}
