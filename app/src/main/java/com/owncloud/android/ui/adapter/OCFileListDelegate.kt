/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.makeRounded
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.GalleryImageGenerationTask.GalleryListener
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList", "TooManyFunctions")
class OCFileListDelegate(
    private val fileUploadHelper: FileUploadHelper,
    private val context: Context,
    private val ocFileListFragmentInterface: OCFileListFragmentInterface,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val hideItemOptions: Boolean,
    private val preferences: AppPreferences,
    private val gridView: Boolean,
    private val transferServiceGetter: ComponentsGetter,
    private val showMetadata: Boolean,
    private var showShareAvatar: Boolean,
    private var viewThemeUtils: ViewThemeUtils,
    private val syncFolderProvider: SyncedFolderProvider? = null
) {
    private val checkedFiles: MutableSet<OCFile> = HashSet()
    private var highlightedItem: OCFile? = null
    var isMultiSelect = false
    private val asyncTasks: MutableList<ThumbnailsCacheManager.ThumbnailGenerationTask> = ArrayList()
    private val asyncGalleryTasks: MutableList<ThumbnailsCacheManager.GalleryImageGenerationTask> = ArrayList()
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

    fun bindGalleryRowThumbnail(
        shimmer: LoaderImageView?,
        imageView: ImageView,
        file: OCFile,
        galleryRowHolder: GalleryRowHolder,
        width: Int
    ) {
        // thumbnail
        imageView.tag = file.fileId
        setGalleryImage(
            file,
            imageView,
            shimmer,
            galleryRowHolder,
            width
        )

        imageView.setOnClickListener {
            ocFileListFragmentInterface.onItemClicked(file)
            GalleryFragment.setLastMediaItemPosition(galleryRowHolder.absoluteAdapterPosition)
        }
    }

    @Suppress("ComplexMethod")
    private fun setGalleryImage(
        file: OCFile,
        thumbnailView: ImageView,
        shimmerThumbnail: LoaderImageView?,
        galleryRowHolder: GalleryRowHolder,
        width: Int
    ) {
        // cancel previous generation, if view is re-used
        if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
            for (task in asyncTasks) {
                if (file.remoteId != null && task.imageKey != null && file.remoteId == task.imageKey) {
                    return
                }
            }
            try {
                val task = ThumbnailsCacheManager.GalleryImageGenerationTask(
                    thumbnailView,
                    user,
                    storageManager,
                    asyncGalleryTasks,
                    file.remoteId,
                    ContextCompat.getColor(context, R.color.bg_default)
                )
                var drawable = MimeTypeUtil.getFileTypeIcon(
                    file.mimeType,
                    file.fileName,
                    context,
                    viewThemeUtils
                )
                if (drawable == null) {
                    drawable = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.file_image,
                        null
                    )
                }
                if (drawable == null) {
                    drawable = ColorDrawable(Color.GRAY)
                }
                val thumbnail = BitmapUtils.drawableToBitmap(drawable, width / 2, width / 2)
                val asyncDrawable = ThumbnailsCacheManager.AsyncGalleryImageDrawable(
                    context.resources,
                    thumbnail,
                    task
                )
                if (shimmerThumbnail != null) {
                    Log_OC.d("Shimmer", "start Shimmer")
                    DisplayUtils.startShimmer(shimmerThumbnail, thumbnailView)
                }
                task.setListener(object : GalleryListener {
                    override fun onSuccess() {
                        galleryRowHolder.binding.rowLayout.invalidate()
                        Log_OC.d("Shimmer", "stop Shimmer")
                        DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
                    }

                    override fun onNewGalleryImage() {
                        galleryRowHolder.redraw()
                    }

                    override fun onError() {
                        Log_OC.d("Shimmer", "stop Shimmer")
                        DisplayUtils.stopShimmer(shimmerThumbnail, thumbnailView)
                    }
                })
                thumbnailView.setImageDrawable(asyncDrawable)
                asyncGalleryTasks.add(task)
                task.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log_OC.d(this, "ThumbnailGenerationTask : " + e.message)
            }
        }
    }

    fun setThumbnail(thumbnail: ImageView, shimmerThumbnail: LoaderImageView?, file: OCFile) {
        DisplayUtils.setThumbnail(
            file,
            thumbnail,
            user,
            storageManager,
            asyncTasks,
            gridView,
            context,
            shimmerThumbnail,
            preferences,
            viewThemeUtils,
            syncFolderProvider
        )
    }

    fun bindGridViewHolder(
        gridViewHolder: ListViewHolder,
        file: OCFile,
        currentDirectory: OCFile?,
        searchType: SearchType?
    ) {
        // thumbnail
        gridViewHolder.imageFileName?.text = file.fileName
        gridViewHolder.thumbnail.tag = file.fileId
        setThumbnail(gridViewHolder.thumbnail, gridViewHolder.shimmerThumbnail, file)

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
        gridViewHolder.localFileIndicator.visibility = View.GONE // default first

        // metadata (downloaded, favorite)
        bindGridMetadataViews(file, gridViewHolder)

        // shares
        val shouldHideShare = (
            hideItemOptions ||
                !file.isFolder &&
                file.isEncrypted ||
                file.isEncrypted &&
                !EncryptionUtils.supportsSecureFiledrop(file, user) ||
                searchType == SearchType.FAVORITE_SEARCH ||
                file.isFolder && currentDirectory?.isEncrypted ?: false
            ) // sharing an encrypted subfolder is not possible
        if (shouldHideShare) {
            gridViewHolder.shared.visibility = View.GONE
        } else {
            configureSharedIconView(gridViewHolder, file)
        }

        if (!file.isOfflineOperation && !file.isFolder) {
            gridViewHolder.thumbnail.makeRounded(context, 4f)
        }
    }

    private fun bindUnreadComments(file: OCFile, gridViewHolder: ListViewHolder) {
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

    private fun bindGridItemLayout(file: OCFile, gridViewHolder: ListViewHolder) {
        setItemLayoutBackgroundColor(file, gridViewHolder)
        setCheckBoxImage(file, gridViewHolder)
        setItemLayoutOnClickListeners(file, gridViewHolder)

        gridViewHolder.more?.setOnClickListener {
            ocFileListFragmentInterface.onOverflowIconClicked(file, it)
        }
    }

    private fun setItemLayoutOnClickListeners(file: OCFile, gridViewHolder: ListViewHolder) {
        gridViewHolder.itemLayout.setOnClickListener { ocFileListFragmentInterface.onItemClicked(file) }

        if (!hideItemOptions) {
            gridViewHolder.itemLayout.apply {
                isLongClickable = true
                setOnLongClickListener {
                    ocFileListFragmentInterface.onLongItemClicked(
                        file
                    )
                }
            }
        }
    }

    private fun setItemLayoutBackgroundColor(file: OCFile, gridViewHolder: ListViewHolder) {
        val cornerRadius = context.resources.getDimension(R.dimen.selected_grid_container_radius)

        val isDarkModeActive = (syncFolderProvider?.preferences?.isDarkModeEnabled == true)
        val selectedItemBackgroundColorId: Int = if (isDarkModeActive) {
            R.color.action_mode_background
        } else {
            R.color.selected_item_background
        }

        val itemLayoutBackgroundColorId: Int = if (file.fileId == highlightedItem?.fileId || isCheckedFile(file)) {
            selectedItemBackgroundColorId
        } else {
            R.color.bg_default
        }

        gridViewHolder.itemLayout.run {
            makeRounded(context, cornerRadius)
            setBackgroundColor(ContextCompat.getColor(context, itemLayoutBackgroundColorId))
        }
    }

    private fun setCheckBoxImage(file: OCFile, gridViewHolder: ListViewHolder) {
        if (isCheckedFile(file)) {
            gridViewHolder.checkbox.setImageDrawable(
                viewThemeUtils.platform.tintDrawable(context, R.drawable.ic_checkbox_marked, ColorRole.PRIMARY)
            )
        } else {
            gridViewHolder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline)
        }
    }

    private fun bindGridMetadataViews(file: OCFile, gridViewHolder: ListViewHolder) {
        if (showMetadata) {
            showLocalFileIndicator(file, gridViewHolder)
            gridViewHolder.favorite.visibility = if (file.isFavorite) View.VISIBLE else View.GONE
        } else {
            gridViewHolder.localFileIndicator.visibility = View.GONE
            gridViewHolder.favorite.visibility = View.GONE
        }
    }

    private fun showLocalFileIndicator(file: OCFile, gridViewHolder: ListViewHolder) {
        val operationsServiceBinder = transferServiceGetter.operationsServiceBinder
        val fileDownloadHelper = FileDownloadHelper.instance()

        val icon: Int? = when {
            operationsServiceBinder?.isSynchronizing(user, file) == true ||
                fileDownloadHelper.isDownloading(user, file) ||
                fileUploadHelper.isUploading(user, file) -> {
                // synchronizing, downloading or uploading
                R.drawable.ic_synchronizing
            }

            file.etagInConflict != null -> {
                R.drawable.ic_synchronizing_error
            }

            file.isDown -> {
                R.drawable.ic_synced
            }

            else -> {
                null
            }
        }

        gridViewHolder.localFileIndicator.run {
            icon?.let {
                setImageResource(icon)
                visibility = View.VISIBLE
            }
        }
    }

    private fun configureSharedIconView(gridViewHolder: ListViewHolder, file: OCFile) {
        val result = getShareIconIdAndContentDescriptionId(gridViewHolder, file)

        gridViewHolder.shared.run {
            if (result == null) {
                visibility = View.GONE
                return
            }

            setImageResource(result.first)
            contentDescription = context.getString(result.second)
            visibility = View.VISIBLE
            setOnClickListener { ocFileListFragmentInterface.onShareIconClick(file) }
        }
    }

    @Suppress("ReturnCount")
    private fun getShareIconIdAndContentDescriptionId(holder: ListViewHolder, file: OCFile): Pair<Int, Int>? {
        if (!MDMConfig.sharingSupport(context)) {
            return null
        }

        if (file.isOfflineOperation) return null

        if (holder !is OCFileListItemViewHolder && file.unreadCommentsCount != 0) return null

        return when {
            file.isSharedWithSharee || file.isSharedWithMe -> {
                if (showShareAvatar) null else R.drawable.shared_via_users to R.string.shared_icon_shared
            }
            file.isSharedViaLink -> R.drawable.shared_via_link to R.string.shared_icon_shared_via_link
            else -> R.drawable.ic_unshared to R.string.shared_icon_share
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

    companion object {
        private val TAG = OCFileListDelegate::class.java.simpleName
    }
}
