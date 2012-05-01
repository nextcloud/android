/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

package eu.alefzero.owncloud.ui.activity;

import java.net.MalformedURLException;
import java.net.URL;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.authenticator.AuthUtils;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;

/**
 * This Activity is used to add an ownCloud account to the App
 * @author Bartek Przybylski
 *
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
    private Thread mAuthThread;
    private final Handler mHandler = new Handler();
    private boolean mUseSSLConnection;

    public static final String PARAM_USERNAME = "param_Username";
    public static final String PARAM_HOSTNAME = "param_Hostname";

    public AuthenticatorActivity() {
      mUseSSLConnection = false;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_setup);
        if (getIntent().hasExtra(PARAM_USERNAME)) {
            String username = getIntent().getStringExtra(PARAM_HOSTNAME);
            TextView host_text, user_text;
            host_text = (TextView) findViewById(R.id.host_URL);
            user_text = (TextView) findViewById(R.id.account_username);
            host_text.setText(host_text.getText() + username.substring(username.lastIndexOf('@')));
            user_text.setText(user_text.getText() + username.substring(0, username.lastIndexOf('@') - 1));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Trying to login");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(getClass().getName(), "Login canceled");
                if (mAuthThread != null) {
                    mAuthThread.interrupt();
                    finish();
                }
            }
        });
        return dialog;
    }

    public void onAuthenticationResult(boolean result, String message) {
        if (result) {
            TextView username_text = (TextView) findViewById(R.id.account_username),
                    password_text = (TextView) findViewById(R.id.account_password);

            URL url;
            try {
                url = new URL(message);
            } catch (MalformedURLException e) {
                // should never happen
                Log.e(getClass().getName(), "Malformed URL: " + message);
                return;
            }

            String username = username_text.getText().toString().trim();
            String accountName = username + "@" + url.getHost();
            Account account = new Account(accountName, AccountAuthenticator.ACCOUNT_TYPE);
            AccountManager accManager = AccountManager.get(this);
            accManager.addAccountExplicitly(account, password_text.getText().toString(), null);
            
            // Add this account as default in the preferences, if there is none already
            Account defaultAccount = AuthUtils.getCurrentOwnCloudAccount(this);
            if(defaultAccount == null){
            	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            	editor.putString("select_oc_account", accountName);
            	editor.commit();
            }

            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.ACCOUNT_TYPE);
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, AccountAuthenticator.ACCOUNT_TYPE);
            accManager.setUserData(account, AccountAuthenticator.KEY_OC_URL, url.toString());

            // TODO prepare this URL using a central service
            intent.putExtra(AccountManager.KEY_USERDATA, username);
            accManager.setUserData(account, AccountAuthenticator.KEY_CONTACT_URL,
                    url.toString().replace(AuthUtils.WEBDAV_PATH_2_0, AuthUtils.CARDDAV_PATH_2_0)
            );

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            getContentResolver().startSync(ProviderTableMeta.CONTENT_URI, bundle);

            dismissDialog(0);
            finish();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            dismissDialog(0);
        }
    }

    public void onCancelClick(View view) {
        Log.i(getClass().getName(), "Account creating canceled");
        this.finish();
    }

    public void onOkClick(View view) {
        TextView url_text = (TextView) findViewById(R.id.host_URL);
        TextView username_text = (TextView) findViewById(R.id.account_username);
        TextView password_text = (TextView) findViewById(R.id.account_password);
        Log.i(getClass().getName(), "OK clicked");
        boolean hasErrors = false;

        URL uri = null;
        if (url_text.getText().toString().trim().length() == 0) {
            url_text.setTextColor(Color.RED);
            hasErrors = true;
        } else {
            url_text.setTextColor(android.R.color.primary_text_light);
        }
        try {
            String url_str = url_text.getText().toString();
            if (!url_str.startsWith("http://") &&
                    !url_str.startsWith("https://")) {
              if (mUseSSLConnection)
                url_str = "https://" + url_str;
              else
                url_str = "http://" + url_str;
            }
            uri = new URL(url_str);
        } catch (MalformedURLException e) {
            url_text.setTextColor(Color.RED);
            e.printStackTrace();
            hasErrors = true;
        }

        if (username_text.getText().toString().contains(" ") ||
                username_text.getText().toString().trim().length() == 0) {
            username_text.setTextColor(Color.RED);
            hasErrors = true;
        } else {
            username_text.setTextColor(android.R.color.primary_text_light);
        }

        if (password_text.getText().toString().trim().length() == 0) {
            password_text.setTextColor(Color.RED);
            hasErrors = true;
        } else {
            password_text.setTextColor(android.R.color.primary_text_light);
        }
        if (hasErrors) {
            return;
        }
        
        int new_port = uri.getPort();
        if (new_port == -1) {
          if (mUseSSLConnection)
            new_port = 443;
          else
            new_port = 80;
        }
        
        try {
          uri = new URL(uri.getProtocol(), uri.getHost(), new_port, uri.getPath());
        } catch (MalformedURLException e) {
          e.printStackTrace(); // should not happend
        }
        
        showDialog(0);
        mAuthThread = AuthUtils.attemptAuth(uri,
                username_text.getText().toString(),
                password_text.getText().toString(),
                mHandler,
                AuthenticatorActivity.this);
    }
    
    public void sslBadgeClick(View view, String val) {
      mUseSSLConnection = ((TextView)view).getText().equals("SSL");
    }
    
    public void passwordBadgeClick(View view, String val) {
      if(val.equals("Hide")) {
        ((TextView)view).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
      } else {
        ((TextView)view).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      }
    }
}
