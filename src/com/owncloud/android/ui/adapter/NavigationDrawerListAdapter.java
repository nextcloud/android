package com.owncloud.android.ui.adapter;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.LayoutParams;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.BitmapUtils;

import org.apache.commons.codec.binary.Hex;

public class NavigationDrawerListAdapter extends BaseAdapter {

    private final static String TAG  = "NavigationDrawerListAdapter";
    private Context mContext;
    private ArrayList<String> mDrawerItems = new ArrayList<String>();
    ArrayList<Object> all = new ArrayList<Object>();
    private Account[] mAccounts;
    private boolean mShowAccounts;
    private Account currentAccount;
    private FileDisplayActivity mFileDisplayActivity;


    public NavigationDrawerListAdapter(Context context, FileDisplayActivity fileDisplayActivity){
        mFileDisplayActivity = fileDisplayActivity;
        mContext = context;

        for (String string : mContext.getResources().getStringArray(R.array.drawer_items)) {
            mDrawerItems.add(string);
        }

        updateAccountList();

        all.addAll(mDrawerItems);
    }

    public void updateAccountList(){
        AccountManager am = (AccountManager) mContext.getSystemService(mContext.ACCOUNT_SERVICE);
        mAccounts = am.getAccountsByType(MainApp.getAccountType());
        currentAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
    }

    @Override
    public int getCount() {
        if (mShowAccounts){
            return mDrawerItems.size() + 1;
        } else {
            return mDrawerItems.size();
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

        if (all.size() > position) {
            // Normal entry
            if (all.get(position) instanceof String){
                View view = inflator.inflate(R.layout.drawer_list_item, null);
                view.setMinimumHeight(40);
                TextView textView = (TextView) view.findViewById(R.id.drawer_textView);

                String entry = (String) all.get(position);
                textView.setText(entry);

                return view;
            }

            // Account
            if (all.get(position) instanceof Account[]){
                final View view = inflator.inflate(R.layout.drawer_account_group, null);

                final RadioGroup group = (RadioGroup) view.findViewById(R.id.drawer_radio_group);

                for (Account account : mAccounts) {
                    RadioButton rb = new RadioButton(mContext);
                    rb.setText(account.name);

                    try {
                        // using adapted algorithm from /core/js/placeholder.js:50
                        int lastAtPos = account.name.lastIndexOf("@");
                        String username  = account.name.substring(0, lastAtPos);
                        byte[] seed = username.getBytes("UTF-8");
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] seedMd5 = md.digest(seed);
                        Integer seedMd5Int = Math.abs(new String(Hex.encodeHex(seedMd5)).hashCode());

                        double maxRange = java.lang.Integer.MAX_VALUE;
                        float hue = (float) (seedMd5Int / maxRange * 360);

                        Log_OC.d(TAG, "hue: " + hue);


                        int[] rgb = BitmapUtils.HSLtoRGB(hue, 90.0f, 65.0f, 1.0f);
                        rb.setTextColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                        Log_OC.d(TAG, "Color: " + rgb[0] + " " + rgb[1] + " " + rgb[2]);

                    } catch (Exception e){
                        Log_OC.d(TAG, e.toString());
                        rb.setTextColor(mContext.getResources().getColor(R.color.black));
                    }
                    RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                    params.weight=1.0f;
                    params.setMargins(15, 5, 5, 5);

                    // Check the current account that is being used
                    if (account.name.equals(currentAccount.name)) {
                        rb.setChecked(true);
                    } else {
                        rb.setChecked(false);
                    }

                    group.addView(rb, params);
                }

                group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        // checkedId is the RadioButton selected
                        RadioButton rb = (RadioButton) view.findViewById(checkedId);

                        AccountUtils.setCurrentOwnCloudAccount(mContext,rb.getText().toString());
                        notifyDataSetChanged();
                        mFileDisplayActivity.closeDrawer();
                        
                        // restart the main activity
                        mFileDisplayActivity.restart();
                    }
                });

                return view;
            }
        }
        return convertView;
    }

    // TODO update Account List after creating a new account and on fresh installation
    public void setShowAccounts(boolean value){
        all.clear();
        all.addAll(mDrawerItems);

        if (value){
            all.add(1, mAccounts);
        }
        mShowAccounts = value;
    }
}
