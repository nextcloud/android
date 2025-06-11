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
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.utils.extensions.ImageViewExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding;
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class ShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareShareItemBinding binding;
    private float avatarRadiusDimension;
    private User user;
    private Context context;
    private ViewThemeUtils viewThemeUtils;
    private boolean encrypted;

    public ShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public ShareViewHolder(FileDetailsShareShareItemBinding binding,
                           User user,
                           Context context,
                           final ViewThemeUtils viewThemeUtils,
                           boolean encrypted) {
        this(binding.getRoot());
        this.binding = binding;
        this.user = user;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
    }

    public void bind(OCShare share,
                     ShareeListAdapterListener listener,
                     DisplayUtils.AvatarGenerationListener avatarListener,
                     String userId,
                     float avatarRadiusDimension) {
        this.avatarRadiusDimension = avatarRadiusDimension;
        String name = share.getSharedWithDisplayName();
        
        if ("".equals(name) && !"".equals(share.getShareWith())) {
            name = share.getShareWith();
        }
        
        binding.icon.setTag(null);

        if (share.getShareType() != null) {
            switch (share.getShareType()) {
                case GROUP:
                    name = context.getString(R.string.share_group_clarification, name);
                    viewThemeUtils.files.createAvatar(share.getShareType(), binding.icon, context);
                    break;
                case ROOM:
                    name = context.getString(R.string.share_room_clarification, name);
                    viewThemeUtils.files.createAvatar(share.getShareType(), binding.icon, context);
                    break;
                case CIRCLE:
                    viewThemeUtils.files.createAvatar(share.getShareType(), binding.icon, context);
                    break;
                case FEDERATED:
                    name = context.getString(R.string.share_remote_clarification, name);
                    setImage(binding.icon, share.getSharedWithDisplayName());
                    break;
                case USER:
                    binding.icon.setTag(share.getShareWith());
                    float avatarRadius = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);

                    if (share.getShareWith() != null) {
                        DisplayUtils.setAvatar(user,
                                               share.getShareWith(),
                                               share.getSharedWithDisplayName(),
                                               avatarListener,
                                               avatarRadius,
                                               context.getResources(),
                                               binding.icon,
                                               context);
                    }

                    binding.icon.setOnClickListener(v -> listener.showProfileBottomSheet(user, share.getShareWith()));
                default:
                    setImage(binding.icon, name);
                    break;
            }
        }

        binding.name.setText(name);

        if (share.getShareWith() != null && share.getShareWith().equalsIgnoreCase(userId) ||
            share.getUserId() != null && share.getUserId().equalsIgnoreCase(userId)) {
            binding.overflowMenu.setVisibility(View.VISIBLE);

            QuickPermissionType quickPermissionType = SharePermissionManager.INSTANCE.getSelectedType(share, encrypted);
            setPermissionName(quickPermissionType.getText(context));

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

    private void setImage(ImageView avatar, String name) {
        if (TextUtils.isEmpty(name)) {
            setUserImage(avatar);
            return;
        }

        try {
            avatar.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
        } catch (StringIndexOutOfBoundsException e) {
            setUserImage(avatar);
        }
    }

    private void setUserImage(ImageView avatar) {
        ImageViewExtensionsKt.makeRoundedWithIcon(avatar, context, R.drawable.ic_user);
        viewThemeUtils.platform.colorImageViewBackgroundAndIcon(avatar);
    }
}
