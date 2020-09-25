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

import android.view.View;

import com.owncloud.android.databinding.FileDetailsSharePublicLinkAddNewItemBinding;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class NewLinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsSharePublicLinkAddNewItemBinding binding;

    public NewLinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public NewLinkShareViewHolder(FileDetailsSharePublicLinkAddNewItemBinding binding) {
        this(binding.getRoot());
        this.binding = binding;
    }

    public void bind(ShareeListAdapterListener listener) {
        binding.addNewPublicShareLink.setOnClickListener(v -> listener.createPublicShareLink());
    }
}
