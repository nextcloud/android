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
import com.nextcloud.android.lib.resources.governance.GetAvailableRetentionLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.GetAvailableSensitivityLabelsRemoteOperation
import com.nextcloud.android.lib.resources.governance.RetentionLabelInfo
import com.nextcloud.android.lib.resources.governance.SensitivityLabelInfo
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.ui.fileInfo.model.GovernanceLabel
import com.owncloud.android.R
import com.owncloud.android.databinding.FileInfoFragmentBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        fragment.lifecycleScope.launch {
            val labels = withContext(Dispatchers.IO) { fetchAvailableSensitivityLabels() }

            if (labels.isEmpty()) {
                binding.sensitivityLabel.visibility = View.GONE
                return@launch
            }

            initDropdown(
                textInputLayout = binding.sensitivityLabel,
                autoComplete = binding.sensitivityLabelAutoComplete,
                items = labels
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchAvailableSensitivityLabels(): List<GovernanceLabel> = try {
        val user = fragment.user ?: return emptyList()
        val file = fragment.file ?: return emptyList()
        val client = ClientFactoryImpl(context).createNextcloudClient(user)
        val result = GetAvailableSensitivityLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)

        if (result.isSuccess) {
            result.resultData.orEmpty().map { it.toGovernanceLabel() }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch available sensitivity labels", e)
        emptyList()
    }

    private fun SensitivityLabelInfo.toGovernanceLabel() = GovernanceLabel(name, color)

    private fun initFileRetentionLabel() {
        fragment.lifecycleScope.launch {
            val labels = withContext(Dispatchers.IO) { fetchAvailableRetentionLabels() }

            if (labels.isEmpty()) {
                binding.fileRetentionLabel.visibility = View.GONE
                return@launch
            }

            initDropdown(
                textInputLayout = binding.fileRetentionLabel,
                autoComplete = binding.fileRetentionAutoComplete,
                items = labels
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchAvailableRetentionLabels(): List<GovernanceLabel> = try {
        val user = fragment.user ?: return emptyList()
        val file = fragment.file ?: return emptyList()
        val client = ClientFactoryImpl(context).createNextcloudClient(user)
        val result = GetAvailableRetentionLabelsRemoteOperation(ENTITY_TYPE_FILES, file.localId).execute(client)

        if (result.isSuccess) {
            result.resultData.orEmpty().map { it.toGovernanceLabel() }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log_OC.e(TAG, "Could not fetch available retention labels", e)
        emptyList()
    }

    private fun RetentionLabelInfo.toGovernanceLabel() = GovernanceLabel(name, color)

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
        private const val ENTITY_TYPE_FILES = "FILES"
    }
}
