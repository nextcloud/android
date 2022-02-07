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

import android.annotation.SuppressLint;
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
import com.nmc.android.ui.GalleryFragmentBottomSheetDialog;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeMenuUtils;

import java.util.ArrayList;
import java.util.List;

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
    private boolean isPreviewShown = false;
    private SearchRemoteOperation searchRemoteOperation;
    private AsyncTask photoSearchTask;
    private SearchEvent searchEvent;
    private boolean refresh;
    private MediaGridItemDecoration mediaGridItemDecoration;
    private OCFile remoteFilePath;
    private String remotePath = "/";
    private List<Object> mediaObject;
    private GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog;
    private List<Object> imageList;
    private List<Object> videoList;


    @Inject AppPreferences appPreferences;



    private void createOperation() {
        if (searchEvent == null) {
            searchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.GALLERY_SEARCH);
        }
        if (searchRemoteOperation == null) {
            OCCapability ocCapability = mContainerActivity.getStorageManager()
                .getCapability(accountManager.getUser().getAccountName());

            searchRemoteOperation = new SearchRemoteOperation(searchEvent.getSearchQuery(),
                                                              searchEvent.getSearchType(),
                                                              false
                                                              );
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setupToolbar();
        setHasOptionsMenu(true);

            refresh = true;

        if (galleryFragmentBottomSheetDialog == null) {
            FileActivity activity = (FileActivity) getActivity();

            galleryFragmentBottomSheetDialog = new GalleryFragmentBottomSheetDialog(activity,
                                                                                    this,
                                                                                    appPreferences);
        }
        imageList = new ArrayList<>();
        videoList = new ArrayList<>();
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
        photoSearchNoNew = false;
        isPreviewShown = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        if(isPreviewShown && !appPreferences.getHideImageClicked()) {
            refresh = true;
            photoSearchNoNew = false;
            photoSearchQueryRunning = false;
            //setAdapterEmpty();
            mediaObject.clear();
            // runGallerySearchTask();
            handleSearchEvent();
        }
        setLoading(photoSearchQueryRunning);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        createOperation();
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
        switch (item.getItemId()) {

            case R.id.action_three_dot_icon:
                if(photoSearchNoNew) {
                    galleryFragmentBottomSheetDialog.show();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
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
        //setAdapterEmpty();

        refresh = true;
        photoSearchNoNew = false;
        photoSearchQueryRunning = false;
        //setAdapterEmpty();
        mediaObject.clear();
       // runGallerySearchTask();
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
            setAdapterEmpty();
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
            runGallerySearchTask();
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
        setAdapterEmpty();
        mediaObject.clear();
        //photoSearchNoNew = false;
        // handleSearchEvent();
        runGallerySearchTask();
    }

    private void runGallerySearchTask() {
        photoSearchTask = new GallerySearchTask(getColumnsCount(),
                                                this,
                                                accountManager.getUser(),
                                                searchRemoteOperation,
                                                mContainerActivity.getStorageManager(),
                                                remoteFilePath.getRemotePath(), mediaObject,
                                                appPreferences.getHideImageClicked(),
                                                appPreferences.getHideVideoClicked())
            .execute();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setAdapterEmpty() {
        mAdapter.setData(
            new ArrayList<>(),
            SearchType.GALLERY_SEARCH,
            mContainerActivity.getStorageManager(),
            mFile,
            true);
        mAdapter.notifyDataSetChanged();
    }

    private void loadMoreWhenEndReached(@NonNull RecyclerView recyclerView, int dy) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

            // scroll down
            if (dy >= 0  && !photoSearchQueryRunning) {
                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItem = gridLayoutManager.findFirstCompletelyVisibleItemPosition();

                if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + MAX_ITEMS_PER_ROW)
                    && (totalItemCount - visibleItemCount) > 0) {
                    // Almost reached the end, continue to load new photos
                    searchAndDisplay();
                }
                else if(preferences.getHideImageClicked())
                {
                    searchAndDisplay();
                }
            }
        }
    }


    //Actions implementation of Bottom Sheet Dialog
    @Override
    public void hideVideos(boolean isHideVideosClicked) {

        if (!mediaObject.isEmpty()) {
            photoSearchQueryRunning = true;
            setAdapterEmpty();

            GallerySearchTask.setAdapterWithHideShowImage(mediaObject, mAdapter, preferences.getHideVideoClicked(),
                                                          preferences.getHideImageClicked(), imageList, videoList,
                                                          mContainerActivity.getStorageManager(),this);

        } else {
            setEmptyListMessage(SearchType.GALLERY_SEARCH);
        }
    }

    @Override
    public void hideImages(boolean isHideImagesClicked) {
        if (!mediaObject.isEmpty()) {
            setAdapterEmpty();

            GallerySearchTask.setAdapterWithHideShowImage(mediaObject, mAdapter, preferences.getHideVideoClicked(),
                                                          preferences.getHideImageClicked(), imageList, videoList,
                                                          mContainerActivity.getStorageManager(),this);

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

}
