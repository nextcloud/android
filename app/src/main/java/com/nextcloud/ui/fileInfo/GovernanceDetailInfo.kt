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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
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
        collectHoldLabels()
    }

    private fun collectSensitivityLabels() {
        fragment.lifecycleScope.launch {
            val labels = viewModel.sensitivityLabels.filterNotNull().first()
            if (labels.isEmpty()) {
                binding.sensitivityLabel.visibility = View.GONE
                return@launch
            }
            val currentId = viewModel.currentSensitivityLabelId.filterNotNull().first()
            val items = listOf(GovernanceLabel("", "", "")) + labels
            val initialItem = items.find { it.id == currentId } ?: items.first()
            initDropdown(
                textInputLayout = binding.sensitivityLabel,
                autoComplete = binding.sensitivityLabelAutoComplete,
                items = items,
                initialItem = initialItem
            ) { label ->
                if (label.id.isEmpty()) {
                    viewModel.removeSensitivityLabel()
                } else {
                    viewModel.setSensitivityLabel(label.id)
                }
            }
        }
    }

    private fun collectRetentionLabels() {
        fragment.lifecycleScope.launch {
            val labels = viewModel.retentionLabels.filterNotNull().first()
            if (labels.isEmpty()) {
                binding.fileRetentionLabel.visibility = View.GONE
                return@launch
            }
            val currentIds = viewModel.currentRetentionLabelIds.filterNotNull().first()
            setupMultiSelectField(
                textInputLayout = binding.fileRetentionLabel,
                labelText = binding.fileRetentionLabelText,
                items = labels,
                title = context.getString(R.string.governance_file_retention_label),
                getCurrentIds = { viewModel.currentRetentionLabelIds.value ?: currentIds }
            ) { newIds ->
                viewModel.updateRetentionLabels(newIds)
            }
        }
    }

    private fun collectHoldLabels() {
        fragment.lifecycleScope.launch {
            val labels = viewModel.holdLabels.filterNotNull().first()
            if (labels.isEmpty()) {
                binding.fileHoldLabel.visibility = View.GONE
                return@launch
            }
            val currentIds = viewModel.currentHoldLabelIds.filterNotNull().first()
            setupMultiSelectField(
                textInputLayout = binding.fileHoldLabel,
                labelText = binding.fileHoldLabelText,
                items = labels,
                title = context.getString(R.string.governance_legal_hold_label),
                getCurrentIds = { viewModel.currentHoldLabelIds.value ?: currentIds }
            ) { newIds ->
                viewModel.updateHoldLabels(newIds)
            }
        }
    }

    private fun setupMultiSelectField(
        textInputLayout: TextInputLayout,
        labelText: TextInputEditText,
        items: List<GovernanceLabel>,
        title: String,
        getCurrentIds: () -> Set<String>,
        onUpdate: (Set<String>) -> Unit
    ) {
        textInputLayout.visibility = View.VISIBLE
        viewThemeUtils.material.colorTextInputLayout(textInputLayout)
        updateMultiSelectText(labelText, items, getCurrentIds())

        val clickListener = View.OnClickListener {
            showMultiSelectDialog(title, items, getCurrentIds()) { newIds ->
                onUpdate(newIds)
                updateMultiSelectText(labelText, items, newIds)
            }
        }
        textInputLayout.setEndIconOnClickListener(clickListener)
        labelText.setOnClickListener(clickListener)
    }

    private fun showMultiSelectDialog(
        title: String,
        items: List<GovernanceLabel>,
        currentIds: Set<String>,
        onConfirm: (Set<String>) -> Unit
    ) {
        val itemNames = items.map { it.text }.toTypedArray()
        val checkedItems = items.map { it.id in currentIds }.toBooleanArray()
        val newSelection = currentIds.toMutableSet()

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMultiChoiceItems(itemNames, checkedItems) { _, which, isChecked ->
                val labelId = items[which].id
                if (isChecked) newSelection.add(labelId) else newSelection.remove(labelId)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(newSelection) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateMultiSelectText(
        labelText: TextInputEditText,
        items: List<GovernanceLabel>,
        selectedIds: Set<String>
    ) {
        val selectedNames = items.filter { it.id in selectedIds }.map { it.text }
        labelText.setText(
            when {
                selectedNames.isEmpty() -> ""
                selectedNames.size == 1 -> selectedNames.first()
                else -> context.getString(R.string.governance_n_selected, selectedNames.size)
            }
        )
    }

    private fun initDropdown(
        textInputLayout: TextInputLayout,
        autoComplete: MaterialAutoCompleteTextView,
        items: List<GovernanceLabel>,
        initialItem: GovernanceLabel? = items.firstOrNull(),
        onSelect: (GovernanceLabel) -> Unit = {}
    ) {
        textInputLayout.visibility = View.VISIBLE
        viewThemeUtils.material.colorTextInputLayout(textInputLayout)
        viewThemeUtils.files.themeAutoCompleteTextView(autoComplete)

        autoComplete.setAdapter(buildAdapter(items))
        initialItem?.let { applySelection(autoComplete, it) }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            applySelection(autoComplete, item)
            onSelect(item)
        }
    }

    private fun buildAdapter(items: List<GovernanceLabel>) =
        object : ArrayAdapter<GovernanceLabel>(context, R.layout.item_dropdown_with_icon, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                getItem(position)?.let { item ->
                    view.text = item.text
                    val dot = if (item.color.isNotEmpty()) colorDot(item.color) else null
                    view.setCompoundDrawablesWithIntrinsicBounds(dot, null, null, null)
                }
                return view
            }
        }

    private fun applySelection(autoComplete: MaterialAutoCompleteTextView, item: GovernanceLabel) {
        autoComplete.setText(item.text, false)
        if (item.color.isNotEmpty()) {
            autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(colorDot(item.color), null, null, null)
            autoComplete.compoundDrawablePadding = fragment.resources.getDimensionPixelSize(R.dimen.standard_padding)
        } else {
            autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
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
