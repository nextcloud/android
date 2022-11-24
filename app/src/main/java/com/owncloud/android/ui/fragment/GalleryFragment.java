/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 * Copyright (C) 2022 TSI-mc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface;
import com.owncloud.android.ui.adapter.GalleryAdapter;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.events.ChangeMenuEvent;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path
 */
public class GalleryFragment extends OCFileListFragment implements GalleryFragmentBottomSheetActions {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private static final String FRAGMENT_TAG_BOTTOM_SHEET = "data";

    private boolean photoSearchQueryRunning = false;
    private AsyncTask<Void, Void, GallerySearchTask.Result> photoSearchTask;
    private long startDate;
    private long endDate;
    private long daySpan = 30;
    private int limit = 300;
    private GalleryAdapter mAdapter;

    private static final int SELECT_LOCATION_REQUEST_CODE = 212;
    private OCFile remoteFile;
    private GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog;

    @Inject FileDataStorageManager fileDataStorageManager;
    private final int maxColumnSizeLandscape = 5;
    private final int maxColumnSizePortrait = 2;
    private int columnSize;

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

        remoteFile = fileDataStorageManager.getDefaultRootPath();

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


        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        mAdapter.setLayoutManager(layoutManager);
        getRecyclerView().setLayoutManager(layoutManager);
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
        setLoading(photoSearchQueryRunning);
        final FragmentActivity activity = getActivity();
        if (activity instanceof FileDisplayActivity) {
            FileDisplayActivity fileDisplayActivity = ((FileDisplayActivity) activity);
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
        // first: always search from now to -30 days
        if (!photoSearchQueryRunning) {
            photoSearchQueryRunning = true;

            startDate = (System.currentTimeMillis() / 1000) - 30 * 24 * 60 * 60;
            endDate = System.currentTimeMillis() / 1000;

            runGallerySearchTask();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void searchCompleted(boolean emptySearch, long lastTimeStamp) {
        photoSearchQueryRunning = false;
        mAdapter.notifyDataSetChanged();

        if (mAdapter.isEmpty()) {
            setEmptyListMessage(SearchType.GALLERY_SEARCH);
        }

        if (emptySearch && mAdapter.getItemCount() > 0) {
            Log_OC.d(this, "End gallery search");
            return;
        }
        if (daySpan == 30) {
            daySpan = 90;
        } else if (daySpan == 90) {
            daySpan = 180;
        } else if (daySpan == 180) {
            daySpan = 999;
        } else if (daySpan == 999 && limit > 0) {
            limit = -1; // no limit
        } else {
            Log_OC.d(this, "End gallery search");
            return;
        }

        if (lastTimeStamp > -1) {
            endDate = lastTimeStamp;
        }

        startDate = endDate - (daySpan * 24 * 60 * 60);

        runGallerySearchTask();
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
        if (item.getItemId() == R.id.action_three_dot_icon && !photoSearchQueryRunning
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
        if (requestCode == SELECT_LOCATION_REQUEST_CODE && data != null) {
            OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
            if (chosenFolder != null) {
                remoteFile = chosenFolder;
                searchAndDisplayAfterChangingFolder();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void searchAndDisplayAfterChangingFolder() {
        mAdapter.clear();
        runGallerySearchTask();
    }

    private void runGallerySearchTask() {
        if (mContainerActivity != null) {
            photoSearchTask = new GallerySearchTask(this,
                                                    accountManager.getUser(),
                                                    mContainerActivity.getStorageManager(),
                                                    startDate,
                                                    endDate,
                                                    limit)
                .execute();
        }
    }

    @Override
    public boolean isLoading() {
        return photoSearchQueryRunning;
    }

    private void loadMoreWhenEndReached(@NonNull RecyclerView recyclerView, int dy) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

            // scroll down
            if (dy > 0 && !photoSearchQueryRunning) {
                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int lastVisibleItem = gridLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisibleItem == RecyclerView.NO_POSITION) {
                    return;
                }

                if ((totalItemCount - visibleItemCount) <= (lastVisibleItem + MAX_ITEMS_PER_ROW)
                    && (totalItemCount - visibleItemCount) > 0) {
                    // Almost reached the end, continue to load new photos
                    OCFile lastFile = mAdapter.getItem(lastVisibleItem - 1);

                    if (lastFile == null) {
                        return;
                    }

                    daySpan = 30;
                    endDate = lastFile.getModificationTimestamp() / 1000;
                    startDate = endDate - (daySpan * 24 * 60 * 60);

                    photoSearchQueryRunning = true;
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
        mAdapter.showAllGalleryItems(remoteFile.getRemotePath(),
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
