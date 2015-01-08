/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;

/**
 * TODO extending SherlockListFragment instead of SherlockFragment
 */
public class ExtendedListFragment extends SherlockFragment 
implements OnItemClickListener, OnEnforceableRefreshListener {

    private static final String TAG = ExtendedListFragment.class.getSimpleName();

    private static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";
    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS= "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";

    protected ExtendedListView mList;

    private SwipeRefreshLayout mRefreshLayout;
    private SwipeRefreshLayout mRefreshEmptyLayout;
    private TextView mEmptyListMessage;
    
    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell = 0;

    private OnEnforceableRefreshListener mOnRefreshListener = null;
    
    
    public void setListAdapter(ListAdapter listAdapter) {
        mList.setAdapter(listAdapter);
        mList.invalidate();
    }

    public void setFooterView(View footer) {
        mList.addFooterView(footer, null, false);
        mList.invalidate();
    }

    public ListView getListView() {
        return mList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.e(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, null);
        mEmptyListMessage = (TextView) v.findViewById(R.id.empty_list_view);
        mList = (ExtendedListView) (v.findViewById(R.id.list_root));
        mList.setOnItemClickListener(this);

        mList.setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        mList.setDividerHeight(1);

        if (savedInstanceState != null) {
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            setReferencePosition(referencePosition);
        }

        // Pull down refresh
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_files);
        mRefreshEmptyLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_files_emptyView);
        
        onCreateSwipeToRefresh(mRefreshLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);
        
        mList.setEmptyView(mRefreshEmptyLayout);

        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (savedInstanceState != null) {
            mIndexes = savedInstanceState.getIntegerArrayList(KEY_INDEXES);
            mFirstPositions = savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS);
            mTops = savedInstanceState.getIntegerArrayList(KEY_TOPS);
            mHeightCell = savedInstanceState.getInt(KEY_HEIGHT_CELL);
            setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE));
            
        } else {
            mIndexes = new ArrayList<Integer>();
            mFirstPositions = new ArrayList<Integer>();
            mTops = new ArrayList<Integer>();
            mHeightCell = 0;
        }
    }    
    
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log_OC.e(TAG, "onSaveInstanceState()");
        savedInstanceState.putInt(KEY_SAVED_LIST_POSITION, getReferencePosition());
        savedInstanceState.putIntegerArrayList(KEY_INDEXES, mIndexes);
        savedInstanceState.putIntegerArrayList(KEY_FIRST_POSITIONS, mFirstPositions);
        savedInstanceState.putIntegerArrayList(KEY_TOPS, mTops);
        savedInstanceState.putInt(KEY_HEIGHT_CELL, mHeightCell);
        savedInstanceState.putString(KEY_EMPTY_LIST_MESSAGE, getEmptyViewText());
    }

    /**
     * Calculates the position of the item that will be used as a reference to
     * reposition the visible items in the list when the device is turned to
     * other position.
     * 
     * THe current policy is take as a reference the visible item in the center
     * of the screen.
     * 
     * @return The position in the list of the visible item in the center of the
     *         screen.
     */
    protected int getReferencePosition() {
        if (mList != null) {
            return (mList.getFirstVisiblePosition() + mList.getLastVisiblePosition()) / 2;
        } else {
            return 0;
        }
    }

    /**
     * Sets the visible part of the list from the reference position.
     * 
     * @param position Reference position previously returned by
     *            {@link LocalFileListFragment#getReferencePosition()}
     */
    protected void setReferencePosition(int position) {
        if (mList != null) {
            mList.setAndCenterSelection(position);
        }
    }


    /*
     * Restore index and position
     */
    protected void restoreIndexAndTopPosition() {
        if (mIndexes.size() > 0) {  
            // needs to be checked; not every browse-up had a browse-down before 
            
            int index = mIndexes.remove(mIndexes.size() - 1);
            
            int firstPosition = mFirstPositions.remove(mFirstPositions.size() -1);
            
            int top = mTops.remove(mTops.size() - 1);
            
            mList.setSelectionFromTop(firstPosition, top);
            
            // Move the scroll if the selection is not visible
            int indexPosition = mHeightCell*index;
            int height = mList.getHeight();
            
            if (indexPosition > height) {
                if (android.os.Build.VERSION.SDK_INT >= 11)
                {
                    mList.smoothScrollToPosition(index); 
                }
                else if (android.os.Build.VERSION.SDK_INT >= 8)
                {
                    mList.setSelectionFromTop(index, 0);
                }
                
            }
        }
    }
    
    /*
     * Save index and top position
     */
    protected void saveIndexAndTopPosition(int index) {
        
        mIndexes.add(index);
        
        int firstPosition = mList.getFirstVisiblePosition();
        mFirstPositions.add(firstPosition);
        
        View view = mList.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop() ;

        mTops.add(top);
        
        // Save the height of a cell
        mHeightCell = (view == null || mHeightCell != 0) ? mHeightCell : view.getHeight();
    }
    
    
    @Override
    public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
        // to be @overriden
    }

    @Override
    public void onRefresh() {
        // to be @overriden
        mRefreshLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);
        
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
    public void setOnRefreshListener(OnEnforceableRefreshListener listener) {
        mOnRefreshListener = listener;
    }
    

    /**
     * Enables swipe gesture
     */
    public void enableSwipe() {
        mRefreshLayout.setEnabled(true);
    }
 
    /**
     * Disables swipe gesture. It prevents manual gestures but keeps the option you show
     * refreshing programmatically.
     */
    public void disableSwipe() {
        mRefreshLayout.setEnabled(false);
    }
    
    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void showSwipeProgress() {
        mRefreshLayout.setRefreshing(true);
    }
 
    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void hideSwipeProgress() {
        mRefreshLayout.setRefreshing(false);
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

    private void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        // Colors in animations: background
        refreshLayout.setColorScheme(R.color.background_color, R.color.background_color, R.color.background_color,
                R.color.background_color);

        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh(ignoreETag);
        }
    }
}
