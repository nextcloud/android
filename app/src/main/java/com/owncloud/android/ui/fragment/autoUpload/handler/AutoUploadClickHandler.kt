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
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.AutoUploadPreferenceDialogFragment
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.ui.fragment.autoUpload.adapter.AutoUploadFolderAdapter
import com.owncloud.android.ui.fragment.autoUpload.adapter.AutoUploadFolderClickListener
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadDisplayItemFactory
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadPersistence
import com.owncloud.android.ui.fragment.autoUpload.ui.AutoUploadDialogs
import com.owncloud.android.utils.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoUploadClickHandler(
    private val fragment: Fragment,
    private val itemFactory: AutoUploadDisplayItemFactory,
    private val persistence: AutoUploadPersistence,
    private val dialogs: AutoUploadDialogs,
    private val getAdapter: () -> AutoUploadFolderAdapter,
    private val onItemVisibilityChanged: () -> Unit
) : AutoUploadFolderClickListener {

    private val fileUploadHelper = FileUploadHelper.instance()

    fun onCreateCustomFolderClick(activity: AppCompatActivity, accountName: String) {
        Log_OC.d(TAG, "Show custom folder dialog")
        if (!PermissionUtil.checkStoragePermission(activity)) {
            PermissionUtil.requestStoragePermissionIfNeeded(activity)
            return
        }

        onSyncFolderSettingsClick(itemFactory.createEmptyCustomFolder(accountName))
    }

    fun onDeleteAllUploadedClick(user: User) {
        dialogs.showDeleteAllUploadedDialog(user)
    }

    override fun onSyncStatusToggleClick(item: SyncedFolderDisplayItem) {
        persistence.persistSyncStatus(item)
        onSyncStateApplied(item)
    }

    override fun onSyncFolderSettingsClick(item: SyncedFolderDisplayItem) {
        check(Looper.getMainLooper().isCurrentThread) { "This must be called on the main thread!" }

        val dialog = AutoUploadPreferenceDialogFragment.newInstance(item)
        dialogs.showDialog(dialog, AUTO_UPLOAD_PREFERENCE_DIALOG_TAG)
    }

    override fun onSyncFolderDeleteUploadedClick(item: SyncedFolderDisplayItem) {
        dialogs.showDeleteUploadedDialog(item)
    }

    override fun onVisibilityToggleClick(item: SyncedFolderDisplayItem) {
        item.isHidden = !item.isHidden
        persistence.saveOrUpdateAutoUploadFolder(item)
        getAdapter().onItemVisibilityChanged(item)
        onItemVisibilityChanged()
    }

    override fun showSubFolderWarningDialog() {
        dialogs.showSubFolderWarningDialog()
    }

    fun onSaveAutoUploadFolderPreference(syncedFolder: SyncedFolderParcelable) {
        val item = if (isNewCustomFolder(syncedFolder)) {
            addFolder(syncedFolder)
        } else {
            updateFolder(syncedFolder)
        }

        onSyncStateApplied(item)
    }

    fun onDeleteAutoUploadFolderPreference(syncedFolder: SyncedFolderParcelable) {
        Log_OC.d(TAG, "deleting auto upload configuration: " + syncedFolder.remotePath)

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(syncedFolder.id)
            persistence.deleteAutoUploadFolder(syncedFolder.id)
            withContext(Dispatchers.Main) {
                val adapter = getAdapter()
                adapter.getItem(findSection(adapter, syncedFolder))?.let(adapter::removeItem)
            }
        }
    }

    private fun isNewCustomFolder(syncedFolder: SyncedFolderParcelable): Boolean =
        syncedFolder.type == MediaFolderType.CUSTOM && syncedFolder.id == SyncedFolder.UNPERSISTED_ID

    private fun findSection(adapter: AutoUploadFolderAdapter, syncedFolder: SyncedFolderParcelable): Int =
        if (syncedFolder.id == SyncedFolder.UNPERSISTED_ID) {
            adapter.getSectionByLocalPathAndType(syncedFolder.localPath, syncedFolder.type.id)
        } else {
            adapter.getSectionById(syncedFolder.id)
        }

    private fun addFolder(syncedFolder: SyncedFolderParcelable): SyncedFolderDisplayItem {
        val item = itemFactory.createAutoUploadFolder(syncedFolder)
        persistence.saveOrUpdateAutoUploadFolder(item)
        getAdapter().addItem(item)

        return item
    }

    private fun updateFolder(syncedFolder: SyncedFolderParcelable): SyncedFolderDisplayItem {
        val adapter = getAdapter()
        val section = findSection(adapter, syncedFolder)
        val item = adapter.getItem(section) ?: return addFolder(syncedFolder)

        itemFactory.updateAutoUploadFolderItem(item, syncedFolder)
        persistence.saveOrUpdateAutoUploadFolder(item)
        adapter.notifyItemChanged(adapter.getSectionHeaderIndex(section))

        return item
    }

    private fun onSyncStateApplied(item: SyncedFolderDisplayItem) {
        Log_OC.d(TAG, "auto-upload configuration enabled=${item.isEnabled}: ${item.remotePath}")

        if (item.isEnabled) {
            dialogs.showBatteryOptimizationDialogIfNeeded()
            return
        }

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(item.id)
        }
    }

    companion object {
        private val TAG = AutoUploadClickHandler::class.java.simpleName
        const val AUTO_UPLOAD_PREFERENCE_DIALOG_TAG = "AUTO_UPLOAD_PREFERENCE_DIALOG"
    }
}
