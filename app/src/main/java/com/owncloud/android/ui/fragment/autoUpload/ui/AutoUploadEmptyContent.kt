/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.autoUpload.ui

import android.view.View
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentAutoUploadBinding
import com.owncloud.android.ui.adapter.SyncedFolderAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadEmptyContent(
    private val binding: FragmentAutoUploadBinding,
    private val adapter: SyncedFolderAdapter,
    viewThemeUtils: ViewThemeUtils
) {

    private val emptyList = binding.emptyList
    private val resources = binding.root.resources

    private val hasOnlyHiddenItems: Boolean
        get() = adapter.sectionCount == 0 && adapter.unfilteredSectionCount > 0

    private val hasNoItems: Boolean
        get() = adapter.sectionCount == 0 && adapter.unfilteredSectionCount == 0

    init {
        emptyList.emptyListIcon.setImageResource(R.drawable.nav_synced_folders)
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(emptyList.emptyListViewAction)
        emptyList.emptyListViewAction.setOnClickListener { showHiddenItems() }
    }

    fun showList() {
        binding.list.visibility = View.VISIBLE
        binding.loadingContent.visibility = View.GONE
        checkAndShowEmptyListContent()
    }

    fun checkAndShowEmptyListContent() {
        when {
            hasOnlyHiddenItems -> showHiddenFoldersAction()
            hasNoItems -> showEmptyContent(resources.getString(R.string.synced_folders_no_results))
            else -> emptyList.emptyListView.visibility = View.GONE
        }
    }

    fun showLoadingContent() {
        binding.loadingContent.visibility = View.VISIBLE
        emptyList.emptyListViewAction.visibility = View.GONE
    }

    private fun showHiddenItems() {
        if (!hasOnlyHiddenItems) {
            return
        }

        adapter.toggleHiddenItemsVisibility()
        emptyList.emptyListView.visibility = View.GONE
        binding.list.visibility = View.VISIBLE
    }

    private fun showHiddenFoldersAction() {
        val hiddenFoldersCount = adapter.hiddenFolderCount
        val hiddenFoldersText = resources.getQuantityString(
            R.plurals.synced_folders_show_hidden_folders,
            hiddenFoldersCount,
            hiddenFoldersCount
        )

        showEmptyContent(hiddenFoldersText)
        emptyList.emptyListViewAction.text = hiddenFoldersText
        emptyList.emptyListViewAction.visibility = View.VISIBLE
        emptyList.emptyListViewText.visibility = View.GONE
    }

    private fun showEmptyContent(message: String) {
        emptyList.emptyListViewAction.visibility = View.GONE
        emptyList.emptyListView.visibility = View.VISIBLE
        binding.list.visibility = View.GONE
        binding.loadingContent.visibility = View.GONE
        emptyList.emptyListViewHeadline.text = resources.getString(R.string.drawer_synced_folders)
        emptyList.emptyListViewText.text = message
        emptyList.emptyListViewText.visibility = View.VISIBLE
        emptyList.emptyListIcon.visibility = View.VISIBLE
    }
}
