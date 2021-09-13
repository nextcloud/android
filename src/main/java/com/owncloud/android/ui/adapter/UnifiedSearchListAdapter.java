/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding;
import com.owncloud.android.databinding.UnifiedSearchItemBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.SearchResult;
import com.owncloud.android.lib.common.SearchResultEntry;
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import kotlin.NotImplementedError;

/**
 * This Adapter populates a SectionedRecyclerView with search results by unified search
 */
public class UnifiedSearchListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private List<SearchResult> list = new ArrayList<>();

    private FileDataStorageManager storageManager;

    private Context context;
    private UnifiedSearchListInterface listInterface;
    private User user;
    private ClientFactory clientFactory;

    public UnifiedSearchListAdapter(FileDataStorageManager storageManager,
                                    UnifiedSearchListInterface listInterface,
                                    User user,
                                    ClientFactory clientFactory,
                                    Context context) {
        this.storageManager = storageManager;
        this.listInterface = listInterface;
        this.user = user;
        this.clientFactory = clientFactory;
        this.context = context;

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
    }

    @NonNull
    @Override
    public SectionedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            UnifiedSearchHeaderBinding binding = UnifiedSearchHeaderBinding.inflate(LayoutInflater.from(context),
                                                                                    parent,
                                                                                    false);

            return new UnifiedSearchHeaderViewHolder(binding, context);
        } else {
            UnifiedSearchItemBinding binding = UnifiedSearchItemBinding.inflate(LayoutInflater.from(context),
                                                                                parent,
                                                                                false);

            return new UnifiedSearchItemViewHolder(binding,
                                                   user,
                                                   clientFactory,
                                                   storageManager,
                                                   listInterface,
                                                   context);
        }
    }

    @Override
    public int getSectionCount() {
        return list.size();
    }

    @Override
    public int getItemCount(int section) {
        return list.get(section).getEntries().size();
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        UnifiedSearchHeaderViewHolder headerViewHolder = (UnifiedSearchHeaderViewHolder) holder;

        headerViewHolder.bind(list.get(section));
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        throw new NotImplementedError();
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        // TODO different binding (and also maybe diff UI) for non-file results
        UnifiedSearchItemViewHolder itemViewHolder = (UnifiedSearchItemViewHolder) holder;
        SearchResultEntry entry = list.get(section).getEntries().get(relativePosition);

        itemViewHolder.bind(entry);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull SectionedViewHolder holder) {
        if (holder instanceof UnifiedSearchItemViewHolder) {
            LoaderImageView thumbnailShimmer = ((UnifiedSearchItemViewHolder) holder).getBinding().thumbnailShimmer;
            if (thumbnailShimmer.getVisibility() == View.VISIBLE) {
                thumbnailShimmer.setImageResource(R.drawable.background);
                thumbnailShimmer.resetLoader();
            }
        }
    }

    public void setList(List<SearchResult> list) {
        this.list = list;
        notifyDataSetChanged();
    }

}
