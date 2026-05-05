/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.ui.fileInfo.model.SensitivityLabel
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class GovernanceDetailInfo(
    private val binding: FileInfoFragmentBinding,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragment: FileInfoFragment
) {

    fun init() {
        val items = listOf(
            SensitivityLabel("Option 1", R.drawable.outline_camera_24),
            SensitivityLabel("Option 2", R.drawable.outline_image_24),
            SensitivityLabel("Option 3", R.drawable.ic_information_outline)
        )

        val adapter = object :
            ArrayAdapter<SensitivityLabel>(fragment.requireContext(), R.layout.item_dropdown_with_icon, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val item = getItem(position)
                if (item != null) {
                    view.text = item.text
                    val drawable = ContextCompat.getDrawable(context, item.iconRes)?.mutate()
                    drawable?.let {
                        viewThemeUtils.platform.tintDrawable(fragment.requireContext(), it, ColorRole.ON_SURFACE)
                    }
                    view.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                return view
            }
        }
        binding.dropdownMenuAutoComplete.setAdapter(adapter)

        val defaultSelectedItem = items.firstOrNull()
        if (defaultSelectedItem != null) {
            binding.dropdownMenuAutoComplete.setText(defaultSelectedItem.text, false)
            val drawable = ContextCompat.getDrawable(fragment.requireContext(), defaultSelectedItem.iconRes)?.mutate()
            drawable?.let {
                viewThemeUtils.platform.tintDrawable(fragment.requireContext(), it, ColorRole.ON_SURFACE)
            }
            binding.dropdownMenuAutoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.dropdownMenuAutoComplete.compoundDrawablePadding =
                fragment.resources.getDimensionPixelSize(R.dimen.standard_padding)
        }

        binding.dropdownMenuAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = items[position]
            binding.dropdownMenuAutoComplete.setText(selected.text, false)
            val drawable = ContextCompat.getDrawable(fragment.requireContext(), selected.iconRes)?.mutate()
            binding.dropdownMenuAutoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.dropdownMenuAutoComplete.compoundDrawablePadding =
                fragment.resources.getDimensionPixelSize(R.dimen.standard_padding)
        }
    }
}
