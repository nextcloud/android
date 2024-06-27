/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.fileactions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileActionsBottomSheet : BottomSheetDialogFragment(), Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var vmFactory: ViewModelFactory

    @Inject
    lateinit var currentUserProvider: CurrentAccountProvider

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    private lateinit var viewModel: FileActionsViewModel

    private var _binding: FileActionsBottomSheetBinding? = null
    val binding
        get() = _binding!!

    private lateinit var componentsGetter: ComponentsGetter

    private val thumbnailAsyncTasks = mutableListOf<ThumbnailsCacheManager.ThumbnailGenerationTask>()

    fun interface ResultListener {
        fun onResult(@IdRes actionId: Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, vmFactory)[FileActionsViewModel::class.java]
        _binding = FileActionsBottomSheetBinding.inflate(inflater, container, false)

        viewModel.uiState.observe(viewLifecycleOwner, this::handleState)

        viewModel.clickActionId.observe(viewLifecycleOwner) { id ->
            dispatchActionClick(id)
        }

        viewModel.load(requireArguments(), componentsGetter)

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        return binding.root
    }

    private fun handleState(state: FileActionsViewModel.UiState) {
        toggleLoadingOrContent(state)
        when (state) {
            is FileActionsViewModel.UiState.LoadedForSingleFile -> {
                loadFileThumbnail(state.titleFile)
                if (state.lockInfo != null) {
                    displayLockInfo(state.lockInfo)
                }
                displayActions(state.actions)
                displayTitle(state.titleFile)
            }

            is FileActionsViewModel.UiState.LoadedForMultipleFiles -> {
                setMultipleFilesThumbnail()
                displayActions(state.actions)
                displayTitle(state.fileCount)
            }

            FileActionsViewModel.UiState.Loading -> {}
            FileActionsViewModel.UiState.Error -> {
                context?.let {
                    Toast.makeText(it, R.string.error_file_actions, Toast.LENGTH_SHORT).show()
                }
                dismissAllowingStateLoss()
            }
        }
    }

    private fun loadFileThumbnail(titleFile: OCFile?) {
        titleFile?.let {
            DisplayUtils.setThumbnail(
                it,
                binding.thumbnailLayout.thumbnail,
                currentUserProvider.user,
                storageManager,
                thumbnailAsyncTasks,
                false,
                context,
                binding.thumbnailLayout.thumbnailShimmer,
                syncedFolderProvider.preferences,
                viewThemeUtils,
                syncedFolderProvider
            )
        }
    }

    private fun setMultipleFilesThumbnail() {
        context?.let {
            val drawable = viewThemeUtils.platform.tintDrawable(it, R.drawable.file_multiple, ColorRole.PRIMARY)
            binding.thumbnailLayout.thumbnail.setImageDrawable(drawable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        require(context is ComponentsGetter) {
            "Context is not a ComponentsGetter"
        }
        this.componentsGetter = context
    }

    fun setResultListener(
        fragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        listener: ResultListener
    ): FileActionsBottomSheet {
        fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, result ->
            @IdRes val actionId = result.getInt(RESULT_KEY_ACTION_ID, -1)
            if (actionId != -1) {
                listener.onResult(actionId)
            }
        }
        return this
    }

    private fun toggleLoadingOrContent(state: FileActionsViewModel.UiState) {
        if (state is FileActionsViewModel.UiState.Loading) {
            binding.bottomSheetLoading.isVisible = true
            binding.bottomSheetHeader.isVisible = false
            viewThemeUtils.platform.colorCircularProgressBar(binding.bottomSheetLoading, ColorRole.PRIMARY)
        } else {
            binding.bottomSheetLoading.isVisible = false
            binding.bottomSheetHeader.isVisible = true
        }
    }

    private fun displayActions(actions: List<FileAction>) {
        if (binding.fileActionsList.isEmpty()) {
            actions.forEach { action ->
                val view = inflateActionView(action)
                binding.fileActionsList.addView(view)
            }
        }
    }

    private fun displayTitle(titleFile: OCFile?) {
        val decryptedFileName = titleFile?.decryptedFileName
        if (decryptedFileName != null) {
            decryptedFileName.let {
                binding.title.text = it
            }
        } else {
            binding.title.isVisible = false
        }
    }

    private fun displayLockInfo(lockInfo: FileActionsViewModel.LockInfo) {
        val view = FileActionsBottomSheetItemBinding.inflate(layoutInflater, binding.fileActionsList, false)
            .apply {
                val textColor = ColorStateList.valueOf(resources.getColor(R.color.secondary_text_color, null))
                root.isClickable = false
                text.setTextColor(textColor)
                text.text = getLockedByText(lockInfo)
                if (lockInfo.lockedUntil != null) {
                    textLine2.text = getLockedUntilText(lockInfo)
                    textLine2.isVisible = true
                }
                if (lockInfo.lockType != FileLockType.COLLABORATIVE) {
                    showLockAvatar(lockInfo)
                }
            }
        binding.fileActionsList.addView(view.root)
    }

    private fun FileActionsBottomSheetItemBinding.showLockAvatar(lockInfo: FileActionsViewModel.LockInfo) {
        val listener = object : AvatarGenerationListener {
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
            listener,
            resources.getDimension(R.dimen.list_item_avatar_icon_radius),
            resources,
            this,
            requireContext()
        )
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

    private fun inflateActionView(action: FileAction): View {
        val itemBinding = FileActionsBottomSheetItemBinding.inflate(layoutInflater, binding.fileActionsList, false)
            .apply {
                root.setOnClickListener {
                    viewModel.onClick(action)
                }
                text.setText(action.title)
                if (action.icon != null) {
                    val drawable =
                        viewThemeUtils.platform.tintDrawable(
                            requireContext(),
                            AppCompatResources.getDrawable(requireContext(), action.icon)!!
                        )
                    icon.setImageDrawable(drawable)
                }
            }
        return itemBinding.root
    }

    private fun dispatchActionClick(id: Int?) {
        if (id != null) {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY_ACTION_ID to id))
            parentFragmentManager.clearFragmentResultListener(REQUEST_KEY)
            dismiss()
        }
    }

    companion object {
        private const val REQUEST_KEY = "REQUEST_KEY_ACTION"
        private const val RESULT_KEY_ACTION_ID = "RESULT_KEY_ACTION_ID"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            file: OCFile,
            isOverflow: Boolean,
            @IdRes
            additionalToHide: List<Int>? = null
        ): FileActionsBottomSheet {
            return newInstance(1, listOf(file), isOverflow, additionalToHide, true)
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            numberOfAllFiles: Int,
            files: Collection<OCFile>,
            isOverflow: Boolean,
            @IdRes
            additionalToHide: List<Int>? = null,
            inSingleFileFragment: Boolean = false
        ): FileActionsBottomSheet {
            return FileActionsBottomSheet().apply {
                val argsBundle = bundleOf(
                    FileActionsViewModel.ARG_ALL_FILES_COUNT to numberOfAllFiles,
                    FileActionsViewModel.ARG_FILES to ArrayList<OCFile>(files),
                    FileActionsViewModel.ARG_IS_OVERFLOW to isOverflow,
                    FileActionsViewModel.ARG_IN_SINGLE_FILE_FRAGMENT to inSingleFileFragment
                )
                additionalToHide?.let {
                    argsBundle.putIntArray(FileActionsViewModel.ARG_ADDITIONAL_FILTER, additionalToHide.toIntArray())
                }
                arguments = argsBundle
            }
        }
    }
}
