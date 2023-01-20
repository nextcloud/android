/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.DurationPickerBinding
import com.owncloud.android.utils.TimeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DurationPickerDialogFragment : DialogFragment(), Injectable {

    private var _binding: DurationPickerBinding? = null
    private val binding get() = _binding!!
    private var resultListener: Listener? = null

    private var duration: Long
        get() = TimeUnit.DAYS.toMillis(binding.daysPicker.value.toLong()) +
            TimeUnit.HOURS.toMillis(binding.hoursPicker.value.toLong()) +
            TimeUnit.MINUTES.toMillis(binding.minutesPicker.value.toLong())
        private set(duration) {
            val durationParts = TimeUtils.getDurationParts(duration)
            binding.daysPicker.value = durationParts.days
            binding.hoursPicker.value = durationParts.hours
            binding.minutesPicker.value = durationParts.minutes
        }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    fun setListener(listener: Listener?) {
        resultListener = listener
    }

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog
        viewThemeUtils.platform.colorTextButtons(
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(DURATION, duration)
    }

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        _binding = DurationPickerBinding.inflate(requireActivity().layoutInflater, null, false)

        setupLimits()

        this.duration = savedState?.getLong(DURATION) ?: requireArguments().getLong(DURATION)

        setHintMessage(requireArguments().getString(HINT_MESSAGE))

        binding.clear.setOnClickListener {
            binding.daysPicker.value = 0
            binding.hoursPicker.value = 0
            binding.minutesPicker.value = 0
        }

        val builder = MaterialAlertDialogBuilder(binding.root.context)
        val dialogTitle = requireArguments().getString(DIALOG_TITLE)
        builder.setTitle(dialogTitle)
        builder.setView(binding.root)
        builder.setPositiveButton(R.string.common_save) { _, _ ->
            if (resultListener != null) {
                resultListener!!.onDurationPickerResult(Activity.RESULT_OK, this.duration)
            }
        }
        builder.setNegativeButton(R.string.common_cancel) { _, _ ->
            if (resultListener != null) {
                resultListener!!.onDurationPickerResult(Activity.RESULT_CANCELED, 0)
            }
        }
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.root.context, builder)
        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupLimits() {
        binding.daysPicker.maxValue = MAX_DAYS_VALUE
        binding.hoursPicker.maxValue = MAX_HOURS_VALUE
        binding.minutesPicker.maxValue = MAX_MINUTES_VALUE
    }

    private fun setHintMessage(hintMessage: String?) {
        binding.pickerHint.visibility = if (hintMessage != null) View.VISIBLE else View.GONE
        binding.pickerHint.text = hintMessage
    }

    interface Listener {
        fun onDurationPickerResult(resultCode: Int, duration: Long)
    }

    companion object {
        private const val MAX_DAYS_VALUE = 30
        private const val MAX_HOURS_VALUE = 24
        private const val MAX_MINUTES_VALUE = 59
        private const val DURATION = "DURATION"
        private const val DIALOG_TITLE = "TITLE"
        private const val HINT_MESSAGE = "HINT"

        fun newInstance(duration: Long, title: String?, hintMessage: String?): DurationPickerDialogFragment {
            val args = Bundle()
            args.putLong(DURATION, duration)
            args.putString(HINT_MESSAGE, hintMessage)
            args.putString(DIALOG_TITLE, title)
            val dialogFragment = DurationPickerDialogFragment()
            dialogFragment.arguments = args
            dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            return dialogFragment
        }
    }
}
