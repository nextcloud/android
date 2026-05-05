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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class GovernanceDetailInfo(
    private val binding: FileInfoFragmentBinding,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragment: FileInfoFragment
) {
    private val context get() = fragment.requireContext()

    fun init() {
        viewThemeUtils.material.themeCardView(binding.governanceLayout)
        initSensitivityLabel()
        initFileRetentionLabel()
    }

    private fun initSensitivityLabel() {
        initDropdown(
            textInputLayout = binding.sensitivityLabel,
            autoComplete = binding.sensitivityLabelAutoComplete,
            items = listOf(
                GovernanceLabel("Sharing restricted", R.drawable.ic_share),
                GovernanceLabel("Download restricted", R.drawable.ic_download_grey600),
                GovernanceLabel("Upload restricted", R.drawable.uploads)
            )
        )
    }

    private fun initFileRetentionLabel() {
        initDropdown(
            textInputLayout = binding.fileRetentionLabel,
            autoComplete = binding.fileRetentionAutoComplete,
            items = listOf(
                GovernanceLabel("Public", R.drawable.file_link),
                GovernanceLabel("Internal use only", R.drawable.ic_group),
                GovernanceLabel("Restricted", R.drawable.ic_cancel)
            )
        )
    }

    private fun initDropdown(
        textInputLayout: TextInputLayout,
        autoComplete: MaterialAutoCompleteTextView,
        items: List<GovernanceLabel>
    ) {
        viewThemeUtils.material.colorTextInputLayout(textInputLayout)
        viewThemeUtils.files.themeAutoCompleteTextView(autoComplete)

        autoComplete.setAdapter(buildAdapter(items))

        items.firstOrNull()?.let { applySelection(autoComplete, it) }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            applySelection(autoComplete, items[position])
        }
    }

    private fun buildAdapter(items: List<GovernanceLabel>) =
        object : ArrayAdapter<GovernanceLabel>(context, R.layout.item_dropdown_with_icon, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                getItem(position)?.let { item ->
                    view.text = item.text
                    view.setCompoundDrawablesWithIntrinsicBounds(tintedDrawable(item), null, null, null)
                }
                return view
            }
        }

    private fun applySelection(autoComplete: MaterialAutoCompleteTextView, item: GovernanceLabel) {
        autoComplete.setText(item.text, false)
        autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(tintedDrawable(item), null, null, null)
        autoComplete.compoundDrawablePadding = fragment.resources.getDimensionPixelSize(R.dimen.standard_padding)
    }

    private fun tintedDrawable(item: GovernanceLabel) =
        ContextCompat.getDrawable(context, item.iconRes)?.mutate()?.also {
            viewThemeUtils.platform.tintDrawable(context, it, ColorRole.ON_SURFACE)
        }
}
