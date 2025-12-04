/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.trashbinFileActions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.nextcloud.utils.extensions.toOCFile
import com.owncloud.android.R
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class TrashbinFileActionsBottomSheet :
    BottomSheetDialogFragment(),
    Injectable {

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

    private lateinit var viewModel: TrashbinFileActionsViewModel

    private var _binding: FileActionsBottomSheetBinding? = null
    val binding
        get() = _binding!!

    private val thumbnailAsyncTasks = mutableListOf<ThumbnailsCacheManager.ThumbnailGenerationTask>()

    fun interface ResultListener {
        fun onResult(@IdRes actionId: Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, vmFactory)[TrashbinFileActionsViewModel::class.java]
        _binding = FileActionsBottomSheetBinding.inflate(inflater, container, false)

        viewModel.uiState.observe(viewLifecycleOwner, this::handleState)

        viewModel.clickActionId.observe(viewLifecycleOwner) { id ->
            dispatchActionClick(id)
        }

        viewModel.load(requireArguments())

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        return binding.root
    }

    private fun handleState(state: TrashbinFileActionsViewModel.UiState) {
        toggleLoadingOrContent(state)
        when (state) {
            is TrashbinFileActionsViewModel.UiState.LoadedForSingleFile -> {
                loadFileThumbnail(state.titleFile)
                displayActions(state.actions)
                displayTitle(state.titleFile)
            }

            is TrashbinFileActionsViewModel.UiState.LoadedForMultipleFiles -> {
                setMultipleFilesThumbnail()
                displayActions(state.actions)
                displayTitle(state.fileCount)
            }

            TrashbinFileActionsViewModel.UiState.Loading -> {}
            TrashbinFileActionsViewModel.UiState.Error -> {
                activity?.let {
                    DisplayUtils.showSnackMessage(it, R.string.error_file_actions)
                }
                dismissAllowingStateLoss()
            }
        }
    }

    private fun loadFileThumbnail(titleFile: TrashbinFile?) {
        titleFile?.let {
            DisplayUtils.setThumbnail(
                it.toOCFile(),
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

    fun setResultListener(
        fragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        listener: ResultListener
    ): TrashbinFileActionsBottomSheet {
        fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, result ->
            @IdRes val actionId = result.getInt(RESULT_KEY_ACTION_ID, -1)
            if (actionId != -1) {
                listener.onResult(actionId)
            }
        }
        return this
    }

    private fun toggleLoadingOrContent(state: TrashbinFileActionsViewModel.UiState) {
        if (state is TrashbinFileActionsViewModel.UiState.Loading) {
            binding.bottomSheetLoading.isVisible = true
            binding.bottomSheetHeader.isVisible = false
            viewThemeUtils.platform.colorCircularProgressBar(binding.bottomSheetLoading, ColorRole.PRIMARY)
        } else {
            binding.bottomSheetLoading.isVisible = false
            binding.bottomSheetHeader.isVisible = true
        }
    }

    private fun displayActions(actions: List<TrashbinFileAction>) {
        if (binding.fileActionsList.isEmpty()) {
            actions.forEach { action ->
                val view = inflateActionView(action)
                binding.fileActionsList.addView(view)
            }
        }
    }

    private fun displayTitle(titleFile: TrashbinFile?) {
        titleFile?.fileName?.let {
            binding.title.text = it
        } ?: { binding.title.isVisible = false }
    }

    private fun displayTitle(fileCount: Int) {
        binding.title.text = resources.getQuantityString(R.plurals.trashbin_list__footer__file, fileCount, fileCount)
    }

    private fun inflateActionView(action: TrashbinFileAction): View {
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
        fun newInstance(numberOfAllFiles: Int, files: Collection<TrashbinFile>): TrashbinFileActionsBottomSheet =
            TrashbinFileActionsBottomSheet().apply {
                val argsBundle = bundleOf(
                    TrashbinFileActionsViewModel.ARG_ALL_FILES_COUNT to numberOfAllFiles,
                    TrashbinFileActionsViewModel.ARG_FILES to ArrayList<TrashbinFile>(files)
                )
                arguments = argsBundle
            }
    }
}
