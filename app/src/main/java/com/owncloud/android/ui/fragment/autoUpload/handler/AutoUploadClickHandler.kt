/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.handler

import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.SyncedFolderAdapter
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadDisplayItemFactory
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadPersistence
import com.owncloud.android.ui.fragment.autoUpload.ui.AutoUploadDialogs
import com.owncloud.android.utils.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "TooManyFunctions")
class AutoUploadClickHandler(
    private val fragment: Fragment,
    private val itemFactory: AutoUploadDisplayItemFactory,
    private val persistence: AutoUploadPersistence,
    private val dialogs: AutoUploadDialogs,
    private val backgroundJobManager: BackgroundJobManager,
    private val getAdapter: () -> SyncedFolderAdapter,
    private val onItemVisibilityChanged: () -> Unit
) : SyncedFolderAdapter.ClickListener {

    private val fileUploadHelper = FileUploadHelper.instance()

    fun onCreateCustomFolderClick(activity: AppCompatActivity, accountName: String) {
        Log_OC.d(TAG, "Show custom folder dialog")
        if (!PermissionUtil.checkStoragePermission(activity)) {
            PermissionUtil.requestStoragePermissionIfNeeded(activity)
            return
        }

        onSyncFolderSettingsClick(0, itemFactory.createEmptyCustomFolder(accountName))
    }

    fun onDeleteAllUploadedClick(user: User) {
        dialogs.showDeleteAllUploadedDialog(user)
    }

    override fun onSyncStatusToggleClick(section: Int, item: SyncedFolderDisplayItem?) {
        item ?: return
        persistence.persistSyncStatus(item)

        if (item.isEnabled) {
            Log_OC.d(TAG, "auto-upload configuration sync status is enabled: " + item.remotePath)
            backgroundJobManager.startAutoUpload(item, overridePowerSaving = false)
            dialogs.showBatteryOptimizationDialogIfNeeded()
            return
        }

        Log_OC.d(TAG, "auto-upload configuration sync status is disabled: " + item.remotePath)
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(item.id)
        }
    }

    override fun onSyncFolderSettingsClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?) {
        check(Looper.getMainLooper().isCurrentThread) { "This must be called on the main thread!" }

        val dialog = SyncedFolderPreferencesDialogFragment.newInstance(syncedFolderDisplayItem, section) ?: return
        dialogs.showDialog(dialog, SYNCED_FOLDER_PREFERENCES_DIALOG_TAG)
    }

    override fun onSyncFolderDeleteUploadedClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?) {
        syncedFolderDisplayItem ?: return
        dialogs.showDeleteUploadedDialog(syncedFolderDisplayItem)
    }

    override fun onVisibilityToggleClick(section: Int, item: SyncedFolderDisplayItem?) {
        item ?: return
        item.isHidden = !item.isHidden
        persistence.saveOrUpdateAutoUploadFolder(item)
        getAdapter().setSyncFolderItem(section, item)
        onItemVisibilityChanged()
    }

    override fun showSubFolderWarningDialog() {
        dialogs.showSubFolderWarningDialog()
    }

    fun onSaveAutoUploadFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        syncedFolder ?: return

        val isSaved = if (isNewCustomFolder(syncedFolder)) {
            addCustomFolder(syncedFolder)
        } else {
            updateExistingFolder(syncedFolder)
        }

        if (isSaved && syncedFolder.isEnabled) {
            dialogs.showBatteryOptimizationDialogIfNeeded()
        }
    }

    fun onDeleteAutoUploadFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        syncedFolder ?: return
        Log_OC.d(TAG, "deleting auto upload configuration: " + syncedFolder.remotePath)

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(syncedFolder.id)
            persistence.deleteAutoUploadFolder(syncedFolder.id)
            withContext(Dispatchers.Main) {
                getAdapter().removeItem(syncedFolder.section)
            }
        }
    }

    private fun isNewCustomFolder(syncedFolder: SyncedFolderParcelable): Boolean =
        syncedFolder.type == MediaFolderType.CUSTOM && syncedFolder.id == SyncedFolder.UNPERSISTED_ID

    private fun addCustomFolder(syncedFolder: SyncedFolderParcelable): Boolean {
        val newCustomFolder = itemFactory.createNewCustomFolder(syncedFolder)
        persistence.saveOrUpdateAutoUploadFolder(newCustomFolder)
        getAdapter().addSyncFolderItem(newCustomFolder)
        return true
    }

    private fun updateExistingFolder(syncedFolder: SyncedFolderParcelable): Boolean {
        val adapter = getAdapter()
        val item = adapter.get(syncedFolder.section) ?: return false
        itemFactory.updateAutoUploadFolderItem(item, syncedFolder)
        persistence.saveOrUpdateAutoUploadFolder(item)
        adapter.notifyItemChanged(adapter.getSectionHeaderIndex(syncedFolder.section))
        return true
    }

    companion object {
        private val TAG = AutoUploadClickHandler::class.java.simpleName
        const val SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG"
    }
}
