/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.adapter

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleCoroutineScope
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.google.android.material.button.MaterialButton
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.calculateScanInterval
import com.nextcloud.utils.extensions.hasEnabledParent
import com.owncloud.android.R
import com.owncloud.android.databinding.SyncedFoldersItemHeaderBinding
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.ui.fragment.autoUpload.util.iconId
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class AutoUploadFolderHeaderViewHolder(
    private val binding: SyncedFoldersItemHeaderBinding,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val clock: Clock,
    private val light: Boolean,
    private val viewThemeUtils: ViewThemeUtils,
    private val clickListener: AutoUploadFolderClickListener,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService
) : SectionedViewHolder(binding.root) {

    private val context = binding.root.context
    private val resources = context.resources
    private val screenWidth = resources.displayMetrics.widthPixels
    private val minimumSizeForTouchableArea = resources.getDimensionPixelSize(R.dimen.minimum_size_for_touchable_area)
    private val standardDoubleMargin = resources.getDimensionPixelSize(R.dimen.standard_double_margin)
    private val titleBottomMargin = resources.getDimensionPixelSize(R.dimen.synced_folders_title_margin)

    private var detailsJob: Job? = null

    fun bind(item: SyncedFolderDisplayItem, visibleItems: List<SyncedFolderDisplayItem>) = with(binding) {
        headerContainer.isVisible = true
        title.text = item.folderName
        type.setImageResource(item.type.iconId())

        syncStatusButton.isVisible = true
        syncStatusButton.showSyncState(item.isEnabled)
        syncStatusButton.setOnClickListener {
            item.setEnabled(!item.isEnabled, clock.currentTime)
            syncStatusButton.showSyncState(item.isEnabled)
            clickListener.onSyncStatusToggleClick(item)
            loadDetails(item, visibleItems)
        }

        settingsButton.isVisible = !light
        settingsButton.setOnClickListener { showOverflowMenu(it, item) }
        subFolderWarningButton.setOnClickListener { clickListener.showSubFolderWarningDialog() }

        loadDetails(item, visibleItems)
    }

    private fun loadDetails(item: SyncedFolderDisplayItem, visibleItems: List<SyncedFolderDisplayItem>) {
        detailsJob?.cancel()
        detailsJob = lifecycleScope.launch {
            val hasEnabledParent = withContext(Dispatchers.IO) { visibleItems.hasEnabledParent(item.localPath) }
            val scanMessage = withContext(Dispatchers.IO) { getNextScanMessage(item) }
            showDetails(item, hasEnabledParent, scanMessage)
        }
    }

    private fun showDetails(item: SyncedFolderDisplayItem, hasEnabledParent: Boolean, scanMessage: String?) =
        with(binding) {
            val showScanMessage = item.isEnabled && scanMessage != null

            subFolderWarningButton.isVisible = hasEnabledParent
            scanIndicatorText.isVisible = showScanMessage
            scanIndicatorText.text = scanMessage
            scanIndicatorText.maxWidth = getScanMessageMaxWidth(hasEnabledParent)
            title.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = if (showScanMessage) 0 else titleBottomMargin
            }
        }

    private fun getScanMessageMaxWidth(showsSubFolderWarning: Boolean): Int {
        val trailingIconCount = if (showsSubFolderWarning) TRAILING_ICON_COUNT + 1 else TRAILING_ICON_COUNT

        return screenWidth - minimumSizeForTouchableArea * trailingIconCount - standardDoubleMargin
    }

    private fun getNextScanMessage(folder: SyncedFolder): String? {
        val (scanIntervalMs, messageId) = folder.calculateScanInterval(connectivityService, powerManagementService)
        messageId ?: return null

        val timeUntilNextScan = (folder.lastScanTimestampMs + scanIntervalMs - clock.currentTime).milliseconds
        if (timeUntilNextScan.inWholeMinutes <= 0) {
            return null
        }

        return context.getString(messageId)
    }

    private fun showOverflowMenu(anchor: View, item: SyncedFolderDisplayItem) {
        PopupMenu(context, anchor).apply {
            inflate(R.menu.synced_folders_adapter)
            menu.findItem(R.id.action_auto_upload_folder_toggle_visibility).isChecked = item.isHidden
            updateDeleteUploadedMenuItem(menu.findItem(R.id.action_auto_upload_folder_delete_uploaded), item)
            setOnMenuItemClickListener { onMenuItemClick(it, item) }
            show()
        }
    }

    private fun onMenuItemClick(menuItem: MenuItem, item: SyncedFolderDisplayItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_auto_upload_folder_toggle_visibility -> clickListener.onVisibilityToggleClick(item)
            R.id.action_auto_upload_folder_settings -> clickListener.onSyncFolderSettingsClick(item)
            R.id.action_auto_upload_folder_delete_uploaded -> clickListener.onSyncFolderDeleteUploadedClick(item)
            else -> return false
        }

        return true
    }

    private fun updateDeleteUploadedMenuItem(menuItem: MenuItem, item: SyncedFolderDisplayItem) {
        val localFolder = item.localPath?.let(::File)
        lifecycleScope.launch {
            val writable = FileUtil.isFolderWritable(localFolder)
            menuItem.isEnabled = writable
            menuItem.isVisible = writable
        }
    }

    private fun MaterialButton.showSyncState(enabled: Boolean) {
        icon = if (enabled) {
            viewThemeUtils.platform.tintDrawable(context, R.drawable.ic_cloud_sync_on, ColorRole.PRIMARY)
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_cloud_sync_off)
        }
    }

    companion object {
        private const val TRAILING_ICON_COUNT = 2
    }
}
