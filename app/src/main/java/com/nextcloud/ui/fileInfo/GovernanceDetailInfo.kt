/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GovernanceDetailInfo(
    private val binding: FileInfoFragmentBinding,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragment: FileInfoFragment,
    private val viewModel: FileInfoViewModel
) {
    private val context get() = fragment.requireContext()

    fun init() {
        viewThemeUtils.material.themeCardView(binding.governanceLayout)
        collectSensitivityLabels()
        collectRetentionLabels()
    }

    private fun collectSensitivityLabels() {
        fragment.lifecycleScope.launch {
            val labels = viewModel.sensitivityLabels.filterNotNull().first()
            if (labels.isEmpty()) {
                binding.sensitivityLabel.visibility = View.GONE
            } else {
                initDropdown(binding.sensitivityLabel, binding.sensitivityLabelAutoComplete, labels)
            }
        }
    }

    private fun collectRetentionLabels() {
        fragment.lifecycleScope.launch {
            val labels = viewModel.retentionLabels.filterNotNull().first()
            if (labels.isEmpty()) {
                binding.fileRetentionLabel.visibility = View.GONE
            } else {
                initDropdown(binding.fileRetentionLabel, binding.fileRetentionAutoComplete, labels)
            }
        }
    }

    private fun initDropdown(
        textInputLayout: TextInputLayout,
        autoComplete: MaterialAutoCompleteTextView,
        items: List<GovernanceLabel>
    ) {
        textInputLayout.visibility = View.VISIBLE
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
                    view.setCompoundDrawablesWithIntrinsicBounds(colorDot(item.color), null, null, null)
                }
                return view
            }
        }

    private fun applySelection(autoComplete: MaterialAutoCompleteTextView, item: GovernanceLabel) {
        autoComplete.setText(item.text, false)
        autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(colorDot(item.color), null, null, null)
        autoComplete.compoundDrawablePadding = fragment.resources.getDimensionPixelSize(R.dimen.standard_padding)
    }

    private fun colorDot(color: String): Drawable {
        val size = context.resources.getDimensionPixelSize(R.dimen.governance_color_dot_size)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(size, size)
            setColor(parseColor(color))
        }
    }

    private fun parseColor(color: String): Int = runCatching { color.toColorInt() }
        .getOrElse {
            Log_OC.w(TAG, "Could not parse label color: $color")
            ContextCompat.getColor(context, R.color.grey_600)
        }

    companion object {
        private val TAG = GovernanceDetailInfo::class.java.simpleName
    }
}
