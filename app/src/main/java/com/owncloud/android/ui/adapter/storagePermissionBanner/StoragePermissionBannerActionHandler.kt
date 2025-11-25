/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.storagePermissionBanner

import android.view.View
import com.nextcloud.utils.BuildHelper.isFlavourGPlay
import com.nextcloud.utils.extensions.openAllFilesAccessSettings
import com.nextcloud.utils.extensions.openMediaPermissions
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.MainApp
import com.owncloud.android.databinding.StoragePermissionWarningBannerBinding
import com.owncloud.android.utils.PermissionUtil

fun StoragePermissionWarningBannerBinding.setup(descriptionId: Int) {
    val context = this.root.context

    description.text = context.getString(descriptionId)

    val isBrandedAndFlavourGplay = (MainApp.isClientBranded() && isFlavourGPlay())
    fullFileAccess.setVisibleIf(!PermissionUtil.checkFullFileAccess() && !isBrandedAndFlavourGplay)
    fullFileAccess.setOnClickListener { context.openAllFilesAccessSettings() }

    mediaReadOnly.setVisibleIf(!PermissionUtil.checkMediaAccess(context))
    mediaReadOnly.setOnClickListener { context.openMediaPermissions() }

    root.visibility = if (PermissionUtil.checkFullFileAccess() || PermissionUtil.checkMediaAccess(context)) {
        View.GONE
    } else {
        View.VISIBLE
    }
}
