/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareInternalShareLinkBinding;
import com.owncloud.android.lib.resources.shares.OCShare;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class InternalShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareInternalShareLinkBinding binding;
    private Context context;

    public InternalShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public InternalShareViewHolder(FileDetailsShareInternalShareLinkBinding binding, Context context) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
    }

    public void bind(OCShare share, ShareeListAdapterListener listener) {
        binding.copyInternalLinkIcon
            .getBackground()
            .setColorFilter(ResourcesCompat.getColor(context.getResources(),
                                                     R.color.nc_grey,
                                                     null),
                            PorterDuff.Mode.SRC_IN);
        binding.copyInternalLinkIcon
            .getDrawable()
            .mutate()
            .setColorFilter(ResourcesCompat.getColor(context.getResources(),
                                                     R.color.icon_on_nc_grey,
                                                     null),
                            PorterDuff.Mode.SRC_IN);

        if (share.isFolder()) {
            binding.shareInternalLinkText.setText(context.getString(R.string.share_internal_link_to_folder_text));
        } else {
            binding.shareInternalLinkText.setText(context.getString(R.string.share_internal_link_to_file_text));
        }

        binding.copyInternalContainer.setOnClickListener(l -> listener.copyInternalLink());
    }
}
