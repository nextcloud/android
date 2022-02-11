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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.asynctasks.GallerySearchTask;
import com.owncloud.android.ui.events.ChangeMenuEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A Fragment that lists all files and folders in a given path
 */
public class GalleryFragment extends OCFileListFragment {
    private static final int MAX_ITEMS_PER_ROW = 10;
    private boolean photoSearchQueryRunning = false;
    private AsyncTask<Void, Void, GallerySearchTask.Result> photoSearchTask;
    private long startDate;
    private long endDate;
    private long daySpan = 30;
    private int limit = 300;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchFragment = true;
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
        mAdapter.setShowMetadata(false);

        currentSearchType = SearchType.GALLERY_SEARCH;

        switchToGridView();

        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
        requireActivity().invalidateOptionsMenu();

        handleSearchEvent();
    }

    @Override
    public void onRefresh() {
        super.onRefresh();

        handleSearchEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        setLoading(photoSearchQueryRunning);
    }

    @Override
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        super.onMessageEvent(changeMenuEvent);
    }

    private void handleSearchEvent() {
        prepareCurrentSearch(searchEvent);
        setEmptyListLoadingMessage();

        // always show first stored items
        mAdapter.showAllGalleryItems(mContainerActivity.getStorageManager());

        setFabVisible(false);

        searchAndDisplay();
    }

    private void searchAndDisplay() {
        // first: always search from now to -30 days
        if (!photoSearchQueryRunning) {
            photoSearchQueryRunning = true;

            startDate = (System.currentTimeMillis() / 1000) - 30 * 24 * 60 * 60;
            endDate = System.currentTimeMillis() / 1000;

            photoSearchTask = new GallerySearchTask(this,
                                                    accountManager.getUser(),
                                                    mContainerActivity.getStorageManager(),
                                                    startDate,
                                                    endDate,
                                                    limit)
                .execute();
        }
    }

    public void searchCompleted(boolean emptySearch, long lastTimeStamp) {
        photoSearchQueryRunning = false;
        mAdapter.notifyDataSetChanged();

        if (emptySearch && getAdapter().getItemCount() > 0) {
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
                    && (totalItemCount - visibleItemCount) > 0) {
                    // Almost reached the end, continue to load new photos
                    OCFile lastFile = mAdapter.getItem(lastVisibleItem - 1);

                    daySpan = 30;
                    endDate = lastFile.getModificationTimestamp() / 1000;
                    startDate = endDate - (daySpan * 24 * 60 * 60);

                    photoSearchQueryRunning = true;
                    photoSearchTask = new GallerySearchTask(this,
                                                            accountManager.getUser(),
                                                            mContainerActivity.getStorageManager(),
                                                            startDate,
                                                            endDate,
                                                            limit)
                        .execute();
                }
            }
        }
    }
}
