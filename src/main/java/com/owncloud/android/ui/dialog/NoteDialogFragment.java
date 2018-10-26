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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Dialog to input a multiline note for a share
 */
public class NoteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_SHARE = "SHARE";

    public static final String NOTE_FRAGMENT = "NOTE_FRAGMENT";

    private OCShare share;
    private Unbinder unbinder;

    @BindView(R.id.user_input)
    EditText noteEditText;

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

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.note_dialog, null, false);

        unbinder = ButterKnife.bind(this, view);

        // Setup layout
        noteEditText.setText(share.getNote());
        noteEditText.requestFocus();
        noteEditText.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setPositiveButton(R.string.note_confirm, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(ThemeUtils.getColoredTitle(getResources().getString(R.string.send_note),
                accentColor));
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
                componentsGetter.getFileOperationsHelper().updateNoteToShare(share,
                    noteEditText.getText().toString().trim());
            } else {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.note_could_not_sent);
            }
        }
    }

    @Override
    public void onStop() {
        unbinder.unbind();

        super.onStop();
    }
}
