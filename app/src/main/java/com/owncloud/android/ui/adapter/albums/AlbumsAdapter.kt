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
import com.nextcloud.utils.date.DateFormatPattern
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.AlbumsGridItemBinding
import com.owncloud.android.databinding.AlbumsListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.utils.DisplayUtils

@Suppress("LongParameterList")
class AlbumsAdapter(
    private val context: Context,
    private val storageManager: FileDataStorageManager?,
    private val albumFragmentInterface: AlbumFragmentInterface,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val gridView: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var albumList: MutableList<PhotoAlbumEntry> = mutableListOf()

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
        gridViewHolder.albumInfo.text = context.resources.getQuantityString(
            R.plurals.album_items_text,
            file.nbItems,
            file.nbItems,
            DisplayUtils.getDateByPattern(file.createdDate, DateFormatPattern.MonthWithYear.pattern)
        )

        gridViewHolder.albumName.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (file.collaborators.isNotEmpty()) R.drawable.ic_share else 0,
            0
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

            thumbnailGenerator.setThumbnail(
                ocLocal,
                gridViewHolder.thumbnail,
                ThumbnailArguments(isGrid = gridView, hideVideoOverlay = true, null)
            )
        } else {
            gridViewHolder.thumbnail.setImageResource(R.drawable.file_image)
            gridViewHolder.thumbnail.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { albumFragmentInterface.onItemClick(file) }
    }

    fun cancelAllPendingTasks() {
        thumbnailGenerator.fileThumbnailGenerator.cancelPendingTasks()
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
