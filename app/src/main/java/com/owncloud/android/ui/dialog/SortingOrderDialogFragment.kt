/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.SortingOrderFragmentBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to show and choose the sorting order for the file listing.
 */
class SortingOrderDialogFragment : DialogFragment(), Injectable {

    private var binding: SortingOrderFragmentBinding? = null

    private var currentSortOrderName: String? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keep the state of the fragment on configuration changes
        retainInstance = true

        binding = null
        currentSortOrderName = requireArguments().getString(KEY_SORT_ORDER, FileSortOrder.SORT_A_TO_Z.name)
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param binding the parent binding
     */
    private fun setupDialogElements(binding: SortingOrderFragmentBinding) {
        val bindings = listOf(
            binding.sortByNameAscending to FileSortOrder.SORT_A_TO_Z,
            binding.sortByNameDescending to FileSortOrder.SORT_Z_TO_A,
            binding.sortByModificationDateAscending to FileSortOrder.SORT_OLD_TO_NEW,
            binding.sortByModificationDateDescending to FileSortOrder.SORT_NEW_TO_OLD,
            binding.sortBySizeAscending to FileSortOrder.SORT_SMALL_TO_BIG,
            binding.sortBySizeDescending to FileSortOrder.SORT_BIG_TO_SMALL
        )

        bindings.forEach { (view, sortOrder) ->
            view.tag = sortOrder
            view.let {
                it.setOnClickListener(OnSortOrderClickListener())
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(it)
            }
        }

        viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(binding.cancel)
        binding.cancel.setOnClickListener { dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = SortingOrderFragmentBinding.inflate(requireActivity().layoutInflater, null, false)
        setupDialogElements(binding!!)

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding?.root)

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    override fun onDestroyView() {
        Log_OC.d(TAG, "destroy SortingOrderDialogFragment view")

        if (dialog != null && retainInstance) {
            dialog?.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    private inner class OnSortOrderClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismissAllowingStateLoss()
            (activity as OnSortingOrderListener?)?.onSortingOrderChosen(v.tag as FileSortOrder)
        }
    }

    interface OnSortingOrderListener {
        fun onSortingOrderChosen(selection: FileSortOrder?)
    }

    companion object {

        private val TAG = SortingOrderDialogFragment::class.java.simpleName
        const val SORTING_ORDER_FRAGMENT = "SORTING_ORDER_FRAGMENT"
        private const val KEY_SORT_ORDER = "SORT_ORDER"

        @JvmStatic
        fun newInstance(sortOrder: FileSortOrder): SortingOrderDialogFragment {
            val dialogFragment = SortingOrderDialogFragment()
            val args = Bundle()
            args.putString(KEY_SORT_ORDER, sortOrder.name)
            dialogFragment.arguments = args
            dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            return dialogFragment
        }
    }
}
