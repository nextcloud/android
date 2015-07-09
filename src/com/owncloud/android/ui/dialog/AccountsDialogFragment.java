/**
 *   ownCloud Android client application
 *
 *   @author masensio on 08/07/2015.
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.utils.DisplayUtils;


public class AccountsDialogFragment extends DialogFragment {
    public static String TAG = "AccountsDialogFragment";
    private AccountDialogFragmentListener mListener;

    public static AccountsDialogFragment getInstance() {
        AccountsDialogFragment df = new AccountsDialogFragment();
        return df;
    }

   public void setOnItemClickListener(AccountDialogFragmentListener listener) {
    mListener = listener;
   }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AccountManager accountManager =
                AccountManager.get(getActivity().getApplicationContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.common_choose_account);
        CharSequence ac[] = new CharSequence[
                accountManager.getAccountsByType(MainApp.getAccountType()).length];
        if (ac.length == 0) {
            // No accounts
            mListener.onNoAccount();
            dismiss();
        }

        for (int i = 0; i < ac.length; ++i) {
            ac[i] = DisplayUtils.convertIdn(
                    accountManager.getAccountsByType(MainApp.getAccountType())[i].name, false);
        }
        builder.setItems(ac, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onAccountSelected(accountManager.getAccountsByType(
                        MainApp.getAccountType())[which].name);
                dismiss();
            }
        });

        builder.setCancelable(true);

        return builder.create();

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mListener.onAccountSelectionCancel();
    }

    public interface AccountDialogFragmentListener {
        void onAccountSelected(String accountName);
        void onNoAccount();
        void onAccountSelectionCancel();
    }

}
