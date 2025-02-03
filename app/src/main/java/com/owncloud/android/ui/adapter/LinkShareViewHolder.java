/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 *
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2021 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.View;

import com.nextcloud.android.lib.resources.files.FileDownloadLimit;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class LinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareLinkShareItemBinding binding;
    private Context context;
    private ViewThemeUtils viewThemeUtils;

    public LinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public LinkShareViewHolder(FileDetailsShareLinkShareItemBinding binding,
                               Context context,
                               final ViewThemeUtils viewThemeUtils) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
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
                if (SharingMenuHelper.isSecureFileDrop(publicShare)) {
                    binding.name.setText(context.getResources().getString(R.string.share_permission_secure_file_drop));
                } else {
                    binding.name.setText(R.string.share_link);
                }
            }

            viewThemeUtils.platform.colorImageViewBackgroundAndIcon(binding.icon);
        }

        FileDownloadLimit downloadLimit = publicShare.getFileDownloadLimit();
        if (downloadLimit != null && downloadLimit.getLimit() > 0) {
            int remaining = downloadLimit.getLimit() - downloadLimit.getCount();
            String text = context.getResources().getQuantityString(R.plurals.share_download_limit_description, remaining, remaining);

            binding.subline.setText(text);
            binding.subline.setVisibility(View.VISIBLE);
        } else {
            binding.subline.setVisibility(View.GONE);
        }

        String permissionName = SharingMenuHelper.getPermissionName(context, publicShare);
        setPermissionName(publicShare, permissionName);

        binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
        if (!SharingMenuHelper.isSecureFileDrop(publicShare)) {
            binding.shareByLinkContainer.setOnClickListener(v -> listener.showPermissionsDialog(publicShare));
        }

        if (MDMConfig.INSTANCE.clipBoardSupport(context)) {
            binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
        } else {
            binding.copyLink.setVisibility(View.GONE);
        }
    }

    private void setPermissionName(OCShare publicShare, String permissionName) {
        if (!TextUtils.isEmpty(permissionName) && !SharingMenuHelper.isSecureFileDrop(publicShare)) {
            binding.permissionName.setText(permissionName);
            binding.permissionName.setVisibility(View.VISIBLE);
            viewThemeUtils.androidx.colorPrimaryTextViewElement(binding.permissionName);
        } else {
            binding.permissionName.setVisibility(View.GONE);
        }
    }
}
