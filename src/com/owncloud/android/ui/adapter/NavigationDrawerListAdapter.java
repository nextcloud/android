/**
 *   ownCloud Android client application
 *
 *   @author tobiasKaminsky
 *   @author masensio
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

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.ui.NavigationDrawerItem;
import com.owncloud.android.ui.activity.FileActivity;

import java.util.ArrayList;

public class NavigationDrawerListAdapter extends BaseAdapter {

    private final static String TAG  = NavigationDrawerListAdapter.class.getSimpleName();

    private Context mContext;

    private ArrayList<NavigationDrawerItem> mNavigationDrawerItems;
    private ArrayList<Object> mAll = new ArrayList<Object>();
    private Account[] mAccounts;
    private boolean mShowAccounts;
    private Account mCurrentAccount;
    private FileActivity mFileActivity;


    public NavigationDrawerListAdapter(Context context, FileActivity fileActivity,
                                       ArrayList<NavigationDrawerItem> navigationDrawerItems){
        mFileActivity = fileActivity;
        mContext = context;
        mNavigationDrawerItems = navigationDrawerItems;

        updateAccountList();

        mAll.addAll(mNavigationDrawerItems);
    }

    public void updateAccountList(){
        AccountManager am = (AccountManager) mContext.getSystemService(mContext.ACCOUNT_SERVICE);
        mAccounts = am.getAccountsByType(MainApp.getAccountType());
        mCurrentAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
    }

    @Override
    public int getCount() {
        if (mShowAccounts){
            return mNavigationDrawerItems.size() + 1;
        } else {
            return mNavigationDrawerItems.size();
        }
    }

    @Override
    public Object getItem(int position) {
        //return all.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflator = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mAll.size() > position) {
            // Normal entry
            if (mAll.get(position) instanceof NavigationDrawerItem){
                NavigationDrawerItem navItem = (NavigationDrawerItem) mAll.get(position);

                View view = inflator.inflate(R.layout.drawer_list_item, null);

                LinearLayout itemLayout = (LinearLayout) view.findViewById(R.id.itemLayout);
                itemLayout.setContentDescription(navItem.getContentDescription());

                TextView itemText = (TextView) view.findViewById(R.id.itemTitle);
                itemText.setText(navItem.getTitle());

                if(navItem.getIcon()!=0) {
                    ImageView itemImage = (ImageView) view.findViewById(R.id.itemIcon);
                    itemImage.setImageResource(navItem.getIcon());
                }

                return view;
            }
            // TODO re-enable when "Accounts" is available in Navigation Drawer
            // Account
//            if (mAll.get(position) instanceof Account[]){
//                final View view = inflator.inflate(R.layout.drawer_account_group, null);
//
//                final RadioGroup group = (RadioGroup) view.findViewById(R.id.drawer_radio_group);
//
//                for (Account account : mAccounts) {
//                    RadioButton rb = new RadioButton(mContext);
//
//                    rb.setText(account.name);
//                    rb.setContentDescription(account.name);
//                    rb.setTextColor(Color.BLACK);
//                    rb.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//                    rb.setSingleLine();
//                    rb.setCompoundDrawablePadding(30);
//
//
//                    try {
//                        // using adapted algorithm from /core/js/placeholder.js:50
//                        int lastAtPos = account.name.lastIndexOf("@");
//                        String username  = account.name.substring(0, lastAtPos);
//                        byte[] seed = username.getBytes("UTF-8");
//                        MessageDigest md = MessageDigest.getInstance("MD5");
////                        Integer seedMd5Int = Math.abs(new String(Hex.encodeHex(seedMd5))
////                      .hashCode());
//                        Integer seedMd5Int = String.format(Locale.ROOT, "%032x",
//                                new BigInteger(1, md.digest(seed))).hashCode();
//
//                        double maxRange = java.lang.Integer.MAX_VALUE;
//                        float hue = (float) (seedMd5Int / maxRange * 360);
//
//                        int[] rgb = BitmapUtils.HSLtoRGB(hue, 90.0f, 65.0f, 1.0f);
//
//                        TextDrawable text = new TextDrawable(username.substring(0, 1).toUpperCase(),
//                                rgb[0], rgb[1], rgb[2]);
//                        rb.setCompoundDrawablesWithIntrinsicBounds(text, null, null, null);
//
//
//                    } catch (Exception e){
//                        Log_OC.d(TAG, e.toString());
//                        rb.setTextColor(mContext.getResources().getColor(R.color.black));
//                    }
//                    RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
//                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//                    params.weight=1.0f;
//                    params.setMargins(15, 5, 5, 5);
//
//                    // Check the current account that is being used
//                    if (account.name.equals(mCurrentAccount.name)) {
//                        rb.setChecked(true);
//                    } else {
//                        rb.setChecked(false);
//                    }
//
//                    group.addView(rb, params);
//                }
//
//                group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
//                    public void onCheckedChanged(RadioGroup group, int checkedId) {
//                        // checkedId is the RadioButton selected
//                        RadioButton rb = (RadioButton) view.findViewById(checkedId);
//
//                        AccountUtils.setCurrentOwnCloudAccount(mContext,rb.getText().toString());
//                        notifyDataSetChanged();
//                        mFileActivity.closeDrawer();
//
//                        // restart the main activity
//                        mFileActivity.restart();
//                    }
//                });
//
//                return view;
//            }
        }
        return convertView;
    }

    //TODO re-enable when "Accounts" is available in Navigation Drawer
    // TODO update Account List after creating a new account and on fresh installation
//    public void setShowAccounts(boolean value){
//        mAll.clear();
//        mAll.addAll(mNavigationDrawerItems);
//
//        if (value){
//            mAll.add(1, mAccounts);
//        }
//        mShowAccounts = value;
//    }
}
