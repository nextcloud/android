/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface;
import com.owncloud.android.ui.adapter.GalleryAdapter;
import com.owncloud.android.ui.adapter.OCFileListDelegate;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.events.ChangeMenuEvent;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path
 */
public class GalleryFragment extends OCFileListFragment implements GalleryFragmentBottomSheetActions {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private static final String FRAGMENT_TAG_BOTTOM_SHEET = "data";

    private static Integer lastMediaItemPosition = null;
    public static final String REFRESH_SEARCH_EVENT_RECEIVER = "refreshSearchEventReceiver";

    private boolean photoSearchQueryRunning = false;
    private AsyncTask<Void, Void, GallerySearchTask.Result> photoSearchTask;
    private long endDate;
    private int limit = 150;
    private GalleryAdapter mAdapter;

    private static final int SELECT_LOCATION_REQUEST_CODE = 212;
    private GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog;

    @Inject FileDataStorageManager fileDataStorageManager;
    private final static int maxColumnSizeLandscape = 5;
    private final static int maxColumnSizePortrait = 2;
    private int columnSize;

    protected void setPhotoSearchQueryRunning(boolean value) {
        this.photoSearchQueryRunning = value;
        this.setLoading(value); // link the photoSearchQueryRunning variable with UI progress loading
    }

    public boolean isPhotoSearchQueryRunning() {
        return this.photoSearchQueryRunning;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchFragment = true;

        setHasOptionsMenu(true);

        if (galleryFragmentBottomSheetDialog == null) {
            galleryFragmentBottomSheetDialog = new GalleryFragmentBottomSheetDialog(this);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnSize = maxColumnSizeLandscape;
        } else {
            columnSize = maxColumnSizePortrait;
        }

        registerRefreshSearchEventReceiver();
    }

