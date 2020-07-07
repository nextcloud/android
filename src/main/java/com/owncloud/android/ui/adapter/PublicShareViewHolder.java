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
import android.text.TextUtils;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsSharePublicLinkItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.utils.ThemeUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class PublicShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsSharePublicLinkItemBinding binding;
    private Context context;

    public PublicShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public PublicShareViewHolder(FileDetailsSharePublicLinkItemBinding binding, Context context) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
    }

    public void bind(OCShare publicShare, ShareeListAdapterListener listener) {
        if (!TextUtils.isEmpty(publicShare.getLabel())) {
            String text = String.format(context.getString(R.string.share_link), publicShare.getLabel());
            binding.publicShareLabel.setText(text);
        }

        ThemeUtils.colorIconImageViewWithBackground(binding.copyInternalLinkIcon, context);

        binding.shareLinkCopyIcon.setOnClickListener(v -> listener.copyLink(publicShare));

        binding.overflowMenuShareLink.setOnClickListener(
            v -> listener.showLinkOverflowMenu(publicShare, binding.overflowMenuShareLink));
    }
}
