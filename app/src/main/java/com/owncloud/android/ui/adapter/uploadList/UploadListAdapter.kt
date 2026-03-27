/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.getStatusText
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.extensions.sortedByUploadOrder
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListHeaderBinding
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.adapter.progressListener.UploadProgressListener
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Optional
import java.util.function.Consumer

@Suppress(
    "LongMethod",
    "TooManyFunctions",
    "LargeClass",
    "LongParameterList",
    "NestedBlockDepth",
    "MaxLineLength",
    "ReturnCount"
)
class UploadListAdapter(
    private val activity: FileActivity,
    private val uploadsStorageManager: UploadsStorageManager,
    private val storageManager: FileDataStorageManager,
    private val accountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val clock: Clock,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {

    private val uploadListSections = UploadListSection.sections()
    private val showUser: Boolean = accountManager.getAccounts().size > 1
    private val uploadHelper = FileUploadHelper.instance()
    private var uploadProgressListener: UploadProgressListener? = null
    private var notificationManager: NotificationManager? = null
    private val helper = UploadListAdapterHelper(activity)

    init {
        Log_OC.d(TAG, "UploadListAdapter")
        shouldShowHeadersForEmptySections(false)
    }

    internal class HeaderViewHolder(val binding: UploadListHeaderBinding) : SectionedViewHolder(binding.root)

    internal class ItemViewHolder(val binding: UploadListItemBinding) : SectionedViewHolder(binding.root)

    override fun getSectionCount(): Int = uploadListSections.size

    override fun getItemCount(section: Int): Int = uploadListSections[section].items.size

    // region header
    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        val headerViewHolder = holder as HeaderViewHolder
        val group = uploadListSections[section]

        bindHeaderTitle(headerViewHolder, group, section)
        bindHeaderActionButton(headerViewHolder, group)
        bindHeaderBatterySaverWarning(headerViewHolder)
        bindHeaderActionClickListener(headerViewHolder, group)
    }

    private fun bindHeaderTitle(holder: HeaderViewHolder, group: UploadListSection, section: Int) {
        val title = activity.getString(group.titleRes)
        val headerText = activity.getString(R.string.uploads_view_group_header)
        holder.binding.uploadListTitle.text = String.format(headerText, title, group.items.size)
        viewThemeUtils.platform.colorTextView(holder.binding.uploadListTitle)

        val toggleExpand = {
            toggleSectionExpanded(section)
            val icon = if (isSectionExpanded(section)) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            holder.binding.uploadListState.setImageResource(icon)
        }
        holder.binding.uploadListTitle.setOnClickListener { toggleExpand() }
        holder.binding.uploadListStateLayout.setOnClickListener { toggleExpand() }
    }

    private fun bindHeaderActionButton(holder: HeaderViewHolder, group: UploadListSection) {
        val iconRes = when (group.type) {
            UploadListType.CURRENT, UploadListType.COMPLETED -> R.drawable.ic_close
            UploadListType.CANCELLED, UploadListType.FAILED -> R.drawable.ic_dots_vertical
            else -> return
        }
        holder.binding.uploadListAction.setImageResource(iconRes)
    }

    private fun bindHeaderBatterySaverWarning(holder: HeaderViewHolder) {
        holder.binding.autoUploadBatterySaverWarningCard.root
            .setVisibleIf(powerManagementService.isPowerSavingEnabled)
        viewThemeUtils.material.themeCardView(holder.binding.autoUploadBatterySaverWarningCard.root)
    }

    private fun bindHeaderActionClickListener(holder: HeaderViewHolder, group: UploadListSection) {
        holder.binding.uploadListAction.setOnClickListener {
            when (group.type) {
                UploadListType.CURRENT -> cancelAllCurrentUploads(group)

                UploadListType.COMPLETED -> {
                    uploadsStorageManager.clearSuccessfulUploads()
                    loadUploadItemsFromDb()
                }

                UploadListType.FAILED -> showFailedPopupMenu(holder)

                UploadListType.CANCELLED -> showCancelledPopupMenu(holder)

                else -> {}
            }
        }
    }

    private fun cancelAllCurrentUploads(group: UploadListSection) {
        val items = group.items.takeIf { it.isNotEmpty() } ?: return
        val accountName = items[0].accountName
        var completedCount = 0
        items.forEach { upload ->
            uploadHelper.updateUploadStatus(
                upload.remotePath,
                accountName,
                UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED
            ) {
                FileUploadWorker.cancelUpload(upload.remotePath, accountName) {
                    completedCount++
                    if (completedCount == items.size) {
                        Log_OC.d(TAG, "refreshing upload items")
                        loadUploadItemsFromDb()
                    }
                }
            }
        }
    }

    private fun showFailedPopupMenu(holder: HeaderViewHolder) {
        PopupMenu(activity, holder.binding.uploadListAction).apply {
            inflate(R.menu.upload_list_failed_options)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload_list_failed_clear -> {
                        uploadsStorageManager.clearFailedButNotDelayedUploads()
                        clearTempEncryptedFolder()
                        loadUploadItemsFromDb()
                    }

                    R.id.action_upload_list_failed_retry ->
                        uploadHelper.retryFailedUploads(
                            uploadsStorageManager,
                            connectivityService,
                            accountManager,
                            powerManagementService
                        )
                }
                true
            }
            show()
        }
    }

    private fun showCancelledPopupMenu(holder: HeaderViewHolder) {
        PopupMenu(activity, holder.binding.uploadListAction).apply {
            inflate(R.menu.upload_list_cancelled_options)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload_list_cancelled_clear -> {
                        uploadsStorageManager.clearCancelledUploadsForCurrentAccount()
                        loadUploadItemsFromDb()
                        clearTempEncryptedFolder()
                    }

                    R.id.action_upload_list_cancelled_resume -> retryCancelledUploads()
                }
                true
            }
            show()
        }
    }

    private fun clearTempEncryptedFolder() {
        val user = activity.user
        user.ifPresent(
            Consumer { value: User? ->
                FileDataStorageManager.clearTempEncryptedFolder(value!!.accountName)
            }
        )
    }

    // FIXME For e2e resume is not working
    private fun retryCancelledUploads() {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val showNotExistMessage = uploadHelper.retryCancelledUploads(
                uploadsStorageManager,
                connectivityService,
                accountManager,
                powerManagementService
            )
            if (showNotExistMessage) {
                withContext(Dispatchers.Main) {
                    DisplayUtils.showSnackMessage(activity, R.string.upload_action_file_not_exist_message)
                }
            }
        }
    }
    // endregion

    // region content
    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (uploadListSections.isEmpty() || section !in uploadListSections.indices) return
        val item = uploadListSections[section].items[relativePosition]
        val itemViewHolder = holder as ItemViewHolder

        bindItemText(holder, item)
        bindItemStatus(itemViewHolder, item)
        bindItemActions(itemViewHolder, item)
        bindItemThumbnail(itemViewHolder, item)
    }

    @SuppressLint("SetTextI18n")
    private fun bindItemText(holder: ItemViewHolder, item: OCUpload) {
        val remoteFile = File(item.remotePath)
        val fileName = remoteFile.name.takeIf { it.isNotEmpty() } ?: File.separator
        holder.binding.uploadName.text = fileName
        holder.binding.uploadRemotePath.text = File(item.remotePath).parent

        val updateTime = item.uploadEndTimestamp
        if (item.fileSize != 0L) {
            var fileSizeFormat = "%s "

            // we have valid update time so we can show the upload date
            if (updateTime > 0) {
                fileSizeFormat = "%s, "
            }

            val fileSizeInBytes = DisplayUtils.bytesToHumanReadable(item.fileSize)
            val uploadFileSize = String.format(fileSizeFormat, fileSizeInBytes)
            holder.binding.uploadFileSize.text = uploadFileSize
        } else {
            holder.binding.uploadFileSize.text = ""
        }

        bindItemDate(holder, item, updateTime)
        bindItemAccount(holder, item)
    }

    private fun bindItemDate(holder: ItemViewHolder, item: OCUpload, updateTime: Long) {
        val showDate = (
            updateTime > 0 &&
                item.uploadStatus == UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED &&
                item.lastResult == UploadResult.UPLOADED
            )

        holder.binding.uploadDate.setVisibleIf(showDate)

        if (showDate) {
            holder.binding.uploadDate.text = DisplayUtils.getRelativeDateTimeString(
                activity,
                updateTime,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            )
        }
    }

    private fun bindItemAccount(holder: ItemViewHolder, item: OCUpload) {
        if (showUser) {
            holder.binding.uploadAccount.visibility = View.VISIBLE
            val optionalUser = accountManager.getUser(item.accountName)
            holder.binding.uploadAccount.text = if (optionalUser.isPresent) {
                DisplayUtils.getAccountNameDisplayText(optionalUser.get())
            } else {
                item.accountName
            }
        } else {
            holder.binding.uploadAccount.visibility = View.GONE
        }
    }

    private fun bindItemStatus(holder: ItemViewHolder, item: OCUpload) {
        holder.binding.run {
            uploadRemotePath.visibility = View.VISIBLE
            uploadFileSize.visibility = View.VISIBLE
            uploadStatus.visibility = View.VISIBLE
            uploadProgressBar.visibility = View.GONE

            val status = item.getStatusText(
                activity,
                activity.appPreferences.isGlobalUploadPaused,
                uploadHelper.isUploadingNow(item)
            )
            when (item.uploadStatus) {
                UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS -> bindItemInProgress(holder, item)

                UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED,
                UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED -> uploadStatus.visibility = View.GONE

                else -> {}
            }

            // Override visibility for edge cases
            if ((
                    item.uploadStatus == UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED &&
                        item.lastResult != UploadResult.UPLOADED
                    ) ||
                item.uploadStatus == UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED
            ) {
                uploadStatus.visibility = View.VISIBLE
                uploadFileSize.visibility = View.GONE
            }

            uploadStatus.text = status
        }
    }

    private fun bindItemInProgress(holder: ItemViewHolder, item: OCUpload) {
        holder.binding.run {
            viewThemeUtils.platform.themeHorizontalProgressBar(uploadProgressBar)
            uploadProgressBar.progress = 0
            uploadProgressBar.visibility = View.VISIBLE
            uploadFileSize.visibility = View.GONE

            if (uploadHelper.isUploadingNow(item)) {
                uploadProgressListener?.upload?.let { prevUpload ->
                    val key = FileUploadHelper.buildRemoteName(prevUpload.accountName, prevUpload.remotePath)
                    uploadHelper.removeUploadTransferProgressListener(uploadProgressListener!!, key)
                }
                uploadProgressListener = UploadProgressListener(item, uploadProgressBar)
                uploadHelper.addUploadTransferProgressListener(
                    uploadProgressListener!!,
                    FileUploadHelper.buildRemoteName(item.accountName, item.remotePath)
                )
            } else if (uploadProgressListener?.isWrapping(uploadProgressBar) == true) {
                uploadProgressListener?.upload?.let { prevUpload ->
                    val key = FileUploadHelper.buildRemoteName(prevUpload.accountName, prevUpload.remotePath)
                    uploadHelper.removeUploadTransferProgressListener(uploadProgressListener!!, key)
                    uploadProgressListener = null
                }
            }

            uploadProgressBar.invalidate()
        }
    }

    private fun bindItemActions(holder: ItemViewHolder, item: OCUpload) {
        holder.binding.run {
            val optionalUser = accountManager.getUser(item.accountName)
            val status = item.getStatusText(
                activity,
                activity.appPreferences.isGlobalUploadPaused,
                uploadHelper.isUploadingNow(item)
            )

            // Right-side button
            when (item.uploadStatus) {
                UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS -> {
                    uploadRightButton.run {
                        setImageResource(R.drawable.ic_action_cancel_grey)
                        visibility = View.VISIBLE
                        setOnClickListener {
                            uploadHelper.updateUploadStatus(
                                item.remotePath,
                                item.accountName,
                                UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED
                            ) {
                                FileUploadWorker.cancelUpload(
                                    item.remotePath,
                                    item.accountName
                                ) { loadUploadItemsFromDb() }
                            }
                        }
                    }
                }

                UploadsStorageManager.UploadStatus.UPLOAD_FAILED -> {
                    uploadRightButton.run {
                        if (item.lastResult == UploadResult.SYNC_CONFLICT) {
                            setImageResource(R.drawable.ic_dots_vertical)
                            setOnClickListener { view ->
                                optionalUser.ifPresent { user ->
                                    showItemConflictPopup(user, holder, item, status, view)
                                }
                            }
                        } else {
                            setImageResource(R.drawable.ic_action_delete_grey)
                            setOnClickListener { removeUpload(item) }
                        }
                        visibility = View.VISIBLE
                    }
                }

                else -> uploadRightButton.visibility = View.INVISIBLE
            }

            // Row click
            uploadListItemLayout.run {
                setOnClickListener(null)
                when (item.uploadStatus) {
                    UploadsStorageManager.UploadStatus.UPLOAD_FAILED,
                    UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED ->
                        setOnClickListener {
                            onFailedOrCancelledItemClick(item, optionalUser, holder, status)
                        }

                    UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED ->
                        setOnClickListener { helper.onUploadedItemClick(item) }

                    else -> {}
                }
            }

            // Thumbnail click to open locally
            if (item.uploadStatus != UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED) {
                thumbnail.setOnClickListener { helper.onUploadingItemClick(item) }
            }
        }
    }

    private fun onFailedOrCancelledItemClick(
        item: OCUpload,
        optionalUser: Optional<User>,
        holder: ItemViewHolder,
        status: String
    ) {
        when (item.lastResult) {
            UploadResult.CREDENTIAL_ERROR -> {
                val user = optionalUser.orElseThrow { RuntimeException() }
                activity.fileOperationsHelper.checkCurrentCredentials(user)
            }

            UploadResult.SYNC_CONFLICT if optionalUser.isPresent -> {
                if (checkAndOpenConflictResolutionDialog(optionalUser.get(), holder, item, status)) return
                retryOrShowError(item)
            }

            else -> retryOrShowError(item)
        }
    }

    private fun retryOrShowError(item: OCUpload) {
        val user = accountManager.getUser(item.accountName)
        if (user.isEmpty) return

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val file = item.localPath.toFile()

            withContext(Dispatchers.Main) {
                if (file != null) {
                    uploadHelper.retryUpload(item, user.get())
                } else {
                    DisplayUtils.showSnackMessage(
                        activity,
                        R.string.local_file_not_found_message
                    )
                }
            }
        }
    }

    private fun bindItemThumbnail(holder: ItemViewHolder, item: OCUpload) {
        holder.binding.thumbnail.setImageResource(R.drawable.file)

        val fakeFile = OCFile(item.remotePath).apply {
            setStoragePath(item.localPath)
            mimeType = item.mimeType
        }

        val allowedToCreateNewThumbnail =
            ThumbnailsCacheManager.cancelPotentialThumbnailWork(fakeFile, holder.binding.thumbnail)

        val optionalUser = accountManager.getUser(item.accountName)
        val fileName = File(item.remotePath).name.takeIf { it.isNotEmpty() } ?: File.separator

        when {
            MimeTypeUtil.isImage(fakeFile) && fakeFile.remoteId != null &&
                item.uploadStatus == UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED ->
                bindRemoteThumbnail(holder, item, fakeFile, allowedToCreateNewThumbnail)

            MimeTypeUtil.isImage(fakeFile) ->
                bindLocalThumbnail(holder, item, allowedToCreateNewThumbnail)

            optionalUser.isPresent -> {
                val icon = MimeTypeUtil.getFileTypeIcon(item.mimeType, fileName, activity, viewThemeUtils)
                holder.binding.thumbnail.setImageDrawable(icon)
            }
        }
    }

    private fun bindRemoteThumbnail(
        holder: ItemViewHolder,
        item: OCUpload,
        fakeFile: OCFile,
        allowedToCreateNewThumbnail: Boolean
    ) {
        val cacheKey = fakeFile.remoteId.toString()
        var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(cacheKey)

        if (thumbnail != null && !fakeFile.isUpdateThumbnailNeeded) {
            holder.binding.thumbnail.setImageBitmap(thumbnail)
        } else if (allowedToCreateNewThumbnail) {
            val user = activity.user
            if (user.isPresent) {
                val task = ThumbnailsCacheManager.ThumbnailGenerationTask(
                    holder.binding.thumbnail,
                    activity.storageManager,
                    user.get()
                )
                thumbnail = thumbnail ?: if (MimeTypeUtil.isVideo(fakeFile)) {
                    ThumbnailsCacheManager.mDefaultVideo
                } else {
                    ThumbnailsCacheManager.mDefaultImg
                }
                holder.binding.thumbnail.setImageDrawable(
                    ThumbnailsCacheManager.AsyncThumbnailDrawable(activity.resources, thumbnail, task)
                )
                task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(fakeFile, null))
            }
        }

        if (item.mimeType == "image/png") {
            holder.binding.thumbnail.setBackgroundColor(ContextCompat.getColor(activity, R.color.bg_default))
        }
    }

    private fun bindLocalThumbnail(holder: ItemViewHolder, item: OCUpload, allowedToCreateNewThumbnail: Boolean) {
        val file = File(item.localPath)
        val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.hashCode().toString())

        if (thumbnail != null) {
            holder.binding.thumbnail.setImageBitmap(thumbnail)
        } else if (allowedToCreateNewThumbnail) {
            getThumbnailFromFileTypeAndSetIcon(item.localPath, holder)
            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(holder.binding.thumbnail)
            val defaultThumbnail = if (MimeTypeUtil.isVideo(file)) {
                ThumbnailsCacheManager.mDefaultVideo
            } else {
                ThumbnailsCacheManager.mDefaultImg
            }
            val asyncDrawable =
                ThumbnailsCacheManager.AsyncThumbnailDrawable(activity.resources, defaultThumbnail, task)
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null))
            task.setListener(object : ThumbnailsCacheManager.ThumbnailGenerationTask.Listener {
                override fun onSuccess() {
                    holder.binding.thumbnail.setImageDrawable(asyncDrawable)
                }

                override fun onError() {
                    getThumbnailFromFileTypeAndSetIcon(item.localPath, holder)
                }
            })
            Log_OC.v(TAG, "Executing task to generate a new thumbnail")
        }

        if (item.mimeType.equals("image/png", ignoreCase = true)) {
            holder.binding.thumbnail.setBackgroundColor(ContextCompat.getColor(activity, R.color.bg_default))
        }
    }
    // endregion

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) = Unit

    private fun getThumbnailFromFileTypeAndSetIcon(localPath: String?, itemViewHolder: ItemViewHolder) {
        val drawable = MimeTypeUtil.getIcon(localPath, activity, viewThemeUtils) ?: return
        itemViewHolder.binding.thumbnail.setImageDrawable(drawable)
    }

    private fun checkAndOpenConflictResolutionDialog(
        user: User?,
        itemViewHolder: ItemViewHolder,
        item: OCUpload,
        status: String?
    ): Boolean {
        val remotePath = item.remotePath
        val localFile = storageManager.getFileByEncryptedRemotePath(remotePath)

        if (localFile == null) {
            // Remote file doesn't exist, try to refresh folder
            val folder = storageManager.getFileByEncryptedRemotePath(File(remotePath).getParent() + "/")

            if (folder != null && folder.isFolder) {
                refreshFolderAndUpdateUI(itemViewHolder, user, folder, remotePath, item, status)
                return true
            }

            // Destination folder doesn't exist anymore
        }

        if (localFile != null) {
            helper.openConflictActivity(localFile, item)
            return true
        }

        // Remote file doesn't exist anymore = there is no more conflict
        return false
    }

    private fun refreshFolderAndUpdateUI(
        holder: ItemViewHolder,
        user: User?,
        folder: OCFile?,
        remotePath: String?,
        item: OCUpload,
        status: String?
    ) {
        refreshFolder(
            holder,
            user,
            folder
        ) { _: RemoteOperation<*>?, result: RemoteOperationResult<*>? ->
            holder.binding.uploadStatus.text = status
            if (result?.isSuccess == true) {
                val fileOnServer = storageManager.getFileByEncryptedRemotePath(remotePath)
                if (fileOnServer != null) {
                    helper.openConflictActivity(fileOnServer, item)
                } else {
                    displayFileNotFoundError(holder.itemView, activity)
                }
            }
        }
    }

    private fun displayFileNotFoundError(itemView: View?, context: Context) {
        val message = context.getString(R.string.uploader_file_not_found_message)
        DisplayUtils.showSnackMessage(itemView, message)
    }

    private fun showItemConflictPopup(
        user: User?,
        holder: ItemViewHolder,
        item: OCUpload,
        status: String?,
        view: View?
    ) {
        PopupMenu(activity, view).apply {
            inflate(R.menu.upload_list_item_file_conflict)
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.action_upload_list_resolve_conflict) {
                    checkAndOpenConflictResolutionDialog(user, holder, item, status)
                } else {
                    removeUpload(item)
                }
                true
            }
            show()
        }
    }

    fun removeUpload(item: OCUpload?) {
        uploadsStorageManager.removeUpload(item)
        cancelOldErrorNotification(item)
        loadUploadItemsFromDb()
    }

    private fun refreshFolder(view: ItemViewHolder, user: User?, folder: OCFile?, listener: OnRemoteOperationListener) {
        view.binding.uploadListItemLayout.isClickable = false
        view.binding.uploadStatus.setText(R.string.uploads_view_upload_status_fetching_server_version)
        RefreshFolderOperation(
            folder,
            clock.currentTime,
            false,
            false,
            true,
            storageManager,
            user,
            activity
        )
            .execute(user, activity, { caller, result ->
                view.binding.uploadListItemLayout.isClickable = true
                listener.onRemoteOperationFinish(caller, result)
            }, activity.handler)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(
                UploadListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            ItemViewHolder(
                UploadListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    @SuppressLint("NotifyDataSetChanged")
    @JvmOverloads
    fun loadUploadItemsFromDb(onCompleted: Runnable = {}) {
        val optionalUser = activity.user
        val optionalCapabilities = activity.capabilities
        if (optionalUser.isEmpty || optionalCapabilities.isEmpty) return

        val accountName = optionalUser.get().accountName
        val capabilities = optionalCapabilities.get()

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val updatedSections = uploadListSections.map { sec ->
                val uploads = uploadHelper.getUploadsByStatus(
                    accountName,
                    sec.status!!,
                    capabilities,
                    sec.collisionPolicy
                )
                uploads.forEach { it.setDataFixed(uploadHelper) }
                sec.withItems(uploads.sortedByUploadOrder())
            }

            withContext(Dispatchers.Main) {
                for (i in uploadListSections.indices) {
                    uploadListSections[i] = updatedSections[i]
                }
                notifyDataSetChanged()
                onCompleted.run()
            }
        }
    }

    fun cancelOldErrorNotification(upload: OCUpload?) {
        if (notificationManager == null) {
            notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }

        if (upload == null) {
            return
        }

        notificationManager?.cancel(upload.uploadId.toInt())
    }

    companion object {
        private val TAG: String = UploadListAdapter::class.java.getSimpleName()
    }
}