    private void registerRefreshSearchEventReceiver() {
        IntentFilter filter = new IntentFilter(REFRESH_SEARCH_EVENT_RECEIVER);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshSearchEventReceiver, filter);
    }

    private final BroadcastReceiver refreshSearchEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() instanceof FileDisplayActivity fileDisplayActivity) {
                fileDisplayActivity.startPhotoSearch(R.id.nav_gallery);
            }
        }
    };

    public static void setLastMediaItemPosition(Integer position) {
        lastMediaItemPosition = position;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshSearchEventReceiver);
        setLastMediaItemPosition(null);
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (photoSearchTask != null) {
            photoSearchTask.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                loadMoreWhenEndReached(recyclerView, dy);
            }
        });

        Log_OC.i(this, "onCreateView() in GalleryFragment end");
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        currentSearchType = SearchType.GALLERY_SEARCH;

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        updateSubtitle(galleryFragmentBottomSheetDialog.getCurrMediaState());

        handleSearchEvent();
    }

    @Override
    protected void setAdapter(Bundle args) {
        mAdapter = new GalleryAdapter(requireContext(),
                                      accountManager.getUser(),
                                      this,
                                      preferences,
                                      mContainerActivity,
                                      viewThemeUtils,
                                      columnSize,
                                      ThumbnailsCacheManager.getThumbnailDimension());

        setRecyclerViewAdapter(mAdapter);

        //update the footer as there is no footer shown in media view
        if (getRecyclerView() instanceof EmptyRecyclerView) {
            ((EmptyRecyclerView) getRecyclerView()).setHasFooter(false);
        }

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        mAdapter.setLayoutManager(layoutManager);
        getRecyclerView().setLayoutManager(layoutManager);

        if (lastMediaItemPosition != null) {
            layoutManager.scrollToPosition(lastMediaItemPosition);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            columnSize = maxColumnSizeLandscape;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            columnSize = maxColumnSizePortrait;
        }
        mAdapter.changeColumn(columnSize);
        showAllGalleryItems();
    }

    @Override
    public int getColumnsCount() {
        return columnSize;
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        handleSearchEvent();
    }

    @Override
    public CommonOCFileListAdapterInterface getCommonAdapter() {
        return mAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();

        setLoading(this.isPhotoSearchQueryRunning());
        if (getActivity() instanceof FileDisplayActivity fileDisplayActivity) {
            fileDisplayActivity.updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_gallery));
            fileDisplayActivity.setMainFabVisible(false);
        }
    }

    @Override
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        super.onMessageEvent(changeMenuEvent);
    }

    private void handleSearchEvent() {
        prepareCurrentSearch(searchEvent);
        setEmptyListLoadingMessage();

        // always show first stored items
        showAllGalleryItems();

        setFabVisible(false);

        searchAndDisplay();
    }

    private void searchAndDisplay() {
        if (!this.isPhotoSearchQueryRunning() && this.endDate <= 0) {
            // fix an issue when the method is called after loading the gallery and pressing play on a movie (--> endDate <= 0)
            // to avoid reloading the gallery, check if endDate has already a value which is not -1 or 0 (which generally means some kind of reset/init)
            endDate = System.currentTimeMillis() / 1000;
            this.setPhotoSearchQueryRunning(true);
            runGallerySearchTask();
        }
    }

    public void searchCompleted(boolean emptySearch, long lastTimeStamp) {
        this.setPhotoSearchQueryRunning(false);

        if (lastTimeStamp > -1) {
            endDate = lastTimeStamp;
        }

        if (mAdapter.isEmpty()) {
            setEmptyListMessage(SearchType.GALLERY_SEARCH);
        }

        if (!emptySearch) {
            this.showAllGalleryItems();
        }

        Log_OC.d(this, "End gallery search");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_gallery_three_dots, menu);

        MenuItem menuItem = menu.findItem(R.id.action_three_dot_icon);

        if (menuItem != null) {
            viewThemeUtils.platform.colorMenuItemText(requireContext(), menuItem);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        if (item.getItemId() == R.id.action_three_dot_icon && !this.isPhotoSearchQueryRunning()
            && galleryFragmentBottomSheetDialog != null) {
            showBottomSheet();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBottomSheet() {
        if (!galleryFragmentBottomSheetDialog.isVisible()) {
            galleryFragmentBottomSheetDialog.show(getChildFragmentManager(), FRAGMENT_TAG_BOTTOM_SHEET);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_LOCATION_REQUEST_CODE && data != null && FolderPickerActivity.EXTRA_FOLDER != null) {
            OCFile chosenFolder = IntentExtensionsKt.getParcelableArgument(data, FolderPickerActivity.EXTRA_FOLDER, OCFile.class);

            if (chosenFolder != null) {
                preferences.setLastSelectedMediaFolder(chosenFolder.getRemotePath());
                searchAndDisplayAfterChangingFolder();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void searchAndDisplayAfterChangingFolder() {
        //TODO: Fix folder change, it seems it doesn't work at all
        this.endDate = System.currentTimeMillis() / 1000;
        this.setPhotoSearchQueryRunning(true);
        runGallerySearchTask();
    }

    private void runGallerySearchTask() {
        if (mContainerActivity != null) {
            photoSearchTask = new GallerySearchTask(this,
                                                    accountManager.getUser(),
                                                    mContainerActivity.getStorageManager(),
                                                    endDate,
                                                    limit)
                .execute();
        }
    }

    private void loadMoreWhenEndReached(@NonNull RecyclerView recyclerView, int dy) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager gridLayoutManager) {

            // scroll down
            if (dy > 0 && !this.isPhotoSearchQueryRunning()) {
                int totalItemCount = gridLayoutManager.getItemCount();
                int lastVisibleItem = gridLayoutManager.findLastCompletelyVisibleItemPosition();
                int visibleItemCount = gridLayoutManager.getChildCount();

                if (lastVisibleItem == RecyclerView.NO_POSITION) {
                    return;
                }

                OCFile lastFile = mAdapter.getItem(lastVisibleItem - 1);
                if (lastFile == null) {
                    return;
                }
                long lastItemTimestamp = lastFile.getModificationTimestamp() / 1000;

                // if we have already older media in the gallery then retrieve file in chronological order to fill the gap
                if (lastItemTimestamp < this.endDate) {

                    if (BuildConfig.DEBUG) {
                        Log_OC.d(this, "Gallery swipe: retrieve items to check the chronology");
                    }

                    this.setPhotoSearchQueryRunning(true);
                    runGallerySearchTask();
                } else if ((totalItemCount - visibleItemCount) <= (lastVisibleItem + MAX_ITEMS_PER_ROW) //no more files in the gallery, retrieve the next ones
                    && (totalItemCount - visibleItemCount) > 0) {

                    if (BuildConfig.DEBUG) {
                        Log_OC.d(this, "Gallery swipe: retrieve items because end of gallery display");
                    }

                    // Almost reached the end, continue to load new photos
                    endDate = lastItemTimestamp;
                    this.setPhotoSearchQueryRunning(true);
                    runGallerySearchTask();
                }
            }
        }
    }

    @Override
    public void updateMediaContent(GalleryFragmentBottomSheetDialog.MediaState mediaState) {
        showAllGalleryItems();
    }

    @Override
    public void selectMediaFolder() {
        Intent action = new Intent(requireActivity(), FolderPickerActivity.class);
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION);
        startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE);
    }

    public void showAllGalleryItems() {
        mAdapter.showAllGalleryItems(preferences.getLastSelectedMediaFolder(),
                                     galleryFragmentBottomSheetDialog.getCurrMediaState(),
                                     this);

        updateSubtitle(galleryFragmentBottomSheetDialog.getCurrMediaState());
    }

    private void updateSubtitle(GalleryFragmentBottomSheetDialog.MediaState mediaState) {
        requireActivity().runOnUiThread(() -> {
            String subTitle = requireContext().getResources().getString(R.string.subtitle_photos_videos);
            if (mediaState == GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_PHOTOS_ONLY) {
                subTitle = requireContext().getResources().getString(R.string.subtitle_photos_only);
            } else if (mediaState == GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_VIDEOS_ONLY) {
                subTitle = requireContext().getResources().getString(R.string.subtitle_videos_only);
            }
            if (requireActivity() instanceof ToolbarActivity) {
                ((ToolbarActivity) requireActivity()).updateToolbarSubtitle(subTitle);
            }
        });
    }

    @Override
    protected void setGridViewColumns(float scaleFactor) {
        // do nothing
    }
}
