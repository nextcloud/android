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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.ui.activity.ComponentsGetter
import javax.inject.Inject

// TODO add file name
// TODO add lock info (see FileLockingMenuCustomization)
// TODO give events back
// TODO viewModel
// TODO drag handle
// TODO theming
class FileActionsBottomSheet : BottomSheetDialogFragment(), Injectable {

    // TODO refactor FileMenuFilter and inject needed things into it
    lateinit var componentsGetter: ComponentsGetter

    // TODO replace with fragment listener from Activity
    lateinit var clickListener: ClickListener

    @Inject
    lateinit var currentAccountProvider: CurrentAccountProvider

    private lateinit var binding: FileActionsBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // TODO pass only IDs, fetch from DB to avoid TransactionTooLarge
        val args = requireArguments()
        val files: Array<OCFile>? = args.getParcelableArray(ARG_FILES) as Array<OCFile>?
        require(files != null)
        val numberOfAllFiles = args.getInt(ARG_ALL_FILES_COUNT, 1)
        val isOverflow = args.getBoolean(ARG_IS_OVERFLOW, false)

        binding = FileActionsBottomSheetBinding.inflate(inflater, container, false)
        val toHide = FileMenuFilter(
            numberOfAllFiles,
            files.toList(),
            componentsGetter,
            requireContext(),
            isOverflow,
            currentAccountProvider.user
        )
            .getToHide(false)
        FileAction.values()
            .filter { it.id !in toHide }.forEach { action ->
                // TODO change icon
                val itemBinding = FileActionsBottomSheetItemBinding.inflate(inflater, binding.fileActionsList, false)
                    .apply {
                        root.setText(action.title)
                        root.setOnClickListener {
                            clickListener.onClick(action.id)
                            dismiss()
                        }
                    }
                binding.fileActionsList.addView(itemBinding.root)
            }
        return binding.root
    }

    interface ClickListener {
        fun onClick(@IdRes itemId: Int)
    }

    companion object {
        private const val ARG_ALL_FILES_COUNT = "ALL_FILES_COUNT"
        private const val ARG_FILES = "FILES"
        private const val ARG_IS_OVERFLOW = "OVERFLOW"

        @JvmStatic
        fun newInstance(
            file: OCFile,
            componentsGetter: ComponentsGetter,
            isOverflow: Boolean,
            onItemClick: ClickListener
        ): FileActionsBottomSheet {
            return newInstance(1, listOf(file), componentsGetter, isOverflow, onItemClick)
        }

        @JvmStatic
        fun newInstance(
            numberOfAllFiles: Int,
            files: Collection<OCFile>,
            componentsGetter: ComponentsGetter,
            isOverflow: Boolean,
            onItemClick: ClickListener
        ): FileActionsBottomSheet {
            return FileActionsBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ALL_FILES_COUNT to numberOfAllFiles,
                    ARG_FILES to files.toTypedArray(),
                    ARG_IS_OVERFLOW to isOverflow
                )
                this.componentsGetter = componentsGetter
                this.clickListener = onItemClick
            }
        }
    }
}
