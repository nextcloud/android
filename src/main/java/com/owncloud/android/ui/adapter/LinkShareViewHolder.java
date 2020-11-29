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
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.ThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class LinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareLinkShareItemBinding binding;
    private Context context;

    public LinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public LinkShareViewHolder(FileDetailsShareLinkShareItemBinding binding, Context context) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
    }

    public void bind(OCShare publicShare, ShareeListAdapterListener listener) {
        if (ShareType.EMAIL == publicShare.getShareType()) {
            binding.name.setText(publicShare.getSharedWithDisplayName());
            binding.icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                                                                      R.drawable.ic_email,
                                                                      null));
            binding.copyLink.setVisibility(View.GONE);

            binding.icon.getBackground().setColorFilter(context.getResources().getColor(R.color.nc_grey),
                                                        PorterDuff.Mode.SRC_IN);
            binding.icon.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.icon_on_nc_grey),
                                                               PorterDuff.Mode.SRC_IN);
        } else {
            if (!TextUtils.isEmpty(publicShare.getLabel())) {
                String text = String.format(context.getString(R.string.share_link_with_label), publicShare.getLabel());
                binding.name.setText(text);
            } else {
                binding.name.setText(R.string.share_link);
            }

            ThemeUtils.colorIconImageViewWithBackground(binding.icon, context);
        }

        binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
        binding.overflowMenu.setOnClickListener(v -> listener.showLinkOverflowMenu(publicShare, binding.overflowMenu));
    }
}
