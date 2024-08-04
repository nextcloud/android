/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.owncloud.android.ui.AvatarGroupLayout

internal interface ListItemViewHolder : ListGridItemViewHolder {
    val fileSize: TextView
    val fileSizeSeparator: View
    val lastModification: TextView
    val overflowMenu: ImageView
    val sharedAvatars: AvatarGroupLayout
    val tagsGroup: ChipGroup
    val firstTag: Chip
    val secondTag: Chip
    val tagMore: Chip
    val fileDetailGroup: LinearLayout
}
