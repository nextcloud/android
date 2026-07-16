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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nextcloud.ui.fileInfo.model.GovernanceEvent
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.nextcloud.ui.fileInfo.model.GovernanceUiState
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.flow.filterIsInstance
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
        fragment.lifecycleScope.launch {
            val state = viewModel.uiState.filterIsInstance<GovernanceUiState.Loaded>().first()
            setupSensitivityLabels(state.sensitivityLabels, state.currentSensitivityLabelId)
            setupMultiSelectField(
                textInputLayout = binding.fileRetentionLabel,
                labelText = binding.fileRetentionLabelText,
                items = state.retentionLabels,
                title = context.getString(R.string.governance_file_retention_label),
                getCurrentIds = { currentLoadedState()?.currentRetentionLabelIds.orEmpty() }
            ) { newIds -> viewModel.updateRetentionLabels(newIds) }
            setupMultiSelectField(
                textInputLayout = binding.fileHoldLabel,
                labelText = binding.fileHoldLabelText,
                items = state.holdLabels,
                title = context.getString(R.string.governance_legal_hold_label),
                getCurrentIds = { currentLoadedState()?.currentHoldLabelIds.orEmpty() }
            ) { newIds -> viewModel.updateHoldLabels(newIds) }
        }
        observeEvents()
    }

    private fun observeEvents() {
        fragment.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    GovernanceEvent.PermissionDenied -> {
                        DisplayUtils.showSnackMessage(fragment, R.string.governance_permission_denied)
                        currentLoadedState()?.let { refresh(it) }
                    }
                }
            }
        }
    }

    private fun refresh(state: GovernanceUiState.Loaded) {
        if (state.retentionLabels.isNotEmpty()) {
            updateMultiSelectText(binding.fileRetentionLabelText, state.retentionLabels, state.currentRetentionLabelIds)
        }
        if (state.holdLabels.isNotEmpty()) {
            updateMultiSelectText(binding.fileHoldLabelText, state.holdLabels, state.currentHoldLabelIds)
        }
        if (state.sensitivityLabels.isNotEmpty()) {
            val items = listOf(noneLabel()) + state.sensitivityLabels
            val item = items.find { it.id == state.currentSensitivityLabelId } ?: items.first()
            applySelection(binding.sensitivityLabelAutoComplete, item)
        }
    }

    private fun noneLabel() = GovernanceLabel("", context.getString(R.string.governance_no_label), "")

    private fun setupSensitivityLabels(labels: List<GovernanceLabel>, currentId: String) {
        if (labels.isEmpty()) {
            binding.sensitivityLabel.visibility = View.GONE
            return
        }
        val items = listOf(noneLabel()) + labels
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

    private fun currentLoadedState(): GovernanceUiState.Loaded? = viewModel.uiState.value as? GovernanceUiState.Loaded

    private fun setupMultiSelectField(
        textInputLayout: TextInputLayout,
        labelText: TextInputEditText,
        items: List<GovernanceLabel>,
        title: String,
        getCurrentIds: () -> Set<String>,
        onUpdate: (Set<String>) -> Unit
    ) {
        if (items.isEmpty()) {
            textInputLayout.visibility = View.GONE
            return
        }
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

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMultiChoiceItems(itemNames, checkedItems) { _, which, isChecked ->
                val labelId = items[which].id
                if (isChecked) newSelection.add(labelId) else newSelection.remove(labelId)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm(newSelection) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        (dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton)?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
        }
    }

    private fun updateMultiSelectText(
        labelText: TextInputEditText,
        items: List<GovernanceLabel>,
        selectedIds: Set<String>
    ) {
        val selectedNames = items.filter { it.id in selectedIds }.map { it.text }
        labelText.setText(selectedNames.joinToString(", "))
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

    private fun parseColor(color: String): Int = runCatching { "#$color".toColorInt() }
        .getOrElse {
            Log_OC.w(TAG, "Could not parse label color: $color")
            ContextCompat.getColor(context, R.color.grey_600)
        }

    companion object {
        private val TAG = GovernanceDetailInfo::class.java.simpleName
    }
}
