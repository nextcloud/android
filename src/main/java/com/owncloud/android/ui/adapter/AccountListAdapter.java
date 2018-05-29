/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

/**
 * This Adapter populates a ListView with all accounts within the app.
 */
public class AccountListAdapter extends ArrayAdapter<AccountListItem> implements DisplayUtils.AvatarGenerationListener {
    private static final String TAG = AccountListAdapter.class.getSimpleName();
    private float mAccountAvatarRadiusDimension;
    private final BaseActivity mContext;
    private List<AccountListItem> mValues;
    private AccountListAdapterListener mListener;
    private Drawable mTintedCheck;

    public AccountListAdapter(BaseActivity context, List<AccountListItem> values, Drawable tintedCheck) {
        super(context, -1, values);
        this.mContext = context;
        this.mValues = values;
        if (context instanceof AccountListAdapterListener) {
            this.mListener = (AccountListAdapterListener) context;
        }
        this.mAccountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.mTintedCheck = tintedCheck;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        AccountViewHolderItem viewHolder;
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            view = inflater.inflate(R.layout.account_item, parent, false);

            viewHolder = new AccountViewHolderItem();
            viewHolder.imageViewItem = view.findViewById(R.id.user_icon);
            viewHolder.checkViewItem = view.findViewById(R.id.ticker);
            viewHolder.checkViewItem.setImageDrawable(mTintedCheck);
            viewHolder.usernameViewItem = view.findViewById(R.id.user_name);
            viewHolder.accountViewItem = view.findViewById(R.id.account);

            view.setTag(viewHolder);
        } else {
            viewHolder = (AccountViewHolderItem) view.getTag();
        }

        AccountListItem accountListItem = mValues.get(position);

        if (accountListItem != null) {
            // create account item
            if (AccountListItem.TYPE_ACCOUNT == accountListItem.getType()) {
                Account account = accountListItem.getAccount();
                setAccount(viewHolder, account);
                setUsername(viewHolder, account);
                setAvatar(viewHolder, account);
                setCurrentlyActiveState(viewHolder, account);

                TextView usernameView = viewHolder.usernameViewItem;
                TextView accountView = viewHolder.accountViewItem;

                if (!accountListItem.isEnabled()) {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

            } // create add account action item
            else if (AccountListItem.TYPE_ACTION_ADD == accountListItem.getType() && mListener != null) {
                return setupAddAccountListItem(parent);
            }
        }

        return view;
    }

    @NonNull
    private View setupAddAccountListItem(ViewGroup parent) {
        LayoutInflater inflater = mContext.getLayoutInflater();
        View actionView = inflater.inflate(R.layout.account_action, parent, false);

        TextView userName = actionView.findViewById(R.id.user_name);
        userName.setText(R.string.prefs_add_account);
        userName.setTextColor(ThemeUtils.primaryColor(getContext()));

        ((ImageView) actionView.findViewById(R.id.user_icon)).setImageResource(R.drawable.ic_account_plus);

        // bind action listener
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.createAccount();
            }
        });
        return actionView;
    }

    private void setAccount(AccountViewHolderItem viewHolder, Account account) {
        viewHolder.accountViewItem.setText(DisplayUtils.convertIdn(account.name, false));
        viewHolder.accountViewItem.setTag(account.name);
    }

    private void setCurrentlyActiveState(AccountViewHolderItem viewHolder, Account account) {
        if (AccountUtils.getCurrentOwnCloudAccount(getContext()).name.equals(account.name)) {
            viewHolder.checkViewItem.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkViewItem.setVisibility(View.INVISIBLE);
        }
    }

    private void setAvatar(AccountViewHolderItem viewHolder, Account account) {
        try {
            View viewItem = viewHolder.imageViewItem;
            viewItem.setTag(account.name);
            DisplayUtils.setAvatar(account, this, mAccountAvatarRadiusDimension, mContext.getResources(),
                    mContext.getStorageManager(), viewItem, mContext);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
            // use user icon as a fallback
            viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
        }
    }

    private void setUsername(AccountViewHolderItem viewHolder, Account account) {
        try {
            OwnCloudAccount oca = new OwnCloudAccount(account, mContext);
            viewHolder.usernameViewItem.setText(oca.getDisplayName());
        } catch (Exception e) {
            Log_OC.w(TAG, "Account not found right after being read; using account name instead");
            viewHolder.usernameViewItem.setText(AccountUtils.getAccountUsername(account.name));
        }
        viewHolder.usernameViewItem.setTag(account.name);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ImageView)callContext).setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return String.valueOf(((ImageView)callContext).getTag()).equals(tag);
    }

    /**
     * Listener interface for Activities using the {@link AccountListAdapter}
     */
    public interface AccountListAdapterListener {

        void createAccount();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    private static class AccountViewHolderItem {
        private ImageView imageViewItem;
        private ImageView checkViewItem;

        private TextView usernameViewItem;
        private TextView accountViewItem;
    }
}
