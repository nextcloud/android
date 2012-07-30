/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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
import java.net.URLEncoder;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.authenticator.AuthenticationRunnable;
import eu.alefzero.owncloud.authenticator.ConnectionCheckerRunnable;
import eu.alefzero.owncloud.authenticator.OnAuthenticationResultListener;
import eu.alefzero.owncloud.authenticator.OnConnectCheckListener;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.owncloud.extensions.ExtensionsAvailableActivity;
import eu.alefzero.owncloud.utils.OwnCloudVersion;

/**
 * This Activity is used to add an ownCloud account to the App
 * 
 * @author Bartek Przybylski
 * 
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements OnAuthenticationResultListener, OnConnectCheckListener,
        OnFocusChangeListener, OnClickListener {
    private static final int DIALOG_LOGIN_PROGRESS = 0;

    private static final String TAG = "AuthActivity";

    private Thread mAuthThread;
    private AuthenticationRunnable mAuthRunnable;
    private ConnectionCheckerRunnable mConnChkRunnable;
    private final Handler mHandler = new Handler();
    private String mBaseUrl;

    private static final String STATUS_TEXT = "STATUS_TEXT";
    private static final String STATUS_ICON = "STATUS_ICON";
    private static final String STATUS_CORRECT = "STATUS_CORRECT";
    private static final String IS_SSL_CONN = "IS_SSL_CONN";
    private int mStatusText, mStatusIcon;
    private boolean mStatusCorrect, mIsSslConn;

    public static final String PARAM_USERNAME = "param_Username";
    public static final String PARAM_HOSTNAME = "param_Hostname";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_setup);
        ImageView iv = (ImageView) findViewById(R.id.refreshButton);
        ImageView iv2 = (ImageView) findViewById(R.id.viewPassword);
        TextView tv = (TextView) findViewById(R.id.host_URL);
        TextView tv2 = (TextView) findViewById(R.id.account_password);

        if (savedInstanceState != null) {
            mStatusIcon = savedInstanceState.getInt(STATUS_ICON);
            mStatusText = savedInstanceState.getInt(STATUS_TEXT);
            mStatusCorrect = savedInstanceState.getBoolean(STATUS_CORRECT);
            mIsSslConn = savedInstanceState.getBoolean(IS_SSL_CONN);
            setResultIconAndText(mStatusIcon, mStatusText);
            findViewById(R.id.buttonOK).setEnabled(mStatusCorrect);
            if (!mStatusCorrect)
                iv.setVisibility(View.VISIBLE);
            else
                iv.setVisibility(View.INVISIBLE);

        } else {
            mStatusText = mStatusIcon = 0;
            mStatusCorrect = false;
            mIsSslConn = false;
        }
        iv.setOnClickListener(this);
        iv2.setOnClickListener(this);
        tv.setOnFocusChangeListener(this);
        tv2.setOnFocusChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATUS_ICON, mStatusIcon);
        outState.putInt(STATUS_TEXT, mStatusText);
        outState.putBoolean(STATUS_CORRECT, mStatusCorrect);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_LOGIN_PROGRESS: {
            ProgressDialog working_dialog = new ProgressDialog(this);
            working_dialog.setMessage(getResources().getString(
                    R.string.auth_trying_to_login));
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(true);
            working_dialog
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Log.i(TAG, "Login canceled");
                            if (mAuthThread != null) {
                                mAuthThread.interrupt();
                                finish();
                            }
                        }
                    });
            dialog = working_dialog;
            break;
        }
        default:
            Log.e(TAG, "Incorrect dialog called with id = " + id);
        }
        return dialog;
    }

    public void onAuthenticationResult(boolean success, String message) {
        if (success) {
            TextView username_text = (TextView) findViewById(R.id.account_username), password_text = (TextView) findViewById(R.id.account_password);

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
            if (url.getPort() >= 0) {
                accountName += ":" + url.getPort();
            }
            Account account = new Account(accountName,
                    AccountAuthenticator.ACCOUNT_TYPE);
            AccountManager accManager = AccountManager.get(this);
            accManager.addAccountExplicitly(account, password_text.getText()
                    .toString(), null);

            // Add this account as default in the preferences, if there is none
            // already
            Account defaultAccount = AccountUtils
                    .getCurrentOwnCloudAccount(this);
            if (defaultAccount == null) {
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.commit();
            }

            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
                    AccountAuthenticator.ACCOUNT_TYPE);
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
            intent.putExtra(AccountManager.KEY_AUTHTOKEN,
                    AccountAuthenticator.ACCOUNT_TYPE);
            intent.putExtra(AccountManager.KEY_USERDATA, username);

            accManager.setUserData(account, AccountAuthenticator.KEY_OC_URL,
                    url.toString());
            accManager.setUserData(account,
                    AccountAuthenticator.KEY_OC_VERSION, mConnChkRunnable
                            .getDiscoveredVersion().toString());
            accManager.setUserData(account,
                    AccountAuthenticator.KEY_OC_BASE_URL, mBaseUrl);

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            //getContentResolver().startSync(ProviderTableMeta.CONTENT_URI,
            //        bundle);
            ContentResolver.requestSync(account, "org.owncloud", bundle);

            /*
             * if
             * (mConnChkRunnable.getDiscoveredVersion().compareTo(OwnCloudVersion
             * .owncloud_v2) >= 0) { Intent i = new Intent(this,
             * ExtensionsAvailableActivity.class); startActivity(i); }
             */

            finish();
        } else {
            dismissDialog(DIALOG_LOGIN_PROGRESS);
            TextView tv = (TextView) findViewById(R.id.account_username);
            tv.setError(message);
        }
    }
    public void onCancelClick(View view) {
        finish();
    }
    
    public void onOkClick(View view) {
        String prefix = "";
        String url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString();
        if (mIsSslConn) {
            prefix = "https://";
        } else {
            prefix = "http://";
        }
        if (url.toLowerCase().startsWith("http://")
                || url.toLowerCase().startsWith("https://")) {
            prefix = "";
        }
        continueConnection(prefix);
    }

    private void continueConnection(String prefix) {
        String url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString();
        String username = ((TextView) findViewById(R.id.account_username))
                .getText().toString();
        String password = ((TextView) findViewById(R.id.account_password))
                .getText().toString();
        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        URL uri = null;
        String webdav_path = AccountUtils.getWebdavPath(mConnChkRunnable
                .getDiscoveredVersion());

        try {
            mBaseUrl = prefix + url;
            String url_str = prefix + url + webdav_path;
            uri = new URL(url_str);
        } catch (MalformedURLException e) {
            // should not happend
            e.printStackTrace();
        }

        showDialog(DIALOG_LOGIN_PROGRESS);
        mAuthRunnable = new AuthenticationRunnable(uri, username, password);
        mAuthRunnable.setOnAuthenticationResultListener(this, mHandler);
        mAuthThread = new Thread(mAuthRunnable);
        mAuthThread.start();
    }

    @Override
    public void onConnectionCheckResult(ResultType type) {
        mStatusText = mStatusIcon = 0;
        mStatusCorrect = false;
        String t_url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString().toLowerCase();

        switch (type) {
        case OK:
            // ugly as hell
            if (t_url.startsWith("http://") || t_url.startsWith("https://")) {
                mIsSslConn = t_url.startsWith("http://") ? false : true;
                mStatusIcon = R.drawable.ic_ok;
                mStatusText = R.string.auth_connection_established;
                mStatusCorrect = true;
            } else {
                mIsSslConn = true;
                mStatusIcon = android.R.drawable.ic_secure;
                mStatusText = R.string.auth_secure_connection;
                mStatusCorrect = true;
            }
            break;
        case OK_NO_SSL:
            mStatusIcon = android.R.drawable.ic_secure;
            mStatusText = R.string.auth_nossl_plain_ok_title;
            mStatusCorrect = true;
            mIsSslConn = false;
            break;
        case TIMEOUT:
        case INORRECT_ADDRESS:
        case SSL_INIT_ERROR:
        case HOST_NOT_AVAILABLE:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_unknow_host_title;
            break;
        case NO_NETWORK_CONNECTION:
            mStatusIcon = R.drawable.no_network;
            mStatusText = R.string.auth_no_net_conn_title;
            break;
        case INSTANCE_NOT_CONFIGURED:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_not_configured_title;
            break;
        case UNKNOWN_ERROR:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_unknow_error;
            break;
        case FILE_NOT_FOUND:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_incorrect_path_title;
            break;
        default:
            Log.e(TAG, "Incorrect connection checker result type: " + type);
        }
        setResultIconAndText(mStatusIcon, mStatusText);
        if (!mStatusCorrect)
            findViewById(R.id.refreshButton).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.refreshButton).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonOK).setEnabled(mStatusCorrect);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.host_URL) {
            if (!hasFocus) {
                TextView tv = ((TextView) findViewById(R.id.host_URL));
                String uri = tv.getText().toString();
                if (uri.length() != 0) {
                    setResultIconAndText(R.drawable.progress_small,
                            R.string.auth_testing_connection);
                    findViewById(R.id.buttonOK).setEnabled(false);  // avoid connect can be clicked if the test was previously passed
                    mConnChkRunnable = new ConnectionCheckerRunnable(uri, this);
                    mConnChkRunnable.setListener(this, mHandler);
                    mAuthThread = new Thread(mConnChkRunnable);
                    mAuthThread.start();
                } else {
                    findViewById(R.id.refreshButton).setVisibility(
                            View.INVISIBLE);
                    setResultIconAndText(0, 0);
                }
            }
        } else if (view.getId() == R.id.account_password) {
            ImageView iv = (ImageView) findViewById(R.id.viewPassword);
            if (hasFocus) {
                iv.setVisibility(View.VISIBLE);
            } else {
                TextView v = (TextView) findViewById(R.id.account_password);
                int input_type = InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                v.setInputType(input_type);
                iv.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setResultIconAndText(int drawable_id, int text_id) {
        ImageView iv = (ImageView) findViewById(R.id.action_indicator);
        TextView tv = (TextView) findViewById(R.id.status_text);

        if (drawable_id == 0 && text_id == 0) {
            iv.setVisibility(View.INVISIBLE);
            tv.setVisibility(View.INVISIBLE);
        } else {
            iv.setImageResource(drawable_id);
            tv.setText(text_id);
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.refreshButton) {
            onFocusChange(findViewById(R.id.host_URL), false);
        } else if (v.getId() == R.id.viewPassword) {
            TextView view = (TextView) findViewById(R.id.account_password);
            int input_type = InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            view.setInputType(input_type);
        }
    }
}
