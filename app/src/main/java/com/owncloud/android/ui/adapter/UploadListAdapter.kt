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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
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
import com.owncloud.android.MainApp
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
import java.io.File
import java.util.function.Consumer
import java.util.function.Supplier

class UploadListAdapter(
    fileActivity: FileActivity,
    uploadsStorageManager: UploadsStorageManager,
    storageManager: FileDataStorageManager,
    accountManager: UserAccountManager,
    connectivityService: ConnectivityService,
    powerManagementService: PowerManagementService,
    clock: Clock,
    viewThemeUtils: ViewThemeUtils
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

    private val sections: MutableList<Section> = ArrayList<Section>(
        listOf(
            Section(
                Type.CURRENT,
                R.string.uploads_view_group_current_uploads,
                UploadStatus.UPLOAD_IN_PROGRESS,
                null,
                listOf(),
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

    private var uploadProgressListener: UploadProgressListener? = null
    private val parentActivity: FileActivity
    private val uploadsStorageManager: UploadsStorageManager
    private val storageManager: FileDataStorageManager
    private val connectivityService: ConnectivityService
    private val powerManagementService: PowerManagementService
    private val accountManager: UserAccountManager
    private val clock: Clock
    private val showUser: Boolean
    private val viewThemeUtils: ViewThemeUtils
    private var mNotificationManager: NotificationManager? = null
    private val uploadHelper = instance()

    init {
        Log_OC.d(TAG, "UploadListAdapter")

        this.parentActivity = fileActivity
        this.uploadsStorageManager = uploadsStorageManager
        this.storageManager = storageManager
        this.accountManager = accountManager
        this.connectivityService = connectivityService
        this.powerManagementService = powerManagementService
        this.clock = clock
        this.viewThemeUtils = viewThemeUtils
        shouldShowHeadersForEmptySections(false)
        showUser = accountManager.getAccounts().size > 1
    }

    override fun getSectionCount(): Int {
        return sections.size
    }

    override fun getItemCount(section: Int): Int {
        return sections[section].items.size
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        val headerViewHolder = holder as HeaderViewHolder

        val group = sections[section]
        val title = parentActivity.getString(group.titleRes)
        val count = group.items.size

        headerViewHolder.binding.uploadListTitle.text =
            String.format(parentActivity.getString(R.string.uploads_view_group_header), title, count)
        viewThemeUtils.platform.colorTextView(headerViewHolder.binding.uploadListTitle)

        headerViewHolder.binding.uploadListTitle.setOnClickListener {
            toggleSectionExpanded(section)
            val icon = if (isSectionExpanded(section)) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            headerViewHolder.binding.uploadListState.setImageResource(icon)
        }

        headerViewHolder.binding.uploadListStateLayout.setOnClickListener {
            toggleSectionExpanded(section)
            val icon = if (isSectionExpanded(section)) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            headerViewHolder.binding.uploadListState.setImageResource(icon)
        }

        when (group.type) {
            Type.CURRENT, Type.COMPLETED -> headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_close)
            Type.CANCELLED, Type.FAILED -> headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_dots_vertical)
            else -> {}
        }

        headerViewHolder.binding.autoUploadBatterySaverWarningCard.root
            .setVisibleIf(powerManagementService.isPowerSavingEnabled)
        viewThemeUtils.material.themeCardView(headerViewHolder.binding.autoUploadBatterySaverWarningCard.root)

        headerViewHolder.binding.uploadListAction.setOnClickListener {
            when (group.type) {
                Type.CURRENT -> {
                    val items = group.items
                    if (items.isEmpty()) return@setOnClickListener

                    val accountName = items[0].accountName

                    val totalUploads = group.items.size
                    val completedCount = intArrayOf(0)

                    for (i in group.items.indices) {
                        val upload = group.items[i]
                        uploadHelper.updateUploadStatus(
                            upload.remotePath,
                            accountName,
                            UploadStatus.UPLOAD_CANCELLED
                        ) {
                            cancelUpload(upload.remotePath, accountName) {
                                completedCount[0]++
                                if (completedCount[0] == totalUploads) {
                                    Log_OC.d(TAG, "refreshing upload items")

                                    // All uploads finished, refresh UI once
                                    loadUploadItemsFromDb(Runnable {})
                                }
                            }
                        }
                    }
                }

                Type.COMPLETED -> {
                    uploadsStorageManager.clearSuccessfulUploads()
                    loadUploadItemsFromDb {}
                }

                Type.FAILED -> showFailedPopupMenu(headerViewHolder)
                Type.CANCELLED -> showCancelledPopupMenu(headerViewHolder)
                else -> {}
            }
        }
    }

    private fun showFailedPopupMenu(headerViewHolder: HeaderViewHolder) {
        val failedPopup = PopupMenu(MainApp.getAppContext(), headerViewHolder.binding.uploadListAction)
        failedPopup.inflate(R.menu.upload_list_failed_options)
        failedPopup.setOnMenuItemClickListener { i: MenuItem? ->
            val itemId = i!!.itemId
            if (itemId == R.id.action_upload_list_failed_clear) {
                uploadsStorageManager.clearFailedButNotDelayedUploads()
                clearTempEncryptedFolder()
                loadUploadItemsFromDb {}
            } else if (itemId == R.id.action_upload_list_failed_retry) {
                uploadHelper.retryFailedUploads(
                    uploadsStorageManager,
                    connectivityService,
                    accountManager,
                    powerManagementService
                )
            }
            true
        }

        failedPopup.show()
    }

    private fun showCancelledPopupMenu(headerViewHolder: HeaderViewHolder) {
        val popup = PopupMenu(MainApp.getAppContext(), headerViewHolder.binding.uploadListAction)
        popup.inflate(R.menu.upload_list_cancelled_options)

        popup.setOnMenuItemClickListener { i: MenuItem ->
            val itemId = i.itemId
            if (itemId == R.id.action_upload_list_cancelled_clear) {
                uploadsStorageManager.clearCancelledUploadsForCurrentAccount()
                loadUploadItemsFromDb(Runnable {})
                clearTempEncryptedFolder()
            } else if (itemId == R.id.action_upload_list_cancelled_resume) {
                retryCancelledUploads()
            }
            true
        }

        popup.show()
    }

    private fun clearTempEncryptedFolder() {
        val user = parentActivity.user
        user.ifPresent(Consumer { value: User? -> FileDataStorageManager.clearTempEncryptedFolder(value!!.accountName) })
    }

    // FIXME For e2e resume is not working
    private fun retryCancelledUploads() {
        Thread {
            val showNotExistMessage = uploadHelper.retryCancelledUploads(
                uploadsStorageManager,
                connectivityService,
                accountManager,
                powerManagementService
            )
            parentActivity.runOnUiThread {
                if (showNotExistMessage) {
                    showNotExistMessage()
                }
            }
        }.start()
    }

    private fun showNotExistMessage() {
        DisplayUtils.showSnackMessage(parentActivity, R.string.upload_action_file_not_exist_message)
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) = Unit

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (sections.isEmpty() || section < 0 || section >= sections.size) {
            return
        }

        val sectionData = sections[section]
        val item = sectionData.items[relativePosition]

        val itemViewHolder = holder as ItemViewHolder
        itemViewHolder.binding.uploadName.text = item.localPath

        // local file name
        val remoteFile = File(item.remotePath)
        var fileName = remoteFile.getName()
        if (fileName.isEmpty()) {
            fileName = File.separator
        }
        itemViewHolder.binding.uploadName.text = fileName

        // remote path to parent folder
        itemViewHolder.binding.uploadRemotePath.text = File(item.remotePath).getParent()

        val updateTime = item.uploadEndTimestamp

        // file size
        if (item.fileSize != 0L) {
            var fileSizeFormat = "%s "

            // we have valid update time so we can show the upload date
            if (updateTime > 0) {
                fileSizeFormat = "%s, "
            }

            val fileSizeInBytes = DisplayUtils.bytesToHumanReadable(item.fileSize)
            val uploadFileSize = String.format(fileSizeFormat, fileSizeInBytes)
            itemViewHolder.binding.uploadFileSize.text = uploadFileSize
        } else {
            itemViewHolder.binding.uploadFileSize.text = ""
        }

        // upload date
        val showUploadDate =
            (updateTime > 0 && item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && item.lastResult == UploadResult.UPLOADED)
        itemViewHolder.binding.uploadDate.visibility = if (showUploadDate) View.VISIBLE else View.GONE
        if (showUploadDate) {
            val dateString = DisplayUtils.getRelativeDateTimeString(
                parentActivity,
                updateTime,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            )
            itemViewHolder.binding.uploadDate.text = dateString
        }

        // account
        val optionalUser = accountManager.getUser(item.accountName)
        if (showUser) {
            itemViewHolder.binding.uploadAccount.visibility = View.VISIBLE
            if (optionalUser.isPresent) {
                itemViewHolder.binding.uploadAccount.text = DisplayUtils.getAccountNameDisplayText(optionalUser.get())
            } else {
                itemViewHolder.binding.uploadAccount.text = item.accountName
            }
        } else {
            itemViewHolder.binding.uploadAccount.visibility = View.GONE
        }

        // Reset fields visibility
        itemViewHolder.binding.uploadRemotePath.visibility = View.VISIBLE
        itemViewHolder.binding.uploadFileSize.visibility = View.VISIBLE
        itemViewHolder.binding.uploadStatus.visibility = View.VISIBLE
        itemViewHolder.binding.uploadProgressBar.visibility = View.GONE

        // Update information depending of upload details
        val status = getStatusText(item)
        when (item.uploadStatus) {
            UploadStatus.UPLOAD_IN_PROGRESS -> {
                viewThemeUtils.platform.themeHorizontalProgressBar(itemViewHolder.binding.uploadProgressBar)
                itemViewHolder.binding.uploadProgressBar.progress = 0
                itemViewHolder.binding.uploadProgressBar.visibility = View.VISIBLE

                if (uploadHelper.isUploadingNow(item)) {
                    // really uploading, so...
                    // ... unbind the old progress bar, if any; ...
                    if (uploadProgressListener != null) {
                        val upload = uploadProgressListener!!.upload
                        if (upload != null) {
                            val targetKey = buildRemoteName(
                                upload.accountName,
                                upload.remotePath
                            )
                            uploadHelper.removeUploadTransferProgressListener(uploadProgressListener!!, targetKey)
                        }
                    }
                    // ... then, bind the current progress bar to listen for updates
                    uploadProgressListener = UploadProgressListener(item, itemViewHolder.binding.uploadProgressBar)
                    val targetKey = buildRemoteName(item.accountName, item.remotePath)
                    uploadHelper.addUploadTransferProgressListener(uploadProgressListener!!, targetKey)
                } else {
                    // not really uploading; stop listening progress if view is reused!
                    if (uploadProgressListener != null &&
                        uploadProgressListener!!.isWrapping(itemViewHolder.binding.uploadProgressBar)
                    ) {
                        val upload = uploadProgressListener!!.upload
                        if (upload != null) {
                            val targetKey = buildRemoteName(
                                upload.accountName,
                                upload.remotePath
                            )
                            uploadHelper.removeUploadTransferProgressListener(uploadProgressListener!!, targetKey)
                            uploadProgressListener = null
                        }
                    }
                }

                itemViewHolder.binding.uploadFileSize.visibility = View.GONE
                itemViewHolder.binding.uploadProgressBar.invalidate()
            }

            UploadStatus.UPLOAD_FAILED -> {
            }

            UploadStatus.UPLOAD_SUCCEEDED, UploadStatus.UPLOAD_CANCELLED -> itemViewHolder.binding.uploadStatus.visibility =
                View.GONE
        }

        // show status if same file conflict or local file deleted or upload cancelled
        if ((item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && item.lastResult != UploadResult.UPLOADED)
            || item.uploadStatus == UploadStatus.UPLOAD_CANCELLED
        ) {
            itemViewHolder.binding.uploadStatus.visibility = View.VISIBLE
            itemViewHolder.binding.uploadFileSize.visibility = View.GONE
        }

        itemViewHolder.binding.uploadStatus.text = status

        // bind listeners to perform actions
        if (item.uploadStatus == UploadStatus.UPLOAD_IN_PROGRESS) {
            // Cancel
            itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_action_cancel_grey)
            itemViewHolder.binding.uploadRightButton.setVisibility(View.VISIBLE)
            itemViewHolder.binding.uploadRightButton.setOnClickListener {
                uploadHelper.updateUploadStatus(
                    item.remotePath,
                    item.accountName,
                    UploadStatus.UPLOAD_CANCELLED
                ) {
                    cancelUpload(item.remotePath, item.accountName) {
                        loadUploadItemsFromDb {}
                    }
                }
            }
        } else if (item.uploadStatus == UploadStatus.UPLOAD_FAILED) {
            if (item.lastResult == UploadResult.SYNC_CONFLICT) {
                itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_dots_vertical)
                itemViewHolder.binding.uploadRightButton.setOnClickListener { view: View? ->
                    optionalUser.ifPresent(
                        Consumer { user: User? -> showItemConflictPopup(user, itemViewHolder, item, status, view) })
                }
            } else {
                // Delete
                itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_action_delete_grey)
                itemViewHolder.binding.uploadRightButton.setOnClickListener {
                    removeUpload(
                        item
                    )
                }
            }
            itemViewHolder.binding.uploadRightButton.setVisibility(View.VISIBLE)
        } else {    // UploadStatus.UPLOAD_SUCCEEDED
            itemViewHolder.binding.uploadRightButton.setVisibility(View.INVISIBLE)
        }

        itemViewHolder.binding.uploadListItemLayout.setOnClickListener(null)

        // Set icon or thumbnail
        itemViewHolder.binding.thumbnail.setImageResource(R.drawable.file)

        // click on item
        if (item.uploadStatus == UploadStatus.UPLOAD_FAILED ||
            item.uploadStatus == UploadStatus.UPLOAD_CANCELLED
        ) {
            val uploadResult = item.lastResult
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener { v: View? ->
                if (uploadResult == UploadResult.CREDENTIAL_ERROR) {
                    val optUser = accountManager.getUser(item.accountName)
                    val user = optUser.orElseThrow(Supplier { RuntimeException() })
                    parentActivity.fileOperationsHelper.checkCurrentCredentials(user)
                    return@setOnClickListener
                } else if (uploadResult == UploadResult.SYNC_CONFLICT && optionalUser.isPresent) {
                    val user = optionalUser.get()
                    if (checkAndOpenConflictResolutionDialog(user, itemViewHolder, item, status)) {
                        return@setOnClickListener
                    }
                }
                // not a credentials error
                val file = File(item.localPath)
                val user = accountManager.getUser(item.accountName)
                if (file.exists() && user.isPresent) {
                    uploadHelper.retryUpload(item, user.get())
                } else {
                    DisplayUtils.showSnackMessage(
                        v!!.getRootView().findViewById(android.R.id.content),
                        R.string.local_file_not_found_message
                    )
                }
            }
        } else if (item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED) {
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener {
                onUploadedItemClick(
                    item
                )
            }
        }

        // click on thumbnail to open locally
        if (item.uploadStatus != UploadStatus.UPLOAD_SUCCEEDED) {
            itemViewHolder.binding.thumbnail.setOnClickListener {
                onUploadingItemClick(
                    item
                )
            }
        }

        /*
         * Cancellation needs do be checked and done before changing the drawable in fileIcon, or
         * {@link ThumbnailsCacheManager#cancelPotentialWork} will NEVER cancel any task.
         */
        val fakeFileToCheatThumbnailsCacheManagerInterface = OCFile(item.remotePath)
        fakeFileToCheatThumbnailsCacheManagerInterface.setStoragePath(item.localPath)
        fakeFileToCheatThumbnailsCacheManagerInterface.mimeType = item.mimeType

        val allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
            fakeFileToCheatThumbnailsCacheManagerInterface, itemViewHolder.binding.thumbnail
        )

        // TODO this code is duplicated; refactor to a common place
        if (MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)
            && fakeFileToCheatThumbnailsCacheManagerInterface.remoteId != null && item.uploadStatus == UploadStatus.UPLOAD_SUCCEEDED
        ) {
            // Thumbnail in Cache?

            val cacheKey = fakeFileToCheatThumbnailsCacheManagerInterface.remoteId.toString()
            var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(cacheKey)

            if (thumbnail != null && !fakeFileToCheatThumbnailsCacheManagerInterface.isUpdateThumbnailNeeded) {
                itemViewHolder.binding.thumbnail.setImageBitmap(thumbnail)
            } else {
                // generate new Thumbnail
                val user = parentActivity.user
                if (allowedToCreateNewThumbnail && user.isPresent) {
                    val task =
                        ThumbnailGenerationTask(
                            itemViewHolder.binding.thumbnail,
                            parentActivity.storageManager,
                            user.get()
                        )
                    if (thumbnail == null) {
                        if (MimeTypeUtil.isVideo(fakeFileToCheatThumbnailsCacheManagerInterface)) {
                            thumbnail = ThumbnailsCacheManager.mDefaultVideo
                        } else {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg
                        }
                    }
                    val asyncDrawable =
                        AsyncThumbnailDrawable(
                            parentActivity.getResources(),
                            thumbnail,
                            task
                        )
                    itemViewHolder.binding.thumbnail.setImageDrawable(asyncDrawable)
                    task.execute(
                        ThumbnailGenerationTaskObject(
                            fakeFileToCheatThumbnailsCacheManagerInterface, null
                        )
                    )
                }
            }

            if ("image/png" == item.mimeType) {
                val backgroundColor = ContextCompat.getColor(parentActivity, R.color.bg_default)
                itemViewHolder.binding.thumbnail.setBackgroundColor(backgroundColor)
            }
        } else if (MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)) {
            val file = File(item.localPath)
            var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.hashCode().toString())

            if (thumbnail != null) {
                itemViewHolder.binding.thumbnail.setImageBitmap(thumbnail)
            } else if (allowedToCreateNewThumbnail) {
                getThumbnailFromFileTypeAndSetIcon(item.localPath, itemViewHolder)

                val task =
                    ThumbnailGenerationTask(itemViewHolder.binding.thumbnail)

                if (MimeTypeUtil.isVideo(file)) {
                    thumbnail = ThumbnailsCacheManager.mDefaultVideo
                } else {
                    thumbnail = ThumbnailsCacheManager.mDefaultImg
                }

                val asyncDrawable =
                    AsyncThumbnailDrawable(parentActivity.getResources(), thumbnail, task)
                task.execute(ThumbnailGenerationTaskObject(file, null))
                task.setListener(object : ThumbnailGenerationTask.Listener {
                    override fun onSuccess() {
                        itemViewHolder.binding.thumbnail.setImageDrawable(asyncDrawable)
                    }

                    override fun onError() {
                        getThumbnailFromFileTypeAndSetIcon(item.localPath, itemViewHolder)
                    }
                })

                Log_OC.v(TAG, "Executing task to generate a new thumbnail")
            }

            if ("image/png".equals(item.mimeType, ignoreCase = true)) {
                val backgroundColor = ContextCompat.getColor(parentActivity, R.color.bg_default)
                itemViewHolder.binding.thumbnail.setBackgroundColor(backgroundColor)
            }
        } else {
            if (optionalUser.isPresent) {
                val icon = MimeTypeUtil.getFileTypeIcon(
                    item.mimeType,
                    fileName,
                    parentActivity,
                    viewThemeUtils
                )
                itemViewHolder.binding.thumbnail.setImageDrawable(icon)
            }
        }
    }

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
        holder: ItemViewHolder, user: User?, folder: OCFile?, remotePath: String?,
        item: OCUpload, status: String?
    ) {
        val context = MainApp.getAppContext()

        this.refreshFolder(
            context,
            holder,
            user,
            folder
        ) { _: RemoteOperation<*>?, result: RemoteOperationResult<*>? ->
            holder.binding.uploadStatus.text = status
            if (result!!.isSuccess) {
                val fileOnServer = storageManager.getFileByEncryptedRemotePath(remotePath)

                if (fileOnServer != null) {
                    openConflictActivity(fileOnServer, item)
                } else {
                    displayFileNotFoundError(holder.itemView, context)
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
        itemViewHolder: ItemViewHolder,
        item: OCUpload,
        status: String?,
        view: View?
    ) {
        val popup = PopupMenu(MainApp.getAppContext(), view)
        popup.inflate(R.menu.upload_list_item_file_conflict)
        popup.setOnMenuItemClickListener { i: MenuItem? ->
            val itemId = i!!.itemId
            if (itemId == R.id.action_upload_list_resolve_conflict) {
                checkAndOpenConflictResolutionDialog(user, itemViewHolder, item, status)
            } else {
                removeUpload(item)
            }
            true
        }
        popup.show()
    }

    fun removeUpload(item: OCUpload?) {
        uploadsStorageManager.removeUpload(item)
        cancelOldErrorNotification(item)
        loadUploadItemsFromDb {}
    }

    private fun refreshFolder(
        context: Context?,
        view: ItemViewHolder,
        user: User?,
        folder: OCFile?,
        listener: OnRemoteOperationListener
    ) {
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
            context
        )
            .execute(
                user,
                context,
                { caller: RemoteOperation<*>?, result: RemoteOperationResult<*>? ->
                    view.binding.uploadListItemLayout.isClickable = true
                    listener.onRemoteOperationFinish(caller, result)
                },
                parentActivity.handler
            )
    }

    private fun openConflictActivity(file: OCFile, upload: OCUpload) {
        file.setStoragePath(upload.localPath)

        val context = MainApp.getAppContext()
        val user = accountManager.getUser(upload.accountName)
        if (user.isPresent) {
            val intent = createIntent(
                file,
                user.get(),
                upload.uploadId,
                Intent.FLAG_ACTIVITY_NEW_TASK,
                context
            )

            context.startActivity(intent)
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
        val statusRes = parentActivity.getResources()
        val prefs = parentActivity.appPreferences
        val uploadStatus = upload.uploadStatus

        when (uploadStatus) {
            UploadStatus.UPLOAD_IN_PROGRESS -> {
                status = if (prefs.isGlobalUploadPaused()) {
                    statusRes.getString(R.string.upload_global_pause_title)
                } else if (uploadHelper.isUploadingNow(upload)) {
                    statusRes.getString(R.string.uploader_upload_in_progress_ticker)
                } else {
                    statusRes.getString(R.string.uploads_view_later_waiting_to_upload)
                }
            }

            UploadStatus.UPLOAD_SUCCEEDED -> {
                val result = upload.lastResult
                status = if (result == UploadResult.SAME_FILE_CONFLICT) {
                    statusRes.getString(R.string.uploads_view_upload_status_succeeded_same_file)
                } else if (result == UploadResult.FILE_NOT_FOUND) {
                    getUploadFailedStatusText(result)
                } else if (upload.nameCollisionPolicy == NameCollisionPolicy.SKIP) {
                    statusRes.getString(R.string.uploads_view_upload_status_skip_reason)
                } else {
                    statusRes.getString(R.string.uploads_view_upload_status_succeeded)
                }
            }

            UploadStatus.UPLOAD_FAILED -> status = getUploadFailedStatusText(upload.lastResult)
            UploadStatus.UPLOAD_CANCELLED -> status = statusRes.getString(R.string.upload_manually_cancelled)
            else -> status = "Uncontrolled status: $uploadStatus"
        }

        return status
    }

    private fun getUploadFailedStatusText(result: UploadResult): String {
        return when (result) {
            UploadResult.CREDENTIAL_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_failed_credentials_error)
            UploadResult.FOLDER_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_failed_folder_error)
            UploadResult.FILE_NOT_FOUND -> parentActivity.getString(R.string.uploads_view_upload_status_failed_localfile_error)
            UploadResult.FILE_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_failed_file_error)
            UploadResult.PRIVILEGES_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_failed_permission_error)
            UploadResult.NETWORK_CONNECTION -> parentActivity.getString(R.string.uploads_view_upload_status_failed_connection_error)
            UploadResult.DELAYED_FOR_WIFI -> parentActivity.getString(R.string.uploads_view_upload_status_waiting_for_wifi)
            UploadResult.DELAYED_FOR_CHARGING -> parentActivity.getString(R.string.uploads_view_upload_status_waiting_for_charging)
            UploadResult.CONFLICT_ERROR -> parentActivity.getString(R.string.uploads_view_upload_status_conflict)
            UploadResult.SERVICE_INTERRUPTED -> parentActivity.getString(R.string.uploads_view_upload_status_service_interrupted)
            UploadResult.CANCELLED ->  // should not get here ; cancelled uploads should be wiped out
                parentActivity.getString(R.string.uploads_view_upload_status_cancelled)

            UploadResult.UPLOADED ->  // should not get here ; status should be UPLOAD_SUCCESS
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(
                UploadListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            ItemViewHolder(
                UploadListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
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
                uploadHelper.getUploadsByStatus(accountName, sec.status!!, capabilities, sec.collisionPolicy) { uploads ->
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
            //show image preview and stay in uploads tab
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
        val myIntent = Intent(Intent.ACTION_VIEW)
        val file = File(localPath)
        var mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
        if ("application/octet-stream" == mimetype) {
            mimetype = "*/*"
        }
        myIntent.setDataAndType(Uri.fromFile(file), mimetype)
        try {
            parentActivity.startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.file_list_no_app_for_file_type)
            Log_OC.i(TAG, "Could not find app for sending log history: $e")
        }
    }

    internal class HeaderViewHolder(var binding: UploadListHeaderBinding) : SectionedViewHolder(
        binding.getRoot()
    )

    internal class ItemViewHolder(var binding: UploadListItemBinding) : SectionedViewHolder(
        binding.getRoot()
    )

    internal enum class Type {
        CURRENT, COMPLETED, FAILED, CANCELLED, SKIPPED
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
