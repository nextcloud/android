/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 2018 TSI-mc
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Calendar
import javax.inject.Inject

/**
 * Dialog requesting a date after today.
 */
class ExpirationDatePickerDialogFragment : DialogFragment(), OnDateSetListener, Injectable {

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var onExpiryDateListener: OnExpiryDateListener? = null

    fun setOnExpiryDateListener(onExpiryDateListener: OnExpiryDateListener?) {
        this.onExpiryDateListener = onExpiryDateListener
    }

    override fun onStart() {
        super.onStart()

        val currentDialog = dialog

        if (currentDialog != null) {
            val dialog = currentDialog as DatePickerDialog

            val positiveButton = dialog.getButton(DatePickerDialog.BUTTON_POSITIVE) as MaterialButton?
            if (positiveButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(positiveButton)
            }
            val negativeButton = dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE) as MaterialButton?
            if (negativeButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
            }
            val neutralButton = dialog.getButton(DatePickerDialog.BUTTON_NEUTRAL) as MaterialButton?
            if (neutralButton != null) {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(neutralButton)
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return A new dialog to let the user choose an expiration date that will be bound to a share link.
     */
    @Suppress("DEPRECATION", "MagicNumber")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Chosen date received as an argument must be later than tomorrow ; default to tomorrow in other case
        val chosenDate = Calendar.getInstance()
        val tomorrowInMillis = chosenDate.timeInMillis + DateUtils.DAY_IN_MILLIS
        val chosenDateInMillis = requireArguments().getLong(ARG_CHOSEN_DATE_IN_MILLIS)
        chosenDate.timeInMillis = chosenDateInMillis.coerceAtLeast(tomorrowInMillis)

        // Create a new instance of DatePickerDialog
        val dialog = DatePickerDialog(
            requireActivity(),
            R.style.FallbackDatePickerDialogTheme,
            this,
            chosenDate[Calendar.YEAR],
            chosenDate[Calendar.MONTH],
            chosenDate[Calendar.DAY_OF_MONTH]
        )

        // show unset button only when date is already selected
        if (chosenDateInMillis > 0) {
            dialog.setButton(
                Dialog.BUTTON_NEGATIVE,
                getText(R.string.share_via_link_unset_password)
            ) { _: DialogInterface?, _: Int ->
                onExpiryDateListener?.onDateUnSet()
            }
        }

        // Prevent days in the past may be chosen
        val picker = dialog.datePicker
        picker.minDate = tomorrowInMillis - 1000

        // Enforce spinners view; ignored by MD-based theme in Android >=5, but calendar is REALLY buggy
        // in Android < 5, so let's be sure it never appears (in tablets both spinners and calendar are
        // shown by default)
        @Suppress("DEPRECATION")
        picker.calendarViewShown = false
        return dialog
    }

    val currentSelectionMillis: Long
        get() {
            val dialog = dialog
            if (dialog != null) {
                val datePickerDialog = dialog as DatePickerDialog
                val picker = datePickerDialog.datePicker
                return yearMonthDayToMillis(picker.year, picker.month, picker.dayOfMonth)
            }
            return 0
        }

    /**
     * Called when the user chooses an expiration date.
     *
     * @param view        View instance where the date was chosen
     * @param year        Year of the date chosen.
     * @param monthOfYear Month of the date chosen [0, 11]
     * @param dayOfMonth  Day of the date chosen
     */
    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val chosenDateInMillis = yearMonthDayToMillis(year, monthOfYear, dayOfMonth)
        if (onExpiryDateListener != null) {
            onExpiryDateListener?.onDateSet(year, monthOfYear, dayOfMonth, chosenDateInMillis)
        }
    }

    private fun yearMonthDayToMillis(year: Int, monthOfYear: Int, dayOfMonth: Int): Long {
        val date = Calendar.getInstance()
        date[Calendar.YEAR] = year
        date[Calendar.MONTH] = monthOfYear
        date[Calendar.DAY_OF_MONTH] = dayOfMonth
        return date.timeInMillis
    }

    interface OnExpiryDateListener {
        fun onDateSet(year: Int, monthOfYear: Int, dayOfMonth: Int, chosenDateInMillis: Long)
        fun onDateUnSet()
    }

    companion object {
        /** Tag for FragmentsManager  */
        const val DATE_PICKER_DIALOG = "DATE_PICKER_DIALOG"

        /** Parameter constant for date chosen initially  */
        private const val ARG_CHOSEN_DATE_IN_MILLIS = "CHOSEN_DATE_IN_MILLIS"

        /**
         * Factory method to create new instances
         *
         * @param chosenDateInMillis Date chosen when the dialog appears
         * @return New dialog instance
         */
        fun newInstance(chosenDateInMillis: Long): ExpirationDatePickerDialogFragment {
            val arguments = Bundle()
            arguments.putLong(ARG_CHOSEN_DATE_IN_MILLIS, chosenDateInMillis)
            val dialog = ExpirationDatePickerDialogFragment()
            dialog.arguments = arguments
            return dialog
        }
    }
}
