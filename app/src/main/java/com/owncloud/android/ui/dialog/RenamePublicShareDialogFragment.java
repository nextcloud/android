/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
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
            MaterialButton positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(requireDialog().getWindow(), binding.userInput);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        publicShare = BundleExtensionsKt.getParcelableArgument(requireArguments(), ARG_PUBLIC_SHARE, OCShare.class);

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
            .setNegativeButton(R.string.common_cancel, this)
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
