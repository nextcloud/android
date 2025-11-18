/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.storagePermissionBanner

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.databinding.StoragePermissionWarningBannerBinding

class StoragePermissionBannerViewHolder(itemView: View, private val appPreferences: AppPreferences) :
    RecyclerView.ViewHolder(itemView) {
    init {
        StoragePermissionWarningBannerBinding.bind(itemView).apply {
            setup(appPreferences)
        }
    }
}
