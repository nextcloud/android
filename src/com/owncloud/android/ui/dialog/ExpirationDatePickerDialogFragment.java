/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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
 *
 */

package com.owncloud.android.ui.dialog;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.widget.DatePicker;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileActivity;

import java.util.Calendar;

/**
 *  Dialog requesting a date after today.
 */
public class ExpirationDatePickerDialogFragment
        extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    /** Tag for FragmentsManager */
    public static final String DATE_PICKER_DIALOG = "DATE_PICKER_DIALOG";

    /** Parameter constant for {@link OCFile} instance to set the expiration date */
    private static final String ARG_FILE = "FILE";

    /** Parameter constant for date chosen initially */
    private static final String ARG_CHOSEN_DATE_IN_MILLIS = "CHOSEN_DATE_IN_MILLIS";

    /** File to bind an expiration date */
    private OCFile mFile;

    /**
     *  Factory method to create new instances
     *
     *  @param file                 File to bind an expiration date
     *  @param chosenDateInMillis   Date chosen when the dialog appears
     *  @return                     New dialog instance
     */
    public static ExpirationDatePickerDialogFragment newInstance(OCFile file, long chosenDateInMillis) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_FILE, file);
        arguments.putLong(ARG_CHOSEN_DATE_IN_MILLIS, chosenDateInMillis);

        ExpirationDatePickerDialogFragment dialog = new ExpirationDatePickerDialogFragment();
        dialog.setArguments(arguments);
        return dialog;
    }

    /**
     * {@inheritDoc}
     *
     * @return      A new dialog to let the user choose an expiration date that will be bound to a share link.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Load arguments
        mFile = getArguments().getParcelable(ARG_FILE);

        // Chosen date received as an argument must be later than tomorrow ; default to tomorrow in other case
        final Calendar chosenDate = Calendar.getInstance();
        long tomorrowInMillis = chosenDate.getTimeInMillis() + DateUtils.DAY_IN_MILLIS;
        long chosenDateInMillis = getArguments().getLong(ARG_CHOSEN_DATE_IN_MILLIS);
        if (chosenDateInMillis > tomorrowInMillis) {
            chosenDate.setTimeInMillis(chosenDateInMillis);
        } else {
            chosenDate.setTimeInMillis(tomorrowInMillis);
        }

        // Create a new instance of DatePickerDialog
        DatePickerDialog dialog = new DatePickerDialog(
                getActivity(),
                this,
                chosenDate.get(Calendar.YEAR),
                chosenDate.get(Calendar.MONTH),
                chosenDate.get(Calendar.DAY_OF_MONTH)
        );

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
     * Called when the user choses an expiration date.
     *
     * @param view              View instance where the date was chosen
     * @param year              Year of the date chosen.
     * @param monthOfYear       Month of the date chosen [0, 11]
     * @param dayOfMonth        Day of the date chosen
     */
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        Calendar chosenDate = Calendar.getInstance();
        chosenDate.set(Calendar.YEAR, year);
        chosenDate.set(Calendar.MONTH, monthOfYear);
        chosenDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        long chosenDateInMillis = chosenDate.getTimeInMillis();

        ((FileActivity)getActivity()).getFileOperationsHelper().setExpirationDateToShareViaLink(
                mFile,
                chosenDateInMillis
        );
    }
}
