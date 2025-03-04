/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.DurationPickerBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DurationPickerDialogFragment : DialogFragment(), Injectable {

    private var resultListener: Listener? = null

    private lateinit var binding: DurationPickerBinding

    private var duration: Long
        get() = TimeUnit.DAYS.toMillis(binding.daysPicker.value.toLong()) +
            TimeUnit.HOURS.toMillis(binding.hoursPicker.value.toLong()) +
            TimeUnit.MINUTES.toMillis(binding.minutesPicker.value.toLong())
        private set(durationMs) {
            val duration = durationMs.toDuration(DurationUnit.MILLISECONDS)
            duration.toComponents { days, hours, minutes, _, _ ->
                binding.daysPicker.value = days.toInt()
                binding.hoursPicker.value = hours
                binding.minutesPicker.value = minutes
            }
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
        binding = DurationPickerBinding.inflate(requireActivity().layoutInflater, null, false)

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
            resultListener?.onDurationPickerResult(Activity.RESULT_OK, this.duration)
        }
        builder.setNegativeButton(R.string.common_cancel) { _, _ ->
            resultListener?.onDurationPickerResult(Activity.RESULT_CANCELED, 0)
        }
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.root.context, builder)
        return builder.create()
    }

    private fun setupLimits() {
        binding.daysPicker.maxValue = MAX_DAYS_VALUE
        binding.daysPicker.displayedValues = getAlignedLeftFromCenterValues(0, MAX_DAYS_VALUE)
        binding.hoursPicker.maxValue = MAX_HOURS_VALUE
        binding.hoursPicker.displayedValues = getAlignedLeftFromCenterValues(0, MAX_HOURS_VALUE)
        binding.minutesPicker.maxValue = MAX_MINUTES_VALUE
        binding.minutesPicker.displayedValues = getAlignedLeftFromCenterValues(0, MAX_MINUTES_VALUE)
    }

    private fun setHintMessage(hintMessage: String?) {
        binding.pickerHint.visibility = if (hintMessage != null) View.VISIBLE else View.GONE
        binding.pickerHint.text = hintMessage
    }

    private fun getAlignedLeftFromCenterValues(min: Int, max: Int): Array<String> {
        return (min..max).map {
            val numberOfDigits = it.toString().length
            "${it}${FIGURE_SPACE.repeat(numberOfDigits)}"
        }.toTypedArray()
    }

    interface Listener {
        fun onDurationPickerResult(resultCode: Int, duration: Long)
    }

    companion object {
        private const val FIGURE_SPACE = "\u2007"
        private const val MAX_DAYS_VALUE = 30
        private const val MAX_HOURS_VALUE = 24
        private const val MAX_MINUTES_VALUE = 59
        private const val DURATION = "DURATION"
        private const val DIALOG_TITLE = "TITLE"
        private const val HINT_MESSAGE = "HINT"

        fun newInstance(duration: Long, title: String?, hintMessage: String?): DurationPickerDialogFragment {
            return DurationPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(DURATION, duration)
                    putString(HINT_MESSAGE, hintMessage)
                    putString(DIALOG_TITLE, title)
                }
                setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            }
        }
    }
}
