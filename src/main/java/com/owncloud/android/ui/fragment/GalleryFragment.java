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

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.SearchEvent;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path. TODO refactor to get rid of direct dependency on
 * FileDisplayActivity
 */
public class GalleryFragment extends OCFileListFragment {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private boolean photoSearchQueryRunning = false;
    private boolean photoSearchNoNew = false;
    private SearchRemoteOperation searchRemoteOperation;
    private AsyncTask photoSearchTask;
    private SearchEvent searchEvent;
    private boolean refresh;

    public GalleryFragment() {
        this.refresh = false;
    }

    public GalleryFragment(boolean refresh) {
        this.refresh = refresh;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.GALLERY_SEARCH);

        searchRemoteOperation = new SearchRemoteOperation(searchEvent.getSearchQuery(),
                                                          searchEvent.getSearchType(),
                                                          false);
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

        switchToGridView();

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        handleSearchEvent();
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        refresh = true;
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

            refresh = false;
        } else {
            mAdapter.showVirtuals(VirtualFolderType.GALLERY, true, mContainerActivity.getStorageManager());
            preferences.setPhotoSearchTimestamp(System.currentTimeMillis());

            return;
        }

        setFabVisible(false);

        searchAndDisplay();
    }

    private void searchAndDisplay() {
        if (!photoSearchQueryRunning && !photoSearchNoNew) {
            photoSearchTask = new GallerySearchTask(getColumnsCount(),
                                                    this,
                                                    accountManager.getUser(),
                                                    searchRemoteOperation,
                                                    mContainerActivity.getStorageManager())
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
}
