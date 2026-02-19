/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
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
import com.owncloud.android.datamodel.OCFile
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var albumList: MutableList<PhotoAlbumEntry> = mutableListOf()
    private val asyncTasks: MutableList<ThumbnailsCacheManager.ThumbnailGenerationTask> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = if (gridView) {
        AlbumGridItemViewHolder(AlbumsGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    } else {
        AlbumListItemViewHolder(AlbumsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = albumList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val gridViewHolder = holder as AlbumItemViewHolder
        val file: PhotoAlbumEntry = albumList[position]

        gridViewHolder.albumName.text = file.albumName
        gridViewHolder.thumbnail.tag = file.lastPhoto
        gridViewHolder.albumInfo.text = String.format(
            context.resources.getString(R.string.album_items_text),
            file.nbItems,
            DisplayUtils.getDateByPattern(file.createdDate, "MMM yyyy")
        )

        if (file.lastPhoto > 0) {
            var ocLocal = storageManager?.getFileByLocalId(file.lastPhoto)
            if (ocLocal == null) {
                // if local file is not present make dummy file with fake remotePath
                // without remotePath it won't work
                // lastPhoto is file id which we can set it to localId and remoteId for thumbnail generation
                val nFile = OCFile("/" + file.albumName)
                nFile.localId = file.lastPhoto
                nFile.remoteId = file.lastPhoto.toString()
                ocLocal = nFile
            }
            DisplayUtils.setThumbnail(
                ocLocal,
                gridViewHolder.thumbnail,
                user,
                storageManager,
                asyncTasks,
                gridView,
                context,
                null,
                preferences,
                viewThemeUtils,
                syncedFolderProvider,
                true
            )
        } else {
            gridViewHolder.thumbnail.setImageResource(R.drawable.file_image)
            gridViewHolder.thumbnail.visibility = View.VISIBLE
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
            // alphabetically sorting
            albumList.addAll(it.sortedBy { album -> album.albumName.lowercase() })
        }
        notifyDataSetChanged()
    }
}
