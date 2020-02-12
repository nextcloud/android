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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.adapter.UserListItem;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MultipleAccountsDialog extends DialogFragment implements Injectable, UserListAdapter.ClickListener {
    @BindView(R.id.list)
    RecyclerView listView;

    @Inject UserAccountManager accountManager;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalArgumentException("Activity may not be null");
        }

        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.multiple_accounts, null);
        ButterKnife.bind(this, view);


        final ReceiveExternalFilesActivity parent = (ReceiveExternalFilesActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        Drawable tintedCheck = DrawableCompat.wrap(ContextCompat.getDrawable(parent, R.drawable.account_circle_white));
        int tint = ThemeUtils.primaryColor(getContext());
        DrawableCompat.setTint(tintedCheck, tint);


        UserListAdapter adapter = new UserListAdapter(parent,
                                                      accountManager,
                                                      getAccountListItems(),
                                                      tintedCheck,
                                                      this,
                                                      false);

        listView.setHasFixedSize(true);
        listView.setLayoutManager(new LinearLayoutManager(activity));
        listView.setAdapter(adapter);

        builder.setView(view)
            .setTitle(ThemeUtils.getColoredTitle(getResources().getString(R.string.common_choose_account), accentColor));
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
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
    public void onClick(User user) {
        final ReceiveExternalFilesActivity parentActivity = (ReceiveExternalFilesActivity) getActivity();
        if (parentActivity != null) {
            parentActivity.changeAccount(user.toPlatformAccount());
        }
        dismiss();
    }
}
