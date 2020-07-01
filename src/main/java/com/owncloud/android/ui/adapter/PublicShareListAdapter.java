/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.owncloud.android.databinding.FileDetailsSharePublicLinkItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PublicShareListAdapter extends RecyclerView.Adapter<PublicShareViewHolder> {
    private Context context;
    private List<OCShare> shares;
    private PublicShareInterface listener;

    public PublicShareListAdapter(Context context, List<OCShare> shares, PublicShareInterface listener) {
        this.context = context;
        this.shares = shares;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PublicShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FileDetailsSharePublicLinkItemBinding binding =
            FileDetailsSharePublicLinkItemBinding.inflate(LayoutInflater.from(context), parent, false);

        return new PublicShareViewHolder(binding, context);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicShareViewHolder holder, int position) {
        OCShare share = shares.get(position);

        holder.bind(share, listener);
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }
}
