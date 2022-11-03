/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH.
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.EditBoxDialogBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.KeyboardUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to rename a public share.
 */
public class RenamePublicShareDialogFragment
    extends DialogFragment implements DialogInterface.OnClickListener, Injectable {

    private static final String ARG_PUBLIC_SHARE = "PUBLIC_SHARE";

    public static final String RENAME_PUBLIC_SHARE_FRAGMENT = "RENAME_PUBLIC_SHARE_FRAGMENT";

    @Inject ViewThemeUtils viewThemeUtils;
    @Inject KeyboardUtils keyboardUtils;

    private EditBoxDialogBinding binding;
    private OCShare publicShare;

    public static RenamePublicShareDialogFragment newInstance(OCShare share) {
        RenamePublicShareDialogFragment frag = new RenamePublicShareDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PUBLIC_SHARE, share);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                     alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(binding.userInput);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        publicShare = requireArguments().getParcelable(ARG_PUBLIC_SHARE);

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = EditBoxDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer);
        binding.userInput.setText(publicShare.getLabel());

        // Build the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view.getContext());
        builder.setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.public_share_name);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInput.getContext(), builder);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newName = "";
            if (binding.userInput.getText() != null) {
                newName = binding.userInput.getText().toString().trim();
            }

            if (TextUtils.isEmpty(newName)) {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.label_empty);
                return;
            }

            ((ComponentsGetter) requireActivity()).getFileOperationsHelper().setLabelToPublicShare(publicShare,
                                                                                                   newName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
