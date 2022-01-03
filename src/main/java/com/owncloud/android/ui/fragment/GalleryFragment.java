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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.ui.GalleryFragmentBottomSheetDialog;
import com.nmc.android.ui.ScanActivity;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.decoration.SimpleListItemDividerDecoration;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.SearchEvent;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path. TODO refactor to get rid of direct dependency on
 * FileDisplayActivity
 */
public class GalleryFragment extends OCFileListFragment implements GalleryFragmentBottomSheetActions {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private static final int SELECT_LOCATION_REQUEST_CODE = 212;
    private boolean photoSearchQueryRunning = false;
    private boolean photoSearchNoNew = false;
    private SearchRemoteOperation searchRemoteOperation;
    private AsyncTask photoSearchTask;
    private SearchEvent searchEvent;
    private boolean refresh;
    private MediaGridItemDecoration mediaGridItemDecoration;
    private OCFile remoteFilePath;
    private String remotePath = "/";

    @Inject AppPreferences appPreferences;

    public GalleryFragment() {
        this.refresh = false;
    }

    public GalleryFragment(boolean refresh) {
        this.refresh = refresh;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setupToolbar();
        setHasOptionsMenu(true);


        searchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.GALLERY_SEARCH);

        searchRemoteOperation = new SearchRemoteOperation(searchEvent.getSearchQuery(),
                                                          searchEvent.getSearchType(),
                                                          false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //  initViews();
        remotePath = setDefaultRemotePath();
    }

    private String setDefaultRemotePath() {
        if (remoteFilePath == null) {
            setRemoteFilePath(remotePath);
        }
        return remotePath;
    }

    private void setRemoteFilePath(String remotePath) {
        remoteFilePath = new OCFile(remotePath);
        remoteFilePath.setFolder();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_gallery_three_dots, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_three_dot_icon:
                FileActivity activity = (FileActivity) getActivity();
                GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog = new GalleryFragmentBottomSheetDialog(activity,
                                                                                                                         this,
                                                                                                                         deviceInfo,
                                                                                                                         accountManager.getUser(),
                                                                                                                         getCurrentFile(), preferences);

                galleryFragmentBottomSheetDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        currentSearchType = SearchType.GALLERY_SEARCH;

        switchToMediaGridView();
        mAdapter.setMediaGallery(true);
        if (getRecyclerView().getLayoutManager() instanceof GridLayoutManager) {
            int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_item_rv_spacing);
            mediaGridItemDecoration = new MediaGridItemDecoration(spacing);
            getRecyclerView().addItemDecoration(mediaGridItemDecoration);
            getRecyclerView().setPadding(spacing, spacing, spacing, spacing);
        }

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        handleSearchEvent();
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        refresh = true;
        photoSearchNoNew = false;
        handleSearchEvent();
    }

    @Override
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        super.onMessageEvent(changeMenuEvent);
    }

    private void handleSearchEvent() {
        prepareCurrentSearch(searchEvent);
        searchFragment = true;
        setEmptyListLoadingMessage();

        if (refresh || preferences.getPhotoSearchTimestamp() == 0 ||
            System.currentTimeMillis() - preferences.getPhotoSearchTimestamp() >= 30 * 1000) {
            mAdapter.setData(
                new ArrayList<>(),
                SearchType.GALLERY_SEARCH,
                mContainerActivity.getStorageManager(),
                mFile,
                true);
            mAdapter.notifyDataSetChanged();
            refresh = false;
        } else {
            // mAdapter.showVirtuals(VirtualFolderType.GALLERY, true, mContainerActivity.getStorageManager());
            preferences.setPhotoSearchTimestamp(System.currentTimeMillis());

            return;
        }
        mAdapter.showVirtuals(VirtualFolderType.GALLERY, true, mContainerActivity.getStorageManager());


        setFabVisible(false);

        searchAndDisplay();
    }

    private void searchAndDisplay() {
        if (!photoSearchQueryRunning && !photoSearchNoNew) {
            photoSearchTask = new GallerySearchTask(getColumnsCount(),
                                                    this,
                                                    accountManager.getUser(),
                                                    searchRemoteOperation,
                                                    mContainerActivity.getStorageManager(),
                                                    remoteFilePath.getRemotePath())
                .execute();
        }
    }

    public void setPhotoSearchQueryRunning(boolean bool) {
        photoSearchQueryRunning = bool;
    }

    public void setSearchDidNotFindNewPhotos(boolean noNewPhotos) {
        photoSearchNoNew = noNewPhotos;
    }

    @Override
    public boolean isLoading() {
        return !photoSearchNoNew;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_LOCATION_REQUEST_CODE) {
            if (data != null) {
                OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
                if (chosenFolder != null) {
                    remoteFilePath = chosenFolder;
                    searchAndDisplayAfterChangingFolder();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void searchAndDisplayAfterChangingFolder() {
        mAdapter.setData(
            new ArrayList<>(),
            SearchType.GALLERY_SEARCH,
            mContainerActivity.getStorageManager(),
            mFile,
            true);
        mAdapter.notifyDataSetChanged();
        //photoSearchNoNew = false;
        // handleSearchEvent();

        photoSearchTask = new GallerySearchTask(getColumnsCount(),
                                                this,
                                                accountManager.getUser(),
                                                searchRemoteOperation,
                                                mContainerActivity.getStorageManager(),
                                                remoteFilePath.getRemotePath())
            .execute();

    }

    private void loadMoreWhenEndReached(@NonNull RecyclerView recyclerView, int dy) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

            // scroll down
            if (dy > 0 && !photoSearchQueryRunning) {
                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItem = gridLayoutManager.findFirstCompletelyVisibleItemPosition();

                if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + MAX_ITEMS_PER_ROW)
                    && (totalItemCount - visibleItemCount) > 0) {
                    // Almost reached the end, continue to load new photos
                    searchAndDisplay();
                }
            }
        }
    }

    //Actions implementation of Bottom Sheet Dialog


    @Override
    public void hideImages() {
        Toast.makeText(getActivity(), "Hide Images Clicked", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void hideVideos() {
        Toast.makeText(getActivity(), "Hide Video Clicked", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void selectMediaFolder() {
        Intent action = new Intent(requireActivity(), FolderPickerActivity.class);
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION);
        startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE);
    }

    @Override
    public void sortByModifiedDate() {
        Toast.makeText(getActivity(), "Sort By Modified Date Clicked", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void sortByCreatedDate() {
        Toast.makeText(getActivity(), "Sort By Created Date Clicked", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void sortByUploadDate() {
        Toast.makeText(getActivity(), "Sort By Upload Date Clicked", Toast.LENGTH_SHORT).show();

    }
}
