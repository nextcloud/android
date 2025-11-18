/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.storagePermissionBanner

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.openAllFilesAccessSettings
import com.nextcloud.utils.extensions.openMediaPermissions
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil

class StoragePermissionBannerViewHolder(itemView: View, private val appPreferences: AppPreferences) :
    RecyclerView.ViewHolder(itemView) {

    private val fullFileAccessButton: MaterialButton =
        itemView.findViewById(R.id.fullFileAccess)
    private val mediaAccessButton: MaterialButton =
        itemView.findViewById(R.id.mediaReadOnly)
    private val dismissButton: MaterialButton =
        itemView.findViewById(R.id.dontShowStoragePermissionBanner)

    init {
        val context = itemView.context

        fullFileAccessButton.setVisibleIf(!PermissionUtil.checkFullFileAccess())
        fullFileAccessButton.setOnClickListener {
            context.openAllFilesAccessSettings()
        }
        mediaAccessButton.setVisibleIf(!PermissionUtil.checkMediaAccess(context))
        mediaAccessButton.setOnClickListener {
            context.openMediaPermissions()
        }

        dismissButton.setOnClickListener {
            dontShowBanner()
        }
    }

    private fun dontShowBanner() {
        itemView.visibility = View.GONE
        appPreferences.setShowStoragePermissionBanner(false)
    }
}
