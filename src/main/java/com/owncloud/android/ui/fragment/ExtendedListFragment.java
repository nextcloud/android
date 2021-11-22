/*
 * ownCloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2012-2016 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ListFragmentBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import org.greenrobot.eventbus.EventBus;
import org.parceler.Parcel;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ExtendedListFragment extends Fragment implements
    OnItemClickListener,
    OnEnforceableRefreshListener,
    SearchView.OnQueryTextListener,
    SearchView.OnCloseListener,
    Injectable {

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

    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    private ScaleGestureDetector mScaleGestureDetector;
    protected SwipeRefreshLayout mRefreshListLayout;
    protected MaterialButton mSortButton;
    protected MaterialButton mSwitchGridViewButton;
    protected LinearLayout mEmptyListContainer;
    protected TextView mEmptyListMessage;
    protected TextView mEmptyListHeadline;
    protected ImageView mEmptyListIcon;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;
    private int mHeightCell;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;

    private EmptyRecyclerView mRecyclerView;

    protected SearchView searchView;
    private ImageView closeButton;
    private Handler handler = new Handler(Looper.getMainLooper());

    private float mScale = AppPreferencesImpl.DEFAULT_GRID_COLUMN;

    private ListFragmentBinding binding;

    @Parcel
    public enum SearchType {
        NO_SEARCH,
        REGULAR_FILTER,
        FILE_SEARCH,
        FAVORITE_SEARCH,
        GALLERY_SEARCH,
        RECENTLY_MODIFIED_SEARCH,
        RECENTLY_ADDED_SEARCH,
        // not a real filter, but nevertheless
        SHARED_FILTER
    }

    protected void setRecyclerViewAdapter(RecyclerView.Adapter recyclerViewAdapter) {
        mRecyclerView.setAdapter(recyclerViewAdapter);
    }

    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setLoading(boolean enabled) {
        mRefreshListLayout.setRefreshing(enabled);
    }

    public void switchToGridView() {
        if (!isGridEnabled()) {
            getRecyclerView().setLayoutManager(new GridLayoutManager(getContext(), getColumnsCount()));
        }
    }

    public void switchToListView() {
        if (isGridEnabled()) {
            getRecyclerView().setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    public boolean isGridEnabled() {
        if (getRecyclerView() != null) {
            return getRecyclerView().getLayoutManager() instanceof GridLayoutManager;
        } else {
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        final MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        ThemeToolbarUtils.themeSearchView(searchView, requireContext());

        SearchView.SearchAutoComplete theTextArea = searchView.findViewById(R.id.search_src_text);
        theTextArea.setHighlightColor(ThemeColorUtils.primaryAccentColor(getContext()));

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
                    searchView.setMaxWidth(width);
                }
            }
        }

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> handler.post(() -> {
            if (getActivity() != null && !(getActivity() instanceof FolderPickerActivity)
                && !(getActivity() instanceof UploadFilesActivity)) {
                if (getActivity() instanceof FileDisplayActivity) {
                    Fragment fragment = ((FileDisplayActivity) getActivity()).getLeftFragment();
                    if (fragment instanceof OCFileListFragment) {
                        ((OCFileListFragment) fragment).setFabVisible(!hasFocus);
                    }
                }
                if (TextUtils.isEmpty(searchView.getQuery())) {
                    closeButton.setVisibility(View.INVISIBLE);
                }
            }
        }));

        // On close -> empty field, show keyboard and
        closeButton.setOnClickListener(view -> {
            searchView.setQuery("", true);
            searchView.requestFocus();
            searchView.onActionViewExpanded();

            InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        final View mSearchEditFrame = searchView
            .findViewById(androidx.appcompat.R.id.search_edit_frame);

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
                        if (MainApp.isOnlyOnDevice()) {
                            setMessageForEmptyList(R.string.file_list_empty_headline,
                                                   R.string.file_list_empty_on_device,
                                                   R.drawable.ic_list_empty_folder,
                                                   true);
                        } else {
                            setEmptyListMessage(ExtendedListFragment.SearchType.NO_SEARCH);
                        }
                    }

                    oldVisibility = currentVisibility;
                }

            }
        });

        LinearLayout searchBar = searchView.findViewById(R.id.search_bar);
        searchBar.setLayoutTransition(new LayoutTransition());
    }

    public boolean onQueryTextChange(final String query) {
        // After 300 ms, set the query

        closeButton.setVisibility(View.VISIBLE);
        if (query.isEmpty()) {
            closeButton.setVisibility(View.INVISIBLE);
        }

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
        performSearch(query, false);
        return true;
    }

    public void performSearch(final String query, boolean isBackPressed) {
        handler.removeCallbacksAndMessages(null);
        RecyclerView.Adapter adapter = getRecyclerView().getAdapter();
        Activity activity = getActivity();
        if (activity != null) {
            if (activity instanceof FileDisplayActivity) {
                if (isBackPressed && TextUtils.isEmpty(query)) {
                    FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) activity;
                    fileDisplayActivity.resetSearchView();
                    fileDisplayActivity.updateListOfFilesFragment(true);
                } else {
                    handler.post(() -> {
                        if (adapter instanceof OCFileListAdapter) {
                            if (accountManager
                                .getUser()
                                .getServer()
                                .getVersion()
                                .isNewerOrEqual(OwnCloudVersion.nextcloud_20)
                            ) {
                                ((FileDisplayActivity) activity).performUnifiedSearch(query);
                            } else {
                                EventBus.getDefault().post(
                                    new SearchEvent(query, SearchRemoteOperation.SearchType.FILE_SEARCH)
                                                          );
                            }
                        } else if (adapter instanceof LocalFileListAdapter) {
                            LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) adapter;
                            localFileListAdapter.filter(query);
                        }
                    });

                    if (searchView != null) {
                        searchView.clearFocus();
                    }
                }
            } else if (activity instanceof UploadFilesActivity) {
                LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) adapter;
                localFileListAdapter.filter(query);
                ((UploadFilesActivity) activity).showToolbarSpinner();
            } else if (activity instanceof FolderPickerActivity) {
                ((FolderPickerActivity) activity).refreshListOfFilesFragment(true);
            }
        }
    }

    @Override
    public boolean onClose() {
        performSearch("", true);

        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        binding = ListFragmentBinding.inflate(inflater, container, false);
        View v = binding.getRoot();

        setupEmptyList(v);

        mRecyclerView = binding.listRoot;
        mRecyclerView.setHasFooter(true);
        mRecyclerView.setEmptyView(binding.emptyList.emptyListView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mScale = preferences.getGridColumns();
        setGridViewColumns(1f);

        mScaleGestureDetector = new ScaleGestureDetector(MainApp.getAppContext(), new ScaleListener());

        getRecyclerView().setOnTouchListener((view, motionEvent) -> {
            mScaleGestureDetector.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                view.performClick();
            }

            return false;
        });

        // Pull-down to refresh layout
        mRefreshListLayout = binding.swipeContainingList;
        ThemeLayoutUtils.colorSwipeRefreshLayout(getContext(), mRefreshListLayout);
        mRefreshListLayout.setOnRefreshListener(this);

        mSortButton = getActivity().findViewById(R.id.sort_button);
        mSwitchGridViewButton = getActivity().findViewById(R.id.switch_grid_view_button);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            setGridViewColumns(detector.getScaleFactor());

            preferences.setGridColumns(mScale);

            getRecyclerView().getAdapter().notifyDataSetChanged();

            return true;
        }
    }

    private void setGridViewColumns(float scaleFactor) {
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            if (mScale == -1f) {
                gridLayoutManager.setSpanCount(GridView.AUTO_FIT);
                mScale = gridLayoutManager.getSpanCount();
            }
            mScale *= 1.f - (scaleFactor - 1.f);
            mScale = Math.max(minColumnSize, Math.min(mScale, maxColumnSize));
            Integer scaleInt = Math.round(mScale);
            gridLayoutManager.setSpanCount(scaleInt);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    protected void setupEmptyList(View view) {
        mEmptyListContainer = binding.emptyList.emptyListView;
        mEmptyListMessage = binding.emptyList.emptyListViewText;
        mEmptyListHeadline = binding.emptyList.emptyListViewHeadline;
        mEmptyListIcon = binding.emptyList.emptyListIcon;
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

            if (savedInstanceState.getBoolean(KEY_IS_GRID_VISIBLE, false) && getRecyclerView().getAdapter() != null) {
                switchToGridView();
            }

            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
            Log_OC.v(TAG, "Setting grid position " + referencePosition);
            scrollToPosition(referencePosition);
        } else {
            mIndexes = new ArrayList<>();
            mFirstPositions = new ArrayList<>();
            mTops = new ArrayList<>();
            mHeightCell = 0;
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log_OC.d(TAG, "onSaveInstanceState()");
        savedInstanceState.putBoolean(KEY_IS_GRID_VISIBLE, isGridEnabled());
        savedInstanceState.putIntegerArrayList(KEY_INDEXES, mIndexes);
        savedInstanceState.putIntegerArrayList(KEY_FIRST_POSITIONS, mFirstPositions);
        savedInstanceState.putIntegerArrayList(KEY_TOPS, mTops);
        savedInstanceState.putInt(KEY_HEIGHT_CELL, mHeightCell);
        savedInstanceState.putString(KEY_EMPTY_LIST_MESSAGE, getEmptyViewText());

        preferences.setGridColumns(mScale);
    }

    public int getColumnsCount() {
        if (mScale == -1) {
            return Math.round(AppPreferencesImpl.DEFAULT_GRID_COLUMN);
        }
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

            scrollToPosition(firstPosition);
        }
    }

    private void scrollToPosition(int position) {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        if (mRecyclerView != null) {
            int visibleItemCount = linearLayoutManager.findLastCompletelyVisibleItemPosition() -
                linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            linearLayoutManager.scrollToPositionWithOffset(position, (visibleItemCount / 2) * mHeightCell);
        }
    }

    /*
     * Save index and top position
     */
    protected void saveIndexAndTopPosition(int index) {

        mIndexes.add(index);

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        int firstPosition;
        if (layoutManager instanceof GridLayoutManager) {
            firstPosition = ((GridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
        } else {
            firstPosition = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
        }

        mFirstPositions.add(firstPosition);

        View view = mRecyclerView.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop();

        mTops.add(top);

        // Save the height of a cell
        mHeightCell = (view == null || mHeightCell != 0) ? mHeightCell : view.getHeight();
    }


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
                fileDisplayActivity.hideSearchView(fileDisplayActivity.getCurrentDir());
            }
        }
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
     * When 'false' is set, prevents user gestures but keeps the option to refresh programmatically,
     *
     * @param enabled Desired state for capturing swipe gesture.
     */
    public void setSwipeEnabled(boolean enabled) {
        mRefreshListLayout.setEnabled(enabled);
    }



    /**
     * /** Set message for empty list view.
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
                        if (getContext() != null) {
                            mEmptyListIcon.setImageDrawable(
                                ThemeDrawableUtils.tintDrawable(icon,
                                                                ThemeColorUtils.primaryColor(getContext(),true)));
                        }
                    } else {
                        mEmptyListIcon.setImageResource(icon);
                    }

                    mEmptyListIcon.setVisibility(View.VISIBLE);
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
                    setMessageForEmptyList(R.string.file_list_empty_headline,
                                           R.string.file_list_empty,
                                           R.drawable.ic_list_empty_folder,
                                           true);
                } else if (searchType == SearchType.FILE_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                                           R.string.file_list_empty,
                                           R.drawable.ic_search_light_grey);
                } else if (searchType == SearchType.FAVORITE_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_favorite_headline,
                                           R.string.file_list_empty_favorites_filter_list,
                                           R.drawable.ic_star_light_yellow);
                } else if (searchType == SearchType.RECENTLY_MODIFIED_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                                           R.string.file_list_empty_recently_modified,
                                           R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.RECENTLY_ADDED_SEARCH) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                                           R.string.file_list_empty_recently_added,
                                           R.drawable.ic_list_empty_recent);
                } else if (searchType == SearchType.REGULAR_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_headline_search,
                                           R.string.file_list_empty_search,
                                           R.drawable.ic_search_light_grey);
                } else if (searchType == SearchType.SHARED_FILTER) {
                    setMessageForEmptyList(R.string.file_list_empty_shared_headline,
                                           R.string.file_list_empty_shared,
                                           R.drawable.ic_list_empty_shared);
                }
            }
        });
    }

    /**
     * Set message for empty list view.
     */
    public void setEmptyListLoadingMessage() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mEmptyListContainer != null && mEmptyListMessage != null) {
                mEmptyListHeadline.setText(R.string.file_list_loading);
                mEmptyListMessage.setText("");

                mEmptyListIcon.setVisibility(View.GONE);
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

    @Override
    public void onRefresh(boolean ignoreETag) {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSize = maxColumnSizeLandscape;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            maxColumnSize = maxColumnSizePortrait;
        }

        if (isGridEnabled() && getColumnsCount() > maxColumnSize) {
            ((GridLayoutManager) getRecyclerView().getLayoutManager()).setSpanCount(maxColumnSize);
        }
    }

    protected void setGridSwitchButton() {
        if (isGridEnabled()) {
            mSwitchGridViewButton.setContentDescription(getString(R.string.action_switch_list_view));
            mSwitchGridViewButton.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_list));
        } else {
            mSwitchGridViewButton.setContentDescription(getString(R.string.action_switch_grid_view));
            mSwitchGridViewButton.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_module));
        }
    }
}
