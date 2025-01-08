/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.owncloud.android.R
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.ui.interfaces.GroupfolderListInterface
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

class GroupfolderListAdapter(
    val context: Context,
    val viewThemeUtils: ViewThemeUtils,
    private val groupfolderListInterface: GroupfolderListInterface
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var list: List<Groupfolder>

    fun setData(result: Map<String, Groupfolder>) {
        list = result.values.sortedBy { it.mountPoint }
    }

    private fun getFolderIcon(): LayerDrawable? {
        val overlayDrawableId = R.drawable.ic_folder_overlay_account_group
        return MimeTypeUtil.getFolderIcon(false, overlayDrawableId, context, viewThemeUtils)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        OCFileListItemViewHolder(
            ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val groupfolder = list[position]
        val listHolder = holder as OCFileListItemViewHolder

        val file = File("/" + groupfolder.mountPoint)

        listHolder.apply {
            fileName.text = file.name
            fileSize.text = file.parentFile?.path ?: "/"
            fileSizeSeparator.visibility = View.GONE
            lastModification.visibility = View.GONE
            checkbox.visibility = View.GONE
            overflowMenu.visibility = View.GONE
            shared.visibility = View.GONE
            localFileIndicator.visibility = View.GONE
            favorite.visibility = View.GONE

            thumbnail.setImageDrawable(getFolderIcon())

            itemLayout.setOnClickListener { groupfolderListInterface.onFolderClick(groupfolder.mountPoint) }
        }
    }
}
