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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import com.owncloud.android.R;
import com.owncloud.android.databinding.NoteDialogBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to input a multiline note for a share
 */
public class NoteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_SHARE = "SHARE";

    public static final String NOTE_FRAGMENT = "NOTE_FRAGMENT";

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = NoteDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        binding.noteText.setText(share.getNote());
        binding.noteText.requestFocus();
        ThemeTextInputUtils.colorTextInput(binding.noteContainer, binding.noteText, primaryColor);
        binding.noteText.setHighlightColor(getResources().getColor(R.color.et_highlight_color));
        binding.noteContainer.setDefaultHintTextColor(new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused},
            },
            new int[]{
                Color.GRAY,
                getResources().getColor(R.color.text_color)
            }
        ));
        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setPositiveButton(R.string.note_confirm, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.send_note);
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
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
