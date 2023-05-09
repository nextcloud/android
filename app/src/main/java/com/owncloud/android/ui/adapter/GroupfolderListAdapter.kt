/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.owncloud.android.R
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.ui.interfaces.GroupfolderListInterface
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

class GroupfolderListAdapter(
    val context: Context,
    val viewThemeUtils: ViewThemeUtils,
    private val groupfolderListInterface: GroupfolderListInterface
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var list: List<Groupfolder>

    private val folderIcon = viewThemeUtils.platform.tintPrimaryDrawable(
        context,
        AppCompatResources.getDrawable(
            context,
            R.drawable.folder_group
        )
    )

    fun setData(result: Map<String, Groupfolder>) {
        list = result.values.sortedBy { it.mountPoint }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return OCFileListItemViewHolder(
            ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

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

            thumbnail.setImageDrawable(folderIcon)

            itemLayout.setOnClickListener { groupfolderListInterface.onFolderClick(groupfolder.mountPoint) }
        }
    }
}
