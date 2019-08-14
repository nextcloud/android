package com.owncloud.android.ui.dialog;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.UserInfoActivity;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import static android.content.Context.ACCOUNT_SERVICE;

public class AccountRemovalConfirmationDialog extends DialogFragment {

    private Account account;

    public static AccountRemovalConfirmationDialog newInstance(Account account, boolean removeDirectly) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(UserInfoActivity.KEY_ACCOUNT, account);
        bundle.putBoolean(UserInfoActivity.KEY_DIRECT_REMOVE, removeDirectly);

        AccountRemovalConfirmationDialog dialog = new AccountRemovalConfirmationDialog();
        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        account = getArguments().getParcelable(UserInfoActivity.KEY_ACCOUNT);
    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getActivity());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean removeDirectly = getArguments().getBoolean(UserInfoActivity.KEY_DIRECT_REMOVE);
        return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.delete_account)
            .setMessage(getResources().getString(R.string.delete_account_warning, account.name))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.common_ok,
                (dialogInterface, i) -> {
                    // remove contact backup job
                    ContactsPreferenceActivity.cancelContactBackupJobForAccount(getActivity(), account);

                    ContentResolver contentResolver = getActivity().getContentResolver();

                    // disable daily backup
                    ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(
                        contentResolver);

                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                        "false");

                    String arbitraryDataPushString;

                    if (!TextUtils.isEmpty(arbitraryDataPushString = arbitraryDataProvider.getValue(
                        account, PushUtils.KEY_PUSH)) &&
                        !TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
                        Gson gson = new Gson();
                        PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryDataPushString,
                            PushConfigurationState.class);
                        pushArbitraryData.setShouldBeDeleted(true);
                        arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PushUtils.KEY_PUSH,
                            gson.toJson(pushArbitraryData));
                        EventBus.getDefault().post(new TokenPushEvent());
                    }


                    if (getActivity() != null && !removeDirectly) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(UserInfoActivity.KEY_ACCOUNT, Parcels.wrap(account));
                        Intent intent = new Intent();
                        intent.putExtras(bundle);
                        getActivity().setResult(UserInfoActivity.KEY_DELETE_CODE, intent);
                        getActivity().finish();
                    } else {
                        AccountManager am = (AccountManager) getActivity().getSystemService(ACCOUNT_SERVICE);

                        if (am != null) {
                            am.removeAccount(account, null, null);
                        }

                        Intent start = new Intent(getActivity(), FileDisplayActivity.class);
                        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(start);
                    }

                })
            .setNegativeButton(R.string.common_cancel, null)
            .create();
    }
}
