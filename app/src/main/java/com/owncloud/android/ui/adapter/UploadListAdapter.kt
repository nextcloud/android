/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.buildRemoteName
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.instance
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.cancelUpload
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.extensions.sortedByUploadOrder
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListHeaderBinding
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity.Companion.createIntent
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FileDisplayActivity.Companion.openFileIntent
import com.owncloud.android.ui.adapter.progressListener.UploadProgressListener
import com.owncloud.android.ui.preview.PreviewImageFragment.Companion.canBePreviewed
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.function.Consumer

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList", "NestedBlockDepth", "MaxLineLength", "ReturnCount")
class UploadListAdapter(
    private val fileActivity: FileActivity,
    private val uploadsStorageManager: UploadsStorageManager,
    private val storageManager: FileDataStorageManager,
    private val accountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val clock: Clock,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedRecyclerViewAdapter<SectionedViewHolder?>() {

    private data class Section(
        val type: Type?,
        val titleRes: Int,
        val status: UploadStatus?,
        val collisionPolicy: NameCollisionPolicy?,
        val items: List<OCUpload>
    ) {
        fun withItems(newItems: List<OCUpload>) = copy(items = newItems)
    }

    internal enum class Type { CURRENT, COMPLETED, FAILED, CANCELLED, SKIPPED }

    internal class HeaderViewHolder(val binding: UploadListHeaderBinding) : SectionedViewHolder(binding.root)

    internal class ItemViewHolder(val binding: UploadListItemBinding) : SectionedViewHolder(binding.root)

    private val sections: MutableList<Section> = ArrayList<Section>(
        listOf(
            Section(
                Type.CURRENT,
                R.string.uploads_view_group_current_uploads,
                UploadStatus.UPLOAD_IN_PROGRESS,
                null,
                listOf()
            ),
            Section(
                Type.FAILED,
                R.string.uploads_view_group_failed_uploads,
                UploadStatus.UPLOAD_FAILED,
                null,
                listOf()
            ),
            Section(
                Type.CANCELLED,
                R.string.uploads_view_group_manually_cancelled_uploads,
                UploadStatus.UPLOAD_CANCELLED,
                null,
                listOf()
            ),
            Section(
                Type.COMPLETED,
                R.string.uploads_view_group_completed_uploads,
                UploadStatus.UPLOAD_SUCCEEDED,
                NameCollisionPolicy.ASK_USER,
                listOf()
            ),
            Section(
                Type.SKIPPED,
                R.string.uploads_view_upload_status_skip,
                UploadStatus.UPLOAD_SUCCEEDED,
                NameCollisionPolicy.SKIP,
                listOf()
            )
        )
    )

    private val parentActivity: FileActivity = fileActivity
    private val showUser: Boolean = accountManager.getAccounts().size > 1
    private val uploadHelper = instance()
    private var uploadProgressListener: UploadProgressListener? = null
    private var mNotificationManager: NotificationManager? = null

    init {
        Log_OC.d(TAG, "UploadListAdapter")
        shouldShowHeadersForEmptySections(false)
    }

    override fun getSectionCount(): Int = sections.size

    override fun getItemCount(section: Int): Int = sections[section].items.size

    // region header
    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        val headerViewHolder = holder as HeaderViewHolder
        val group = sections[section]

        bindHeaderTitle(headerViewHolder, group, section)
        bindHeaderActionButton(headerViewHolder, group)
        bindHeaderBatterySaverWarning(headerViewHolder)
        bindHeaderActionClickListener(headerViewHolder, group)
    }

    private fun bindHeaderTitle(holder: HeaderViewHolder, group: Section, section: Int) {
        val title = parentActivity.getString(group.titleRes)
        val headerText = parentActivity.getString(R.string.uploads_view_group_header)
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

    private fun bindHeaderActionButton(holder: HeaderViewHolder, group: Section) {
        val iconRes = when (group.type) {
            Type.CURRENT, Type.COMPLETED -> R.drawable.ic_close
            Type.CANCELLED, Type.FAILED -> R.drawable.ic_dots_vertical
            else -> return
        }
        holder.binding.uploadListAction.setImageResource(iconRes)
    }

    private fun bindHeaderBatterySaverWarning(holder: HeaderViewHolder) {
        holder.binding.autoUploadBatterySaverWarningCard.root
            .setVisibleIf(powerManagementService.isPowerSavingEnabled)
        viewThemeUtils.material.themeCardView(holder.binding.autoUploadBatterySaverWarningCard.root)
    }

    private fun bindHeaderActionClickListener(holder: HeaderViewHolder, group: Section) {
        holder.binding.uploadListAction.setOnClickListener {
            when (group.type) {
                Type.CURRENT -> cancelAllCurrentUploads(group)

                Type.COMPLETED -> {
                    uploadsStorageManager.clearSuccessfulUploads()
                    loadUploadItemsFromDb {}
                }

                Type.FAILED -> showFailedPopupMenu(holder)

                Type.CANCELLED -> showCancelledPopupMenu(holder)

                else -> {}
            }
        }
    }

    private fun cancelAllCurrentUploads(group: Section) {
        val items = group.items.takeIf { it.isNotEmpty() } ?: return
        val accountName = items[0].accountName
        var completedCount = 0
        items.forEach { upload ->
            uploadHelper.updateUploadStatus(upload.remotePath, accountName, UploadStatus.UPLOAD_CANCELLED) {
                cancelUpload(upload.remotePath, accountName) {
                    completedCount++
                    if (completedCount == items.size) {
                        Log_OC.d(TAG, "refreshing upload items")
                        loadUploadItemsFromDb {}
                    }
                }
            }
        }
    }

    private fun showFailedPopupMenu(holder: HeaderViewHolder) {
        PopupMenu(fileActivity, holder.binding.uploadListAction).apply {
            inflate(R.menu.upload_list_failed_options)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload_list_failed_clear -> {
                        uploadsStorageManager.clearFailedButNotDelayedUploads()
                        clearTempEncryptedFolder()
                        loadUploadItemsFromDb {}
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
        PopupMenu(fileActivity, holder.binding.uploadListAction).apply {
            inflate(R.menu.upload_list_cancelled_options)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_upload_list_cancelled_clear -> {
                        uploadsStorageManager.clearCancelledUploadsForCurrentAccount()
                        loadUploadItemsFromDb {}
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
        val user = parentActivity.user
        user.ifPresent(
            Consumer { value: User? ->
                FileDataStorageManager.clearTempEncryptedFolder(value!!.accountName)
            }
        )
    }

    // FIXME For e2e resume is not working
    private fun retryCancelledUploads() {
        fileActivity.lifecycleScope.launch(Dispatchers.IO) {
            val showNotExistMessage = uploadHelper.retryCancelledUploads(
                uploadsStorageManager,
                connectivityService,
                accountManager,
                powerManagementService
            )
            if (showNotExistMessage) {
                withContext(Dispatchers.Main) {
                    DisplayUtils.showSnackMessage(parentActivity, R.string.upload_action_file_not_exist_message)
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
        if (sections.isEmpty() || section !in sections.indices) return
        val item = sections[section].items[relativePosition]
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
                item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED &&
                item.lastResult == UploadResult.UPLOADED
            )

        holder.binding.uploadDate.setVisibleIf(showDate)

        if (showDate) {
            holder.binding.uploadDate.text = DisplayUtils.getRelativeDateTimeString(
                parentActivity,
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

            val status = getStatusText(item)
            when (item.uploadStatus) {
                UploadStatus.UPLOAD_IN_PROGRESS -> bindItemInProgress(holder, item)

                UploadStatus.UPLOAD_SUCCEEDED,
                UploadStatus.UPLOAD_CANCELLED -> uploadStatus.visibility = View.GONE

                else -> {}
            }

            // Override visibility for edge cases
            if ((item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && item.lastResult != UploadResult.UPLOADED) ||
                item.uploadStatus == UploadStatus.UPLOAD_CANCELLED
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
                    val key = buildRemoteName(prevUpload.accountName, prevUpload.remotePath)
                    uploadHelper.removeUploadTransferProgressListener(uploadProgressListener!!, key)
                }
                uploadProgressListener = UploadProgressListener(item, uploadProgressBar)
                uploadHelper.addUploadTransferProgressListener(
                    uploadProgressListener!!,
                    buildRemoteName(item.accountName, item.remotePath)
                )
            } else if (uploadProgressListener?.isWrapping(uploadProgressBar) == true) {
                uploadProgressListener?.upload?.let { prevUpload ->
                    val key = buildRemoteName(prevUpload.accountName, prevUpload.remotePath)
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
            val status = getStatusText(item)

            // Right-side button
            when (item.uploadStatus) {
                UploadStatus.UPLOAD_IN_PROGRESS -> {
                    uploadRightButton.run {
                        setImageResource(R.drawable.ic_action_cancel_grey)
                        visibility = View.VISIBLE
                        setOnClickListener {
                            uploadHelper.updateUploadStatus(
                                item.remotePath,
                                item.accountName,
                                UploadStatus.UPLOAD_CANCELLED
                            ) {
                                cancelUpload(item.remotePath, item.accountName) { loadUploadItemsFromDb {} }
                            }
                        }
                    }
                }

                UploadStatus.UPLOAD_FAILED -> {
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
                    UploadStatus.UPLOAD_FAILED, UploadStatus.UPLOAD_CANCELLED ->
                        setOnClickListener {
                            onFailedOrCancelledItemClick(item, optionalUser, holder, status)
                        }

                    UploadStatus.UPLOAD_SUCCEEDED ->
                        setOnClickListener { onUploadedItemClick(item) }

                    else -> {}
                }
            }

            // Thumbnail click to open locally
            if (item.uploadStatus != UploadStatus.UPLOAD_SUCCEEDED) {
                thumbnail.setOnClickListener { onUploadingItemClick(item) }
            }
        }
    }

    private fun onFailedOrCancelledItemClick(
        item: OCUpload,
        optionalUser: java.util.Optional<User>,
        holder: ItemViewHolder,
        status: String
    ) {
        when (item.lastResult) {
            UploadResult.CREDENTIAL_ERROR -> {
                val user = optionalUser.orElseThrow { RuntimeException() }
                parentActivity.fileOperationsHelper.checkCurrentCredentials(user)
            }

            UploadResult.SYNC_CONFLICT if optionalUser.isPresent -> {
                if (checkAndOpenConflictResolutionDialog(optionalUser.get(), holder, item, status)) return
                retryOrShowError(item)
            }

            else -> retryOrShowError(item)
        }
    }

    private fun retryOrShowError(item: OCUpload) {
        val file = File(item.localPath)
        val user = accountManager.getUser(item.accountName)
        if (file.exists() && user.isPresent) {
            uploadHelper.retryUpload(item, user.get())
        } else {
            DisplayUtils.showSnackMessage(
                fileActivity,
                R.string.local_file_not_found_message
            )
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
                item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED ->
                bindRemoteThumbnail(holder, item, fakeFile, allowedToCreateNewThumbnail)

            MimeTypeUtil.isImage(fakeFile) ->
                bindLocalThumbnail(holder, item, allowedToCreateNewThumbnail)

            optionalUser.isPresent -> {
                val icon = MimeTypeUtil.getFileTypeIcon(item.mimeType, fileName, parentActivity, viewThemeUtils)
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
            val user = parentActivity.user
            if (user.isPresent) {
                val task = ThumbnailGenerationTask(holder.binding.thumbnail, parentActivity.storageManager, user.get())
                thumbnail = thumbnail ?: if (MimeTypeUtil.isVideo(fakeFile)) {
                    ThumbnailsCacheManager.mDefaultVideo
                } else {
                    ThumbnailsCacheManager.mDefaultImg
                }
                holder.binding.thumbnail.setImageDrawable(
                    AsyncThumbnailDrawable(parentActivity.resources, thumbnail, task)
                )
                task.execute(ThumbnailGenerationTaskObject(fakeFile, null))
            }
        }

        if (item.mimeType == "image/png") {
            holder.binding.thumbnail.setBackgroundColor(ContextCompat.getColor(parentActivity, R.color.bg_default))
        }
    }

    private fun bindLocalThumbnail(holder: ItemViewHolder, item: OCUpload, allowedToCreateNewThumbnail: Boolean) {
        val file = File(item.localPath)
        val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.hashCode().toString())

        if (thumbnail != null) {
            holder.binding.thumbnail.setImageBitmap(thumbnail)
        } else if (allowedToCreateNewThumbnail) {
            getThumbnailFromFileTypeAndSetIcon(item.localPath, holder)
            val task = ThumbnailGenerationTask(holder.binding.thumbnail)
            val defaultThumbnail = if (MimeTypeUtil.isVideo(file)) {
                ThumbnailsCacheManager.mDefaultVideo
            } else {
                ThumbnailsCacheManager.mDefaultImg
            }
            val asyncDrawable = AsyncThumbnailDrawable(parentActivity.resources, defaultThumbnail, task)
            task.execute(ThumbnailGenerationTaskObject(file, null))
            task.setListener(object : ThumbnailGenerationTask.Listener {
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
            holder.binding.thumbnail.setBackgroundColor(ContextCompat.getColor(parentActivity, R.color.bg_default))
        }
    }
    // endregion

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) = Unit

    private fun getThumbnailFromFileTypeAndSetIcon(localPath: String?, itemViewHolder: ItemViewHolder) {
        val drawable = MimeTypeUtil.getIcon(localPath, parentActivity, viewThemeUtils) ?: return
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
            this.openConflictActivity(localFile, item)
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
                    openConflictActivity(fileOnServer, item)
                } else {
                    displayFileNotFoundError(holder.itemView, fileActivity)
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
        PopupMenu(fileActivity, view).apply {
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
        loadUploadItemsFromDb {}
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
            fileActivity
        )
            .execute(user, fileActivity, { caller, result ->
                view.binding.uploadListItemLayout.isClickable = true
                listener.onRemoteOperationFinish(caller, result)
            }, parentActivity.handler)
    }

    private fun openConflictActivity(file: OCFile, upload: OCUpload) {
        file.setStoragePath(upload.localPath)
        val user = accountManager.getUser(upload.accountName)
        if (user.isPresent) {
            val intent = createIntent(
                file,
                user.get(),
                upload.uploadId,
                Intent.FLAG_ACTIVITY_NEW_TASK,
                fileActivity
            )
            fileActivity.startActivity(intent)
        }
    }

    /**
     * Gets the status text to show to the user according to the status and last result of the the given upload.
     *
     * @param upload Upload to describe.
     * @return Text describing the status of the given upload.
     */
    private fun getStatusText(upload: OCUpload): String {
        val status: String
        val res = parentActivity.getResources()
        val prefs = parentActivity.appPreferences
        when (val uploadStatus = upload.uploadStatus) {
            UploadStatus.UPLOAD_IN_PROGRESS -> {
                status = if (prefs.isGlobalUploadPaused()) {
                    res.getString(R.string.upload_global_pause_title)
                } else if (uploadHelper.isUploadingNow(upload)) {
                    res.getString(R.string.uploader_upload_in_progress_ticker)
                } else {
                    res.getString(R.string.uploads_view_later_waiting_to_upload)
                }
            }

            UploadStatus.UPLOAD_SUCCEEDED -> {
                val result = upload.lastResult
                status = if (result == UploadResult.SAME_FILE_CONFLICT) {
                    res.getString(R.string.uploads_view_upload_status_succeeded_same_file)
                } else if (result == UploadResult.FILE_NOT_FOUND) {
                    getUploadFailedStatusText(result)
                } else if (upload.nameCollisionPolicy == NameCollisionPolicy.SKIP) {
                    res.getString(R.string.uploads_view_upload_status_skip_reason)
                } else {
                    res.getString(R.string.uploads_view_upload_status_succeeded)
                }
            }

            UploadStatus.UPLOAD_FAILED -> status = getUploadFailedStatusText(upload.lastResult)

            UploadStatus.UPLOAD_CANCELLED -> status = res.getString(R.string.upload_manually_cancelled)

            else -> status = "Uncontrolled status: $uploadStatus"
        }

        return status
    }

    private fun getUploadFailedStatusText(result: UploadResult): String = when (result) {
        UploadResult.CREDENTIAL_ERROR ->
            parentActivity.getString(R.string.uploads_view_upload_status_failed_credentials_error)

        UploadResult.FOLDER_ERROR ->
            parentActivity.getString(R.string.uploads_view_upload_status_failed_folder_error)

        UploadResult.FILE_NOT_FOUND ->
            parentActivity.getString(R.string.uploads_view_upload_status_failed_localfile_error)

        UploadResult.FILE_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_failed_file_error)

        UploadResult.PRIVILEGES_ERROR -> parentActivity.getString(
            R.string.uploads_view_upload_status_failed_permission_error
        )

        UploadResult.NETWORK_CONNECTION ->
            parentActivity.getString(R.string.uploads_view_upload_status_failed_connection_error)

        UploadResult.DELAYED_FOR_WIFI -> parentActivity.getString(
            R.string.uploads_view_upload_status_waiting_for_wifi
        )

        UploadResult.DELAYED_FOR_CHARGING ->
            parentActivity.getString(R.string.uploads_view_upload_status_waiting_for_charging)

        UploadResult.CONFLICT_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_conflict)

        UploadResult.SERVICE_INTERRUPTED -> parentActivity.getString(
            R.string.uploads_view_upload_status_service_interrupted
        )

        UploadResult.CANCELLED -> // should not get here ; cancelled uploads should be wiped out
            parentActivity.getString(R.string.uploads_view_upload_status_cancelled)

        UploadResult.UPLOADED -> // should not get here ; status should be UPLOAD_SUCCESS
            parentActivity.getString(R.string.uploads_view_upload_status_succeeded)

        UploadResult.MAINTENANCE_MODE -> parentActivity.getString(R.string.maintenance_mode)

        UploadResult.SSL_RECOVERABLE_PEER_UNVERIFIED -> parentActivity.getString(
            R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted
        )

        UploadResult.UNKNOWN -> parentActivity.getString(R.string.uploads_view_upload_status_unknown_fail)

        UploadResult.LOCK_FAILED -> parentActivity.getString(R.string.upload_lock_failed)

        UploadResult.DELAYED_IN_POWER_SAVE_MODE -> parentActivity.getString(
            R.string.uploads_view_upload_status_waiting_exit_power_save_mode
        )

        UploadResult.VIRUS_DETECTED -> parentActivity.getString(R.string.uploads_view_upload_status_virus_detected)

        UploadResult.LOCAL_STORAGE_FULL -> parentActivity.getString(R.string.upload_local_storage_full)

        UploadResult.OLD_ANDROID_API -> parentActivity.getString(R.string.upload_old_android)

        UploadResult.SYNC_CONFLICT -> parentActivity.getString(R.string.upload_sync_conflict)

        UploadResult.CANNOT_CREATE_FILE -> parentActivity.getString(R.string.upload_cannot_create_file)

        UploadResult.LOCAL_STORAGE_NOT_COPIED -> parentActivity.getString(R.string.upload_local_storage_not_copied)

        UploadResult.QUOTA_EXCEEDED -> parentActivity.getString(R.string.upload_quota_exceeded)

        else -> parentActivity.getString(R.string.upload_unknown_error)
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
    fun loadUploadItemsFromDb(onCompleted: Runnable) {
        parentActivity.user.ifPresent { user ->
            val accountName = user.accountName
            val optionalCapabilities = parentActivity.capabilities
            if (!optionalCapabilities.isPresent) return@ifPresent
            val capabilities = optionalCapabilities.get()

            sections.indices.forEach { i ->
                val sec = sections[i]
                uploadHelper.getUploadsByStatus(
                    accountName,
                    sec.status!!,
                    capabilities,
                    sec.collisionPolicy
                ) { uploads ->
                    uploads.forEach { it.setDataFixed(uploadHelper) }
                    sections[i] = sec.withItems(uploads.sortedByUploadOrder())
                    parentActivity.runOnUiThread {
                        notifyDataSetChanged()
                        onCompleted.run()
                    }
                }
            }
        }
    }

    /**
     * Open local file.
     */
    private fun onUploadingItemClick(file: OCUpload) {
        val f = File(file.localPath)
        if (!f.exists()) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.local_file_not_found_message)
        } else {
            openFileWithDefault(file.localPath)
        }
    }

    /**
     * Open remote file.
     */
    private fun onUploadedItemClick(upload: OCUpload) {
        val file = parentActivity.storageManager.getFileByEncryptedRemotePath(upload.remotePath)
        if (file == null) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.error_retrieving_file)
            Log_OC.i(TAG, "Could not find uploaded file on remote.")
            return
        }

        val optionalUser = parentActivity.user

        if (canBePreviewed(file) && optionalUser.isPresent) {
            // show image preview and stay in uploads tab
            val intent = openFileIntent(parentActivity, optionalUser.get(), file)
            parentActivity.startActivity(intent)
        } else {
            val intent = Intent(parentActivity, FileDisplayActivity::class.java)
            intent.setAction(Intent.ACTION_VIEW)
            intent.putExtra(FileDisplayActivity.KEY_FILE_PATH, upload.remotePath)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            parentActivity.startActivity(intent)
        }
    }

    /**
     * Open file with app associates with its MIME type. If MIME type unknown, show list with all apps.
     */
    private fun openFileWithDefault(localPath: String) {
        var mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
        if (mimetype == "application/octet-stream") mimetype = "*/*"
        try {
            parentActivity.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(File(localPath)), mimetype)
                }
            )
        } catch (e: ActivityNotFoundException) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.file_list_no_app_for_file_type)
            Log_OC.i(TAG, "Could not find app for sending log history: $e")
        }
    }

    fun cancelOldErrorNotification(upload: OCUpload?) {
        if (mNotificationManager == null) {
            mNotificationManager = parentActivity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }

        if (upload == null) {
            return
        }

        mNotificationManager!!.cancel(upload.uploadId.toInt())
    }

    companion object {
        private val TAG: String = UploadListAdapter::class.java.getSimpleName()
    }
}
