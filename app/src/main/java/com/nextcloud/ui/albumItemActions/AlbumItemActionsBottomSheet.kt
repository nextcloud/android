/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.albumItemActions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.view.isEmpty
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AlbumItemActionsBottomSheet : BottomSheetDialogFragment(), Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var _binding: FileActionsBottomSheetBinding? = null
    val binding
        get() = _binding!!

    fun interface ResultListener {
        fun onResult(@IdRes actionId: Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FileActionsBottomSheetBinding.inflate(inflater, container, false)

        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bottomSheetHeader.visibility = View.GONE
        binding.bottomSheetLoading.visibility = View.GONE
        displayActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setResultListener(
        fragmentManager: FragmentManager,
        lifecycleOwner: LifecycleOwner,
        listener: ResultListener
    ): AlbumItemActionsBottomSheet {
        fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, result ->
            @IdRes val actionId = result.getInt(RESULT_KEY_ACTION_ID, -1)
            if (actionId != -1) {
                listener.onResult(actionId)
            }
        }
        return this
    }

    private fun displayActions() {
        if (binding.fileActionsList.isEmpty()) {
            AlbumItemAction.SORTED_VALUES.forEach { action ->
                val view = inflateActionView(action)
                binding.fileActionsList.addView(view)
            }
        }
    }

    private fun inflateActionView(action: AlbumItemAction): View {
        val itemBinding = FileActionsBottomSheetItemBinding.inflate(layoutInflater, binding.fileActionsList, false)
            .apply {
                root.setOnClickListener {
                    dispatchActionClick(action.id)
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
        fun newInstance(): AlbumItemActionsBottomSheet {
            return AlbumItemActionsBottomSheet()
        }
    }
}
