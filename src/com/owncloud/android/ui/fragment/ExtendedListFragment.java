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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.GestureDetector;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FilterableListAdapter;

import java.util.ArrayList;

import third_parties.in.srain.cube.GridViewWithHeaderAndFooter;

public class ExtendedListFragment extends Fragment
        implements OnItemClickListener, OnEnforceableRefreshListener, SearchView.OnQueryTextListener {

    protected static final String TAG = ExtendedListFragment.class.getSimpleName();

    protected static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION"; 

    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS= "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    private static final String KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE";

    private ScaleGestureDetector SGD = null;

    protected SwipeRefreshLayout mRefreshListLayout;
    private SwipeRefreshLayout mRefreshGridLayout;
    protected SwipeRefreshLayout mRefreshEmptyLayout;
    protected TextView mEmptyListMessage;

    private FloatingActionsMenu mFabMain;
    private FloatingActionButton mFabUpload;
    private FloatingActionButton mFabMkdir;
    private FloatingActionButton mFabUploadFromApp;

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

    private FilterableListAdapter mAdapter;

p   private float scale = -1f;

    protected void setListAdapter(FilterableListAdapter listAdapter) {
        mAdapter = listAdapter;
        mCurrentListView.setAdapter(listAdapter);
        mCurrentListView.invalidateViews();
    }

    protected AbsListView getListView() {
        return mCurrentListView;
    }

    public FloatingActionButton getFabUpload() {
        return mFabUpload;
    }

    public FloatingActionButton getFabUploadFromApp() {
        return mFabUploadFromApp;
    }

    public FloatingActionButton getFabMkdir() {
        return mFabMkdir;
    }

    public FloatingActionsMenu getFabMain() {
        return mFabMain;
    }

    public void switchToGridView() {
        if (!isGridEnabled()) {
            mListView.setAdapter(null);
            mRefreshListLayout.setVisibility(View.GONE);
            mRefreshGridLayout.setVisibility(View.VISIBLE);
            mCurrentListView = mGridView;
            setListAdapter(mAdapter);
        }
    }

    public void switchToListView() {
        if (isGridEnabled()) {
            mGridView.setAdapter(null);
            mRefreshGridLayout.setVisibility(View.GONE);
            mRefreshListLayout.setVisibility(View.VISIBLE);
            mCurrentListView = mListView;
            setListAdapter(mAdapter);
        }
    }

    public boolean isGridEnabled(){
        return (mCurrentListView == mGridView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
    }

    public boolean onQueryTextChange(String query) {
        mAdapter.filter(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mAdapter.filter(query);
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, null);

        mListView = (ExtendedListView)(v.findViewById(R.id.list_root));
        mListView.setOnItemClickListener(this);
        //mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mGridView = (GridViewWithHeaderAndFooter) (v.findViewById(R.id.grid_root));
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setOnItemClickListener(this);

        mGridFooterView = inflater.inflate(R.layout.list_footer, null, false);

        SGD = new ScaleGestureDetector(MainApp.getAppContext(),new ScaleListener());
//        gestureDetector = new GestureDetector(MainApp.getAppContext(), new SingleTapConfirm());

        mGridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (SGD.onTouchEvent(motionEvent)) {
//                    return false;
//                }
//                return false;

                SGD.onTouchEvent(motionEvent);
                return false;
            }
        });

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

        mFabMain = (FloatingActionsMenu) v.findViewById(R.id.fab_main);
        mFabUpload = (FloatingActionButton) v.findViewById(R.id.fab_upload);
        mFabMkdir = (FloatingActionButton) v.findViewById(R.id.fab_mkdir);
        mFabUploadFromApp = (FloatingActionButton) v.findViewById(R.id.fab_upload_from_app);

        mCurrentListView = mListView;   // list by default
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(KEY_IS_GRID_VISIBLE, false)) {
                switchToGridView();
            }
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            if (isGridEnabled()) {
                Log_OC.v(TAG, "Setting grid position " + referencePosition);
                mGridView.setSelection(referencePosition);
            } else {
                Log_OC.v(TAG, "Setting and centering around list position " + referencePosition);
                mListView.setAndCenterSelection(referencePosition);
            }
        }

        return v;
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (scale == -1f){
                mGridView.setNumColumns(GridView.AUTO_FIT);
                scale = mGridView.getNumColumns();
            }
            scale *= 1-(detector.getScaleFactor()- 1);
            scale = Math.max(2.0f, Math.min(scale, 10.0f));
            Integer scaleInt = Math.round(scale);
            mGridView.setNumColumns(scaleInt);
            mGridView.invalidateViews();

            return true;
        }
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
        savedInstanceState.putBoolean(KEY_IS_GRID_VISIBLE, isGridEnabled());
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
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    protected void setChoiceMode(int choiceMode) {
        mListView.setChoiceMode(choiceMode);
        mGridView.setChoiceMode(choiceMode);
    }

    protected void setMultiChoiceModeListener(AbsListView.MultiChoiceModeListener listener) {
        mListView.setMultiChoiceModeListener(listener);
        mGridView.setMultiChoiceModeListener(listener);
    }

    /**
     * TODO doc
     * To be called before setAdapter, or GridViewWithHeaderAndFooter will throw an exception
     *
     * @param enabled
     */
    protected void setFooterEnabled(boolean enabled) {
        if (enabled) {
            if (mGridView.getFooterViewCount() == 0 && mGridView.isCorrectAdapter()) {
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
