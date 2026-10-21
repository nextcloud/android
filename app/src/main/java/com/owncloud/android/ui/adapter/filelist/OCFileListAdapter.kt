/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter.filelist

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.avatar.AvatarGenerationListener
import com.nextcloud.utils.avatar.AvatarGenerator
import com.nextcloud.utils.date.DateFormatPattern
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.extensions.stopShimmer
import com.nextcloud.utils.mdm.MDMConfig
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.GridItemBinding
import com.owncloud.android.databinding.ListFooterBinding
import com.owncloud.android.databinding.ListHeaderBinding
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileListAdapterDataProviderImpl
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.filelist.data.OCFileListAdapterDataProvider
import com.owncloud.android.ui.adapter.filelist.data.OCFileListAdapterHelper
import com.owncloud.android.ui.adapter.filelist.data.OCFileListDiffer
import com.owncloud.android.ui.adapter.filelist.footer.OCFileListFooterBinder
import com.owncloud.android.ui.adapter.filelist.footer.OCFileListFooterViewHolder
import com.owncloud.android.ui.adapter.filelist.header.OCFileListHeaderBinder
import com.owncloud.android.ui.adapter.filelist.header.OCFileListHeaderViewHolder
import com.owncloud.android.ui.adapter.filelist.item.ListGridItemViewHolder
import com.owncloud.android.ui.adapter.filelist.item.ListItemViewHolder
import com.owncloud.android.ui.adapter.filelist.item.ListViewHolder
import com.owncloud.android.ui.adapter.filelist.item.OCFileListDelegate
import com.owncloud.android.ui.adapter.filelist.item.OCFileListGridItemViewHolder
import com.owncloud.android.ui.adapter.filelist.item.OCFileListItemBinder
import com.owncloud.android.ui.adapter.filelist.item.OCFileListItemViewHolder
import com.owncloud.android.ui.adapter.filelist.recommended.RecommendedFilesAdapter
import com.owncloud.android.ui.adapter.filelist.recommended.RecommendedFilesViewHolder
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileSortOrder.SortType
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Suppress("LongParameterList", "TooManyFunctions")
class OCFileListAdapter(
    private val activity: Activity,
    private var user: User,
    private val preferences: AppPreferences,
    syncedFolderProvider: SyncedFolderProvider?,
    transferServiceGetter: ComponentsGetter,
    ocFileListFragmentInterface: OCFileListFragmentInterface,
    private val hideItemOptions: Boolean,
    private var gridView: Boolean,
    viewThemeUtils: ViewThemeUtils,
    private val thumbnailGenerator: ThumbnailGenerator,
    avatarGenerator: AvatarGenerator
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    AvatarGenerationListener,
    CommonOCFileListAdapterInterface,
    PopupTextProvider {

    val listOfHiddenFiles = ArrayList<String>()

    private val allFiles = mutableListOf<OCFile>()
    private val recommendedFiles = mutableListOf<OCFile>()
    private var recommendedFilesAdapter: RecommendedFilesAdapter? = null

    private val popupDateFormat = SimpleDateFormat(DateFormatPattern.FullMonthWithYear.pattern, Locale.getDefault())
    private val footerId = UUID.randomUUID().leastSignificantBits
    private val headerId = UUID.randomUUID().leastSignificantBits
    private val helper = OCFileListAdapterHelper()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val differ = OCFileListDiffer(this, preferences, ::shouldShowHeader)

    private var currentDirectory: OCFile? = null
    private var listedDirectory: OCFile? = null
    private var onlyOnDevice = false
    private var displayedSortOrder: FileSortOrder? = null
    private var searchType: SearchType? = null

    private var storageManager: FileDataStorageManager =
        transferServiceGetter.storageManager ?: FileDataStorageManager(user, activity.contentResolver)
    private var adapterDataProvider: OCFileListAdapterDataProvider = OCFileListAdapterDataProviderImpl(storageManager)
    private val capability: OCCapability = CapabilityUtils.getCapability(user, activity)

    @Suppress("DEPRECATION")
    private val userId: String? = AccountManager
        .get(activity)
        .getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)

    private val ocFileListDelegate = OCFileListDelegate(
        FileUploadHelper.instance(),
        activity,
        ocFileListFragmentInterface,
        user,
        storageManager,
        hideItemOptions,
        gridView,
        transferServiceGetter,
        showMetadata = true,
        showShareAvatar = true,
        viewThemeUtils,
        syncedFolderProvider
    )

    private val headerBinder = OCFileListHeaderBinder(
        activity,
        viewThemeUtils,
        ocFileListFragmentInterface,
        preferences,
        capability
    ) { user }

    private val footerBinder = OCFileListFooterBinder(
        activity,
        viewThemeUtils,
        preferences,
        ocFileListFragmentInterface
    )

    private val itemBinder = OCFileListItemBinder(
        activity,
        viewThemeUtils,
        ocFileListFragmentInterface,
        avatarGenerator,
        userId
    )

    val isEmpty: Boolean
        get() = differ.latestFiles.isEmpty()

    val isHeaderDisplayed: Boolean
        get() = differ.isHeaderDisplayed

    val files: List<OCFile>
        get() = differ.files

    init {
        (activity as? FileDisplayActivity)?.showSortListGroup(true)
        setHasStableIds(true)
        ThumbnailsCacheManager.initDiskCacheAsync()
    }

    // region Overridden methods
    override fun getItemCount(): Int = files.size + differ.headerOffset() + FOOTER_ITEM_COUNT

    override fun getItemId(position: Int): Long {
        if (isHeaderDisplayed && position == 0) {
            return headerId
        }

        val files = files
        val filePosition = position - differ.headerOffset()
        return when {
            filePosition == files.size -> footerId
            filePosition < files.size -> files[filePosition].fileId
            else -> RecyclerView.NO_ID
        }
    }

    override fun getItemViewType(position: Int): Int = viewTypeAt(position).ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (OCFileListViewType.fromOrdinal(viewType)) {
            OCFileListViewType.FOOTER -> OCFileListFooterViewHolder(ListFooterBinding.inflate(inflater, parent, false))

            OCFileListViewType.HEADER -> OCFileListHeaderViewHolder(ListHeaderBinding.inflate(inflater, parent, false))

            OCFileListViewType.ITEM, OCFileListViewType.IMAGE -> if (gridView) {
                OCFileListGridItemViewHolder(GridItemBinding.inflate(inflater, parent, false))
            } else {
                OCFileListItemViewHolder(ListItemBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OCFileListFooterViewHolder -> footerBinder.bind(holder, files)
            is OCFileListHeaderViewHolder -> bindHeader(holder)
            else -> bindFileAt(holder as ListViewHolder, position)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val iconId = payloads.firstOrNull() as? Int
        if (iconId != null && OCFileListDiffer.REBIND_PAYLOAD !in payloads && holder is ListViewHolder) {
            holder.localFileIndicator.setImageResource(iconId)
            holder.localFileIndicator.visibility = View.VISIBLE
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        val thumbnailShimmer = (holder as? ListViewHolder)?.shimmerThumbnail ?: return
        if (thumbnailShimmer.isVisible) {
            thumbnailShimmer.setImageResource(R.drawable.background)
            thumbnailShimmer.resetLoader()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ListViewHolder) {
            holder.thumbnail.stopShimmer(holder.shimmerThumbnail)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        differ.attach(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        differ.detach()
    }

    override fun isMultiSelect(): Boolean = ocFileListDelegate.isMultiSelect

    @SuppressLint("NotifyDataSetChanged")
    override fun setMultiSelect(isMultiSelect: Boolean) {
        ocFileListDelegate.isMultiSelect = isMultiSelect
        notifyDataSetChanged()
    }

    override fun cancelAllPendingTasks() = thumbnailGenerator.fileThumbnailGenerator.cancelPendingTasks()

    override fun getItemPosition(file: OCFile): Int {
        val position = files.indexOf(file)
        return if (position == RecyclerView.NO_POSITION) position else position + differ.headerOffset()
    }

    override fun swapDirectory(
        user: User,
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        mLimitToMimeType: String
    ) {
        this.onlyOnDevice = onlyOnDevice

        if (storageManager != this.storageManager) {
            this.storageManager = storageManager
            adapterDataProvider = OCFileListAdapterDataProviderImpl(storageManager)
            ocFileListDelegate.setShowShareAvatar(true)
            this.user = user
        }

        val accountUserId = userId ?: return

        helper.prepareFileList(
            directory,
            adapterDataProvider,
            onlyOnDevice,
            mLimitToMimeType,
            preferences,
            accountUserId
        ) { newList, _ ->
            updateAdapter(newList, directory)
        }
    }

    override fun setHighlightedItem(file: OCFile) = ocFileListDelegate.setHighlightedItem(file)

    override fun setSortOrder(mFile: OCFile?, sortOrder: FileSortOrder) {
        if (searchType == SearchType.FAVORITE_SEARCH) {
            preferences.setSortOrder(FileSortOrder.Type.favoritesListView, sortOrder)
        } else {
            preferences.setSortOrder(mFile, sortOrder)
        }

        val sortedFiles = sortOrder.sortCloudFiles(
            differ.latestFiles.toMutableList(),
            preferences.isSortFoldersBeforeFiles,
            preferences.isSortFavoritesFirst
        )
        differ.replace(sortedFiles)

        displayedSortOrder = sortOrder
    }

    override fun addCheckedFile(file: OCFile) = ocFileListDelegate.addCheckedFile(file)

    override fun isCheckedFile(file: OCFile): Boolean = ocFileListDelegate.isCheckedFile(file)

    override fun getCheckedItems(): Set<OCFile> = ocFileListDelegate.checkedItems

    override fun removeCheckedFile(file: OCFile) = ocFileListDelegate.removeCheckedFile(file)

    override fun notifyItemChanged(file: OCFile) {
        val recommendedAdapter = visibleRecommendedFilesAdapterFor(file)
        if (recommendedAdapter != null) {
            recommendedAdapter.notifyItemChanged(recommendedAdapter.getItemPosition(file))
        } else {
            notifyItemChangedIfPresent(file)
        }
    }

    override fun getFilesCount(): Int = files.size

    override fun clearCheckedItems() = ocFileListDelegate.clearCheckedItems()

    override fun selectAll(value: Boolean) {
        if (value) {
            ocFileListDelegate.addToCheckedFiles(files)
        } else {
            clearCheckedItems()
        }
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        (callContext as ImageView).setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean =
        (callContext as ImageView).tag == tag

    override fun getPopupText(view: View, position: Int): String {
        val file = getItem(position)
        val sortOrder = displayedSortOrder
        if (file == null || sortOrder == null) {
            return ""
        }

        return when (sortOrder.getType()) {
            SortType.ALPHABET -> file.fileName.take(1).uppercase(Locale.getDefault())
            SortType.DATE -> popupDateFormat.format(Date(file.modificationTimestamp))
            SortType.SIZE -> HumanReadableFormatter.formatBytes(file.fileLength)
        }
    }
    // endregion

    // region Public methods
    fun getItem(position: Int): OCFile? = files.getOrNull(position - differ.headerOffset())

    fun bindRecommendedFilesHolder(holder: RecommendedFilesViewHolder, file: OCFile) = bindFile(holder, file)

    fun shouldShowHeader(): Boolean {
        val directory = currentDirectory
        return !(directory == null || MainApp.isOnlyOnDevice()) && (
            shouldShowRecommendedFiles() ||
                headerBinder.shouldShowOpenInNotes(directory) ||
                headerBinder.hasRichWorkspace(directory)
            )
    }

    fun updateAdapter(newFiles: List<OCFile>, directory: OCFile?) {
        Log_OC.d(TAG, "updating the adapter")

        allFiles.clear()
        allFiles.addAll(newFiles)

        activity.runOnUiThread {
            val isSameDirectoryRefresh = directory != null && directory == listedDirectory
            listedDirectory = directory

            if (directory != null) {
                currentDirectory = directory
            }

            differ.updateHeader()

            val files = newFiles.toList()
            if (isSameDirectoryRefresh) {
                differ.submitAndRebind(files)
            } else {
                differ.replace(files)
            }
        }
    }

    fun prepareForSearchData(searchType: SearchType) {
        this.searchType = searchType
        preferences.setPhotoSearchTimestamp(0)

        val type = when (searchType) {
            SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE
            SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY
            else -> VirtualFolderType.NONE
        }

        if (type != VirtualFolderType.GALLERY) {
            storageManager.deleteVirtuals(type)
        }
    }

    fun setDisplayedSortOrder(sortOrder: FileSortOrder?) {
        displayedSortOrder = sortOrder
    }

    fun setCheckedItem(files: Set<OCFile>?) = ocFileListDelegate.setCheckedItem(files)

    fun setFiles(files: List<OCFile>) = differ.submit(files.toList())

    fun getFileByRemoteId(remoteId: String?): OCFile? = allFiles.firstOrNull { it.remoteId == remoteId }

    fun insertFile(file: OCFile?) {
        if (file == null || file in allFiles) {
            return
        }

        allFiles.add(file)
        differ.submit(differ.latestFiles + file)
    }

    fun replaceFile(file: OCFile) {
        val index = differ.latestFiles.indexOf(file)
        if (index == RecyclerView.NO_POSITION) {
            Log_OC.d(TAG, "File cannot be found in adapter's files")
            return
        }

        differ.submit(differ.latestFiles.toMutableList().apply { set(index, file) })
        notifyItemChanged(file)
    }

    fun updateFile(updatedFile: OCFile) {
        val allIndex = helper.indexOfSameRemoteFile(allFiles, updatedFile)
        if (allIndex != RecyclerView.NO_POSITION) {
            allFiles[allIndex] = updatedFile
        }

        val newFiles = differ.latestFiles.toMutableList()
        val oldIndex = helper.indexOfSameRemoteFile(newFiles, updatedFile)
        if (oldIndex == RecyclerView.NO_POSITION) {
            return
        }

        newFiles.removeAt(oldIndex)
        newFiles.add(updatedFile)
        differ.submit(differ.sortForCurrentView(newFiles, searchType, currentDirectory))

        visibleRecommendedFilesAdapterFor(updatedFile)?.let { adapter ->
            val position = adapter.getItemPosition(updatedFile)
            if (position != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(position)
            }
        }
    }

    fun removeFile(file: OCFile) {
        allFiles.remove(file)
        differ.submit(differ.latestFiles - file)
    }

    fun removeAllFiles() {
        allFiles.clear()
        differ.submit(emptyList())
    }

    fun updateFavoriteState(remotePath: String?, isFavorite: Boolean, removeFromList: Boolean) {
        val filesToDelete = mutableListOf<OCFile>()

        val changedFile = differ.latestFiles.firstOrNull { it.remotePath == remotePath }?.also { file ->
            file.isFavorite = isFavorite
            if (removeFromList) {
                filesToDelete.add(file)
            }
        }

        allFiles.firstOrNull { it.remotePath == remotePath }?.let { file ->
            file.isFavorite = isFavorite
            storageManager.saveFile(file)
            if (removeFromList) {
                filesToDelete.add(file)
            }
        }

        if (currentDirectory?.remotePath == remotePath) {
            currentDirectory?.isFavorite = isFavorite
            storageManager.saveFile(currentDirectory)
        }

        mainHandler.post {
            val sortedFiles = differ.sortForCurrentView(
                differ.latestFiles.toMutableList(),
                searchType,
                currentDirectory
            )
            sortedFiles.removeAll(filesToDelete)
            differ.submit(sortedFiles)

            if (changedFile != null && !removeFromList) {
                notifyItemChangedIfPresent(changedFile)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun markCommentsAsRead(remoteId: String?) {
        files.firstOrNull { it.remoteId == remoteId }?.unreadCommentsCount = 0
        allFiles.firstOrNull { it.remoteId == remoteId }?.unreadCommentsCount = 0
        mainHandler.post { notifyDataSetChanged() }
    }

    fun updateFileEncryptionById(remoteId: String?, encrypted: Boolean) {
        val file = remoteId?.let { id -> allFiles.firstOrNull { it.remoteId == id } } ?: return

        file.isEncrypted = encrypted
        val e2eCounter = if (E2EVersionHelper.isV2Plus(capability)) {
            EncryptionUtils.E2E_V2_INITIAL_COUNTER
        } else {
            EncryptionUtils.E2E_V1_INITIAL_COUNTER
        }
        storageManager.updateE2EECounter(file, e2eCounter)
        notifyItemChangedIfPresent(file)
    }

    fun updateFileIndicator(iconId: Int, file: OCFile?) {
        file?.let { notifyItemChangedIfPresent(it, iconId) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateRecommendedFiles(value: List<OCFile>) {
        recommendedFiles.clear()
        recommendedFiles.addAll(value)
        differ.syncHeaderState()
        notifyDataSetChanged()
    }

    fun setGridView(bool: Boolean) {
        gridView = bool
    }

    fun setShowMetadata(bool: Boolean) {
        ocFileListDelegate.isMultiSelect = bool
    }

    @VisibleForTesting
    fun setShowShareAvatar(bool: Boolean) = ocFileListDelegate.setShowShareAvatar(bool)

    @VisibleForTesting
    fun setCurrentDirectory(folder: OCFile?) {
        currentDirectory = folder
        differ.syncHeaderState()
    }

    fun cleanup() {
        ocFileListDelegate.cleanup()
        helper.cleanup()
    }
    // endregion

    // region Private methods
    private fun viewTypeAt(position: Int): OCFileListViewType = when {
        isHeaderDisplayed && position == 0 -> OCFileListViewType.HEADER
        position - differ.headerOffset() == files.size -> OCFileListViewType.FOOTER
        getItem(position)?.let { MimeTypeUtil.isImageOrVideo(it) } == true -> OCFileListViewType.IMAGE
        else -> OCFileListViewType.ITEM
    }

    private fun bindHeader(holder: OCFileListHeaderViewHolder) {
        val visibleRecommendedFilesAdapter = if (shouldShowRecommendedFiles()) {
            RecommendedFilesAdapter(this, recommendedFiles).also { recommendedFilesAdapter = it }
        } else {
            null
        }
        headerBinder.bind(holder, currentDirectory, visibleRecommendedFilesAdapter)
    }

    private fun bindFileAt(holder: ListViewHolder, position: Int) {
        val file = getItem(position)
        if (file == null) {
            Log_OC.e(this, "Cannot bind on view holder on a null file")
            return
        }
        bindFile(holder, file)
    }

    private fun bindFile(holder: ListViewHolder, file: OCFile) {
        ocFileListDelegate.bindViewHolder(holder, file, currentDirectory, searchType, thumbnailGenerator)

        if (holder is ListItemViewHolder) {
            itemBinder.bindDetails(holder, file, user, storageManager, shouldShowItemOptions(), onlyOnDevice)
        }

        if (holder is ListGridItemViewHolder) {
            itemBinder.bindFilename(holder, file, storageManager.getFilenameConsideringOfflineOperation(file))
        }

        itemBinder.bindLivePhotoIndicator(holder, file)

        if (!MDMConfig.sharingSupport(activity)) {
            holder.shared.visibility = View.GONE
        }

        bindMoreOptionsVisibility(holder)
        holder.fileFeaturesLayout?.setVisibleIf(holder.hasVisibleFeatureIndicators)
    }

    private fun shouldShowItemOptions(): Boolean = !(isMultiSelect() || gridView || hideItemOptions)

    private fun bindMoreOptionsVisibility(holder: ListViewHolder) {
        val showMoreOptions = !isMultiSelect() && !OCFileListFragment.isMultipleFileSelectedForCopyOrMove

        if (holder is ListItemViewHolder) {
            holder.overflowMenu.setVisibleIf(showMoreOptions)
        } else {
            holder.more.setVisibleIf(showMoreOptions)
        }
    }

    private fun shouldShowRecommendedFiles(): Boolean =
        recommendedFiles.isNotEmpty() && currentDirectory?.isRootDirectory == true

    private fun visibleRecommendedFilesAdapterFor(file: OCFile): RecommendedFilesAdapter? =
        recommendedFilesAdapter?.takeIf { shouldShowRecommendedFiles() && file.isRecommendedFile }

    private fun notifyItemChangedIfPresent(file: OCFile, payload: Any? = null) {
        val position = getItemPosition(file)
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position, payload)
        }
    }
    // endregion

    companion object {
        private val TAG = OCFileListAdapter::class.java.simpleName

        private const val FOOTER_ITEM_COUNT = 1
    }
}
