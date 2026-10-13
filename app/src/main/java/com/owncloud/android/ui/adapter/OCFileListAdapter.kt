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
package com.owncloud.android.ui.adapter

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemApp
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.avatar.AvatarGenerationListener
import com.nextcloud.utils.avatar.AvatarGenerator
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
import com.owncloud.android.databinding.ListHeaderOpenInBinding
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
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.filelist.OCFileListAdapterDataProvider
import com.owncloud.android.ui.adapter.filelist.OCFileListAdapterHelper
import com.owncloud.android.ui.adapter.filelist.OCFileListDiffer
import com.owncloud.android.ui.adapter.filelist.OCFileListItemBinder
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewTextFragment
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

/**
 * This Adapter populates a RecyclerView with all files and folders in a Nextcloud instance.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class OCFileListAdapter(
    private val activity: Activity,
    private var user: User,
    private val preferences: AppPreferences,
    syncedFolderProvider: SyncedFolderProvider?,
    transferServiceGetter: ComponentsGetter,
    private val ocFileListFragmentInterface: OCFileListFragmentInterface,
    private val hideItemOptions: Boolean,
    private var gridView: Boolean,
    private val viewThemeUtils: ViewThemeUtils,
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

    private val dateFormat = SimpleDateFormat(POPUP_DATE_FORMAT, Locale.getDefault())
    private val footerId = UUID.randomUUID().leastSignificantBits
    private val headerId = UUID.randomUUID().leastSignificantBits
    private val helper = OCFileListAdapterHelper()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val differ = OCFileListDiffer(this, preferences, ::shouldShowHeader)

    private var currentDirectory: OCFile? = null
    private var listedDirectory: OCFile? = null
    private var onlyOnDevice = false
    private var sortOrder: FileSortOrder? = null
    private var searchType: SearchType? = null

    private var storageManager: FileDataStorageManager =
        transferServiceGetter.storageManager ?: FileDataStorageManager(user, activity.contentResolver)
    private var adapterDataProvider: OCFileListAdapterDataProvider = OCFileListAdapterDataProviderImpl(storageManager)
    private val capability: OCCapability = CapabilityUtils.getCapability(user, activity)
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

    override fun isMultiSelect(): Boolean = ocFileListDelegate.isMultiSelect

    @SuppressLint("NotifyDataSetChanged")
    override fun setMultiSelect(isMultiSelect: Boolean) {
        ocFileListDelegate.isMultiSelect = isMultiSelect
        notifyDataSetChanged()
    }

    override fun removeCheckedFile(file: OCFile) = ocFileListDelegate.removeCheckedFile(file)

    override fun selectAll(value: Boolean) {
        if (value) {
            ocFileListDelegate.addToCheckedFiles(files)
        } else {
            clearCheckedItems()
        }
    }

    override fun getItemPosition(file: OCFile): Int {
        val position = files.indexOf(file)
        return if (position == RecyclerView.NO_POSITION) position else position + differ.headerOffset()
    }

    fun setFavoriteAttributeForItemID(remotePath: String?, favorite: Boolean, removeFromList: Boolean) {
        val filesToDelete = mutableListOf<OCFile>()

        val changedFile = differ.latestFiles.firstOrNull { it.remotePath == remotePath }?.also { file ->
            file.isFavorite = favorite
            if (removeFromList) {
                filesToDelete.add(file)
            }
        }

        allFiles.firstOrNull { it.remotePath == remotePath }?.let { file ->
            file.isFavorite = favorite
            storageManager.saveFile(file)
            if (removeFromList) {
                filesToDelete.add(file)
            }
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
    fun refreshCommentsCount(fileId: String?) {
        files.firstOrNull { it.remoteId == fileId }?.unreadCommentsCount = 0
        allFiles.firstOrNull { it.remoteId == fileId }?.unreadCommentsCount = 0
        mainHandler.post { notifyDataSetChanged() }
    }

    fun updateFileEncryptionById(fileId: String?, encrypted: Boolean) {
        val file = fileId?.let { id -> allFiles.firstOrNull { it.remoteId == id } } ?: return

        file.isEncrypted = encrypted
        val e2eCounter = if (E2EVersionHelper.isV2Plus(capability)) {
            EncryptionUtils.E2E_V2_INITIAL_COUNTER
        } else {
            EncryptionUtils.E2E_V1_INITIAL_COUNTER
        }
        storageManager.updateE2EECounter(file, e2eCounter)
        notifyItemChangedIfPresent(file)
    }

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

    override fun getItemCount(): Int = files.size + differ.headerOffset() + FOOTER_ITEM_COUNT

    fun getItem(position: Int): OCFile? = files.getOrNull(position - differ.headerOffset())

    override fun getItemViewType(position: Int): Int = when {
        isHeaderDisplayed && position == 0 -> VIEW_TYPE_HEADER
        position - differ.headerOffset() == files.size -> VIEW_TYPE_FOOTER
        getItem(position)?.let { MimeTypeUtil.isImageOrVideo(it) } == true -> VIEW_TYPE_IMAGE
        else -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when {
            viewType == VIEW_TYPE_FOOTER -> OCFileListFooterViewHolder(
                ListFooterBinding.inflate(inflater, parent, false)
            )

            viewType == VIEW_TYPE_HEADER -> OCFileListHeaderViewHolder(
                ListHeaderBinding.inflate(inflater, parent, false)
            )

            gridView -> OCFileListGridItemViewHolder(GridItemBinding.inflate(inflater, parent, false))

            else -> OCFileListItemViewHolder(ListItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OCFileListFooterViewHolder -> bindFooter(holder)
            is OCFileListHeaderViewHolder -> bindHeader(holder)
            else -> bindItem(holder as ListViewHolder, position)
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

    fun bindRecommendedFilesHolder(holder: OCFileListRecommendedItemViewHolder, file: OCFile) =
        bindListViewHolder(holder, file)

    private fun bindFooter(holder: OCFileListFooterViewHolder) {
        holder.footerText.text = footerText()
        viewThemeUtils.platform.colorCircularProgressBar(holder.loadingProgressBar, ColorRole.ON_SURFACE_VARIANT)
        holder.loadingProgressBar.setVisibleIf(ocFileListFragmentInterface.isLoading)
    }

    private fun bindHeader(holder: OCFileListHeaderViewHolder) {
        holder.headerView.setOnClickListener { ocFileListFragmentInterface.onHeaderClicked() }
        bindRichWorkspace(holder)
        bindRecommendedFiles(holder.binding)
        bindOpenInNotes(holder.binding.openIn)
    }

    private fun bindRichWorkspace(holder: OCFileListHeaderViewHolder) {
        val text = currentDirectory?.richWorkspace
        PreviewTextFragment.setText(
            holder.headerText,
            text,
            null,
            activity,
            ignoreMimetype = true,
            preview = true,
            viewThemeUtils
        )
        holder.headerText.setVisibleIf(isRichWorkspaceTextVisible(text))
    }

    private fun isRichWorkspaceTextVisible(text: String?): Boolean =
        text != null && text != EMPTY_RICH_WORKSPACE && text.any { !Character.isWhitespace(it) }

    private fun bindRecommendedFiles(binding: ListHeaderBinding) {
        val showRecommendedFiles = shouldShowRecommendedFiles()
        binding.recommendedFilesRecyclerView.setVisibleIf(showRecommendedFiles)
        binding.recommendedFilesTitle.setVisibleIf(showRecommendedFiles)
        binding.allFilesTitle.setVisibleIf(showRecommendedFiles)

        if (!showRecommendedFiles) {
            return
        }

        binding.recommendedFilesRecyclerView.run {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = RecommendedFilesAdapter(this@OCFileListAdapter, recommendedFiles)
                .also { recommendedFilesAdapter = it }
        }
    }

    private fun bindOpenInNotes(binding: ListHeaderOpenInBinding) {
        val showOpenInNotes = shouldShowOpenInNotes()
        binding.root.setVisibleIf(showOpenInNotes)

        if (!showOpenInNotes) {
            return
        }

        viewThemeUtils.files.themeFilledCardView(binding.infoCard)
        binding.infoText.text = activity.getString(R.string.folder_best_viewed_in)
            .format(activity.getString(R.string.ecosystem_apps_notes))
        binding.openInButton.text = activity.getString(R.string.open_in_app)
            .format(activity.getString(R.string.ecosystem_apps_display_notes))

        val ecosystemManager = (activity as? DrawerActivity)?.ecosystemManager ?: return
        binding.openInButton.setOnClickListener { ecosystemManager.openApp(EcosystemApp.NOTES, user.accountName) }
    }

    private fun bindItem(holder: ListViewHolder, position: Int) {
        val file = getItem(position)
        if (file == null) {
            Log_OC.e(this, "Cannot bind on view holder on a null file")
            return
        }
        bindListViewHolder(holder, file)
    }

    private fun bindListViewHolder(holder: ListViewHolder, file: OCFile) {
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

        bindMoreOptionVisibility(holder)
        holder.fileFeaturesLayout?.setVisibleIf(holder.hasVisibleFeatureIndicators)
    }

    private fun shouldShowItemOptions(): Boolean = !(isMultiSelect() || gridView || hideItemOptions)

    private fun bindMoreOptionVisibility(holder: ListViewHolder) {
        val showMoreOptions = !isMultiSelect() && !OCFileListFragment.isMultipleFileSelectedForCopyOrMove

        if (holder is ListItemViewHolder) {
            holder.overflowMenu.setVisibleIf(showMoreOptions)
        } else {
            holder.more.setVisibleIf(showMoreOptions)
        }
    }

    private fun shouldShowRecommendedFiles(): Boolean =
        recommendedFiles.isNotEmpty() && currentDirectory?.isRootDirectory == true

    private fun shouldShowOpenInNotes(): Boolean {
        if (!preferences.isShowEcosystemApps) {
            return false
        }

        val notesFolderPath = capability.notesFolderPath
        val currentPath = currentDirectory?.decryptedRemotePath
        return notesFolderPath != null && currentPath?.startsWith(notesFolderPath) == true
    }

    private fun visibleRecommendedFilesAdapterFor(file: OCFile): RecommendedFilesAdapter? =
        recommendedFilesAdapter?.takeIf { shouldShowRecommendedFiles() && file.isRecommendedFile }

    @SuppressLint("NotifyDataSetChanged")
    fun updateRecommendedFiles(value: List<OCFile>) {
        recommendedFiles.clear()
        recommendedFiles.addAll(value)
        differ.syncHeaderState()
        notifyDataSetChanged()
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

    private fun footerText(): String {
        val showHiddenFiles = preferences.isShowHiddenFilesEnabled
        val foldersCount = files.count { it.isFolder }
        val filesCount = files.count { !it.isFolder && (!it.isHidden || showHiddenFiles) }
        val resources = activity.resources

        return listOfNotNull(
            filesCount.takeIf { it > 0 }?.let {
                resources.getQuantityString(R.plurals.file_list__footer__file, it, it)
            },
            foldersCount.takeIf { it > 0 }?.let {
                resources.getQuantityString(R.plurals.file_list__footer__folder, it, it)
            }
        ).joinToString(FOOTER_TEXT_SEPARATOR)
    }

    fun shouldShowHeader(): Boolean {
        val directory = currentDirectory
        if (directory == null || MainApp.isOnlyOnDevice()) {
            return false
        }

        return shouldShowRecommendedFiles() ||
            shouldShowOpenInNotes() ||
            directory.richWorkspace?.any { it > ' ' } == true
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory        New folder to adapt.
     * @param storageManager   Replaces the current storage manager when it differs from it.
     * @param mLimitToMimeType show only files of this mimeType
     */
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

    fun setSortOrder(newSortOrder: FileSortOrder?) {
        sortOrder = newSortOrder
    }

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

        this.sortOrder = sortOrder
    }

    override fun getCheckedItems(): Set<OCFile> = ocFileListDelegate.checkedItems

    fun setCheckedItem(files: Set<OCFile>?) = ocFileListDelegate.setCheckedItem(files)

    override fun clearCheckedItems() = ocFileListDelegate.clearCheckedItems()

    fun setFiles(files: List<OCFile>) = differ.submit(files.toList())

    fun replaceFile(file: OCFile) {
        val index = differ.latestFiles.indexOf(file)
        if (index == RecyclerView.NO_POSITION) {
            Log_OC.d(TAG, "File cannot be found in adapter's files")
            return
        }

        differ.submit(differ.latestFiles.toMutableList().apply { set(index, file) })
        notifyItemChanged(file)
    }

    fun getFileByRemoteId(fileId: String?): OCFile? = allFiles.firstOrNull { it.remoteId == fileId }

    fun insertFile(file: OCFile?) {
        if (file == null || file in allFiles) {
            return
        }

        allFiles.add(file)
        differ.submit(differ.latestFiles + file)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        differ.attach(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        differ.detach()
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        (callContext as ImageView).setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean =
        (callContext as ImageView).tag == tag

    override fun isCheckedFile(file: OCFile): Boolean = ocFileListDelegate.isCheckedFile(file)

    override fun addCheckedFile(file: OCFile) = ocFileListDelegate.addCheckedFile(file)

    override fun setHighlightedItem(file: OCFile) = ocFileListDelegate.setHighlightedItem(file)

    override fun cancelAllPendingTasks() = thumbnailGenerator.fileThumbnailGenerator.cancelPendingTasks()

    fun setGridView(bool: Boolean) {
        gridView = bool
    }

    fun setShowMetadata(bool: Boolean) {
        ocFileListDelegate.isMultiSelect = bool
    }

    override fun getPopupText(view: View, position: Int): String {
        val file = getItem(position)
        val sortOrder = sortOrder
        if (file == null || sortOrder == null) {
            return ""
        }

        return when (sortOrder.getType()) {
            SortType.ALPHABET -> file.fileName.take(1).uppercase(Locale.getDefault())
            SortType.DATE -> dateFormat.format(Date(file.modificationTimestamp))
            SortType.SIZE -> HumanReadableFormatter.formatBytes(file.fileLength)
        }
    }

    @VisibleForTesting
    fun setShowShareAvatar(bool: Boolean) = ocFileListDelegate.setShowShareAvatar(bool)

    override fun getFilesCount(): Int = files.size

    override fun notifyItemChanged(file: OCFile) {
        val recommendedAdapter = visibleRecommendedFilesAdapterFor(file)
        if (recommendedAdapter != null) {
            recommendedAdapter.notifyItemChanged(recommendedAdapter.getItemPosition(file))
        } else {
            notifyItemChangedIfPresent(file)
        }
    }

    private fun notifyItemChangedIfPresent(file: OCFile, payload: Any? = null) {
        val position = getItemPosition(file)
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position, payload)
        }
    }

    @VisibleForTesting
    fun setCurrentDirectory(folder: OCFile?) {
        currentDirectory = folder
        differ.syncHeaderState()
    }

    fun updateFileIndicator(iconId: Int, file: OCFile?) {
        file?.let { notifyItemChangedIfPresent(it, iconId) }
    }

    fun cleanup() {
        ocFileListDelegate.cleanup()
        helper.cleanup()
    }

    fun removeAllFiles() {
        allFiles.clear()
        differ.submit(emptyList())
    }

    fun removeFile(file: OCFile) {
        allFiles.remove(file)
        differ.submit(differ.latestFiles - file)
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

    companion object {
        private val TAG = OCFileListAdapter::class.java.simpleName

        private const val VIEW_TYPE_FOOTER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_IMAGE = 2
        private const val VIEW_TYPE_HEADER = 3

        private const val FOOTER_ITEM_COUNT = 1
        private const val FOOTER_TEXT_SEPARATOR = ", "
        private const val EMPTY_RICH_WORKSPACE = " "
        private const val POPUP_DATE_FORMAT = "MMMM yyyy"
    }
}
