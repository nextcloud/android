/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.ui.adapter.UserInfoAdapter;

import org.parceler.Parcels;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by mdjanic on 25/01/2017.
 */

public class UserInfoActivity extends FileActivity {
    private static final String TAG = UserInfoActivity.class.getSimpleName();

    private static final String KEY_USER_DATA = "USER_DATA";
    private static final String KEY_ACCOUNT = "ACCOUNT";
    private static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";

    private static final int KEY_DELETE_CODE = 101;

    @BindView(R.id.generic_rv)
    RecyclerView genericRecyclerView;

    @BindView(R.id.multi_view)
    RelativeLayout multiView;
    @BindView(R.id.empty_list_view)
    LinearLayout multiListContainer;
    @BindView(R.id.empty_list_view_text)
    TextView multiListMessage;
    @BindView(R.id.empty_list_view_headline)
    TextView multiListHeadline;
    @BindView(R.id.empty_list_icon)
    ImageView multiListIcon;
    @BindView(R.id.empty_list_progress)
    ProgressBar multiListProgressBar;

    @BindString(R.string.preview_sorry)
    String sorryMessage;

    RecyclerView.LayoutManager layoutManager;

    RecyclerView.Adapter adapter;

    private Unbinder unbinder;

    private UserInfo userInfo;
    private Account account;

    private String displayName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        account = Parcels.unwrap(bundle.getParcelable(KEY_ACCOUNT));

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_USER_DATA)) {
            userInfo = Parcels.unwrap(savedInstanceState.getParcelable(KEY_USER_DATA));
        }

        if (bundle.containsKey(KEY_DISPLAY_NAME)) {
            displayName = bundle.getString(KEY_DISPLAY_NAME);
        } else if (userInfo != null && !TextUtils.isEmpty(userInfo.getDisplayName())) {
            displayName = userInfo.getDisplayName();
        }

        setContentView(R.layout.user_info_layout);
        unbinder = ButterKnife.bind(this);

        setupToolbar();
        if (!TextUtils.isEmpty(displayName)) {
            updateActionBarTitleAndHomeButtonByString(displayName);
        } else {
            updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.user_information_description));
        }

        layoutManager = new LinearLayoutManager(this);
        genericRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(genericRecyclerView.getContext(),
                ((LinearLayoutManager)layoutManager).getOrientation());
        genericRecyclerView.addItemDecoration(dividerItemDecoration);

        if (userInfo != null) {
            adapter = new UserInfoAdapter(userInfo, UserInfoActivity.this);
            genericRecyclerView.setAdapter(adapter);
            multiView.setVisibility(View.GONE);
            genericRecyclerView.setVisibility(View.VISIBLE);
        } else {
            setMultiListLoadingMessage();
            fetchAndSetData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_info_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.change_password:
                changeAccountPassword(account);
                break;
            case R.id.delete_account:
                openAccountRemovalConfirmationDialog(account);
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }


    private void setMultiListLoadingMessage() {
        if (multiView != null) {
            multiListHeadline.setText(R.string.file_list_loading);
            multiListMessage.setText("");

            multiListIcon.setVisibility(View.GONE);
            multiListProgressBar.setVisibility(View.VISIBLE);
        }
    }

    public void setMessageForMultiList(String headline, String message) {
        if (multiView != null && multiListMessage != null) {
            multiListHeadline.setText(headline);
            multiListMessage.setText(message);

            multiListProgressBar.setVisibility(View.GONE);
        }
    }


    private void changeAccountPassword(Account account) {
        Intent updateAccountCredentials = new Intent(UserInfoActivity.this, AuthenticatorActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_TOKEN);
        startActivity(updateAccountCredentials);
    }

    private void openAccountRemovalConfirmationDialog(Account account) {
        UserInfoActivity.AccountRemovalConfirmationDialog dialog =
                UserInfoActivity.AccountRemovalConfirmationDialog.newInstance(account);
        dialog.show(getFragmentManager(), "dialog");
    }

    public static class AccountRemovalConfirmationDialog extends DialogFragment {

        private Account account;

        public static UserInfoActivity.AccountRemovalConfirmationDialog newInstance(Account account) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_ACCOUNT, account);

            UserInfoActivity.AccountRemovalConfirmationDialog dialog = new
                    UserInfoActivity.AccountRemovalConfirmationDialog();
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            account = getArguments().getParcelable(KEY_ACCOUNT);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.delete_account)
                    .setMessage(getResources().getString(R.string.delete_account_warning, account.name))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.common_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable(KEY_ACCOUNT, Parcels.wrap(account));
                                    Intent intent = new Intent();
                                    intent.putExtras(bundle);
                                    if (getActivity() != null) {
                                        getActivity().setResult(KEY_DELETE_CODE, intent);
                                        getActivity().finish();
                                    }

                                }
                            })
                    .setNegativeButton(R.string.common_cancel, null)
                    .create();
        }
    }


    private void fetchAndSetData() {
        Thread t = new Thread(new Runnable() {
            public void run() {

                RemoteOperation getRemoteUserInfoOperation = new GetRemoteUserInfoOperation();
                RemoteOperationResult result = getRemoteUserInfoOperation.execute(account, UserInfoActivity.this);

                if (result.isSuccess() && result.getData() != null) {
                    userInfo = (UserInfo) result.getData().get(0);
                    adapter = new UserInfoAdapter(userInfo, UserInfoActivity.this);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            genericRecyclerView.setAdapter(adapter);
                            multiView.setVisibility(View.GONE);
                            genericRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    // show error
                    setMessageForMultiList(result.getLogMessage(), sorryMessage);
                    Log_OC.d(TAG, result.getLogMessage());
                }
            }
        });

        t.start();
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userInfo != null) {
            outState.putParcelable(KEY_USER_DATA, Parcels.wrap(userInfo));
        }
    }

}
