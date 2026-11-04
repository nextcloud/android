/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023-2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018-2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Atsushi Matsuo <matsuo@dds.co.jp>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.AppConstants
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.SnackbarUtil.show
import com.nextcloud.utils.extensions.isLocalETagOutdated
import com.nextcloud.utils.extensions.toEncryptionEvent
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation
import com.owncloud.android.lib.resources.files.model.FileVersion
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.services.OperationsService
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView
import com.owncloud.android.ui.activity.ShareActivity
import com.owncloud.android.ui.activity.TextEditorWebView
import com.owncloud.android.ui.dialog.SendFilesDialog
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.events.FileLockEvent
import com.owncloud.android.ui.events.SyncEventFinished
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.UriUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("LargeClass", "DEPRECATION")
class FileOperationsHelper(
    private val fileActivity: FileActivity,
    private val currentAccount: CurrentAccountProvider,
    private val connectivityService: ConnectivityService,
    private val editorUtils: EditorUtils
) {
    var opIdWaitingFor: Long = Long.MAX_VALUE

    // region Sync before preview
    fun startSyncForFileAndIntent(file: OCFile, intent: Intent) {
        fileActivity.lifecycleScope.launch(Dispatchers.IO) {
            val user = fileActivity.user.orElseThrow { RuntimeException() }
            val storageManager = FileDataStorageManager(user, fileActivity.contentResolver)
            syncFileIfOutdated(file, user, storageManager)
            postSyncEventFinished(intent)
        }
    }

    fun startSyncForOutdatedFileAndIntent(file: OCFile, intent: Intent) {
        fileActivity.lifecycleScope.launch(Dispatchers.IO) {
            val user = currentAccount.user
            val storageManager = FileDataStorageManager(user, fileActivity.contentResolver)
            if (!isDownloadedAndOutdated(storageManager.getFileById(file.fileId))) {
                postSyncEventFinished(intent)
                return@launch
            }

            synchronizeFile(file, user, storageManager) { postSyncEventFinished(intent) }
        }
    }

    private fun syncFileIfOutdated(file: OCFile, user: User, storageManager: FileDataStorageManager) {
        if (file.isInConflict) {
            syncFileShowingProgress(file, user, storageManager)
            return
        }

        if (isParentRecentlySynced(file, storageManager)) {
            return
        }

        if (!connectivityService.connectivity.isConnected || connectivityService.isInternetWalled()) {
            fileActivity.runOnUiThread { show(fileActivity, R.string.file_not_synced) }
            return
        }

        val etagResult = CheckEtagRemoteOperation(file.remotePath, file.etag).execute(user, fileActivity)
        if (etagResult.code == ResultCode.ETAG_CHANGED) {
            syncFileShowingProgress(file, user, storageManager)
        }
    }

    private fun isParentRecentlySynced(file: OCFile, storageManager: FileDataStorageManager): Boolean {
        val parent = storageManager.getFileById(file.parentId) ?: return false
        return parent.lastSyncDateForData + AppConstants.THIRTY_SECONDS_IN_MILLIS > System.currentTimeMillis()
    }

    private fun syncFileShowingProgress(file: OCFile, user: User, storageManager: FileDataStorageManager) {
        fileActivity.showLoadingDialog(fileActivity.getString(R.string.sync_in_progress))

        val result = SynchronizeFileOperation(file, null, user, true, fileActivity, storageManager, false)
            .execute(fileActivity)

        when {
            result.code == ResultCode.SYNC_CONFLICT -> startConflictsResolveActivity(file, user)

            file.isDown -> {
                FileStorageUtils.checkIfFileFinishedSaving(file)
                if (!result.isSuccess) {
                    showFileNotSyncedMessage(FILE_NOT_SYNCED_AFTER_CONFLICT_CHECK_WAIT_MILLIS)
                }
            }
        }

        fileActivity.dismissLoadingDialog()
    }

    private fun synchronizeFile(
        file: OCFile,
        user: User,
        storageManager: FileDataStorageManager,
        onSynced: () -> Unit
    ) {
        val result = SynchronizeFileOperation(file, null, user, true, fileActivity, storageManager, false)
            .execute(fileActivity)

        if (result.code == ResultCode.SYNC_CONFLICT) {
            startConflictsResolveActivity(file, user)
            return
        }

        if (!result.isSuccess) {
            showFileNotSyncedMessage(AppConstants.MILLIS_PER_SECOND.toLong())
        }

        onSynced()
    }

    private fun isDownloadedAndOutdated(storedFile: OCFile?): Boolean =
        storedFile != null && storedFile.isDown && storedFile.isLocalETagOutdated()

    private fun postSyncEventFinished(intent: Intent) {
        EventBus.getDefault().post(SyncEventFinished(intent))
    }

    private fun startConflictsResolveActivity(file: OCFile, user: User) {
        val intent = ConflictsResolveActivity.createIntent(
            file,
            user,
            NO_CONFLICT_UPLOAD_ID,
            Intent.FLAG_ACTIVITY_NEW_TASK,
            fileActivity
        )
        fileActivity.startActivity(intent)
    }

    private fun showFileNotSyncedMessage(snackbarWaitMillis: Long) {
        fileActivity.runOnUiThread { show(fileActivity, R.string.file_not_synced) }

        try {
            Thread.sleep(snackbarWaitMillis)
        } catch (_: InterruptedException) {
            Log_OC.e(TAG, "Failed to sleep")
        }
    }
    // endregion

    // region Open file
    fun canOpenFile(file: OCFile): Boolean = hasAppToOpen(createOpenFileIntent(file))

    fun openFile(file: OCFile?) {
        if (file == null) {
            Log_OC.e(TAG, "Trying to open a NULL OCFile")
            return
        }

        val openFileIntent = createOpenFileIntent(file)
        if (!hasAppToOpen(openFileIntent)) {
            openFileWithoutAvailableApps(file)
            return
        }

        fileActivity.lifecycleScope.launch(Dispatchers.IO) { syncAndOpenFile(file, openFileIntent) }
    }

    fun openFileAsRichDocument(file: OCFile?, context: Context?) {
        context ?: return

        val intent = Intent(context, RichDocumentsEditorWebView::class.java).apply {
            putExtra(ExternalSiteWebView.EXTRA_TITLE, RICH_DOCUMENTS_EDITOR_TITLE)
            putExtra(FileActivity.EXTRA_FILE, file)
            putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
        }
        context.startActivity(intent)
    }

    fun openRichWorkspaceWithTextEditor(file: OCFile?, url: String?, context: Context?) {
        context ?: return

        val intent = Intent(context, TextEditorWebView::class.java).apply {
            putExtra(ExternalSiteWebView.EXTRA_TITLE, TEXT_EDITOR_TITLE)
            putExtra(ExternalSiteWebView.EXTRA_URL, url)
            putExtra(FileActivity.EXTRA_FILE, file)
            putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
        }
        context.startActivity(intent)
    }

    private fun hasAppToOpen(intent: Intent): Boolean = fileActivity.packageManager
        .queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        .isNotEmpty()

    private fun openFileWithoutAvailableApps(file: OCFile) {
        val user = fileActivity.user.orElse(null)
        if (user != null && editorUtils.isEditorAvailable(user, file.mimeType)) {
            TextEditorWebView.startTextEditor(file, fileActivity)
            return
        }

        openRichDocumentFileWithoutAvailableApps(file)
    }

    private fun openRichDocumentFileWithoutAvailableApps(file: OCFile) {
        val capability = fileActivity.storageManager.getCapability(fileActivity.account.name)
        val canOpenAsRichDocument = capability.richDocumentsMimeTypeList?.contains(file.mimeType) == true &&
            capability.richDocumentsDirectEditing.isTrue

        if (canOpenAsRichDocument) {
            openFileAsRichDocument(file, fileActivity)
        } else {
            show(fileActivity, R.string.file_list_no_app_for_file_type)
        }
    }

    private fun syncAndOpenFile(file: OCFile, openFileIntent: Intent) {
        val user = currentAccount.user
        val storageManager = FileDataStorageManager(user, fileActivity.contentResolver)
        if (isDownloadedAndUpToDate(storageManager.getFileById(file.fileId))) {
            startOpenFileIntent(openFileIntent)
            return
        }

        synchronizeFile(file, user, storageManager) { startOpenFileIntent(openFileIntent) }
    }

    private fun isDownloadedAndUpToDate(storedFile: OCFile?): Boolean =
        storedFile != null && storedFile.isDown && !storedFile.isLocalETagOutdated()

    private fun startOpenFileIntent(openFileIntent: Intent) {
        fileActivity.runOnUiThread {
            try {
                openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                fileActivity.startActivity(openFileIntent)
            } catch (_: ActivityNotFoundException) {
                show(fileActivity, R.string.file_list_no_app_for_file_type)
            }
        }
    }

    private fun createOpenFileIntent(file: OCFile): Intent {
        val storagePath = file.storagePath
        val fileUri = file.getExposedFileUri(fileActivity)
        val extension = storagePath.substringAfterLast('.', missingDelimiterValue = "")
        val guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        val openFileIntent = guessedMimeType?.let { Intent(Intent.ACTION_VIEW).setDataAndType(fileUri, it) }
            ?: createIntentFromShortcutFile(storagePath, extension)
            ?: Intent(Intent.ACTION_VIEW).setDataAndType(fileUri, file.mimeType)

        openFileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return openFileIntent
    }

    private fun createIntentFromShortcutFile(storagePath: String, extension: String): Intent? {
        val urlPattern = shortcutUrlPattern(extension) ?: return null
        val url = findUrlInFile(storagePath, urlPattern) ?: return null
        return Intent(Intent.ACTION_VIEW, url.toUri())
    }

    private fun shortcutUrlPattern(extension: String): Regex? = when {
        extension.equals(FILE_EXTENSION_URL, ignoreCase = true) ||
            extension.equals(FILE_EXTENSION_DESKTOP, ignoreCase = true) -> URL_LINE_PATTERN

        extension.equals(FILE_EXTENSION_WEBLOC, ignoreCase = true) -> WEBLOC_STRING_PATTERN

        else -> null
    }

    private fun findUrlInFile(storagePath: String, pattern: Regex): String? = try {
        File(storagePath).useLines(Charsets.UTF_8) { lines ->
            lines.firstNotNullOfOrNull { line -> pattern.find(line)?.groupValues?.get(1) }
        }
    } catch (e: IOException) {
        Log_OC.d(TAG, e.message)
        null
    }
    // endregion

    // region Stream media
    fun streamMediaFile(file: OCFile) {
        showWaitDialog()
        val user = currentAccount.user
        fileActivity.lifecycleScope.launch(Dispatchers.IO) {
            val result = StreamMediaFileOperation(file.localId).execute(user, fileActivity)
            fileActivity.dismissLoadingDialog()

            val streamUrl = result.data?.firstOrNull() as? String
            if (!result.isSuccess || streamUrl == null) {
                fileActivity.runOnUiThread { show(fileActivity, R.string.stream_not_possible_headline) }
                return@launch
            }

            val streamIntent = Intent(Intent.ACTION_VIEW).setDataAndType(streamUrl.toUri(), file.mimeType)
            fileActivity.startActivity(Intent.createChooser(streamIntent, fileActivity.getString(R.string.stream)))
        }
    }
    // endregion

    // region Share
    fun shareFileViaPublicShare(file: OCFile?, password: String?) {
        if (file == null) {
            Log_OC.e(TAG, "Trying to share a NULL OCFile")
            return
        }

        showWaitDialog()
        val intent = operationIntent(OperationsService.ACTION_CREATE_SHARE_VIA_LINK).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            if (!password.isNullOrEmpty()) {
                putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password)
            }
        }
        queueOperation(intent)
    }

    fun shareFolderViaSecureFileDrop(file: OCFile) {
        showWaitDialog()
        val intent = operationIntent(OperationsService.ACTION_CREATE_SECURE_FILE_DROP).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
        }
        queueOperation(intent)
    }

    fun getFileWithLink(file: OCFile, viewThemeUtils: ViewThemeUtils?) {
        val shares = fileActivity.storageManager.getSharesByPathAndType(file.remotePath, ShareType.PUBLIC_LINK, "")

        when {
            shares.size == SINGLE_LINK_SIZE ->
                FileActivity.copyAndShareFileLink(fileActivity, file, shares[0].shareLink, viewThemeUtils)

            fileActivity is FileDisplayActivity -> fileActivity.showDetails(file, SHARING_TAB)

            else -> showShareFile(file)
        }

        fileActivity.refreshList()
    }

    fun shareFileWithSharee(
        file: OCFile?,
        shareeName: String?,
        shareType: ShareType?,
        permissions: Int,
        hideFileDownload: Boolean,
        password: String?,
        expirationTimeInMillis: Long,
        note: String?,
        attributes: String?,
        label: String?,
        showLoadingDialog: Boolean
    ) {
        if (file == null) {
            Log_OC.e(TAG, "Trying to share a NULL OCFile")
            return
        }

        if (showLoadingDialog) {
            showWaitDialog()
        }

        val intent = operationIntent(OperationsService.ACTION_CREATE_SHARE_WITH_SHAREE).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            putExtra(OperationsService.EXTRA_SHARE_WITH, shareeName)
            putExtra(OperationsService.EXTRA_SHARE_TYPE, shareType)
            putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions)
            putExtra(OperationsService.EXTRA_SHARE_HIDE_FILE_DOWNLOAD, hideFileDownload)
            putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password.orEmpty())
            putExtra(OperationsService.EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS, expirationTimeInMillis)
            putExtra(OperationsService.EXTRA_SHARE_NOTE, note.orEmpty())
            putExtra(OperationsService.EXTRA_SHARE_PUBLIC_LABEL, label.orEmpty())
            putExtra(OperationsService.EXTRA_SHARE_ATTRIBUTES, attributes)
        }
        queueOperation(intent)
    }

    fun unShareShare(file: ServerFileInterface?, shareId: Long) {
        file ?: return

        val intent = operationIntent(OperationsService.ACTION_UNSHARE).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            putExtra(OperationsService.EXTRA_SHARE_ID, shareId)
        }
        queueShareIntent(intent)
    }

    fun showShareFile(file: OCFile?) {
        val intent = Intent(fileActivity, ShareActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, file)
            putExtra(FileActivity.EXTRA_USER, fileActivity.user.orElseThrow { RuntimeException() })
        }
        fileActivity.startActivity(intent)
    }

    fun setPasswordToShare(share: OCShare?, password: String?) {
        share ?: return

        val action = if (share.shareLink.isNullOrEmpty()) {
            OperationsService.ACTION_UPDATE_USER_SHARE
        } else {
            OperationsService.ACTION_UPDATE_PUBLIC_SHARE
        }
        val intent = shareUpdateIntent(action, share).apply {
            putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password.orEmpty())
        }
        queueShareIntent(intent)
    }

    fun setPermissionsToShare(share: OCShare, permissions: Int) {
        val intent = shareUpdateIntent(OperationsService.ACTION_UPDATE_USER_SHARE, share).apply {
            putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions)
        }
        queueShareIntent(intent)
    }

    fun updateNoteToShare(share: OCShare?, note: String?) {
        share ?: return

        val intent = shareUpdateIntent(OperationsService.ACTION_UPDATE_SHARE_NOTE, share).apply {
            putExtra(OperationsService.EXTRA_SHARE_NOTE, note)
        }
        queueShareIntent(intent)
    }

    fun updateFilesDownloadLimit(share: OCShare?, newLimit: Int) {
        share ?: return

        val intent = shareUpdateIntent(OperationsService.ACTION_UPDATE_FILES_DOWNLOAD_LIMIT, share).apply {
            putExtra(OperationsService.EXTRA_FILES_DOWNLOAD_LIMIT, newLimit)
        }
        queueShareIntent(intent)
    }

    fun updateShareInformation(
        share: OCShare?,
        permissions: Int,
        hideFileDownload: Boolean,
        password: String?,
        expirationTimeInMillis: Long,
        label: String?
    ) {
        share ?: return

        val attributes = share.attributes

        Log_OC.i(TAG, "-----AFTER UPDATE SHARE-----")
        Log_OC.i(TAG, "ID: ${share.id}")
        Log_OC.i(TAG, "Permission: $permissions")
        Log_OC.i(TAG, "Hide File Download: $hideFileDownload")
        Log_OC.i(TAG, "Label: $label")
        Log_OC.i(TAG, "Attributes: $attributes")

        val intent = shareUpdateIntent(OperationsService.ACTION_UPDATE_SHARE_INFO, share).apply {
            putExtra(OperationsService.EXTRA_SHARE_REMOTE_ID, share.remoteId)
            putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions)
            putExtra(OperationsService.EXTRA_SHARE_HIDE_FILE_DOWNLOAD, hideFileDownload)
            putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password)
            putExtra(OperationsService.EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS, expirationTimeInMillis)
            putExtra(OperationsService.EXTRA_SHARE_PUBLIC_LABEL, label.orEmpty())
            putExtra(OperationsService.EXTRA_SHARE_ATTRIBUTES, attributes)
        }
        queueShareIntent(intent)
    }

    private fun shareUpdateIntent(action: String, share: OCShare): Intent = operationIntent(action).apply {
        putExtra(OperationsService.EXTRA_SHARE_ID, share.id)
    }

    private fun queueShareIntent(intent: Intent) {
        queueOperation(intent)
        showWaitDialog()
    }
    // endregion

    // region Send and set as
    fun sendShareFile(file: OCFile) {
        sendShareFile(file, !file.canReshare())
    }

    fun sendShareFile(file: OCFile?, hideNcSharingOptions: Boolean) {
        val fragmentManager = fileActivity.supportFragmentManager
        if (fragmentManager.findFragmentByTag(TAG_SEND_SHARE_DIALOG) != null) {
            return
        }

        val transaction = fragmentManager.beginTransaction().addToBackStack(null)
        val capability = fileActivity.storageManager.getCapability(fileActivity.account.name)
        val sendShareDialog = SendShareDialog.newInstance(file, hideNcSharingOptions, capability)
        sendShareDialog.setFileOperationsHelper(this)
        sendShareDialog.show(transaction, TAG_SEND_SHARE_DIALOG)
    }

    fun sendFiles(files: Set<OCFile>) {
        val transaction = fileActivity.supportFragmentManager.beginTransaction().addToBackStack(null)
        SendFilesDialog.newInstance(files).show(transaction, TAG_SEND_SHARE_DIALOG)
    }

    fun sendCachedImage(file: OCFile?, packageName: String, activityName: String) {
        if (file == null) {
            Log_OC.wtf(TAG, "Trying to send a NULL OCFile")
            return
        }

        val context = MainApp.getAppContext()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            component = ComponentName(packageName, activityName)
            putExtra(Intent.EXTRA_STREAM, imageCacheUri(context, file))
            putExtra(Intent.ACTION_SEND, true)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        fileActivity.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.actionbar_send_file)))
    }

    fun setPictureAs(file: OCFile?, view: View?) {
        if (file == null) {
            Log_OC.wtf(TAG, "Trying to send a NULL OCFile")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(pictureUri(file), file.mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val chooser = Intent.createChooser(intent, fileActivity.getString(R.string.set_as))
            fileActivity.startActivityForResult(chooser, REQUEST_CODE_SET_PICTURE_AS)
        } catch (_: ActivityNotFoundException) {
            show(view, R.string.picture_set_as_no_app)
        }
    }

    private fun pictureUri(file: OCFile): Uri {
        val context = MainApp.getAppContext()
        if (!file.isDown) {
            return imageCacheUri(context, file)
        }

        val authority = context.getString(R.string.file_provider_authority)
        return FileProvider.getUriForFile(context, authority, File(file.storagePath))
    }

    private fun imageCacheUri(context: Context, file: OCFile): Uri {
        val authority = context.getString(R.string.image_cache_provider_authority)
        return "${UriUtils.URI_CONTENT_SCHEME}$authority${file.remotePath}".toUri()
    }
    // endregion

    // region Sync file or folder
    fun syncFileOrFolder(file: OCFile, postDialogEvent: Boolean = false, syncAll: Boolean = false) {
        if (file.isFolder) {
            startSyncFolderIntent(file, syncAll)
        } else {
            queueSyncFileIntent(file, postDialogEvent)
        }
    }

    fun syncFolderIncludingNestedFiles(file: OCFile) {
        syncFileOrFolder(file, postDialogEvent = false, syncAll = true)
    }

    private fun startSyncFolderIntent(file: ServerFileInterface, syncAll: Boolean) {
        val intent = operationIntent(OperationsService.ACTION_SYNC_FOLDER).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            putExtra(OperationsService.EXTRA_SYNC_ALL, syncAll)
        }
        fileActivity.startService(intent)
    }

    private fun queueSyncFileIntent(file: ServerFileInterface, postDialog: Boolean) {
        val intent = operationIntent(OperationsService.ACTION_SYNC_FILE).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            putExtra(OperationsService.EXTRA_SYNC_FILE_CONTENTS, true)
            putExtra(OperationsService.EXTRA_POST_DIALOG_EVENT, postDialog)
        }
        queueOperation(intent)
    }
    // endregion

    // region Toggle favorite, encryption and lock
    fun toggleFavoriteFiles(files: Collection<OCFile>, shouldBeFavorite: Boolean) {
        files
            .filter { it.isFavorite != shouldBeFavorite }
            .forEach { toggleFavoriteFile(it, shouldBeFavorite) }
    }

    fun toggleFavoriteFile(file: ServerFileInterface, shouldBeFavorite: Boolean) {
        if (file.isFavorite != shouldBeFavorite) {
            EventBus.getDefault().post(FavoriteEvent(file.remotePath, shouldBeFavorite))
        }
    }

    fun toggleEncryption(file: OCFile, shouldBeEncrypted: Boolean) {
        if (file.isEncrypted == shouldBeEncrypted) {
            Log_OC.d(
                TAG,
                "file already in wanted encryption state. " +
                    "isEncrypted: ${file.isEncrypted} should be encrypted: $shouldBeEncrypted"
            )
            return
        }

        if (file.isRootDirectory) {
            showCreateEncryptedFolderDialog()
            return
        }

        Log_OC.d(TAG, "toggling encryption for: ${file.remotePath}")
        EventBus.getDefault().post(file.toEncryptionEvent(shouldBeEncrypted))
    }

    private fun showCreateEncryptedFolderDialog() {
        Log_OC.d(TAG, "toggle encryption triggered in root directory, this call is for creating encrypted folder")
        if (fileActivity !is FileDisplayActivity) {
            Log_OC.e(TAG, "file display activity is not active, cannot show create folder dialog")
            return
        }

        val fragment = fileActivity.listOfFilesFragment
        if (fragment == null) {
            Log_OC.e(TAG, "file list fragment is null, cannot show create folder dialog")
            return
        }

        fragment.createFolder(true)
    }

    fun toggleFileLock(file: OCFile, shouldBeLocked: Boolean) {
        if (file.isLocked != shouldBeLocked) {
            EventBus.getDefault().post(FileLockEvent(file.remotePath, shouldBeLocked))
        }
    }
    // endregion

    // region File operations
    fun renameFile(file: ServerFileInterface?, newFilename: String?) {
        file ?: return

        val intent = operationIntent(OperationsService.ACTION_RENAME).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, file.remotePath)
            putExtra(OperationsService.EXTRA_NEWNAME, newFilename)
        }
        queueOperation(intent)
    }

    fun removeFiles(files: Collection<OCFile?>, onlyLocalCopy: Boolean, inBackground: Boolean) {
        files.forEach { removeFile(it, onlyLocalCopy, inBackground) }
    }

    private fun removeFile(file: OCFile?, onlyLocalCopy: Boolean, inBackground: Boolean) {
        val intent = operationIntent(OperationsService.ACTION_REMOVE).apply {
            putExtra(OperationsService.EXTRA_FILE, file)
            putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy)
            putExtra(OperationsService.EXTRA_IN_BACKGROUND, inBackground)
        }
        queueOperation(intent)
    }

    fun createFolder(remotePath: String?, encrypted: Boolean) {
        val intent = operationIntent(OperationsService.ACTION_CREATE_FOLDER).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath)
            putExtra(OperationsService.EXTRA_ENCRYPTED, encrypted)
        }
        queueOperationAndShowWaitDialog(intent)
    }

    fun restoreFileVersion(fileVersion: FileVersion?) {
        if (fileVersion == null) {
            Log_OC.e(TAG, "Trying to restore a NULL FileVersion")
            return
        }

        showWaitDialog()
        val intent = operationIntent(OperationsService.ACTION_RESTORE_VERSION).apply {
            putExtra(OperationsService.EXTRA_FILE_VERSION, fileVersion)
        }
        queueOperation(intent)
    }

    fun moveOrCopyFiles(action: String?, filePaths: List<String?>, targetFolder: OCFile?) {
        targetFolder ?: return

        filePaths.forEach { path ->
            val intent = operationIntent(action).apply {
                putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.remotePath)
                putExtra(OperationsService.EXTRA_REMOTE_PATH, path)
            }
            queueOperation(intent)
        }
        showWaitDialog()
    }

    fun cancelTransference(file: OCFile) {
        val currentUser = fileActivity.user.orElseThrow { IllegalStateException() }
        if (file.isFolder) {
            fileActivity.operationsServiceBinder?.cancel(currentUser.toPlatformAccount(), file)
        }

        val fileDownloadHelper = FileDownloadHelper.instance()
        if (fileDownloadHelper.isDownloading(currentUser, file)) {
            val files = fileActivity.storageManager.getAllFilesRecursivelyInsideFolder(file)
            fileDownloadHelper.cancelPendingOrCurrentDownloads(currentUser, files)
        }

        if (file.isFolder) {
            fileDownloadHelper.cancelFolderDownload()
        }

        val accountName = currentUser.accountName
        val fileUploadHelper = FileUploadHelper.instance()
        if (fileUploadHelper.isUploading(file.remotePath, accountName)) {
            FileUploadWorker.cancelUpload(file.remotePath, accountName) {
                fileUploadHelper.updateUploadStatus(
                    file.remotePath,
                    accountName,
                    UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED
                )
            }
        }
    }

    fun exportFiles(files: Collection<OCFile>, backgroundJobManager: BackgroundJobManager) {
        backgroundJobManager.startImmediateFilesExportJob(files)
    }
    // endregion

    // region Albums
    fun createAlbum(albumName: String?) {
        val intent = operationIntent(OperationsService.ACTION_CREATE_ALBUM).apply {
            putExtra(OperationsService.EXTRA_ALBUM_NAME, albumName)
        }
        queueOperationAndShowWaitDialog(intent)
    }

    fun addFileToAlbum(files: Collection<OCFile>) {
        val paths = files.mapTo(ArrayList<String>(files.size)) { it.remotePath }
        fileActivity.startActivity(AlbumsPickerActivity.intentForPickingAlbum(fileActivity, paths))
    }

    fun albumCopyFiles(filePaths: List<String?>?, targetFolder: String?) {
        if (filePaths.isNullOrEmpty()) {
            return
        }

        connectivityService.isNetworkAndServerAvailable { isAvailable ->
            if (!isAvailable) {
                show(fileActivity, fileActivity.getString(R.string.offline_mode))
                return@isNetworkAndServerAvailable
            }

            filePaths.forEach { path ->
                val intent = operationIntent(OperationsService.ACTION_ALBUM_COPY_FILE).apply {
                    putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder)
                    putExtra(OperationsService.EXTRA_REMOTE_PATH, path)
                }
                queueOperation(intent)
            }
            showWaitDialog()
        }
    }

    fun renameAlbum(oldAlbumName: String?, newAlbumName: String?) {
        val intent = operationIntent(OperationsService.ACTION_RENAME_ALBUM).apply {
            putExtra(OperationsService.EXTRA_REMOTE_PATH, oldAlbumName)
            putExtra(OperationsService.EXTRA_NEWNAME, newAlbumName)
        }
        queueOperationAndShowWaitDialog(intent)
    }

    fun removeAlbum(albumName: String?) {
        val intent = operationIntent(OperationsService.ACTION_REMOVE_ALBUM).apply {
            putExtra(OperationsService.EXTRA_ALBUM_NAME, albumName)
        }
        queueOperationAndShowWaitDialog(intent)
    }

    fun albumPublicShareLink(albumName: String?, isCreateShare: Boolean) {
        val intent = operationIntent(OperationsService.ACTION_PUBLIC_SHARE_LINK_ALBUM).apply {
            putExtra(OperationsService.EXTRA_ALBUM_NAME, albumName)
            putExtra(OperationsService.EXTRA_CREATE_ALBUM_SHARE, isCreateShare)
        }
        queueOperationAndShowWaitDialog(intent)
    }
    // endregion

    // region Camera upload
    fun uploadFromCamera(activity: Activity, requestCode: Int, isVideo: Boolean) {
        val action = if (isVideo) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE
        val cameraUri = FileProvider.getUriForFile(
            activity.applicationContext,
            activity.getString(R.string.file_provider_authority),
            createCameraFile(activity, isVideo)
        )
        val intent = Intent(action).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        if (intent.resolveActivity(activity.packageManager) == null) {
            show(activity, R.string.upload_direct_camera_not_found)
            return
        }

        activity.startActivityForResult(intent, requestCode)
    }
    // endregion

    // region Operations service
    private fun operationIntent(action: String?): Intent = Intent(fileActivity, OperationsService::class.java).apply {
        this.action = action
        putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.account)
    }

    private fun queueOperation(intent: Intent) {
        opIdWaitingFor = fileActivity.operationsServiceBinder.queueNewOperation(intent)
    }

    private fun queueOperationAndShowWaitDialog(intent: Intent) {
        queueOperation(intent)
        showWaitDialog()
    }

    private fun showWaitDialog() {
        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment))
    }
    // endregion

    companion object {
        private val TAG = FileOperationsHelper::class.java.simpleName
        private val URL_LINE_PATTERN = Regex("^URL=(.+)$")
        private val WEBLOC_STRING_PATTERN = Regex("<string>(.+)</string>")
        private const val FILE_EXTENSION_URL = "url"
        private const val FILE_EXTENSION_DESKTOP = "desktop"
        private const val FILE_EXTENSION_WEBLOC = "webloc"
        private const val TAG_SEND_SHARE_DIALOG = "TAG_SEND_SHARE_DIALOG"
        private const val RICH_DOCUMENTS_EDITOR_TITLE = "Collabora"
        private const val TEXT_EDITOR_TITLE = "Text"
        private const val SINGLE_LINK_SIZE = 1
        private const val SHARING_TAB = 1
        private const val NO_CONFLICT_UPLOAD_ID = -1L
        private const val REQUEST_CODE_SET_PICTURE_AS = 200
        private const val FILE_NOT_SYNCED_AFTER_CONFLICT_CHECK_WAIT_MILLIS = 3L * AppConstants.MILLIS_PER_SECOND
        private const val UNKNOWN_AVAILABLE_SPACE = -1L
        private const val TIMESTAMP_FILE_NAME_PATTERN = "yyyy-MM-dd_HHmmss"
        private const val CAMERA_PHOTO_FILE_NAME = "/directCameraUpload.jpg"
        private const val CAMERA_VIDEO_FILE_NAME = "/directCameraUpload.mp4"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

        fun createCameraFile(activity: Activity, isVideo: Boolean): File {
            val directory = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val fileName = if (isVideo) CAMERA_VIDEO_FILE_NAME else CAMERA_PHOTO_FILE_NAME
            return File("${activity.getExternalFilesDir(directory)}$fileName")
        }

        fun getCapturedImageName(): String = getTimestampedFileName(PHOTO_EXTENSION)

        fun getCapturedVideoName(): String = getTimestampedFileName(VIDEO_EXTENSION)

        fun getTimestampedFileName(extension: String): String =
            SimpleDateFormat(TIMESTAMP_FILE_NAME_PATTERN, Locale.US).format(Date()) + extension

        fun getAvailableSpaceOnDevice(): Long {
            val storagePath = MainApp.getStoragePath() ?: return UNKNOWN_AVAILABLE_SPACE
            val stat = try {
                StatFs(storagePath)
            } catch (_: IllegalArgumentException) {
                return UNKNOWN_AVAILABLE_SPACE
            }

            return stat.blockSizeLong * stat.availableBlocksLong
        }

        @JvmStatic
        fun isEndToEndEncryptionSetup(context: Context?, user: User?): Boolean {
            val arbitraryDataProvider = ArbitraryDataProviderImpl(context)
            return arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY).isNotEmpty() &&
                arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY).isNotEmpty()
        }
    }
}
