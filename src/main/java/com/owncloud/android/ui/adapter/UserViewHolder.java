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
import android.view.View;
import android.widget.ImageView;

import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareUserItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.TextDrawable;

import java.security.NoSuchAlgorithmException;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class UserViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareUserItemBinding binding;
    private float avatarRadiusDimension;
    private Context context;

    public UserViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public UserViewHolder(FileDetailsShareUserItemBinding binding, Context context) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
    }

    public void bind(OCShare share,
                     ShareeListAdapterListener listener,
                     String userId,
                     float avatarRadiusDimension) {
        this.avatarRadiusDimension = avatarRadiusDimension;
        String name = share.getSharedWithDisplayName();

        switch (share.getShareType()) {
            case GROUP:
                name = context.getString(R.string.share_group_clarification, name);
                setImage(binding.avatar, name, R.drawable.ic_group);
                break;
            case EMAIL:
                name = context.getString(R.string.share_email_clarification, name);
                setImage(binding.avatar, name, R.drawable.ic_email);
                break;
            case ROOM:
                name = context.getString(R.string.share_room_clarification, name);
                setImage(binding.avatar, name, R.drawable.ic_chat_bubble);
                break;
            case CIRCLE:
                binding.avatar.setImageResource(R.drawable.ic_circles);
                break;
            default:
                setImage(binding.avatar, name, R.drawable.ic_user);
                break;
        }

        binding.name.setText(name);

        if (share.getShareWith().equalsIgnoreCase(userId) || share.getUserId().equalsIgnoreCase(userId)) {
            binding.editShareButton.setVisibility(View.VISIBLE);

            // bind listener to edit privileges
            binding.editShareButton.setOnClickListener(v -> listener.showUserOverflowMenu(share,
                                                                                          binding.editShareButton));
        } else {
            binding.editShareButton.setVisibility(View.GONE);
        }
    }

    private void setImage(ImageView avatar, String name, @DrawableRes int fallback) {
        try {
            avatar.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
        } catch (NoSuchAlgorithmException e) {
            avatar.setImageResource(fallback);
        }
    }
}
