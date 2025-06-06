/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.albums

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumsGridItemBinding
import com.owncloud.android.databinding.AlbumsListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class AlbumsAdapter(
    val context: Context,
    private val storageManager: FileDataStorageManager?,
    private val user: User,
    private val albumFragmentInterface: AlbumFragmentInterface,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val gridView: Boolean = true
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var albumList: MutableList<PhotoAlbumEntry> = mutableListOf()
    private val asyncTasks: MutableList<ThumbnailsCacheManager.ThumbnailGenerationTask> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (gridView) {
            AlbumGridItemViewHolder(AlbumsGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            AlbumListItemViewHolder(AlbumsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val gridViewHolder = holder as AlbumItemViewHolder
        val file: PhotoAlbumEntry = albumList[position]

        gridViewHolder.albumName.text = file.albumName
        gridViewHolder.thumbnail.tag = file.lastPhoto
        gridViewHolder.albumInfo.text = String.format(
            context.resources.getString(R.string.album_items_text),
            file.nbItems,
            file.createdDate
        )

        if (file.lastPhoto > 0) {
            val ocLocal = storageManager?.getFileByLocalId(file.lastPhoto)
            DisplayUtils.setThumbnail(
                ocLocal,
                gridViewHolder.thumbnail,
                user,
                storageManager,
                asyncTasks,
                gridView,
                context,
                gridViewHolder.shimmerThumbnail,
                preferences,
                viewThemeUtils,
                syncedFolderProvider
            )
        } else {
            gridViewHolder.thumbnail.setImageResource(R.drawable.file_image)
            gridViewHolder.thumbnail.visibility = View.VISIBLE
            gridViewHolder.shimmerThumbnail.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { albumFragmentInterface.onItemClick(file) }
    }

    fun cancelAllPendingTasks() {
        for (task in asyncTasks) {
            task.cancel(true)
            if (task.getMethod != null) {
                Log_OC.d("AlbumsAdapter", "cancel: abort get method directly")
                task.getMethod.abort()
            }
        }
        asyncTasks.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAlbumItems(albumItems: List<PhotoAlbumEntry>?) {
        albumList.clear()
        albumItems?.let {
            albumList.addAll(it)
        }
        notifyDataSetChanged()
    }
}
