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
package com.owncloud.android.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.fragment.util.SharePermissionManager.getSelectedType
import com.owncloud.android.ui.fragment.util.SharePermissionManager.isSecureFileDrop
import com.owncloud.android.utils.theme.ViewThemeUtils

internal class LinkShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var binding: FileDetailsShareLinkShareItemBinding? = null
    private var context: Context? = null
    private var viewThemeUtils: ViewThemeUtils? = null
    private var encrypted = false

    constructor(
        binding: FileDetailsShareLinkShareItemBinding,
        context: Context,
        viewThemeUtils: ViewThemeUtils,
        encrypted: Boolean
    ) : this(binding.getRoot()) {
        this.binding = binding
        this.context = context
        this.viewThemeUtils = viewThemeUtils
        this.encrypted = encrypted
    }

    fun bind(publicShare: OCShare, listener: ShareeListAdapterListener, position: Int) {
        val quickPermissionType = getSelectedType(publicShare, encrypted)

        setName(binding, context, publicShare, quickPermissionType, position)
        setSubline(binding, context, publicShare)
        setPermissionName(binding, context, publicShare, quickPermissionType)
        setOnClickListeners(binding, listener, publicShare)
        configureCopyLink(binding, context, listener, publicShare)
    }

    private fun setName(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        publicShare: OCShare,
        quickPermissionType: QuickPermissionType,
        position: Int
    ) {
        if (binding == null || context == null) {
            return
        }

        if (ShareType.EMAIL == publicShare.shareType) {
            val res = context.resources
            binding.name.text = publicShare.sharedWithDisplayName

            val emailDrawable = ResourcesCompat.getDrawable(res, R.drawable.ic_email, null)
            binding.icon.setImageDrawable(emailDrawable)
            binding.copyLink.setVisibility(View.GONE)
        } else {
            val label = publicShare.label

            if (!TextUtils.isEmpty(label)) {
                binding.name.text = context.getString(R.string.share_link_with_label, label)
            } else if (quickPermissionType != QuickPermissionType.NONE) {
                binding.name.text = quickPermissionType.getText(context)
            } else {
                val textRes = if (position == 0) R.string.share_link else R.string.share_link_with_label
                val arg = if (position == 0) null else position.toString()
                binding.name.text = if (position == 0)
                    context.getString(textRes)
                else
                    context.getString(textRes, arg)
            }
        }
    }

    private fun setSubline(binding: FileDetailsShareLinkShareItemBinding?, context: Context?, publicShare: OCShare) {
        if (binding == null || context == null) {
            return
        }

        val downloadLimit = publicShare.fileDownloadLimit
        if (downloadLimit != null && downloadLimit.limit > 0) {
            val remaining = downloadLimit.limit - downloadLimit.count
            val text = context.resources.getQuantityString(
                R.plurals.share_download_limit_description,
                remaining,
                remaining
            )

            binding.subline.text = text
            binding.subline.visibility = View.VISIBLE
        } else {
            binding.subline.visibility = View.GONE
        }
    }

    private fun setPermissionName(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        publicShare: OCShare?,
        quickPermissionType: QuickPermissionType
    ) {
        if (binding == null || context == null) {
            return
        }

        val permissionName = quickPermissionType.getText(context)

        if (TextUtils.isEmpty(permissionName) || (isSecureFileDrop(publicShare) && encrypted)) {
            binding.permissionName.visibility = View.GONE
            return
        }

        binding.permissionName.text = permissionName
        binding.permissionName.visibility = View.VISIBLE
        viewThemeUtils?.androidx?.colorPrimaryTextViewElement(binding.permissionName)
    }

    private fun setOnClickListeners(
        binding: FileDetailsShareLinkShareItemBinding?,
        listener: ShareeListAdapterListener,
        publicShare: OCShare,
    ) {
        if (binding == null) {
            return
        }

        viewThemeUtils?.platform?.colorImageViewBackgroundAndIcon(binding.icon)

        binding.overflowMenu.setOnClickListener {
            listener.showSharingMenuActionSheet(publicShare)
        }
        binding.shareByLinkContainer.setOnClickListener {
            listener.showPermissionsDialog(publicShare)
        }
    }

    private fun configureCopyLink(
        binding: FileDetailsShareLinkShareItemBinding?,
        context: Context?,
        listener: ShareeListAdapterListener,
        publicShare: OCShare,
    ) {
        if (binding == null || context == null) {
            return
        }

        if (MDMConfig.clipBoardSupport(context)) {
            binding.copyLink.setOnClickListener { v: View? -> listener.copyLink(publicShare) }
        } else {
            binding.copyLink.setVisibility(View.GONE)
        }
    }
}
