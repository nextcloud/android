/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2012-2016 ownCloud Inc.
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

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;

import java.util.ArrayList;

import third_parties.in.srain.cube.GridViewWithHeaderAndFooter;

public class ExtendedListFragment extends Fragment
        implements OnItemClickListener, OnEnforceableRefreshListener, SearchView.OnQueryTextListener, ExtendedListFragmentListener {

    protected static final String TAG = ExtendedListFragment.class.getSimpleName();

    protected static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";

    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS = "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    private static final String KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE";
    private static final String KEY_IS_SEARCH_OPEN = "IS_SEARCH_OPEN";
    private static final String KEY_SEARCH_QUERY = "SEARCH_QUERY";

    protected SwipeRefreshLayout mRefreshListLayout;
    protected SwipeRefreshLayout mRefreshEmptyLayout;
    protected LinearLayout mEmptyListContainer;
    protected TextView mEmptyListMessage;
    protected TextView mEmptyListHeadline;
    protected ImageView mEmptyListIcon;
    protected ProgressBar mEmptyListProgress;
    //save the search state
    protected boolean mSearchIsOpen;
    protected String mSearchQuery = null;
    protected MenuItem mMenuItem;
    protected SearchView mSearchView;
    protected AbsListView mCurrentListView;
    protected Handler mHandler;
    protected SwipeRefreshLayout mRefreshGridLayout;
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
    private ExtendedListView mListView;
    private TextView mTooManyFilesTextView;
    private FrameLayout mFrameLayout;
    private View mListFooterView;
    private GridViewWithHeaderAndFooter mGridView;
    private View mGridFooterView;
    private BaseAdapter mAdapter;

    protected void setListAdapter(BaseAdapter listAdapter) {
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

    public boolean isGridEnabled() {
        return (mCurrentListView != null && mCurrentListView.equals(mGridView));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mMenuItem);
        mSearchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);

    }

    public boolean onQueryTextChange(final String query) {
        mSearchQuery = query;
        mHandler.removeCallbacksAndMessages(null);

        mRefreshListLayout.setRefreshing(true);
        mRefreshGridLayout.setRefreshing(true);
        mRefreshEmptyLayout.setRefreshing(true);

        if (mAdapter != null && mAdapter instanceof FileListListAdapter) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    FileListListAdapter fileListListAdapter = (FileListListAdapter) mAdapter;
                    fileListListAdapter.getFilter().filter(mSearchQuery);
                }
            }, 500);
        } else if (mAdapter != null && mAdapter instanceof LocalFileListAdapter) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) mAdapter;
                    localFileListAdapter.getFilter().filter(mSearchQuery);
                }
            }, 500);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;
        mHandler.removeCallbacksAndMessages(null);

        mRefreshListLayout.setRefreshing(true);
        mRefreshGridLayout.setRefreshing(true);
        mRefreshEmptyLayout.setRefreshing(true);

        if (mAdapter.getClass().equals(FileListListAdapter.class)) {
            FileListListAdapter fileListListAdapter = (FileListListAdapter) mAdapter;
            fileListListAdapter.getFilter().filter(query);
        } else if (mAdapter.getClass().equals(LocalFileListAdapter.class)) {
            LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) mAdapter;
            localFileListAdapter.getFilter().filter(query);
        }

        if (mSearchView != null) {
            mSearchView.clearFocus();
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        if (savedInstanceState != null) {
            mIndexes = savedInstanceState.getIntegerArrayList(KEY_INDEXES);
            mFirstPositions = savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS);
            mTops = savedInstanceState.getIntegerArrayList(KEY_TOPS);
            mHeightCell = savedInstanceState.getInt(KEY_HEIGHT_CELL);
            setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE));
            mSearchIsOpen = savedInstanceState.getBoolean(KEY_IS_SEARCH_OPEN);
            if (savedInstanceState.getString(KEY_SEARCH_QUERY) != null) {
                mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            } else {
                mSearchQuery = "";
            }
        } else {
            mIndexes = new ArrayList<>();
            mFirstPositions = new ArrayList<>();
            mTops = new ArrayList<>();
            mHeightCell = 0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, null);
        setupEmptyList(v);

        mListView = (ExtendedListView) (v.findViewById(R.id.list_root));
        mListView.setOnItemClickListener(this);
        mListFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mGridView = (GridViewWithHeaderAndFooter) (v.findViewById(R.id.grid_root));
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setOnItemClickListener(this);

        mGridFooterView = inflater.inflate(R.layout.list_footer, null, false);

        // Pull-down to refresh layout
        mRefreshListLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_list);
        mRefreshGridLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_grid);
        mRefreshEmptyLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_empty);

        onCreateSwipeToRefresh(mRefreshListLayout);
        onCreateSwipeToRefresh(mRefreshGridLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);

        mListView.setEmptyView(mRefreshEmptyLayout);
        mGridView.setEmptyView(mRefreshEmptyLayout);

        mFabMain = (FloatingActionsMenu) v.findViewById(R.id.fab_main);
        mFabUpload = (FloatingActionButton) v.findViewById(R.id.fab_upload);
        mFabMkdir = (FloatingActionButton) v.findViewById(R.id.fab_mkdir);
        mFabUploadFromApp = (FloatingActionButton) v.findViewById(R.id.fab_upload_from_app);

        mFrameLayout = (FrameLayout) v.findViewById(R.id.listFragment_framelayout);
        mTooManyFilesTextView = (TextView) v.findViewById(R.id.too_many_results_textview);

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

    protected void setupEmptyList(View view) {
        mEmptyListContainer = (LinearLayout) view.findViewById(R.id.empty_list_view);
        mEmptyListMessage = (TextView) view.findViewById(R.id.empty_list_view_text);
        mEmptyListHeadline = (TextView) view.findViewById(R.id.empty_list_view_headline);
        mEmptyListIcon = (ImageView) view.findViewById(R.id.empty_list_icon);
        mEmptyListProgress = (ProgressBar) view.findViewById(R.id.empty_list_progress);
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
        if (mSearchView != null) {
            savedInstanceState.putBoolean(KEY_IS_SEARCH_OPEN, !mSearchView.isIconified());
        }
        if (mSearchQuery != null && !mSearchQuery.isEmpty()) {
            savedInstanceState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        } else {
            mSearchQuery = "";
        }
    }

    /**
     * Calculates the position of the item that will be used as a reference to
     * reposition the visible items in the list when the device is turned to
     * other position.
     * <p>
     * The current policy is take as a reference the visible item in the center
     * of the screen.
     *
     * @return The position in the list of the visible item in the center of the
     * screen.
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
            final int firstPosition = mFirstPositions.remove(mFirstPositions.size() - 1);
            int top = mTops.remove(mTops.size() - 1);

            Log_OC.v(TAG, "Setting selection to position: " + firstPosition + "; top: "
                    + top + "; index: " + index);

            if (mCurrentListView != null && mCurrentListView.equals(mListView)) {
                if (mHeightCell * index <= mListView.getHeight()) {
                    mListView.setSelectionFromTop(firstPosition, top);
                } else {
                    mListView.setSelectionFromTop(index, 0);
                }

            } else {
                if (mHeightCell * index <= mGridView.getHeight()) {
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
        int top = (view == null) ? 0 : view.getTop();

        mTops.add(top);

        // Save the height of a cell
        mHeightCell = (view == null || mHeightCell != 0) ? mHeightCell : view.getHeight();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
     * <p>
     * Sets the 'enabled' state of the refresh layouts contained in the fragment.
     * <p>
     * When 'false' is set, prevents user gestures but keeps the option to refresh programatically,
     *
     * @param enabled Desired state for capturing swipe gesture.
     */
    public void setSwipeEnabled(boolean enabled) {
        mRefreshListLayout.setEnabled(enabled);
        mRefreshGridLayout.setEnabled(enabled);
        mRefreshEmptyLayout.setEnabled(enabled);
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB visibility is set to View.GONE programmatically,
     *
     * @param enabled Desired visibility for the FAB.
     */
    public void setFabEnabled(boolean enabled) {
        if (enabled) {
            mFabMain.setVisibility(View.VISIBLE);
        } else {
            mFabMain.setVisibility(View.GONE);
        }
    }

    /**
     * Set message for empty list view.
     */
    public void setMessageForEmptyList(String message) {
        if (mEmptyListContainer != null && mEmptyListMessage != null) {
            mEmptyListMessage.setText(message);
        }
    }

    /**
     * displays an empty list information with a headline, a message and an icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     */
    public void setMessageForEmptyList(@StringRes int headline, @StringRes int message, @DrawableRes int icon) {
        if (mEmptyListContainer != null && mEmptyListMessage != null) {
            mEmptyListHeadline.setText(headline);
            mEmptyListMessage.setText(message);
            mEmptyListIcon.setImageResource(icon);

            mEmptyListIcon.setVisibility(View.VISIBLE);
            mEmptyListProgress.setVisibility(View.GONE);
        }
    }

    /**
     * Set message for empty list view.
     */
    public void setEmptyListMessage() {
        setMessageForEmptyList(
                R.string.file_list_empty_headline,
                R.string.file_list_empty,
                R.drawable.ic_list_empty_folder
        );
    }

    /**
     * Set message for empty list view.
     */
    public void setEmptyListLoadingMessage() {
        if (mEmptyListContainer != null && mEmptyListMessage != null) {
            mEmptyListHeadline.setText(R.string.file_list_loading);
            mEmptyListMessage.setText("");

            mEmptyListIcon.setVisibility(View.GONE);
            mEmptyListProgress.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get the text of EmptyListMessage TextView.
     *
     * @return String empty text view text-value
     */
    public String getEmptyViewText() {
        return (mEmptyListContainer != null && mEmptyListMessage != null) ? mEmptyListMessage.getText().toString() : "";
    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        // Colors in animations
        refreshLayout.setColorSchemeResources(R.color.color_accent, R.color.primary, R.color.primary_dark);

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
     * @param enabled flag if footer should be shown/calculated
     */
    protected void setFooterEnabled(boolean enabled) {
        if (enabled) {
            if (mGridView.getFooterViewCount() == 0 && mGridView.isCorrectAdapter()) {
                if (mGridFooterView.getParent() != null) {
                    ((ViewGroup) mGridFooterView.getParent()).removeView(mGridFooterView);
                }
                mGridView.addFooterView(mGridFooterView, null, false);
            }
            mGridFooterView.invalidate();

            if (mListView.getFooterViewsCount() == 0) {
                if (mListFooterView.getParent() != null) {
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
     * set the list/grid footer text.
     *
     * @param text the footer text
     */
    protected void setFooterText(String text) {
        if (text != null && text.length() > 0) {
            ((TextView) mListFooterView.findViewById(R.id.footerText)).setText(text);
            ((TextView) mGridFooterView.findViewById(R.id.footerText)).setText(text);
            setFooterEnabled(true);

        } else {
            setFooterEnabled(false);
        }
    }

    @Override
    public void endFilterRefresh() {
        mRefreshGridLayout.setRefreshing(false);
        mRefreshListLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);
    }

    @Override
    public void showHundredFilesMessage(boolean show) {
        if (mTooManyFilesTextView.getVisibility() == View.GONE && show) {
            mTooManyFilesTextView.setVisibility(View.VISIBLE);
            mTooManyFilesTextView.measure(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);

            mFrameLayout.setPadding(0, pxToDp(mTooManyFilesTextView.getMeasuredHeight()) + 10, 0, 0);
        } else {
            mTooManyFilesTextView.setVisibility(View.GONE);
            mFrameLayout.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void collapseFab() {
        if (mFabMain != null && mFabMain.isExpanded()) {
            mFabMain.collapse();
        }
    }

    private int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public boolean isSearchOpen() {
        return mSearchIsOpen;
    }

}
