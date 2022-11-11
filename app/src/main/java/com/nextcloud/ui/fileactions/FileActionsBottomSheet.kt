/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.ui.fileactions

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

// TODO give events back
// TODO theming
class FileActionsBottomSheet private constructor() : BottomSheetDialogFragment(), Injectable {

    lateinit var componentsGetter: ComponentsGetter

    // TODO replace with fragment listener from Activity
    lateinit var clickListener: ClickListener

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var vmFactory: ViewModelFactory

    @Inject
    lateinit var currentUserProvider: CurrentAccountProvider

    lateinit var viewModel: FileActionsViewModel

    private var _binding: FileActionsBottomSheetBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val args = requireArguments()
        // TODO pass only IDs, fetch from DB to avoid TransactionTooLarge
        val files: Array<OCFile>? = args.getParcelableArray(ARG_FILES) as Array<OCFile>?
        require(files != null)
        val numberOfAllFiles: Int = args.getInt(ARG_ALL_FILES_COUNT, 1)
        val isOverflow = args.getBoolean(ARG_IS_OVERFLOW, false)
        val additionalFilter: IntArray? = args.getIntArray(ARG_ADDITIONAL_FILTER)

        viewModel = ViewModelProvider(this, vmFactory)[FileActionsViewModel::class.java]
        _binding = FileActionsBottomSheetBinding.inflate(inflater, container, false)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            toggleLoadingOrContent(state)
            when (state) {
                is FileActionsViewModel.UiState.LoadedForSingleFile -> {
                    if (state.lockInfo != null) {
                        displayLockInfo(state.lockInfo, inflater)
                    }
                    displayActions(state.actions, inflater)
                    displayTitle(state.titleFile)
                }
                is FileActionsViewModel.UiState.LoadedForMultipleFiles -> {
                    displayActions(state.actions, inflater)
                    displayTitle(state.fileCount)
                }
                FileActionsViewModel.UiState.Loading -> {}
            }
        }

        viewModel.clickActionId.observe(viewLifecycleOwner) { id ->
            dispatchActionClick(id)
        }

        viewModel.load(files.toList(), componentsGetter, numberOfAllFiles, isOverflow, additionalFilter)

        return binding.root
    }

    private fun toggleLoadingOrContent(state: FileActionsViewModel.UiState) {
        if (state is FileActionsViewModel.UiState.Loading) {
            binding.bottomSheetLoading.isVisible = true
            binding.bottomSheetContent.isVisible = false
        } else {
            binding.bottomSheetLoading.isVisible = false
            binding.bottomSheetContent.isVisible = true
        }
    }

    private fun displayActions(
        actions: List<FileAction>,
        inflater: LayoutInflater
    ) {
        actions.forEach { action ->
            val view = inflateActionView(inflater, action)
            binding.fileActionsList.addView(view)
        }
    }

    private fun displayTitle(titleFile: OCFile?) {
        titleFile?.decryptedFileName?.let {
            binding.title.text = it
        }
    }

    private fun displayLockInfo(lockInfo: FileActionsViewModel.LockInfo, inflater: LayoutInflater) {
        val view = FileActionsBottomSheetItemBinding.inflate(inflater, binding.fileActionsList, false)
            .apply {
                // TODO theme
                val textColor = ColorStateList.valueOf(resources.getColor(R.color.secondary_text_color, null))
                root.isClickable = false
                text.setTextColor(textColor)
                text.text = getLockedByText(lockInfo)
                if (lockInfo.lockedUntil != null) {
                    textLine2.text = getLockedUntilText(lockInfo)
                    textLine2.isVisible = true
                }
                if (lockInfo.lockType != FileLockType.COLLABORATIVE) {
                    val drawable = object : AvatarGenerationListener {
                        override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
                            icon.setImageDrawable(avatarDrawable)
                        }

                        override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
                            return false
                        }
                    }
                    DisplayUtils.setAvatar(
                        currentUserProvider.user,
                        lockInfo.lockedBy,
                        drawable,
                        resources.getDimension(R.dimen.list_item_avatar_icon_radius),
                        resources,
                        this,
                        requireContext()
                    )
                }
            }
        binding.fileActionsList.addView(view.root)
    }

    private fun getLockedByText(lockInfo: FileActionsViewModel.LockInfo): CharSequence {
        val resource = when (lockInfo.lockType) {
            FileLockType.COLLABORATIVE -> R.string.locked_by_app
            else -> R.string.locked_by
        }
        return DisplayUtils.createTextWithSpan(
            getString(resource, lockInfo.lockedBy),
            lockInfo.lockedBy,
            StyleSpan(Typeface.BOLD)
        )
    }

    private fun getLockedUntilText(lockInfo: FileActionsViewModel.LockInfo): CharSequence {
        val relativeTimestamp = DisplayUtils.getRelativeTimestamp(context, lockInfo.lockedUntil!!, true)
        return getString(R.string.lock_expiration_info, relativeTimestamp)
    }

    private fun displayTitle(fileCount: Int) {
        binding.title.text = resources.getQuantityString(R.plurals.file_list__footer__file, fileCount, fileCount)
    }

    private fun inflateActionView(inflater: LayoutInflater, action: FileAction): View {
        val itemBinding = FileActionsBottomSheetItemBinding.inflate(inflater, binding.fileActionsList, false)
            .apply {
                root.setOnClickListener {
                    viewModel.onClick(action)
                }
                text.setText(action.title)
                if (action.icon != null) {
                    val drawable =
                        viewThemeUtils.platform.tintDrawable(
                            requireContext(),
                            resources.getDrawable(action.icon)
                        )
                    icon.setImageDrawable(drawable)
                }
            }
        return itemBinding.root
    }

    private fun dispatchActionClick(id: Int?) {
        if (id != null) {
            clickListener.onClick(id)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface ClickListener {
        fun onClick(@IdRes itemId: Int)
    }

    companion object {
        private const val ARG_ALL_FILES_COUNT = "ALL_FILES_COUNT"
        private const val ARG_FILES = "FILES"
        private const val ARG_IS_OVERFLOW = "OVERFLOW"
        private const val ARG_ADDITIONAL_FILTER = "ADDITIONAL_FILTER"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            file: OCFile,
            componentsGetter: ComponentsGetter,
            isOverflow: Boolean,
            onItemClick: ClickListener,
            @IdRes
            additionalToHide: List<Int>? = null
        ): FileActionsBottomSheet {
            return newInstance(1, listOf(file), componentsGetter, isOverflow, onItemClick, additionalToHide)
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            numberOfAllFiles: Int,
            files: Collection<OCFile>,
            componentsGetter: ComponentsGetter,
            isOverflow: Boolean,
            onItemClick: ClickListener,
            @IdRes
            additionalToHide: List<Int>? = null
        ): FileActionsBottomSheet {
            return FileActionsBottomSheet().apply {
                val argsBundle = bundleOf(
                    ARG_ALL_FILES_COUNT to numberOfAllFiles,
                    ARG_FILES to files.toTypedArray(),
                    ARG_IS_OVERFLOW to isOverflow
                )
                additionalToHide?.let {
                    argsBundle.putIntArray(ARG_ADDITIONAL_FILTER, additionalToHide.toIntArray())
                }
                arguments = argsBundle
                this.componentsGetter = componentsGetter
                this.clickListener = onItemClick
            }
        }
    }
}
