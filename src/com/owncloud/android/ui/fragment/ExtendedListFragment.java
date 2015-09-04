/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2015 ownCloud Inc.
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

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FileListListAdapter;

import third_parties.in.srain.cube.GridViewWithHeaderAndFooter;

/**
 * TODO extending SherlockListFragment instead of SherlockFragment
 */
public class ExtendedListFragment extends Fragment
        implements OnItemClickListener, OnEnforceableRefreshListener {

    private static final String TAG = ExtendedListFragment.class.getSimpleName();

    private static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";
    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS= "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";

    private SwipeRefreshLayout mRefreshListLayout;
    private SwipeRefreshLayout mRefreshGridLayout;
    private SwipeRefreshLayout mRefreshEmptyLayout;
    private TextView mEmptyListMessage;
    
    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell = 0;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = null;

    protected AbsListView mCurrentListView;
    private ExtendedListView mListView;
    private View mListFooterView;
    private GridViewWithHeaderAndFooter mGridView;
    private View mGridFooterView;

    private ListAdapter mAdapter;

    protected void setListAdapter(ListAdapter listAdapter) {
        mAdapter = listAdapter;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mCurrentListView.setAdapter(listAdapter);
        } else {
            ((ListView)mCurrentListView).setAdapter(listAdapter);
        }

        mCurrentListView.invalidate();
    }

    protected AbsListView getListView() {
        return mCurrentListView;
    }


    protected void switchToGridView() {
        if ((mCurrentListView == mListView)) {

            mListView.setAdapter(null);
            mRefreshListLayout.setVisibility(View.GONE);

            if (mAdapter instanceof FileListListAdapter) {
                ((FileListListAdapter) mAdapter).setGridMode(true);
            }
            mGridView.setAdapter(mAdapter);
            mRefreshGridLayout.setVisibility(View.VISIBLE);

            mCurrentListView = mGridView;
        }
    }
    
    protected void switchToListView() {
        if (mCurrentListView == mGridView) {
            mGridView.setAdapter(null);
            mRefreshGridLayout.setVisibility(View.GONE);

            if (mAdapter instanceof FileListListAdapter) {
                ((FileListListAdapter) mAdapter).setGridMode(false);
            }
            mListView.setAdapter(mAdapter);
            mRefreshListLayout.setVisibility(View.VISIBLE);

            mCurrentListView = mListView;
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, null);

        mListView = (ExtendedListView)(v.findViewById(R.id.list_root));
        mListView.setOnItemClickListener(this);
        mListFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mGridView = (GridViewWithHeaderAndFooter) (v.findViewById(R.id.grid_root));
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setOnItemClickListener(this);
        mGridFooterView = inflater.inflate(R.layout.list_footer, null, false);

        if (savedInstanceState != null) {
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            if (mCurrentListView == mListView) {
                Log_OC.v(TAG, "Setting and centering around list position " + referencePosition);
                mListView.setAndCenterSelection(referencePosition);
            } else {
                Log_OC.v(TAG, "Setting grid position " + referencePosition);
                mGridView.setSelection(referencePosition);
            }
        }

        // Pull-down to refresh layout
        mRefreshListLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_list);
        mRefreshGridLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_grid);
        mRefreshEmptyLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_empty);
        mEmptyListMessage = (TextView) v.findViewById(R.id.empty_list_view);
        
        onCreateSwipeToRefresh(mRefreshListLayout);
        onCreateSwipeToRefresh(mRefreshGridLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);

        mListView.setEmptyView(mRefreshEmptyLayout);
        mGridView.setEmptyView(mRefreshEmptyLayout);

        mCurrentListView = mListView;   // list as default

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
        Log_OC.d(TAG, "onSaveInstanceState()");
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
     * The current policy is take as a reference the visible item in the center
     * of the screen.
     * 
     * @return The position in the list of the visible item in the center of the
     *         screen.
     */
    protected int getReferencePosition() {
        if (mCurrentListView != null) {
            return (mCurrentListView.getFirstVisiblePosition() +
                    mCurrentListView.getLastVisiblePosition()) / 2;
        } else {
            return 0;
        }
    }


    /*
     * Restore index and position
     */
    protected void restoreIndexAndTopPosition() {
        if (mIndexes.size() > 0) {  
            // needs to be checked; not every browse-up had a browse-down before 
            
            int index = mIndexes.remove(mIndexes.size() - 1);
            final int firstPosition = mFirstPositions.remove(mFirstPositions.size() -1);
            int top = mTops.remove(mTops.size() - 1);

            Log_OC.v(TAG, "Setting selection to position: " + firstPosition + "; top: "
                    + top + "; index: " + index);

            if (mCurrentListView == mListView) {
                if (mHeightCell*index <= mListView.getHeight()) {
                    mListView.setSelectionFromTop(firstPosition, top);
                } else {
                    mListView.setSelectionFromTop(index, 0);
                }

            } else {
                if (mHeightCell*index <= mGridView.getHeight()) {
                    mGridView.setSelection(firstPosition);
                    //mGridView.smoothScrollToPosition(firstPosition);
                } else {
                    mGridView.setSelection(index);
                    //mGridView.smoothScrollToPosition(index);
                }
            }

        }
    }
    
    /*
     * Save index and top position
     */
    protected void saveIndexAndTopPosition(int index) {
        
        mIndexes.add(index);
        
        int firstPosition = mCurrentListView.getFirstVisiblePosition();
        mFirstPositions.add(firstPosition);
        
        View view = mCurrentListView.getChildAt(0);
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
        mRefreshListLayout.setRefreshing(false);
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

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
        mRefreshGridLayout.setEnabled(enabled);
        mRefreshEmptyLayout.setEnabled(enabled);
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
        // Colors in animations
        refreshLayout.setColorSchemeResources(R.color.color_accent, R.color.primary,
                R.color.primary_dark);

        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshListLayout.setRefreshing(false);
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    protected void setChoiceMode(int choiceMode) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mListView.setChoiceMode(choiceMode);
            mGridView.setChoiceMode(choiceMode);
        } else {
            ((ListView)mListView).setChoiceMode(choiceMode);
        }
    }

    protected void registerForContextMenu() {
        registerForContextMenu(mListView);
        registerForContextMenu(mGridView);
        mListView.setOnCreateContextMenuListener(this);
        mGridView.setOnCreateContextMenuListener(this);
    }

    /**
     * TODO doc
     * To be called before setAdapter, or GridViewWithHeaderAndFooter will throw an exception
     *
     * @param enabled
     */
    protected void setFooterEnabled(boolean enabled) {
        if (enabled) {
            if (mGridView.getFooterViewCount() == 0) {
                if (mGridFooterView.getParent() != null ) {
                    ((ViewGroup) mGridFooterView.getParent()).removeView(mGridFooterView);
                }
                mGridView.addFooterView(mGridFooterView, null, false);
            }
            mGridFooterView.invalidate();

            if (mListView.getFooterViewsCount() == 0) {
                if (mListFooterView.getParent() != null ) {
                    ((ViewGroup) mListFooterView.getParent()).removeView(mListFooterView);
                }
                mListView.addFooterView(mListFooterView, null, false);
            }
            mListFooterView.invalidate();

        } else {
            mGridView.removeFooterView(mGridFooterView);
            mListView.removeFooterView(mListFooterView);
        }
    }

    /**
     * TODO doc
     * @param text
     */
    protected void setFooterText(String text) {
        if (text != null && text.length() > 0) {
            ((TextView)mListFooterView.findViewById(R.id.footerText)).setText(text);
            ((TextView)mGridFooterView.findViewById(R.id.footerText)).setText(text);
            setFooterEnabled(true);

        } else {
            setFooterEnabled(false);
        }
    }

}
