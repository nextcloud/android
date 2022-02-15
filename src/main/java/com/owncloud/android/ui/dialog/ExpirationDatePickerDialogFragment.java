/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Andy Scherzinger
 *   @author TSI-mc
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2018 Andy Scherzinger
 *   Copyright (C) 2018 TSI-mc
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.DatePicker;

import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 *  Dialog requesting a date after today.
 */
public class ExpirationDatePickerDialogFragment
        extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    /** Tag for FragmentsManager */
    public static final String DATE_PICKER_DIALOG = "DATE_PICKER_DIALOG";

    /** Parameter constant for date chosen initially */
    private static final String ARG_CHOSEN_DATE_IN_MILLIS = "CHOSEN_DATE_IN_MILLIS";

    private OnExpiryDateListener onExpiryDateListener;

    /**
     * Factory method to create new instances
     *
     * @param chosenDateInMillis Date chosen when the dialog appears
     * @return New dialog instance
     */
    public static ExpirationDatePickerDialogFragment newInstance(long chosenDateInMillis) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_CHOSEN_DATE_IN_MILLIS, chosenDateInMillis);

        ExpirationDatePickerDialogFragment dialog = new ExpirationDatePickerDialogFragment();
        dialog.setArguments(arguments);
        return dialog;
    }

    public void setOnExpiryDateListener(OnExpiryDateListener onExpiryDateListener) {
        this.onExpiryDateListener = onExpiryDateListener;
    }

    /**
     * {@inheritDoc}
     *
     * @return A new dialog to let the user choose an expiration date that will be bound to a share link.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Chosen date received as an argument must be later than tomorrow ; default to tomorrow in other case
        final Calendar chosenDate = Calendar.getInstance();
        long tomorrowInMillis = chosenDate.getTimeInMillis() + DateUtils.DAY_IN_MILLIS;
        long chosenDateInMillis = requireArguments().getLong(ARG_CHOSEN_DATE_IN_MILLIS);
        chosenDate.setTimeInMillis(Math.max(chosenDateInMillis, tomorrowInMillis));

        // Create a new instance of DatePickerDialog
        DatePickerDialog dialog = new DatePickerDialog(
            requireActivity(),
            R.style.FallbackDatePickerDialogTheme,
            this,
            chosenDate.get(Calendar.YEAR),
            chosenDate.get(Calendar.MONTH),
            chosenDate.get(Calendar.DAY_OF_MONTH)
        );

        //show unset button only when date is already selected
        if (chosenDateInMillis > 0) {
            dialog.setButton(
                Dialog.BUTTON_NEUTRAL,
                getText(R.string.share_via_link_unset_password),
                (dialog1, which) -> {
                    if (onExpiryDateListener != null) {
                        onExpiryDateListener.onDateUnSet();
                    }
                });
        }

        dialog.show();
        dialog.getButton(DatePickerDialog.BUTTON_NEUTRAL).setTextColor(ThemeColorUtils.primaryColor(getContext(), true));
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ThemeColorUtils.primaryColor(getContext(), true));
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ThemeColorUtils.primaryColor(getContext(), true));

        // Prevent days in the past may be chosen
        DatePicker picker = dialog.getDatePicker();
        picker.setMinDate(tomorrowInMillis - 1000);

        // Enforce spinners view; ignored by MD-based theme in Android >=5, but calendar is REALLY buggy
        // in Android < 5, so let's be sure it never appears (in tablets both spinners and calendar are
        // shown by default)
        picker.setCalendarViewShown(false);

        return dialog;
    }

    /**
     * Called when the user chooses an expiration date.
     *
     * @param view        View instance where the date was chosen
     * @param year        Year of the date chosen.
     * @param monthOfYear Month of the date chosen [0, 11]
     * @param dayOfMonth  Day of the date chosen
     */
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        Calendar chosenDate = Calendar.getInstance();
        chosenDate.set(Calendar.YEAR, year);
        chosenDate.set(Calendar.MONTH, monthOfYear);
        chosenDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        long chosenDateInMillis = chosenDate.getTimeInMillis();

        if (onExpiryDateListener != null) {
            onExpiryDateListener.onDateSet(year, monthOfYear, dayOfMonth, chosenDateInMillis);
        }
    }

    public interface OnExpiryDateListener {
        void onDateSet(int year, int monthOfYear, int dayOfMonth, long chosenDateInMillis);

        void onDateUnSet();
    }
}
