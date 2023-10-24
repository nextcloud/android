/*
 *
 *  Nextcloud Android client application
 *
 *  @author Tobias Kaminsky
 *  @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 *  Copyright (C) 2019 Tobias Kaminsky
 *  Copyright (C) 2019 Nextcloud GmbH
 *  Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.MultipleAccountsBinding;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.adapter.UserListItem;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

public class MultipleAccountsDialog extends DialogFragment implements Injectable, UserListAdapter.ClickListener {

    @Inject UserAccountManager accountManager;
    @Inject ViewThemeUtils viewThemeUtils;
    public boolean highlightCurrentlyActiveAccount = true;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalArgumentException("Activity may not be null");
        }

        // Inflate the layout for the dialog
        LayoutInflater inflater = activity.getLayoutInflater();
        MultipleAccountsBinding binding = MultipleAccountsBinding.inflate(inflater, null, false);

        final Context parent = getActivity();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.getRoot().getContext());

        UserListAdapter adapter = new UserListAdapter(parent,
                                                      accountManager,
                                                      getAccountListItems(),
                                                      this,
                                                      false,
                                                      highlightCurrentlyActiveAccount,
                                                      false,
                                                      viewThemeUtils);

        binding.list.setHasFixedSize(true);
        binding.list.setLayoutManager(new LinearLayoutManager(activity));
        binding.list.setAdapter(adapter);

        builder.setView(binding.getRoot()).setTitle(R.string.common_choose_account);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.getRoot().getContext(), builder);

        return builder.create();
    }

    /**
     * creates the account list items list including the add-account action in case
     * multiaccount_support is enabled.
     *
     * @return list of account list items
     */
    private List<UserListItem> getAccountListItems() {
        List<User> users = accountManager.getAllUsers();
        List<UserListItem> adapterUserList = new ArrayList<>(users.size());
        for (User user : users) {
            adapterUserList.add(new UserListItem(user));
        }

        return adapterUserList;
    }

    @Override
    public void onOptionItemClicked(User user, View view) {
        // By default, access account if option is clicked
        onAccountClicked(user);
    }

    @Override
    public void onAccountClicked(User user) {
        final AccountChooserInterface parentActivity = (AccountChooserInterface) getActivity();
        if (parentActivity != null) {
            parentActivity.onAccountChosen(user);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
