/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2019 Andy Scherzinger
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.databinding.StoragePathItemBinding;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StoragePathAdapter extends RecyclerView.Adapter<StoragePathAdapter.StoragePathViewHolder> {
    private List<StoragePathItem> pathList;
    private StoragePathAdapterListener storagePathAdapterListener;

    public StoragePathAdapter(List<StoragePathItem> pathList, StoragePathAdapterListener storagePathAdapterListener) {
        this.pathList = pathList;
        this.storagePathAdapterListener = storagePathAdapterListener;
    }

    @NonNull
    @Override
    public StoragePathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StoragePathAdapter.StoragePathViewHolder(
            StoragePathItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull StoragePathViewHolder holder, int position) {
        if (pathList != null && pathList.size() > position) {
            StoragePathItem storagePathItem = pathList.get(position);

            holder.binding.icon.setImageResource(storagePathItem.getIcon());
            holder.binding.name.setText(storagePathItem.getName());
        }
    }

    @Override
    public int getItemCount() {
        return pathList.size();
    }

    public interface StoragePathAdapterListener {
        /**
         * sets the chosen path.
         *
         * @param path chosen path
         */
        void chosenPath(String path);
    }

    class StoragePathViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        StoragePathItemBinding binding;

        public StoragePathViewHolder(StoragePathItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.binding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            storagePathAdapterListener.chosenPath(pathList.get(getAdapterPosition()).getPath());
        }
    }
}
