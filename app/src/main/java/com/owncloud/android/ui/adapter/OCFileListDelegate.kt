/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.fragment.ExtendedListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import com.owncloud.android.utils.theme.ThemeDrawableUtils

@Suppress("LongParameterList", "TooManyFunctions")
class OCFileListDelegate(
    private val context: Context,
    private val ocFileListFragmentInterface: OCFileListFragmentInterface,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val hideItemOptions: Boolean,
    private val preferences: AppPreferences,
    private var gridView: Boolean,
    private val transferServiceGetter: ComponentsGetter,
    private val showMetadata: Boolean,
    private var showShareAvatar: Boolean,
    private val isMediaGallery: Boolean,
    private val syncFolderProvider: SyncedFolderProvider? = null
) {
    private val checkedFiles: MutableSet<OCFile> = HashSet()
    private var highlightedItem: OCFile? = null
    var isMultiSelect = false
    private val asyncTasks: MutableList<ThumbnailGenerationTask> = ArrayList()
    private var searchType: SearchType? = null
    fun setHighlightedItem(highlightedItem: OCFile?) {
        this.highlightedItem = highlightedItem
    }

    fun isCheckedFile(file: OCFile): Boolean {
        return checkedFiles.contains(file)
    }

    fun addCheckedFile(file: OCFile) {
        checkedFiles.add(file)
        highlightedItem = null
    }

    fun removeCheckedFile(file: OCFile) {
        checkedFiles.remove(file)
    }

    fun addToCheckedFiles(files: List<OCFile>?) {
        checkedFiles.addAll(files!!)
    }

    val checkedItems: Set<OCFile>
        get() = checkedFiles

    fun setCheckedItem(files: Set<OCFile>?) {
        checkedFiles.clear()
        checkedFiles.addAll(files!!)
    }

    fun clearCheckedItems() {
        checkedFiles.clear()
    }

    fun bindGridViewHolder(gridViewHolder: ListGridImageViewHolder, file: OCFile) {
        // thumbnail
        gridViewHolder.thumbnail.tag = file.fileId
        DisplayUtils.setThumbnail(
            file,
            gridViewHolder.thumbnail,
            user,
            storageManager,
            asyncTasks,
            gridView,
            context,
            gridViewHolder.shimmerThumbnail,
            preferences,
            syncFolderProvider,
            isMediaGallery
        )

        //remove padding if gallery media is there else enable padding
        if (isMediaGallery) {
            gridViewHolder.thumbnail.setPadding(0, 0, 0, 0)
        }

        // item layout + click listeners
        bindGridItemLayout(file, gridViewHolder)

        // unread comments
        bindUnreadComments(file, gridViewHolder)

        // multiSelect (Checkbox)
        if (isMultiSelect) {
            gridViewHolder.checkbox.visibility = View.VISIBLE
        } else {
            gridViewHolder.checkbox.visibility = View.GONE
        }

        // download state
        gridViewHolder.localFileIndicator.visibility = View.INVISIBLE // default first

        // metadata (downloaded, favorite)
        bindGridMetadataViews(file, gridViewHolder)

        // shares
        val shouldHideShare = gridView || hideItemOptions
            || searchType === SearchType.FAVORITE_SEARCH
            || searchType === SearchType.RECENTLY_MODIFIED_SEARCH
            //|| file.isFolder && !file.canReshare()
        if (shouldHideShare) {
            gridViewHolder.shared.visibility = View.GONE
        } else {
            showShareIcon(gridViewHolder, file)
        }
    }

    private fun bindUnreadComments(file: OCFile, gridViewHolder: ListGridImageViewHolder) {
        if (file.unreadCommentsCount > 0) {
            gridViewHolder.unreadComments.visibility = View.VISIBLE
            gridViewHolder.unreadComments.setOnClickListener {
                ocFileListFragmentInterface
                    .showActivityDetailView(file)
            }
        } else {
            gridViewHolder.unreadComments.visibility = View.GONE
        }
    }

    private fun bindGridItemLayout(file: OCFile, gridViewHolder: ListGridImageViewHolder) {
        if (highlightedItem != null && file.fileId == highlightedItem!!.fileId) {
            gridViewHolder.itemLayout.setBackgroundColor(
                context.resources
                    .getColor(R.color.selected_item_background)
            )
        } else if (isCheckedFile(file)) {
            gridViewHolder.itemLayout.setBackgroundColor(
                context.resources
                    .getColor(R.color.selected_item_background)
            )
            gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_marked)

        } else {
            gridViewHolder.itemLayout.setBackgroundColor(context.resources.getColor(R.color.bg_default))
            gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline)
        }
        gridViewHolder.itemLayout.setOnClickListener { ocFileListFragmentInterface.onItemClicked(file) }
        if (!hideItemOptions) {
            gridViewHolder.itemLayout.isLongClickable = true
            gridViewHolder.itemLayout.setOnLongClickListener {
                ocFileListFragmentInterface.onLongItemClicked(
                    file
                )
            }
        }
    }

    private fun bindGridMetadataViews(file: OCFile, gridViewHolder: ListGridImageViewHolder) {
        if (!isMediaGallery && showMetadata) {
            showLocalFileIndicator(file, gridViewHolder)
            gridViewHolder.favorite.visibility = if (file.isFavorite) View.VISIBLE else View.GONE
        } else {
            gridViewHolder.localFileIndicator.visibility = View.GONE
            gridViewHolder.favorite.visibility = View.GONE
        }
    }

    private fun showLocalFileIndicator(file: OCFile, gridViewHolder: ListGridImageViewHolder) {
        val operationsServiceBinder = transferServiceGetter.operationsServiceBinder
        val fileDownloaderBinder = transferServiceGetter.fileDownloaderBinder
        val fileUploaderBinder = transferServiceGetter.fileUploaderBinder
        when {
            operationsServiceBinder?.isSynchronizing(user, file) == true ||
                fileDownloaderBinder?.isDownloading(user, file) == true ||
                fileUploaderBinder?.isUploading(user, file) == true -> {
                // synchronizing, downloading or uploading
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing)
                gridViewHolder.localFileIndicator.visibility = View.VISIBLE
            }
            file.etagInConflict != null -> {
                // conflict
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synchronizing_error)
                gridViewHolder.localFileIndicator.visibility = View.VISIBLE
            }
            file.isDown -> {
                // downloaded
                gridViewHolder.localFileIndicator.setImageResource(R.drawable.ic_synced)
                gridViewHolder.localFileIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun showShareIcon(gridViewHolder: ListGridImageViewHolder, file: OCFile) {
        val sharedIconView = gridViewHolder.shared
        //Initialising Textview for Message and setting its visibility
        val sharedMessageView: TextView = gridViewHolder.sharedMessage
        sharedMessageView.visibility = if (DisplayUtils.isShowDividerForList()) View.VISIBLE else View.GONE

        if (gridViewHolder is OCFileListItemViewHolder || file.unreadCommentsCount == 0) {
            sharedIconView.visibility = View.VISIBLE
            when {
                file.isSharedWithMe -> {
                    val sharedWithMeColor = ResourcesCompat.getColor(context.resources,
                        R.color.shared_with_me_color, null)
                    sharedIconView.setImageDrawable(ThemeDrawableUtils.tintDrawable(R.drawable.ic_shared_with_me, sharedWithMeColor))
                    //Added Code For Message Text
                    sharedMessageView.text = context.resources.getString(R.string.placeholder_receivedMessage)
                    sharedMessageView.setTextColor(sharedWithMeColor)
                    sharedIconView.contentDescription = context.getString(R.string.shared_icon_shared)
                }
                file.isSharedWithSharee -> {
                    val primaryColor = ThemeColorUtils.primaryColor(context, true)
                    sharedIconView.setImageDrawable(ThemeDrawableUtils.tintDrawable(R.drawable.ic_shared, primaryColor))
                    sharedIconView.contentDescription = context.getString(R.string.shared_icon_shared)
                    //Added Code For Message Text
                    sharedMessageView.text = context.resources.getString(R.string.placeholder_sharedMessage)
                    sharedMessageView.setTextColor(primaryColor)

                }
                file.isSharedViaLink -> {
                    val primaryColor = ThemeColorUtils.primaryColor(context, true)
                    sharedIconView.setImageDrawable(ThemeDrawableUtils.tintDrawable(R.drawable.ic_shared, primaryColor))
                    sharedIconView.contentDescription = context.getString(R.string.shared_icon_shared_via_link)
                    //Added Code For Message Text
                    sharedMessageView.text = context.resources.getString(R.string.placeholder_sharedMessage)
                    sharedMessageView.setTextColor(primaryColor)
                }
                file.isFolder && file.isEncrypted -> {
                    sharedIconView.visibility = View.GONE;
                }
                else -> {
                    sharedIconView.setImageDrawable(ThemeDrawableUtils.tintDrawable(R.drawable.ic_unshared,
                        ResourcesCompat.getColor(context.resources,
                            R.color.list_icon_color, null)))
                    sharedIconView.contentDescription = context.getString(R.string.shared_icon_share)
                    sharedMessageView.visibility = View.GONE
                }
            }
            sharedIconView.setOnClickListener { ocFileListFragmentInterface.onShareIconClick(file) }
        } else {
            sharedIconView.visibility = View.GONE
        }
    }

    fun cancelAllPendingTasks() {
        for (task in asyncTasks) {
            task.cancel(true)
            if (task.getMethod != null) {
                Log_OC.d(TAG, "cancel: abort get method directly")
                task.getMethod.abort()
            }
        }
        asyncTasks.clear()
    }

    fun setShowShareAvatar(bool: Boolean) {
        showShareAvatar = bool
    }

    fun setGridView(bool: Boolean){
        gridView = bool
    }

    fun setSearchType(searchType: SearchType?) {
        this.searchType = searchType
    }

    companion object {
        private val TAG = OCFileListDelegate::class.java.simpleName
    }
}
