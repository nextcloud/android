/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * @author Nick Antoniou
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.UserInfoActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This Adapter populates a RecyclerView with all accounts within the app.
 */
public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountViewHolderItem>
                                implements DisplayUtils.AvatarGenerationListener {
    private static final String TAG = AccountListAdapter.class.getSimpleName();
    private float mAccountAvatarRadiusDimension;
    private final BaseActivity mContext;
    private List<AccountListItem> mValues;
    private AccountListAdapterListener mListener;
    private Drawable mTintedCheck;
    private RecyclerView mRecyclerView;
    private UserAccountManager accountManager;

    private static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";
    private static final int KEY_USER_INFO_REQUEST_CODE = 13;

    public AccountListAdapter(BaseActivity context, List<AccountListItem> values, Drawable tintedCheck) {
        this.mContext = context;
        this.accountManager = accountManager;
        this.mValues = values;
        if (context instanceof AccountListAdapterListener) {
            this.mListener = (AccountListAdapterListener) context;
        }
        this.mAccountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.mTintedCheck = tintedCheck;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public AccountViewHolderItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AccountViewHolderItem viewHolder;
        View view = LayoutInflater.from(mContext).inflate(R.layout.account_item, parent, false);
        viewHolder = new AccountViewHolderItem(view);
        viewHolder.checkViewItem.setImageDrawable(mTintedCheck);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolderItem holder, int position) {
        AccountListItem accountListItem = mValues.get(position);

        if (accountListItem != null) {
            // create account item
            if (AccountListItem.TYPE_ACCOUNT == accountListItem.getType()) {
                Account account = accountListItem.getAccount();
                setAccount(holder, account);
                setUsername(holder, account);
                setAvatar(holder, account);
                setCurrentlyActiveState(holder, account);

                TextView usernameView = holder.usernameViewItem;
                TextView accountView = holder.accountViewItem;

                // OnClickListener for when the user selects an account
                holder.itemView.setOnClickListener(view -> {
                    final Intent intent = new Intent(mContext, UserInfoActivity.class);
                    if (accountListItem.isEnabled()) {
                        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, Parcels.wrap(account));
                        try {
                            OwnCloudAccount oca = new OwnCloudAccount(account, MainApp.getAppContext());
                            intent.putExtra(KEY_DISPLAY_NAME, oca.getDisplayName());
                        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                            Log_OC.d(TAG, "Failed to find NC account");
                        }
                        mContext.startActivityForResult(intent, KEY_USER_INFO_REQUEST_CODE);
                    }
                });

                if (!accountListItem.isEnabled()) {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

            } // create add account action item
            else if (AccountListItem.TYPE_ACTION_ADD == accountListItem.getType() && mListener != null) {
                setupAddAccountListItem(holder);
            }
        }

    }

    /**
     * Sets up a View to be used for adding a new account
     *
     * @param holder the holder which contains the View to be used for the Add Account action
     */
    private void setupAddAccountListItem(AccountViewHolderItem holder) {
        View actionView = holder.itemView;

        holder.accountViewItem.setVisibility(View.INVISIBLE);
        holder.checkViewItem.setVisibility(View.INVISIBLE);
        TextView userName = actionView.findViewById(R.id.user_name);
        userName.setText(R.string.prefs_add_account);
        userName.setTextColor(ThemeUtils.primaryColor(mContext, true));

        ((ImageView) actionView.findViewById(R.id.user_icon)).setImageResource(R.drawable.ic_account_plus);

        // bind action listener
        boolean isProviderOrOwnInstallationVisible = mContext.getResources()
                .getBoolean(R.bool.show_provider_or_own_installation);

        if (isProviderOrOwnInstallationVisible) {
            actionView.setOnClickListener(v -> mListener.showFirstRunActivity());
        } else {
            actionView.setOnClickListener(v -> mListener.createAccount());
        }
    }

    /**
     * Sets the name of the account, in the view holder
     *
     * @param viewHolder the view holder that contains the account
     * @param account the account
     */
    private void setAccount(AccountViewHolderItem viewHolder, Account account) {
        viewHolder.accountViewItem.setText(DisplayUtils.convertIdn(account.name, false));
        viewHolder.accountViewItem.setTag(account.name);
    }

    /**
     * Sets the current active state of the account to true if it is the account being used currently,
     * false otherwise
     *
     * @param viewHolder the view holder that contains the account
     * @param account the account
     */
    private void setCurrentlyActiveState(AccountViewHolderItem viewHolder, Account account) {
        Account currentAccount = accountManager.getCurrentAccount();
        if (currentAccount != null && currentAccount.name.equals(account.name)) {
            viewHolder.checkViewItem.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkViewItem.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets the avatar of the account
     *
     * @param viewHolder the view holder that contains the account
     * @param account the account
     */
    private void setAvatar(AccountViewHolderItem viewHolder, Account account) {
        try {
            View viewItem = viewHolder.imageViewItem;
            viewItem.setTag(account.name);
            DisplayUtils.setAvatar(account, this, mAccountAvatarRadiusDimension, mContext.getResources(), viewItem,
                    mContext);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
            // use user icon as a fallback
            viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
        }
    }

    /**
     * Sets the username of the account
     *
     * @param viewHolder the view holder that contains the account
     * @param account the account
     */
    private void setUsername(AccountViewHolderItem viewHolder, Account account) {
        try {
            OwnCloudAccount oca = new OwnCloudAccount(account, mContext);
            viewHolder.usernameViewItem.setText(oca.getDisplayName());
        } catch (Exception e) {
            Log_OC.w(TAG, "Account not found right after being read; using account name instead");
            viewHolder.usernameViewItem.setText(UserAccountManager.getUsername(account));
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
     * Returns the total number of items in the data set held by the adapter
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return this.mValues.size();
    }

    /**
     * Returns an AccountListItem from the specified position in the mValues list
     *
     * @param position of the object to be returned
     * @return An AccountListItem of the specified position
     */
    public AccountListItem getItem(int position) {
        return mValues.get(position);
    }

    /**
     * Deletes the elements in the mValues list and notifies the Adapter
     */
    public void clear() {
        final int size = mValues.size();
        mValues.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Adds all of the items to the data set
     *
     * @param items The item list to be added
     */
    public void addAll(List<AccountListItem> items){
        if(mValues == null){
            mValues = new ArrayList<>();
        }
        mValues.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * Listener interface for Activities using the {@link AccountListAdapter}
     */
    public interface AccountListAdapterListener {

        void showFirstRunActivity();

        void createAccount();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AccountViewHolderItem extends RecyclerView.ViewHolder {
        private ImageView imageViewItem;
        private ImageView checkViewItem;

        private TextView usernameViewItem;
        private TextView accountViewItem;

        AccountViewHolderItem(@NonNull View view) {
            super(view);
            this.imageViewItem = view.findViewById(R.id.user_icon);
            this.checkViewItem = view.findViewById(R.id.ticker);
            this.usernameViewItem = view.findViewById(R.id.user_name);
            this.accountViewItem = view.findViewById(R.id.account);
        }


    }
}
