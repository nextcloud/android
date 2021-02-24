/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ThemeButtonUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AccountRemovalConfirmationDialog extends DialogFragment implements Injectable {
    private static final String KEY_USER = "USER";

    @Inject BackgroundJobManager backgroundJobManager;
    private User user;

    public static AccountRemovalConfirmationDialog newInstance(User user) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_USER, user);

        AccountRemovalConfirmationDialog dialog = new AccountRemovalConfirmationDialog();
        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = getArguments().getParcelable(KEY_USER);
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                               alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireActivity(), R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.delete_account)
            .setMessage(getResources().getString(R.string.delete_account_warning, user.getAccountName()))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.common_ok,
                               (dialogInterface, i) -> backgroundJobManager.startAccountRemovalJob(user.getAccountName(),
                                                                                                   false))
            .setNeutralButton(R.string.common_cancel, null)
            .create();
    }
}
