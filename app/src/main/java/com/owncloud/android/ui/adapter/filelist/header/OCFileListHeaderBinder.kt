/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist.header

import android.app.Activity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemApp
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.ListHeaderBinding
import com.owncloud.android.databinding.ListHeaderOpenInBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.adapter.filelist.recommended.RecommendedFilesAdapter
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
internal class OCFileListHeaderBinder(
    private val activity: Activity,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragmentInterface: OCFileListFragmentInterface,
    private val preferences: AppPreferences,
    private val capability: OCCapability,
    private val currentUser: () -> User
) {

    // region Public methods
    fun bind(
        holder: OCFileListHeaderViewHolder,
        directory: OCFile?,
        recommendedFilesAdapter: RecommendedFilesAdapter?
    ) {
        holder.headerView.setOnClickListener { fragmentInterface.onHeaderClicked() }
        bindRichWorkspace(holder, directory)
        bindRecommendedFiles(holder.binding, recommendedFilesAdapter)
        bindOpenInNotes(holder.binding.openIn, directory)
    }

    fun hasRichWorkspace(directory: OCFile): Boolean = directory.richWorkspace?.any { it > ' ' } == true

    fun shouldShowOpenInNotes(directory: OCFile?): Boolean {
        if (!preferences.isShowEcosystemApps) {
            return false
        }

        val notesFolderPath = capability.notesFolderPath
        val currentPath = directory?.decryptedRemotePath
        return notesFolderPath != null && currentPath?.startsWith(notesFolderPath) == true
    }
    // endregion

    // region Private methods
    private fun bindRichWorkspace(holder: OCFileListHeaderViewHolder, directory: OCFile?) {
        val text = directory?.richWorkspace
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

    private fun bindRecommendedFiles(binding: ListHeaderBinding, recommendedFilesAdapter: RecommendedFilesAdapter?) {
        val showRecommendedFiles = recommendedFilesAdapter != null
        binding.recommendedFilesRecyclerView.setVisibleIf(showRecommendedFiles)
        binding.recommendedFilesTitle.setVisibleIf(showRecommendedFiles)
        binding.allFilesTitle.setVisibleIf(showRecommendedFiles)

        if (recommendedFilesAdapter == null) {
            return
        }

        binding.recommendedFilesRecyclerView.run {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedFilesAdapter
        }
    }

    private fun bindOpenInNotes(binding: ListHeaderOpenInBinding, directory: OCFile?) {
        val showOpenInNotes = shouldShowOpenInNotes(directory)
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
        binding.openInButton.setOnClickListener {
            ecosystemManager.openApp(EcosystemApp.NOTES, currentUser().accountName)
        }
    }
    // endregion

    companion object {
        private const val EMPTY_RICH_WORKSPACE = " "
    }
}
