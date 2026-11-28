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
import com.owncloud.android.datamodel.AutoUploadFolder
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.AutoUploadPreferenceDialogFragment
import com.owncloud.android.ui.dialog.parcel.AutoUploadFolderParcelable
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

    override fun onSyncStatusToggleClick(item: AutoUploadFolder) {
        persistence.persistSyncStatus(item)
        onSyncStateApplied(item)
    }

    override fun onSyncFolderSettingsClick(item: AutoUploadFolder) {
        check(Looper.getMainLooper().isCurrentThread) { "This must be called on the main thread!" }

        val dialog = AutoUploadPreferenceDialogFragment.newInstance(item)
        dialogs.showDialog(dialog, AUTO_UPLOAD_PREFERENCE_DIALOG_TAG)
    }

    override fun onSyncFolderDeleteUploadedClick(item: AutoUploadFolder) {
        dialogs.showDeleteUploadedDialog(item)
    }

    override fun onVisibilityToggleClick(item: AutoUploadFolder) {
        item.isHidden = !item.isHidden
        persistence.saveOrUpdateAutoUploadFolder(item)
        getAdapter().onItemVisibilityChanged(item)
        onItemVisibilityChanged()
    }

    override fun showSubFolderWarningDialog() {
        dialogs.showSubFolderWarningDialog()
    }

    fun onSaveAutoUploadFolderPreference(syncedFolder: AutoUploadFolderParcelable) {
        if (isNewCustomFolder(syncedFolder)) {
            addFolder(syncedFolder)
            return
        }

        updateFolder(syncedFolder)
    }

    fun onDeleteAutoUploadFolderPreference(syncedFolder: AutoUploadFolderParcelable) {
        Log_OC.d(TAG, "deleting auto upload configuration: " + syncedFolder.remotePath)

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(syncedFolder.id, syncedFolder.account)
            persistence.deleteAutoUploadFolder(syncedFolder.id)
            withContext(Dispatchers.Main) {
                val adapter = getAdapter()
                adapter.getItem(findSection(adapter, syncedFolder))?.let(adapter::removeItem)
            }
        }
    }

    private fun isNewCustomFolder(syncedFolder: AutoUploadFolderParcelable): Boolean =
        syncedFolder.type == MediaFolderType.CUSTOM && syncedFolder.id == AutoUploadFolder.UNPERSISTED_ID

    private fun findSection(adapter: AutoUploadFolderAdapter, syncedFolder: AutoUploadFolderParcelable): Int =
        if (syncedFolder.id == AutoUploadFolder.UNPERSISTED_ID) {
            adapter.getSectionByLocalPathAndType(syncedFolder.localPath, syncedFolder.type.id)
        } else {
            adapter.getSectionById(syncedFolder.id)
        }

    private fun addFolder(syncedFolder: AutoUploadFolderParcelable) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            val item = itemFactory.createAutoUploadFolder(syncedFolder)
            persistence.saveOrUpdateAutoUploadFolder(item)

            withContext(Dispatchers.Main) {
                getAdapter().addItem(item)

                onSyncStateApplied(item)
            }
        }
    }

    private fun updateFolder(syncedFolder: AutoUploadFolderParcelable) {
        val adapter = getAdapter()
        val section = findSection(adapter, syncedFolder)
        val item = adapter.getItem(section)
        if (item == null) {
            addFolder(syncedFolder)
            return
        }

        itemFactory.updateAutoUploadFolderItem(item, syncedFolder)
        persistence.saveOrUpdateAutoUploadFolder(item)
        adapter.notifyItemChanged(adapter.getSectionHeaderIndex(section))

        onSyncStateApplied(item)
    }

    private fun onSyncStateApplied(item: AutoUploadFolder) {
        Log_OC.d(TAG, "auto-upload configuration enabled=${item.isEnabled}: ${item.remotePath}")

        if (item.isEnabled) {
            dialogs.showBatteryOptimizationDialogIfNeeded()
            return
        }

        val accountName = item.account ?: return
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            fileUploadHelper.removeEntityFromUploadEntities(item.id, accountName)
        }
    }

    companion object {
        private val TAG = AutoUploadClickHandler::class.java.simpleName
        const val AUTO_UPLOAD_PREFERENCE_DIALOG_TAG = "AUTO_UPLOAD_PREFERENCE_DIALOG"
    }
}
