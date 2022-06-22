/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.utils.view.FastScroll;
import com.nmc.android.ui.GalleryFragmentBottomSheetDialog;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface;
import com.owncloud.android.ui.adapter.GalleryAdapter;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.fragment.util.GalleryFastScrollViewHelper;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeMenuUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path. TODO refactor to get rid of direct dependency on
 * FileDisplayActivity
 */
public class GalleryFragment extends OCFileListFragment implements GalleryFragmentBottomSheetActions {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private boolean photoSearchQueryRunning = false;
    private AsyncTask<Void, Void, GallerySearchTask.Result> photoSearchTask;
    private long startDate;
    private long endDate;
    private long daySpan = 30;
    private int limit = 300;
    private GalleryAdapter mAdapter;

    private static final int SELECT_LOCATION_REQUEST_CODE = 212;
    private OCFile remoteFile = new OCFile("/");//default root path
    private List<OCFile> mediaObject;
    private GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog;

    @Inject AppPreferences appPreferences;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchFragment = true;

        setHasOptionsMenu(true);

        if (galleryFragmentBottomSheetDialog == null) {
            FileActivity activity = (FileActivity) getActivity();

            galleryFragmentBottomSheetDialog = new GalleryFragmentBottomSheetDialog(activity,
                                                                                    this,
                                                                                    appPreferences);
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

        getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                loadMoreWhenEndReached(recyclerView, dy);
            }
        });

        enableRecyclerViewGridZooming();

        Log_OC.i(this, "onCreateView() in GalleryFragment end");
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mediaObject == null) {
            mediaObject = new ArrayList<>();
        } else {
            mediaObject.clear();
        }

        currentSearchType = SearchType.GALLERY_SEARCH;

        switchToMediaGridView();
        if (getRecyclerView().getLayoutManager() instanceof GridLayoutManager) {
            int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_item_rv_spacing);
            MediaGridItemDecoration mediaGridItemDecoration = new MediaGridItemDecoration(spacing);
            getRecyclerView().addItemDecoration(mediaGridItemDecoration);
            getRecyclerView().setPadding(spacing, spacing, spacing, spacing);
        }

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        //updating the title if user comes back to this fragment from other fragments
        if (requireActivity() instanceof FileDisplayActivity) {
            ((FileDisplayActivity) requireActivity()).updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_gallery));
        }
        handleSearchEvent();
    }

    @Override
    protected void setAdapter(Bundle args) {
        mAdapter = new GalleryAdapter(requireContext(),
                                      accountManager.getUser(),
                                      this,
                                      preferences,
                                      mContainerActivity);

        setRecyclerViewAdapter(mAdapter);
        setRecyclerViewHasFooter(false);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), getColumnsCount());
        mAdapter.setLayoutManager(layoutManager);
        getRecyclerView().setLayoutManager(layoutManager);

        FastScroll.applyFastScroll(getRecyclerView(), new GalleryFastScrollViewHelper(getRecyclerView(), mAdapter));
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        mediaObject.clear();
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
            ThemeMenuUtils.tintMenuIcon(requireContext(), menuItem,
                                        ThemeColorUtils.appBarPrimaryFontColor(requireContext()));
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        if (item.getItemId() == R.id.action_three_dot_icon && !photoSearchQueryRunning
            && galleryFragmentBottomSheetDialog != null) {
            galleryFragmentBottomSheetDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        setEmptyListLoadingMessage();
        mAdapter.clear();
        mediaObject.clear();
        searchAndDisplay();
        setLoading(photoSearchQueryRunning);
    }

    private void runGallerySearchTask() {
        photoSearchTask = new GallerySearchTask(this,
                                                accountManager.getUser(),
                                                mContainerActivity.getStorageManager(),
                                                startDate,
                                                endDate,
                                                limit)
            .execute();
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

                if ((totalItemCount - visibleItemCount) <= (lastVisibleItem + MAX_ITEMS_PER_ROW)
                    && (totalItemCount - visibleItemCount) > 0 && lastVisibleItem > 0) {
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
    public void hideVideos(boolean isHideVideosClicked) {
        if (!mediaObject.isEmpty()) {
            mAdapter.setMediaFilter(mediaObject,
                                                 preferences.getHideVideoClicked(),
                                                 preferences.getHideImageClicked(),
                                                 this);

        } else {
            setEmptyListMessage(SearchType.GALLERY_SEARCH);
        }
    }

    @Override
    public void hideImages(boolean isHideImagesClicked) {
        if (!mediaObject.isEmpty()) {
            mAdapter.setMediaFilter(mediaObject,
                                                 preferences.getHideVideoClicked(),
                                                 preferences.getHideImageClicked(),
                                                 this);

        } else {
            setEmptyListMessage(SearchType.GALLERY_SEARCH);
        }
    }

    @Override
    public void selectMediaFolder() {
        Intent action = new Intent(requireActivity(), FolderPickerActivity.class);
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION);
        startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE);
    }

    @Override
    public void sortByModifiedDate() {
       /* mAdapter.setData(
            new ArrayList<>(),
            SearchType.GALLERY_SEARCH,
            mContainerActivity.getStorageManager(),
            mFile,
            true);
        mAdapter.notifyDataSetChanged();

        final int multiplier = 1;
        Collections.sort((List<RemoteFile>)(List)mediaObject, (o1, o2) -> {
            return multiplier * Long.compare(o1.getModifiedTimestamp(),o2.getModifiedTimestamp());
        });
       Toast.makeText(getActivity(), "Sort By Modified Date Clicked", Toast.LENGTH_SHORT).show();

        System.out.println(mediaObject);

        mAdapter.setData(mediaObject,
                         SearchType.GALLERY_SEARCH,
                         mContainerActivity.getStorageManager(),
                         null,
                         true);

        mAdapter.notifyDataSetChanged(); */
    }

    @Override
    public void sortByCreatedDate() {

    }

    @Override
    public void sortByUploadDate() {

    }

    public void showAllGalleryItems() {
        mAdapter.showAllGalleryItems(mContainerActivity.getStorageManager(),
                                     remoteFile.getRemotePath(),
                                     mediaObject,
                                     preferences.getHideVideoClicked(),
                                     preferences.getHideImageClicked(),
                                     this);

    }
}
