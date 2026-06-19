/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.ui.tags.adapter.TagListAdapter
import com.nextcloud.ui.tags.model.TagUiState
import com.nextcloud.ui.tags.repository.TagManagementRepositoryImpl
import com.nextcloud.utils.extensions.getTypedActivity
import com.owncloud.android.databinding.TagManagementBottomSheetBinding
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.launch
import javax.inject.Inject

class TagManagementBottomSheet :
    BottomSheetDialogFragment(),
    Injectable {

    @Inject
    lateinit var vmFactory: ViewModelFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var _binding: TagManagementBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TagManagementViewModel
    private lateinit var tagAdapter: TagListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = getTypedActivity(BaseActivity::class.java)
        lifecycleScope.launch {
            val ocClient =
                activity?.clientRepository?.getOwncloudClient() ?: throw Exception("oc client cannot constructed")

            val ncClient =
                activity.clientRepository?.getNextcloudClient() ?: throw Exception("nc client cannot constructed")

            val repository = TagManagementRepositoryImpl(ocClient, ncClient)
            viewModel = TagManagementViewModel(repository)

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TagManagementBottomSheetBinding.inflate(inflater, container, false)

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        setupAdapter()
        setupSearch()
        observeState()

        val fileId = requireArguments().getLong(ARG_FILE_ID)
        val currentTags = BundleCompat.getParcelableArrayList(requireArguments(),ARG_CURRENT_TAGS, Tag::class.java)
            ?: arrayListOf()
        viewModel.fetch(fileId, currentTags)

        return binding.root
    }

    private fun setupAdapter() {
        tagAdapter = TagListAdapter(
            onTagChecked = { tag, isChecked ->
                if (isChecked) {
                    viewModel.assignTag(tag)
                } else {
                    viewModel.unassignTag(tag)
                }
            },
            onCreateTag = { name ->
                viewModel.createAndAssignTag(name)
                binding.searchEditText.text?.clear()
            }
        )

        binding.tagList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tagAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TagUiState.Loading -> {
                            binding.loadingIndicator.visibility = View.VISIBLE
                            binding.tagList.visibility = View.GONE
                        }

                        is TagUiState.Loaded -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.tagList.visibility = View.VISIBLE
                            tagAdapter.update(state.allTags, state.assignedTagIds, state.query)
                        }

                        is TagUiState.Error -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.tagList.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        val assignedTags = viewModel.getAssignedTags()
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY_TAGS to ArrayList(assignedTags)))

        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "TAG_MANAGEMENT_REQUEST"
        const val RESULT_KEY_TAGS = "RESULT_TAGS"
        private const val ARG_FILE_ID = "ARG_FILE_ID"
        private const val ARG_CURRENT_TAGS = "ARG_CURRENT_TAGS"

        fun newInstance(fileId: Long, currentTags: List<Tag>): TagManagementBottomSheet =
            TagManagementBottomSheet().apply {
                arguments = bundleOf(
                    ARG_FILE_ID to fileId,
                    ARG_CURRENT_TAGS to ArrayList(currentTags)
                )
            }
    }
}
