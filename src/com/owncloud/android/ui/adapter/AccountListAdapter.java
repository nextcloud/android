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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.utils.BitmapUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * This Adapter populates a ListView with all accounts within the app.
 */
public class AccountListAdapter extends ArrayAdapter<AccountListItem> {
    private static final String TAG = AccountListAdapter.class.getSimpleName();

    private final Context mContext;
    private List<AccountListItem> mValues;
    private AccountListAdapterListener mListener;

    public AccountListAdapter(Context context, List<AccountListItem> values) {
        super(context, -1, values);
        this.mContext = context;
        this.mValues = values;
        this.mListener = (AccountListAdapterListener) context;
    }

    public void setAccountList(List<AccountListItem> values) {
        this.mValues = values;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        AccountViewHolderItem viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = ((ManageAccountsActivity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.account_item, parent, false);

            viewHolder = new AccountViewHolderItem();
            viewHolder.textViewItem = (TextView) convertView.findViewById(R.id.user_name);
            viewHolder.imageViewItem = (ImageView) convertView.findViewById(R.id.user_icon);
            viewHolder.passwordButtonItem = (ImageView) convertView.findViewById(R.id.passwordButton);
            viewHolder.removeButtonItem = (ImageView) convertView.findViewById(R.id.removeButton);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AccountViewHolderItem) convertView.getTag();
        }

        AccountListItem accountListItem = mValues.get(position);


        if (accountListItem != null) {
            // create account item
            if (AccountListItem.TYPE_ACCOUNT == accountListItem.getType()) {
                Account account = accountListItem.getAccount();
                viewHolder.textViewItem.setText(account.name);
                viewHolder.textViewItem.setTag(account.name);

                try {
                    int[] rgb = BitmapUtils.calculateRGB(account.name);
                    TextDrawable icon = new TextDrawable(account.name.substring(0, 1).toUpperCase()
                            , rgb[0], rgb[1], rgb[2]);
                    viewHolder.imageViewItem.setImageDrawable(icon);
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
                    // use user icon as a fallback
                    viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
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
                        mListener.removeAccount(mValues.get(position).getAccount());
                        mValues.remove(position);
                        AccountListAdapter.this.notifyDataSetChanged();
                    }
                });
            } // create add account action item
            else if (AccountListItem.TYPE_ACTION_ADD == accountListItem.getType()) {
                LayoutInflater inflater = ((ManageAccountsActivity) mContext).getLayoutInflater();
                View actionView = inflater.inflate(R.layout.account_item, parent, false);
                ((TextView) actionView.findViewById(R.id.user_name)).setText(R.string.prefs_add_account);
                ((ImageView) actionView.findViewById(R.id.user_icon)).setImageResource(R.drawable.ic_account_plus);
                actionView.findViewById(R.id.passwordButton).setVisibility(View.GONE);
                actionView.findViewById(R.id.removeButton).setVisibility(View.GONE);

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

    /**
     * Listener interface for Activities using the {@link AccountListAdapter}
     */
    public interface AccountListAdapterListener {
        void removeAccount(Account account);

        void changePasswordOfAccount(Account account);

        void createAccount();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AccountViewHolderItem {
        TextView textViewItem;
        ImageView imageViewItem;

        ImageView passwordButtonItem;
        ImageView removeButtonItem;
    }
}
