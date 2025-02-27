/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
import com.nextcloud.utils.extensions.FragmentExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ListFragmentBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.LocalFileListAdapter;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject ViewThemeUtils viewThemeUtils;

    private ScaleGestureDetector mScaleGestureDetector;
    protected SwipeRefreshLayout mRefreshListLayout;
    protected MaterialButton mSortButton;
    protected MaterialButton mSwitchGridViewButton;
    protected ViewGroup mEmptyListContainer;
    protected TextView mEmptyListMessage;
    protected TextView mEmptyListHeadline;
    protected ImageView mEmptyListIcon;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes = new ArrayList<>();
    private ArrayList<Integer> mFirstPositions = new ArrayList<>();
    private ArrayList<Integer> mTops = new ArrayList<>();
    private int mHeightCell = 0;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;

    private EmptyRecyclerView mRecyclerView;

    protected SearchView searchView;
    private ImageView closeButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private float mScale = AppPreferencesImpl.DEFAULT_GRID_COLUMN;

    private ListFragmentBinding binding;

    public ListFragmentBinding getBinding() {
        return binding;
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
        viewThemeUtils.androidx.themeToolbarSearchView(searchView);
        closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        final Handler handler = new Handler(Looper.getMainLooper());

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

        LinearLayout searchBar = searchView.findViewById(R.id.search_bar);
        searchBar.setLayoutTransition(new LayoutTransition());
    }

    public boolean onQueryTextChange(final String query) {
        // After 300 ms, set the query

        closeButton.setVisibility(View.VISIBLE);
        if (query.isEmpty()) {
            closeButton.setVisibility(View.INVISIBLE);
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        RecyclerView.Adapter adapter = getRecyclerView().getAdapter();
        if (adapter instanceof OCFileListAdapter) {
            ArrayList<String> listOfHiddenFiles = ((OCFileListAdapter) adapter).listOfHiddenFiles;
            performSearch(query, listOfHiddenFiles, false);
            return true;
        }
        if (adapter instanceof LocalFileListAdapter) {
            performSearch(query, new ArrayList<>(), false);
            return true;
        }
        return false;
    }

    public void performSearch(final String query, final ArrayList<String> listOfHiddenFiles, boolean isBackPressed) {
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
                                ((FileDisplayActivity) activity).performUnifiedSearch(query, listOfHiddenFiles);
                            } else {
                                EventBus.getDefault().post(
                                    new SearchEvent(query, SearchRemoteOperation.SearchType.FILE_SEARCH)
                                                          );
                            }
                        } else if (adapter instanceof LocalFileListAdapter localFileListAdapter) {
                            localFileListAdapter.filter(query);
                        }
                    });

                    if (searchView != null) {
                        searchView.clearFocus();
                    }
                }
            } else if (activity instanceof UploadFilesActivity uploadFilesActivity) {
                LocalFileListAdapter localFileListAdapter = (LocalFileListAdapter) adapter;
                if (localFileListAdapter != null) {
                    localFileListAdapter.filter(query);
                    uploadFilesActivity.getFileListFragment().setLoading(false);
                }
            } else if (activity instanceof FolderPickerActivity) {
                ((FolderPickerActivity) activity).search(query);
            }
        }
    }

    @Override
    public boolean onClose() {
        RecyclerView.Adapter adapter = getRecyclerView().getAdapter();
        if (adapter instanceof OCFileListAdapter) {
            ArrayList<String> listOfHiddenFiles = ((OCFileListAdapter) adapter).listOfHiddenFiles;
            performSearch("", listOfHiddenFiles,true);
            return false;
        }
        return true;
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
        viewThemeUtils.androidx.themeSwipeRefreshLayout(mRefreshListLayout);
        mRefreshListLayout.setOnRefreshListener(this);

        mSortButton = getActivity().findViewById(R.id.sort_button);
        if (mSortButton != null) {
            viewThemeUtils.material.colorMaterialTextButton(mSortButton);
        }
        mSwitchGridViewButton = getActivity().findViewById(R.id.switch_grid_view_button);
        if (mSwitchGridViewButton != null) {
            viewThemeUtils.material.colorMaterialTextButton(mSwitchGridViewButton);
        }

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

    protected void setGridViewColumns(float scaleFactor) {
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager gridLayoutManager) {
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }

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
        if (mIndexes == null || mIndexes.isEmpty()) {
            Log_OC.d(TAG,"Indexes is null or empty");
            return;
        }

        // needs to be checked; not every browse-up had a browse-down before

        int index = mIndexes.remove(mIndexes.size() - 1);
        final int firstPosition = mFirstPositions.remove(mFirstPositions.size() - 1);
        int top = mTops.remove(mTops.size() - 1);

        Log_OC.v(TAG, "Setting selection to position: " + firstPosition + "; top: "
            + top + "; index: " + index);

        scrollToPosition(firstPosition);
    }

    private void scrollToPosition(int position) {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        if (linearLayoutManager != null) {
            int visibleItemCount = linearLayoutManager.findLastCompletelyVisibleItemPosition() -
                linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            linearLayoutManager.scrollToPositionWithOffset(position, (visibleItemCount / 2) * mHeightCell);
        }
    }

    /*
     * Save index and top position
     */
    protected void saveIndexAndTopPosition(int index) {
        if (mIndexes != null) {
            mIndexes.add(index);
        }

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
            if ((activity = getActivity()) != null && activity instanceof FileDisplayActivity fileDisplayActivity) {
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
        new Handler(Looper.getMainLooper()).post(() -> {

            if (mEmptyListContainer != null && mEmptyListMessage != null) {
                mEmptyListHeadline.setText(headline);
                mEmptyListMessage.setText(message);

                if (tintIcon) {
                    if (getContext() != null) {
                        mEmptyListIcon.setImageDrawable(
                            viewThemeUtils.platform.tintPrimaryDrawable(getContext(), icon));
                    }
                } else {
                    mEmptyListIcon.setImageResource(icon);
                }

                mEmptyListIcon.setVisibility(View.VISIBLE);
                mEmptyListMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setEmptyListMessage(final SearchType searchType) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (searchType == SearchType.OFFLINE_MODE) {
                setMessageForEmptyList(R.string.offline_mode_info_title,
                                       R.string.offline_mode_info_description,
                                       R.drawable.ic_cloud_sync,
                                       true);
            } else if (searchType == SearchType.NO_SEARCH) {
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
            } else if (searchType == SearchType.REGULAR_FILTER) {
                setMessageForEmptyList(R.string.file_list_empty_headline_search,
                                       R.string.file_list_empty_search,
                                       R.drawable.ic_search_light_grey);
            } else if (searchType == SearchType.SHARED_FILTER) {
                setMessageForEmptyList(R.string.file_list_empty_shared_headline,
                                       R.string.file_list_empty_shared,
                                       R.drawable.ic_list_empty_shared);
            } else if (searchType == SearchType.GALLERY_SEARCH) {
                setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                                       R.string.file_list_empty_gallery,
                                       R.drawable.file_image);
            } else if (searchType == SearchType.LOCAL_SEARCH) {
                setMessageForEmptyList(R.string.file_list_empty_headline_server_search,
                                       R.string.file_list_empty_local_search,
                                       R.drawable.ic_search_light_grey);
            }
        });
    }

    /**
     * Set message for empty list view.
     */
    public void setEmptyListLoadingMessage() {
        new Handler(Looper.getMainLooper()).post(() -> {
            FileActivity fileActivity = FragmentExtensionsKt.getTypedActivity(this, FileActivity.class);
            if (fileActivity != null) {
                fileActivity.connectivityService.isNetworkAndServerAvailable(result -> {
                    if (!result || mEmptyListContainer == null || mEmptyListMessage == null) return;

                    mEmptyListHeadline.setText(R.string.file_list_loading);
                    mEmptyListMessage.setText("");
                    mEmptyListIcon.setVisibility(View.GONE);
                });
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
            if (mOnRefreshListener instanceof FileDisplayActivity) {
                ((FileDisplayActivity) mOnRefreshListener).onRefresh(ignoreETag);
            } else {
                mOnRefreshListener.onRefresh();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            maxColumnSize = 10;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            maxColumnSize = 5;
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
