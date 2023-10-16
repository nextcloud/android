/*
 * ownCloud Android client application
 *
 * Copyright (C) 2012 Bartek Przybylski Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


public class ConfirmationDialogFragment extends DialogFragment implements Injectable {

    final static String ARG_MESSAGE_RESOURCE_ID = "resource_id";
    final static String ARG_MESSAGE_ARGUMENTS = "string_array";
    final static String ARG_TITLE_ID = "title_id";

    final static String ARG_POSITIVE_BTN_RES = "positive_btn_res";
    final static String ARG_NEUTRAL_BTN_RES = "neutral_btn_res";
    final static String ARG_NEGATIVE_BTN_RES = "negative_btn_res";

    public static final String FTAG_CONFIRMATION = "CONFIRMATION_FRAGMENT";

    @Inject ViewThemeUtils viewThemeUtils;


    private ConfirmationDialogFragmentListener mListener;

    /**
     * Public factory method to create new ConfirmationDialogFragment instances.
     *
     * @param messageResId         Resource id for a message to show in the dialog.
     * @param messageArguments     Arguments to complete the message, if it's a format string. May be null.
     * @param titleResId           Resource id for a text to show in the title. 0 for default alert title, -1 for no
     *                             title.
     * @param positiveButtonTextId Resource id for the text of the positive button. -1 for no positive button.
     * @param neutralButtonTextId  Resource id for the text of the neutral button. -1 for no neutral button.
     * @param negativeButtonTextId Resource id for the text of the negative button. -1 for no negative button.
     * @return Dialog ready to show.
     */
    public static ConfirmationDialogFragment newInstance(int messageResId, String[] messageArguments, int titleResId,
                                                         int positiveButtonTextId, int neutralButtonTextId, int negativeButtonTextId) {
        if (messageResId == -1) {
            throw new IllegalStateException("Calling confirmation dialog without message resource");
        }

        ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageResId);
        args.putStringArray(ARG_MESSAGE_ARGUMENTS, messageArguments);
        args.putInt(ARG_TITLE_ID, titleResId);

        args.putInt(ARG_POSITIVE_BTN_RES, positiveButtonTextId);
        args.putInt(ARG_NEGATIVE_BTN_RES, negativeButtonTextId);
        args.putInt(ARG_NEUTRAL_BTN_RES, neutralButtonTextId);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            MaterialButton positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);
            }

            MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);
            }

            MaterialButton neutralButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(neutralButton);
            }
        }
    }

    public void setOnConfirmationListener(ConfirmationDialogFragmentListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        Activity activity = getActivity();

        if (activity == null) {
            throw new IllegalArgumentException("Activity may not be null");
        }

        Object[] messageArguments = arguments.getStringArray(ARG_MESSAGE_ARGUMENTS);
        int messageId = arguments.getInt(ARG_MESSAGE_RESOURCE_ID, -1);
        int titleId = arguments.getInt(ARG_TITLE_ID, -1);

        int positiveButtonTextId = arguments.getInt(ARG_POSITIVE_BTN_RES, -1);
        int negativeButtonTextId = arguments.getInt(ARG_NEGATIVE_BTN_RES, -1);
        int neutralButtonTextId = arguments.getInt(ARG_NEUTRAL_BTN_RES, -1);

        if (messageArguments == null) {
            messageArguments = new String[]{};
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
            .setIcon(R.drawable.ic_warning)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(String.format(getString(messageId), messageArguments));

        if (titleId == 0) {
            builder.setTitle(android.R.string.dialog_alert_title);
        } else if (titleId != -1) {
            builder.setTitle(titleId);
        }

        if (positiveButtonTextId != -1) {
            builder.setPositiveButton(positiveButtonTextId, (dialog, whichButton) -> {
                if (mListener != null) {
                    mListener.onConfirmation(getTag());
                }
                dialog.dismiss();
            });
        }

        if (neutralButtonTextId != -1) {
            builder.setNeutralButton(neutralButtonTextId, (dialog, whichButton) -> {
                if (mListener != null) {
                    mListener.onNeutral(getTag());
                }
                dialog.dismiss();
            });
        }
        if (negativeButtonTextId != -1) {
            builder.setNegativeButton(negativeButtonTextId, (dialog, which) -> {
                if (mListener != null) {
                    mListener.onCancel(getTag());
                }
                dialog.dismiss();
            });
        }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder);

        return builder.create();
    }

    public interface ConfirmationDialogFragmentListener {
        void onConfirmation(String callerTag);

        void onNeutral(String callerTag);

        void onCancel(String callerTag);
    }
}

