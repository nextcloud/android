/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2021 TSI-mc
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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeAvatarUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class ShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareShareItemBinding binding;
    private float avatarRadiusDimension;
    private User user;
    private Context context;

    public ShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public ShareViewHolder(FileDetailsShareShareItemBinding binding, User user, Context context) {
        this(binding.getRoot());
        this.binding = binding;
        this.user = user;
        this.context = context;
    }

    public void bind(OCShare share,
                     ShareeListAdapterListener listener,
                     DisplayUtils.AvatarGenerationListener avatarListener,
                     String userId,
                     float avatarRadiusDimension) {
        this.avatarRadiusDimension = avatarRadiusDimension;
        String name = share.getSharedWithDisplayName();
        binding.icon.setTag(null);

        switch (share.getShareType()) {
            case GROUP:
                name = context.getString(R.string.share_group_clarification, name);
                ThemeAvatarUtils.createAvatar(share.getShareType(), binding.icon, context);
                break;
            case ROOM:
                name = context.getString(R.string.share_room_clarification, name);
                ThemeAvatarUtils.createAvatar(share.getShareType(), binding.icon, context);
                break;
            case CIRCLE:
                ThemeAvatarUtils.createAvatar(share.getShareType(), binding.icon, context);
                break;
            case FEDERATED:
                name = context.getString(R.string.share_remote_clarification, name);
                setImage(binding.icon, share.getSharedWithDisplayName(), R.drawable.ic_user);
                break;
            case USER:
                binding.icon.setTag(share.getShareWith());
                float avatarRadius = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
                DisplayUtils.setAvatar(user,
                                       share.getShareWith(),
                                       share.getSharedWithDisplayName(),
                                       avatarListener,
                                       avatarRadius,
                                       context.getResources(),
                                       binding.icon,
                                       context);
            default:
                setImage(binding.icon, name, R.drawable.ic_user);
                break;
        }

        binding.name.setText(name);

        if (share.getShareWith().equalsIgnoreCase(userId) || share.getUserId().equalsIgnoreCase(userId)) {
            binding.overflowMenu.setVisibility(View.VISIBLE);

            String permissionName = SharingMenuHelper.getPermissionName(context, share);
            setPermissionName(permissionName);

            // bind listener to edit privileges
            binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(share));
            binding.shareNameLayout.setOnClickListener(v -> listener.showPermissionsDialog(share));
        } else {
            binding.overflowMenu.setVisibility(View.GONE);
        }
    }

    private void setPermissionName(String permissionName) {
        if (!TextUtils.isEmpty(permissionName)) {
            binding.permissionName.setText(permissionName);
            binding.permissionName.setVisibility(View.VISIBLE);
        } else {
            binding.permissionName.setVisibility(View.GONE);
        }
    }

    private void setImage(ImageView avatar, String name, @DrawableRes int fallback) {
        try {
            avatar.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
        } catch (StringIndexOutOfBoundsException e) {
            avatar.setImageResource(fallback);
        }
    }
}
