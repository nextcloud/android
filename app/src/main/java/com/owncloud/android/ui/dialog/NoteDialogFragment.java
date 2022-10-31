/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.NoteDialogBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.KeyboardUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to input a multiline note for a share
 */
public class NoteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, Injectable {

    private static final String ARG_SHARE = "SHARE";

    @Inject ViewThemeUtils viewThemeUtils;
    @Inject KeyboardUtils keyboardUtils;

    private OCShare share;
    private NoteDialogBinding binding;

    public static NoteDialogFragment newInstance(OCShare share) {
        NoteDialogFragment frag = new NoteDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_SHARE, share);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }
        share = getArguments().getParcelable(ARG_SHARE);
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                 alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(binding.noteText);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = NoteDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        binding.noteText.setText(share.getNote());
        viewThemeUtils.material.colorTextInputLayout(binding.noteContainer);

        // Build the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.noteContainer.getContext());
        builder.setView(view)
            .setPositiveButton(R.string.note_confirm, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.send_note);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.noteContainer.getContext(), builder);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            ComponentsGetter componentsGetter = (ComponentsGetter) getActivity();

            if (componentsGetter != null) {
                String note = "";

                if (binding.noteText.getText() != null) {
                    note = binding.noteText.getText().toString().trim();
                }

                componentsGetter.getFileOperationsHelper().updateNoteToShare(share, note);
            } else {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.note_could_not_sent);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
