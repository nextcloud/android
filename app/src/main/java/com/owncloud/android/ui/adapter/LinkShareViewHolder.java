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
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

class LinkShareViewHolder extends RecyclerView.ViewHolder {
    private FileDetailsShareLinkShareItemBinding binding;
    private Context context;
    private ViewThemeUtils viewThemeUtils;
    private boolean encrypted;

    public LinkShareViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public LinkShareViewHolder(FileDetailsShareLinkShareItemBinding binding,
                               Context context,
                               final ViewThemeUtils viewThemeUtils,
                               boolean encrypted) {
        this(binding.getRoot());
        this.binding = binding;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
    }

    public void bind(OCShare publicShare, ShareeListAdapterListener listener, int position) {
        if (ShareType.EMAIL == publicShare.getShareType()) {
            final var res = context.getResources();
            binding.name.setText(publicShare.getSharedWithDisplayName());

            final var emailDrawable = ResourcesCompat.getDrawable(res, R.drawable.ic_email, null);
            binding.icon.setImageDrawable(emailDrawable);
            binding.copyLink.setVisibility(View.GONE);

            final var backgroundColor = ContextCompat.getColor(context, R.color.nc_grey);
            binding.icon.getBackground().setColorFilter(backgroundColor, PorterDuff.Mode.SRC_IN);

            final var drawableColor = ContextCompat.getColor(context, R.color.icon_on_nc_grey);
            binding.icon.getDrawable().mutate().setColorFilter(drawableColor, PorterDuff.Mode.SRC_IN);
        } else {
            String label = publicShare.getLabel();

            if (!TextUtils.isEmpty(label)) {
                binding.name.setText(context.getString(R.string.share_link_with_label, label));
            } else if (SharingMenuHelper.isFileRequest(publicShare)) {
                binding.name.setText(R.string.share_permission_file_request);
            } else if (SharingMenuHelper.isSecureFileDrop(publicShare) && encrypted) {
                binding.name.setText(R.string.share_permission_secure_file_drop);
            } else {
                int textRes = (position == 0) ? R.string.share_link : R.string.share_link_with_label;
                Object arg = (position == 0) ? null : String.valueOf(position);
                binding.name.setText((position == 0) ? context.getString(textRes)
                                         : context.getString(textRes, arg));
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

        QuickPermissionType quickPermissionType = SharingMenuHelper.getSelectedType(publicShare, encrypted);
        setPermissionName(publicShare, quickPermissionType.getText(context));

        binding.overflowMenu.setOnClickListener(v -> listener.showSharingMenuActionSheet(publicShare));
        if (!SharingMenuHelper.isSecureFileDrop(publicShare) && !encrypted) {
            binding.shareByLinkContainer.setOnClickListener(v -> listener.showPermissionsDialog(publicShare));
        }

        if (MDMConfig.INSTANCE.clipBoardSupport(context)) {
            binding.copyLink.setOnClickListener(v -> listener.copyLink(publicShare));
        } else {
            binding.copyLink.setVisibility(View.GONE);
        }
    }

    private void setPermissionName(OCShare publicShare, String permissionName) {
        if (TextUtils.isEmpty(permissionName) || (SharingMenuHelper.isSecureFileDrop(publicShare) && encrypted)) {
            binding.permissionName.setVisibility(View.GONE);
            return;
        }

        binding.permissionName.setText(permissionName);
        binding.permissionName.setVisibility(View.VISIBLE);
        viewThemeUtils.androidx.colorPrimaryTextViewElement(binding.permissionName);
    }
}
