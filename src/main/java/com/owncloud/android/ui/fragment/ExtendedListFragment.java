/*
 * ownCloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2012-2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchOperation;
import com.owncloud.android.ui.ExtendedListView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.parceler.Parcel;

import java.util.ArrayList;

import third_parties.in.srain.cube.GridViewWithHeaderAndFooter;

public class ExtendedListFragment extends Fragment
        implements OnItemClickListener, OnEnforceableRefreshListener, SearchView.OnQueryTextListener {

    protected static final String TAG = ExtendedListFragment.class.getSimpleName();

    protected static final String KEY_SAVED_LIST_POSITION = "SAVED_LIST_POSITION";

    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS = "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    private static final String KEY_IS_GRID_VISIBLE = "IS_GRID_VISIBLE";
    public static final float minColumnSize = 2.0f;

    private int maxColumnSize = 5;
    private int maxColumnSizePortrait = 5;
    private int maxColumnSizeLandscape = 10;

    private ScaleGestureDetector mScaleGestureDetector = null;
    protected SwipeRefreshLayout mRefreshListLayout;
    protected SwipeRefreshLayout mRefreshGridLayout;
    protected SwipeRefreshLayout mRefreshEmptyLayout;
    protected LinearLayout mEmptyListContainer;
    protected TextView mEmptyListMessage;
    protected TextView mEmptyListHeadline;
    protected ImageView mEmptyListIcon;
    protected ProgressBar mEmptyListProgress;

    private FloatingActionsMenu mFabMain;
    private FloatingActionButton mFabUpload;
    private FloatingActionButton mFabMkdir;
    private FloatingActionButton mFabUploadFromApp;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell = 0;

    private OnEnforceableRefreshListener mOnRefreshListener = null;

    protected AbsListView mCurrentListView;
    private ExtendedListView mListView;
    private View mListFooterView;
    private GridViewWithHeaderAndFooter mGridView;
    private View mGridFooterView;

    private BaseAdapter mAdapter;

    protected SearchView searchView;
    private Handler handler = new Handler();

    private float mScale = -1f;

    @Parcel
    public enum SearchType {
        NO_SEARCH,
        REGULAR_FILTER,
        FILE_SEARCH,
        FAVORITE_SEARCH,
        FAVORITE_SEARCH_FILTER,
        VIDEO_SEARCH,
        VIDEO_SEARCH_FILTER,
        PHOTO_SEARCH,
        PHOTOS_SEARCH_FILTER,
        RECENTLY_MODIFIED_SEARCH,
        RECENTLY_MODIFIED_SEARCH_FILTER,
        RECENTLY_ADDED_SEARCH,
        RECENTLY_ADDED_SEARCH_FILTER,
        // not a real filter, but nevertheless
        SHARED_FILTER
    }

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
        final MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);

        final Handler handler = new Handler();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        Activity activity;
        if ((activity = getActivity()) != null) {
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = displaymetrics.widthPixels;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                searchView.setMaxWidth((int) (width * 0.4));
            } else {
                if (activity instanceof FolderPickerActivity) {
                    searchView.setMaxWidth((int) (width * 0.8));
                } else {
                    searchView.setMaxWidth((int) (width * 0.7));
                }
            }
        }

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, final boolean hasFocus) {
                if (hasFocus) {
                    mFabMain.collapse();
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null && !(getActivity() instanceof FolderPickerActivity)) {
                            setFabEnabled(!hasFocus);

                            boolean searchSupported = AccountUtils.hasSearchSupport(AccountUtils.
                                    getCurrentOwnCloudAccount(MainApp.getAppContext()));

                            if (getResources().getBoolean(R.bool.bottom_toolbar_enabled) && searchSupported) {
                                BottomNavigationView bottomNavigationView = getActivity().
                                        findViewById(R.id.bottom_navigation_view);
                                if (hasFocus) {
                                    bottomNavigationView.setVisibility(View.GONE);
                                } else {
                                    bottomNavigationView.setVisibility(View.VISIBLE);
                                }
                            }

                        }
                    }
                }, 100);
            }
        });

        final View mSearchEditFrame = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_edit_frame);

        ViewTreeObserver vto = mSearchEditFrame.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int oldVisibility = -1;

            @Override
            public void onGlobalLayout() {

                int currentVisibility = mSearchEditFrame.getVisibility();

                if (currentVisibility != oldVisibility) {
                    if (currentVisibility == View.VISIBLE) {

                        setEmptyListMessage(SearchType.REGULAR_FILTER);
                    } else {
                        setEmptyListMessage(SearchType.NO_SEARCH);
                    }

                    oldVisibility = currentVisibility;
                }

            }
        });

        int fontColor = ThemeUtils.fontColor();

        LinearLayout searchBar = searchView.findViewById(R.id.search_bar);
        TextView searchBadge = searchView.findViewById(R.id.search_badge);

        searchBadge.setTextColor(fontColor);
        searchBadge.setHintTextColor(fontColor);

        ImageView searchButton = searchView.findViewById(R.id.search_button);
        searchButton.setImageDrawable(ThemeUtils.tintDrawable(R.drawable.ic_search, fontColor));

        searchBar.setLayoutTransition(new LayoutTransition());
    }

    public boolean onQueryTextChange(final String query) {
        if (getFragmentManager() != null && getFragmentManager().
                findFragmentByTag(FileDisplayActivity.TAG_SECOND_FRAGMENT) instanceof ExtendedListFragment) {
            performSearch(query, false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        performSearch(query, true);
        return true;
    }

    private void performSearch(final String query, boolean isSubmit) {
        handler.removeCallbacksAndMessages(null);

        if (!TextUtils.isEmpty(query)) {

            int delay = 500;

            if (isSubmit) {
                delay = 0;
            }

            if (mAdapter != null && mAdapter instanceof FileListListAdapter) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (AccountUtils.hasSearchSupport(AccountUtils.
                                getCurrentOwnCloudAccount(MainApp.getAppContext()))) {
                            EventBus.getDefault().post(new SearchEvent(query, SearchOperation.SearchType.FILE_SEARCH,
                                    SearchEvent.UnsetType.NO_UNSET));
                        } else {
                            FileListListAdapter fileListListAdapter = (FileListListAdapter) mAdapter;
                            fileListListAdapter.getFilter().filter(query);
                        }
                    }
                }, delay);
            } else if (mAdapter != null && mAdapter instanceof LocalFileListAdapter) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) mAdapter;
                        localFileListAdapter.filter(query);
                    }
                }, delay);
            }

            if (searchView != null && delay == 0) {
                searchView.clearFocus();
            }
        } else {
            Activity activity;
            if ((activity = getActivity()) != null) {
                if (activity instanceof FileDisplayActivity) {
                    FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) activity;
                    fileDisplayActivity.resetSearchView();
                    fileDisplayActivity.refreshListOfFilesFragment(true);
                } else if (activity instanceof UploadFilesActivity) {
                    LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) mAdapter;
                    localFileListAdapter.filter(query);
                } else if (activity instanceof FolderPickerActivity) {
                    ((FolderPickerActivity) activity).refreshListOfFilesFragment(true);
                }

            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.list_fragment, null);
        setupEmptyList(v);

        mListView = v.findViewById(R.id.list_root);
        mListView.setOnItemClickListener(this);
        mListFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mGridView = v.findViewById(R.id.grid_root);

        mScale = PreferenceManager.getGridColumns(getContext());
        setGridViewColumns(1f);

        mGridView.setOnItemClickListener(this);

        mGridFooterView = inflater.inflate(R.layout.list_footer, null, false);

        mScaleGestureDetector = new ScaleGestureDetector(MainApp.getAppContext(),new ScaleListener());

        mGridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mScaleGestureDetector.onTouchEvent(motionEvent);

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }

                return false;
            }
        });

        if (savedInstanceState != null) {
            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            if (mCurrentListView!= null && mCurrentListView.equals(mListView)) {
                Log_OC.v(TAG, "Setting and centering around list position " + referencePosition);
                mListView.setAndCenterSelection(referencePosition);
            } else {
                Log_OC.v(TAG, "Setting grid position " + referencePosition);
                mGridView.setSelection(referencePosition);
            }
        }

        // Pull-down to refresh layout
        mRefreshListLayout = v.findViewById(R.id.swipe_containing_list);
        mRefreshGridLayout = v.findViewById(R.id.swipe_containing_grid);
        mRefreshEmptyLayout = v.findViewById(R.id.swipe_containing_empty);

        onCreateSwipeToRefresh(mRefreshListLayout);
        onCreateSwipeToRefresh(mRefreshGridLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);

        mListView.setEmptyView(mRefreshEmptyLayout);
        mGridView.setEmptyView(mRefreshEmptyLayout);

        mFabMain = v.findViewById(R.id.fab_main);
        mFabUpload = v.findViewById(R.id.fab_upload);
        mFabMkdir = v.findViewById(R.id.fab_mkdir);
        mFabUploadFromApp = v.findViewById(R.id.fab_upload_from_app);

        applyFABTheming();

        boolean searchSupported = AccountUtils.hasSearchSupport(AccountUtils.
                getCurrentOwnCloudAccount(MainApp.getAppContext()));

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled) && searchSupported) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFabMain.getLayoutParams();
            final float scale = v.getResources().getDisplayMetrics().density;

            BottomNavigationView bottomNavigationView = v.findViewById(R.id.bottom_navigation_view);

            // convert the DP into pixel
            int pixel = (int) (32 * scale + 0.5f);
            layoutParams.setMargins(0, 0, pixel / 2, bottomNavigationView.getMeasuredHeight() + pixel * 2);
        }

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

    public void setEmptyListVisible() {
        mEmptyListContainer.setVisibility(View.VISIBLE);
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
            setGridViewColumns(detector.getScaleFactor());

            PreferenceManager.setGridColumns(getContext(), mScale);

            mAdapter.notifyDataSetChanged();

            return true;
        }
    }

    private void setGridViewColumns(float scaleFactor) {
        if (mScale == -1f) {
            mGridView.setNumColumns(GridView.AUTO_FIT);
            mScale = mGridView.getNumColumns();
        }
        mScale *= 1.f - (scaleFactor - 1.f);
        mScale = Math.max(minColumnSize, Math.min(mScale, maxColumnSize));
        Integer scaleInt = Math.round(mScale);
        mGridView.setNumColumns(scaleInt);
        mGridView.invalidateViews();
    }

    protected void setupEmptyList(View view) {
        mEmptyListContainer = view.findViewById(R.id.empty_list_view);
        mEmptyListMessage = view.findViewById(R.id.empty_list_view_text);
        mEmptyListHeadline = view.findViewById(R.id.empty_list_view_headline);
        mEmptyListIcon = view.findViewById(R.id.empty_list_icon);
        mEmptyListProgress = view.findViewById(R.id.empty_list_progress);
        mEmptyListProgress.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryColor(),
                PorterDuff.Mode.SRC_IN);
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
            mIndexes = new ArrayList<>();
            mFirstPositions = new ArrayList<>();
            mTops = new ArrayList<>();
            mHeightCell = 0;
        }

        mScale = PreferenceManager.getGridColumns(getContext());
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

        PreferenceManager.setGridColumns(getContext(), mScale);
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

    public int getColumnSize() {
        return Math.round(mScale);
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
        // to be @overridden
    }

    @Override
    public void onRefresh() {

        if (searchView != null) {
            searchView.onActionViewCollapsed();

            Activity activity;
            if ((activity = getActivity()) != null && activity instanceof FileDisplayActivity) {
                FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) activity;
                fileDisplayActivity.setDrawerIndicatorEnabled(fileDisplayActivity.isDrawerIndicatorAvailable());
            }
        }

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
    public void setFabEnabled(final boolean enabled) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (enabled) {
                        mFabMain.setVisibility(View.VISIBLE);
                    } else {
                        mFabMain.setVisibility(View.GONE);
                    }
                }
            });
        }
    }


    /**
     * Set tinting of FAB's from server data
     */
    private void applyFABTheming() {
        AddFloatingActionButton addButton = getFabMain().getAddButton();
        addButton.setColorNormal(ThemeUtils.primaryColor());
        addButton.setColorPressed(ThemeUtils.primaryDarkColor());
        addButton.setPlusColor(ThemeUtils.fontColor());

        ThemeUtils.tintFloatingActionButton(getFabUpload(), R.drawable.ic_action_upload);
        ThemeUtils.tintFloatingActionButton(getFabMkdir(), R.drawable.ic_action_create_dir);
        ThemeUtils.tintFloatingActionButton(getFabUploadFromApp(), R.drawable.ic_import);
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
     * displays an empty list information with a headline, a message and a not to be tinted icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     */
    public void setMessageForEmptyList(@StringRes final int headline, @StringRes final int message,
                                       @DrawableRes final int icon) {
        setMessageForEmptyList(headline, message, icon, false);
    }

    /**
     * displays an empty list information with a headline, a message and an icon.
     *
     * @param headline the headline
     * @param message  the message
     * @param icon     the icon to be shown
     * @param tintIcon flag if the given icon should be tinted with primary color
     */
    public void setMessageForEmptyList(@StringRes final int headline, @StringRes final int message,
                                       @DrawableRes final int icon, final boolean tintIcon) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                if (mEmptyListContainer != null && mEmptyListMessage != null) {
                    mEmptyListHeadline.setText(headline);
                    mEmptyListMessage.setText(message);

                    if (tintIcon) {
                        mEmptyListIcon.setImageDrawable(ThemeUtils.tintDrawable(icon, ThemeUtils.primaryColor()));
                    } else {
                        mEmptyListIcon.setImageResource(icon);
                    }

                    mEmptyListIcon.setVisibility(View.VISIBLE);
                    mEmptyListProgress.setVisibility(View.GONE);
                    mEmptyListMessage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void setEmptyListMessage(final SearchType searchType) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                if (searchType == SearchType.NO_SEARCH) {
                    setMessageForEmptyList(
                            R.string.file_list_empty_headline,
                            R.string.file_list_empty,
                            R.drawable.ic_list_empty_folder,
                            true
                    );
                } else if (searchType == SearchType.FILE_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty, R.drawable.ic_search_light_grey);
                } else if (searchType == SearchType.FAVORITE_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_favorite_headline,
                            R.string.file_list_empty_favorites_filter_list, R.drawable.ic_star_light_yellow);
                } else if (searchType == SearchType.VIDEO_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search_videos,
                            R.string.file_list_empty_text_videos, R.drawable.ic_list_empty_video);
                } else if (searchType == SearchType.PHOTO_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search_photos,
                            R.string.file_list_empty_text_photos, R.drawable.ic_list_empty_image);
                } else if (searchType == SearchType.RECENTLY_MODIFIED_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty_recently_modified, R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.RECENTLY_ADDED_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty_recently_added, R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.REGULAR_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_search,
                            R.string.file_list_empty_search, R.drawable.ic_search_light_grey);
                } else if (searchType == SearchType.FAVORITE_SEARCH_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty_favorites_filter, R.drawable.ic_star_light_yellow);
                } else if (searchType == SearchType.VIDEO_SEARCH_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search_videos,
                            R.string.file_list_empty_text_videos_filter, R.drawable.ic_list_empty_video);
                } else if (searchType == SearchType.PHOTOS_SEARCH_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search_photos,
                            R.string.file_list_empty_text_photos_filter, R.drawable.ic_list_empty_image);
                } else if (searchType == SearchType.RECENTLY_MODIFIED_SEARCH_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty_recently_modified_filter, R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.RECENTLY_ADDED_SEARCH_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                            R.string.file_list_empty_recently_added_filter, R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.SHARED_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_shared_headline,
                            R.string.file_list_empty_shared, R.drawable.ic_list_empty_shared);
                }
            }
        });
    }

    /**
     * Set message for empty list view.
     */
    public void setEmptyListLoadingMessage() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mEmptyListContainer != null && mEmptyListMessage != null) {
                    mEmptyListHeadline.setText(R.string.file_list_loading);
                    mEmptyListMessage.setText("");

                    mEmptyListIcon.setVisibility(View.GONE);
                    mEmptyListProgress.setVisibility(View.VISIBLE);
                }
            }
        });
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
        int primaryColor = ThemeUtils.primaryColor();
        int darkColor = ThemeUtils.primaryDarkColor();
        int accentColor = ThemeUtils.primaryAccentColor();

        // Colors in animations
        // TODO change this to use darker and lighter color, again.
        refreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshListLayout.setRefreshing(false);
        mRefreshGridLayout.setRefreshing(false);
        mRefreshEmptyLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh(ignoreETag);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSize = maxColumnSizeLandscape;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            maxColumnSize = maxColumnSizePortrait;
        } else {
            maxColumnSize = maxColumnSizePortrait;
        }

        if (mGridView != null && mGridView.getNumColumns() > maxColumnSize) {
            mGridView.setNumColumns(maxColumnSize);
            mGridView.invalidateViews();
        }
    }
}
