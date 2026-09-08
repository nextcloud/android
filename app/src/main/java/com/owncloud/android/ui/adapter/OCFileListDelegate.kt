/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.gallery.GalleryImageGenerationJob
import com.nextcloud.client.jobs.gallery.GalleryImageGenerationListener
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.utils.OCFileUtils
import com.nextcloud.utils.extensions.getBigThumbnailKey
import com.nextcloud.utils.extensions.getSmallThumbnailKey
import com.nextcloud.utils.extensions.makeRounded
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.extensions.stopShimmer
import com.nextcloud.utils.mdm.MDMConfig
import com.nextcloud.utils.extensions.videoOverlayKey
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.nextcloud.utils.thumbnail.ThumbnailMemoryCache
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class OCFileListDelegate(
    private val fileUploadHelper: FileUploadHelper,
    private val context: Context,
    private val ocFileListFragmentInterface: OCFileListFragmentInterface,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val hideItemOptions: Boolean,
    private val gridView: Boolean,
    private val transferServiceGetter: ComponentsGetter,
    private val showMetadata: Boolean,
    private var showShareAvatar: Boolean,
    private var viewThemeUtils: ViewThemeUtils,
    private val syncFolderProvider: SyncedFolderProvider? = null
) {
    private val tag = "OCFileListDelegate"
    private val checkedFiles: MutableSet<OCFile> = HashSet()
    private var highlightedItem: OCFile? = null
    var isMultiSelect = false
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val galleryImageGenerationJob = GalleryImageGenerationJob(user, storageManager)

    fun setHighlightedItem(highlightedItem: OCFile?) {
        this.highlightedItem = highlightedItem
    }

    fun isCheckedFile(file: OCFile): Boolean = checkedFiles.contains(file)

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

    fun bindGalleryRow(
        shimmer: LoaderImageView?,
        imageView: ImageView,
        file: OCFile,
        galleryRowHolder: GalleryRowHolder,
        imageDimension: Pair<Int, Int>
    ) {
        GalleryImageGenerationJob.cancelPreviousJob(imageView)

        imageView.tag = file.fileId

        val displayable = file.takeUnless { it.isUpdateThumbnailNeeded }?.displayableThumbnailFromMemory()
        if (displayable != null) {
            imageView.setImageBitmap(displayable)
            imageView.stopShimmer(shimmer)
            bindGalleryRowListeners(imageView, file, galleryRowHolder)
            return
        }

        imageView.setImageDrawable(OCFileUtils.getMediaPlaceholder(file, imageDimension))

        val job = ioScope.launch {
            try {
                galleryImageGenerationJob.run(
                    file,
                    imageView,
                    object : GalleryImageGenerationListener {
                        override fun onSuccess() {
                            if (imageView.tag == file.fileId) {
                                Log_OC.d(tag, "setGalleryImage.onSuccess()")
                                galleryRowHolder.binding.rowLayout.invalidate()
                                imageView.stopShimmer(shimmer)
                            }
                        }

                        override fun onNewGalleryImage() {
                            if (imageView.tag == file.fileId) {
                                Log_OC.d(tag, "setGalleryImage.updateRowVisuals()")
                                galleryRowHolder.updateRowVisuals()
                            }
                        }

                        override fun onError() {
                            if (imageView.tag == file.fileId) {
                                Log_OC.d(tag, "setGalleryImage.onError()")
                                imageView.stopShimmer(shimmer)
                            }
                        }
                    }
                )
            } finally {
                val currentJob = coroutineContext.job
                GalleryImageGenerationJob.removeActiveJob(imageView, currentJob)
            }
        }

        GalleryImageGenerationJob.storeJob(job, imageView)

        bindGalleryRowListeners(imageView, file, galleryRowHolder)
    }

    private fun OCFile.displayableThumbnailFromMemory(): Bitmap? {
        val thumbnailKey = listOf(getBigThumbnailKey(), getSmallThumbnailKey())
            .firstOrNull { ThumbnailMemoryCache.get(it) != null }
            ?: return null
        val thumbnail = ThumbnailMemoryCache.get(thumbnailKey)

        return when {
            thumbnail == null -> null
            MimeTypeUtil.isVideo(this) -> withVideoOverlay(thumbnailKey, thumbnail)
            else -> thumbnail
        }
    }

    private fun withVideoOverlay(thumbnailKey: String, thumbnail: Bitmap): Bitmap {
        val overlayKey = videoOverlayKey(thumbnailKey)

        return ThumbnailMemoryCache.get(overlayKey)
            ?: ThumbnailsCacheManager.addVideoOverlay(thumbnail, context).also {
                ThumbnailMemoryCache.put(overlayKey, it)
            }
    }

    private fun bindGalleryRowListeners(imageView: ImageView, file: OCFile, galleryRowHolder: GalleryRowHolder) {
        imageView.setOnClickListener {
            if (context is AlbumsPickerActivity) {
                ocFileListFragmentInterface.onLongItemClicked(
                    file
                )
            } else {
                ocFileListFragmentInterface.onItemClicked(file)
                AlbumItemsFragment.lastMediaItemPosition = galleryRowHolder.absoluteAdapterPosition
            }
        }

        if (!hideItemOptions) {
            imageView.apply {
                isLongClickable = true
                setOnLongClickListener {
                    ocFileListFragmentInterface.onLongItemClicked(
                        file
                    )
                }
            }
        }
    }

    @Suppress("MagicNumber")
    fun bindViewHolder(
        viewHolder: ListViewHolder,
        file: OCFile,
        currentDirectory: OCFile?,
        searchType: SearchType?,
        thumbnailGenerator: ThumbnailGenerator
    ) {
        // thumbnail
        viewHolder.imageFileName?.text = file.fileName
        viewHolder.thumbnail.tag = file.fileId
        if (gridView) {
            if (MimeTypeUtil.isImageOrVideo(file)) {
                viewHolder.thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                viewHolder.thumbnail.setPadding(0, 0, 0, 0)
            } else {
                viewHolder.thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
                val padding = context.resources.getDimensionPixelSize(R.dimen.standard_padding)
                viewHolder.thumbnail.setPadding(padding, padding, padding, padding)
            }
        }

        thumbnailGenerator.setThumbnail(
            file,
            viewHolder.thumbnail,
            ThumbnailArguments(isGrid = gridView, hideVideoOverlay = false, viewHolder.shimmerThumbnail)
        )

        // item layout + click listeners
        bindGridItemLayout(file, viewHolder)

        // unread comments
        bindUnreadComments(file, viewHolder)

        // multiSelect (Checkbox)
        val isFolderPickerActivity = (context is FolderPickerActivity)
        viewHolder.checkbox.setVisibleIf(isMultiSelect && !isFolderPickerActivity)

        // download state
        viewHolder.localFileIndicator.visibility = View.GONE // default first

        // metadata (downloaded, favorite)
        bindGridMetadataViews(file, viewHolder)

        // shares
        val shouldHideShare = (
            (
                hideItemOptions ||
                    (
                        !file.isFolder &&
                            file.isEncrypted
                        ) ||
                    (
                        file.isEncrypted &&
                            !EncryptionUtils.supportsSecureFiledrop(file, user)
                        ) ||
                    (searchType == SearchType.FAVORITE_SEARCH) ||
                    (
                        file.isFolder &&
                            (currentDirectory?.isEncrypted ?: false)
                        )
                )
            ) // sharing an encrypted subfolder is not possible
        if (shouldHideShare) {
            viewHolder.shared.visibility = View.GONE
        } else {
            configureSharedIconView(viewHolder, file)
        }

        if (!file.isOfflineOperation && !file.isFolder) {
            viewHolder.thumbnail.makeRounded(context, 4f)
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

        if (!hideItemOptions && gridViewHolder !is OCFileListRecommendedItemViewHolder) {
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

    private fun isSynchronizing(file: OCFile): Boolean {
        val operationsServiceBinder = transferServiceGetter.operationsServiceBinder
        val fileDownloadHelper = FileDownloadHelper.instance()

        return operationsServiceBinder?.isSynchronizing(user, file) == true ||
            fileDownloadHelper.isDownloading(user, file) ||
            fileUploadHelper.isUploading(file.remotePath, user.accountName)
    }

    private fun OCFile.canCheckFolderDown(): Boolean = mimeType != null &&
        isFolder &&
        !isEncrypted &&
        fileLength != 0L &&
        !etag.isNullOrBlank()

    @Suppress("ComplexCondition")
    private fun showLocalFileIndicator(file: OCFile, holder: ListViewHolder) {
        var isFolderDown = false
        if (file.canCheckFolderDown()) {
            isFolderDown = storageManager.fileDao.areAllFilesHaveMediaPath(file.fileId, user.accountName)
        }

        val isSyncing = isSynchronizing(file)
        val hasConflict = (file.etagInConflict != null)
        val isDown = file.isDown

        val icon = when {
            isSyncing -> R.drawable.ic_synchronizing
            hasConflict -> R.drawable.ic_synchronizing_error
            isDown || isFolderDown -> R.drawable.ic_synced
            else -> null
        }

        holder.localFileIndicator.run {
            if (icon != null && showMetadata) {
                setImageResource(icon)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
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

            file.isShared || file.isSharedViaLink -> R.drawable.shared_via_link to R.string.shared_icon_shared_via_link

            else -> R.drawable.ic_unshared to R.string.shared_icon_share
        }
    }

    fun setShowShareAvatar(bool: Boolean) {
        showShareAvatar = bool
    }

    @Suppress("TooGenericExceptionCaught")
    fun cleanup() {
        ioScope.coroutineContext.cancelChildren()

        try {
            GalleryImageGenerationJob.cancelAllActiveJobs()
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception: ", e)
        }

        Log_OC.d(TAG, "background jobs cancelled")
    }

    companion object {
        private val TAG = OCFileListDelegate::class.java.simpleName
    }
}
