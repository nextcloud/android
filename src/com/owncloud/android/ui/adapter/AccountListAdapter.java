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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.utils.DisplayUtils;

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
        this.mListener = (AccountListAdapterListener) context;
        this.mAccountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.mTintedCheck = tintedCheck;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        AccountViewHolderItem viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.account_item, parent, false);

            viewHolder = new AccountViewHolderItem();
            viewHolder.imageViewItem = (ImageView) convertView.findViewById(R.id.user_icon);
            viewHolder.checkViewItem = (ImageView) convertView.findViewById(R.id.ticker);
            viewHolder.checkViewItem.setImageDrawable(mTintedCheck);
            viewHolder.accountViewItem = (TextView) convertView.findViewById(R.id.user_name);
            viewHolder.passwordButtonItem = (ImageButton) convertView.findViewById(R.id.passwordButton);
            viewHolder.removeButtonItem = (ImageButton) convertView.findViewById(R.id.removeButton);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AccountViewHolderItem) convertView.getTag();
        }

        AccountListItem accountListItem = mValues.get(position);


        if (accountListItem != null) {
            // create account item
            if (AccountListItem.TYPE_ACCOUNT == accountListItem.getType()) {
                Account account = accountListItem.getAccount();
                viewHolder.accountViewItem.setText(account.name);
                viewHolder.accountViewItem.setTag(account.name);

                try {
                    DisplayUtils.setAvatar(account, this, mAccountAvatarRadiusDimension,
                            mContext.getResources(), mContext.getStorageManager(), viewHolder.imageViewItem);
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
                    // use user icon as a fallback
                    viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
                }

                if (AccountUtils.getCurrentOwnCloudAccount(getContext()).name.equals(account.name)) {
                    viewHolder.checkViewItem.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.checkViewItem.setVisibility(View.INVISIBLE);
                }

                /// bind listener to change password
                viewHolder.passwordButtonItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.changePasswordOfAccount(mValues.get(position).getAccount());
                    }
                });

                /// bind listener to remove account
                viewHolder.removeButtonItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.performAccountRemoval(mValues.get(position).getAccount());
                    }
                });
            } // create add account action item
            else if (AccountListItem.TYPE_ACTION_ADD == accountListItem.getType()) {
                LayoutInflater inflater = mContext.getLayoutInflater();
                View actionView = inflater.inflate(R.layout.account_action, parent, false);
                ((TextView) actionView.findViewById(R.id.user_name)).setText(R.string.prefs_add_account);
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
        }

        return convertView;
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
        void performAccountRemoval(Account account);

        void changePasswordOfAccount(Account account);

        void createAccount();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    private static class AccountViewHolderItem {
        ImageView imageViewItem;
        ImageView checkViewItem;

        TextView accountViewItem;

        ImageButton passwordButtonItem;
        ImageButton removeButtonItem;
    }
}
