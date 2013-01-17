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

package com.owncloud.android.ui.activity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.authenticator.AuthenticationRunnable;
import com.owncloud.android.authenticator.OnAuthenticationResultListener;
import com.owncloud.android.authenticator.OnConnectCheckListener;
import com.owncloud.android.authenticator.oauth2.OAuth2Context;
import com.owncloud.android.authenticator.oauth2.OAuth2GetCodeRunnable;
import com.owncloud.android.authenticator.oauth2.OnOAuth2GetCodeResultListener;
import com.owncloud.android.authenticator.oauth2.connection.ConnectorOAuth2;
import com.owncloud.android.authenticator.oauth2.services.OAuth2GetTokenService;
import com.owncloud.android.ui.dialog.SslValidatorDialog;
import com.owncloud.android.ui.dialog.SslValidatorDialog.OnSslValidatorListener;
import com.owncloud.android.utils.OwnCloudVersion;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.ConnectionCheckOperation;
import com.owncloud.android.operations.ExistenceCheckOperation;
import com.owncloud.android.operations.GetOAuth2AccessToken;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.owncloud.android.R;

import eu.alefzero.webdav.WebdavClient;

/**
 * This Activity is used to add an ownCloud account to the App
 * 
 * @author Bartek Przybylski
 * 
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements OnAuthenticationResultListener, OnConnectCheckListener, OnRemoteOperationListener, OnSslValidatorListener, 
        OnFocusChangeListener, OnClickListener, OnOAuth2GetCodeResultListener {

    private static final int DIALOG_LOGIN_PROGRESS = 0;
    private static final int DIALOG_SSL_VALIDATOR = 1;
    private static final int DIALOG_CERT_NOT_SAVED = 2;

    private static final String TAG = "AuthActivity";

    private Thread mAuthThread;
    private AuthenticationRunnable mAuthRunnable;
    private ConnectionCheckOperation mConnChkRunnable;
    private ExistenceCheckOperation mAuthChkOperation;
    private final Handler mHandler = new Handler();
    private String mBaseUrl;
    private OwnCloudVersion mDiscoveredVersion;
    
    private static final String STATUS_TEXT = "STATUS_TEXT";
    private static final String STATUS_ICON = "STATUS_ICON";
    private static final String STATUS_CORRECT = "STATUS_CORRECT";
    private static final String IS_SSL_CONN = "IS_SSL_CONN";
    private static final String OC_VERSION = "OC_VERSION";
    private int mStatusText, mStatusIcon;
    private boolean mStatusCorrect, mIsSslConn;
    private RemoteOperationResult mLastSslUntrustedServerResult;

    public static final String PARAM_ACCOUNTNAME = "param_Accountname";
    
    public static final String PARAM_USERNAME = "param_Username";
    public static final String PARAM_HOSTNAME = "param_Hostname";

    // oAuth2 variables.
    private static final int OAUTH2_LOGIN_PROGRESS = 3;
    private static final String OAUTH2_STATUS_TEXT = "OAUTH2_STATUS_TEXT";
    private static final String OAUTH2_STATUS_ICON = "OAUTH2_STATUS_ICON";
    private static final String OAUTH2_CODE_RESULT = "CODE_RESULT";
    private static final String OAUTH2_IS_CHECKED = "OAUTH2_IS_CHECKED";    
    private Thread mOAuth2GetCodeThread;
    private OAuth2GetCodeRunnable mOAuth2GetCodeRunnable;     
    private TokenReceiver tokenReceiver;
    private JSONObject codeResponseJson; 
    private int mOAuth2StatusText, mOAuth2StatusIcon;    
    
    public ConnectorOAuth2 connectorOAuth2;
    
    // Variables used to save the on the state the contents of all fields.
    private static final String HOST_URL_TEXT = "HOST_URL_TEXT";
    private static final String ACCOUNT_USERNAME = "ACCOUNT_USERNAME";
    private static final String ACCOUNT_PASSWORD = "ACCOUNT_PASSWORD";
    
    //private boolean mNewRedirectUriCaptured;
    private Uri mNewCapturedUriFromOAuth2Redirection;

    // END of oAuth2 variables.
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_setup);
        ImageView iv = (ImageView) findViewById(R.id.refreshButton);
        ImageView iv2 = (ImageView) findViewById(R.id.viewPassword);
        TextView tv = (TextView) findViewById(R.id.host_URL);
        TextView tv2 = (TextView) findViewById(R.id.account_password);
        EditText oauth2Url = (EditText)findViewById(R.id.oAuth_URL);
        oauth2Url.setText("OWNCLOUD AUTHORIZATION PROVIDER IN TEST");

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
            
            String ocVersion = savedInstanceState.getString(OC_VERSION, null);
            if (ocVersion != null)
                mDiscoveredVersion = new OwnCloudVersion(ocVersion);
            
            // Getting the state of oAuth2 components.
            mOAuth2StatusIcon = savedInstanceState.getInt(OAUTH2_STATUS_ICON);
            mOAuth2StatusText = savedInstanceState.getInt(OAUTH2_STATUS_TEXT);
                // We set this to true if the rotation happens when the user is validating oAuth2 user_code.
            changeViewByOAuth2Check(savedInstanceState.getBoolean(OAUTH2_IS_CHECKED));
                // We store a JSon object with all the data returned from oAuth2 server when we get user_code.
                // Is better than store variable by variable. We use String object to serialize from/to it.
            try {
                if (savedInstanceState.containsKey(OAUTH2_CODE_RESULT)) {
                    codeResponseJson = new JSONObject(savedInstanceState.getString(OAUTH2_CODE_RESULT));
                }
            } catch (JSONException e) {
                Log.e(TAG, "onCreate->JSONException: " + e.toString());
            }
            // END of getting the state of oAuth2 components.
            
            // Getting contents of each field.
            EditText hostUrl = (EditText)findViewById(R.id.host_URL);
            hostUrl.setText(savedInstanceState.getString(HOST_URL_TEXT), TextView.BufferType.EDITABLE);
            EditText accountUsername = (EditText)findViewById(R.id.account_username);
            accountUsername.setText(savedInstanceState.getString(ACCOUNT_USERNAME), TextView.BufferType.EDITABLE);
            EditText accountPassword = (EditText)findViewById(R.id.account_password);
            accountPassword.setText(savedInstanceState.getString(ACCOUNT_PASSWORD), TextView.BufferType.EDITABLE);
            // END of getting contents of each field

        } else {
            mStatusText = mStatusIcon = 0;
            mStatusCorrect = false;
            mIsSslConn = false;
            
            String accountName = getIntent().getExtras().getString(PARAM_ACCOUNTNAME);
            String tokenType = getIntent().getExtras().getString(AccountAuthenticator.KEY_AUTH_TOKEN_TYPE);
            if (AccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN.equals(tokenType)) {
                CheckBox oAuth2Check = (CheckBox) findViewById(R.id.oauth_onOff_check);
                oAuth2Check.setChecked(true);
                changeViewByOAuth2Check(true);
            } 
            
            if (accountName != null) {
                ((TextView) findViewById(R.id.account_username)).setText(accountName.substring(0, accountName.lastIndexOf('@')));
                tv.setText(accountName.substring(accountName.lastIndexOf('@') + 1));
            }
        }
        iv.setOnClickListener(this);
        iv2.setOnClickListener(this);
        tv.setOnFocusChangeListener(this);
        tv2.setOnFocusChangeListener(this);
        
        Button b = (Button) findViewById(R.id.account_register);
        if (b != null) {
            b.setText(String.format(getString(R.string.auth_register), getString(R.string.app_name)));
        }

        mNewCapturedUriFromOAuth2Redirection = null;
    }

    
    @Override
    protected void onNewIntent (Intent intent) {
        Uri data = intent.getData();
        //mNewRedirectUriCaptured = (data != null && data.toString().startsWith(OAuth2Context.MY_REDIRECT_URI));
        if (data != null && data.toString().startsWith(OAuth2Context.MY_REDIRECT_URI)) {
            mNewCapturedUriFromOAuth2Redirection = data;
        }
        Log.d(TAG, "onNewIntent()");
    
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATUS_ICON, mStatusIcon);
        outState.putInt(STATUS_TEXT, mStatusText);
        outState.putBoolean(STATUS_CORRECT, mStatusCorrect);
        if (mDiscoveredVersion != null) 
            outState.putString(OC_VERSION, mDiscoveredVersion.toString());
        
        // Saving the state of oAuth2 components.
        outState.putInt(OAUTH2_STATUS_ICON, mOAuth2StatusIcon);
        outState.putInt(OAUTH2_STATUS_TEXT, mOAuth2StatusText);
        CheckBox oAuth2Check = (CheckBox) findViewById(R.id.oauth_onOff_check);
        outState.putBoolean(OAUTH2_IS_CHECKED, oAuth2Check.isChecked());
        if (codeResponseJson != null){
            outState.putString(OAUTH2_CODE_RESULT, codeResponseJson.toString());
        }
        // END of saving the state of oAuth2 components.
        
        // Saving contents of each field.
        outState.putString(HOST_URL_TEXT,((TextView) findViewById(R.id.host_URL)).getText().toString().trim());
        outState.putString(ACCOUNT_USERNAME,((TextView) findViewById(R.id.account_username)).getText().toString().trim());
        outState.putString(ACCOUNT_PASSWORD,((TextView) findViewById(R.id.account_password)).getText().toString().trim());
        
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
        // oAuth2 dialog. We show here to the user the URL and user_code that the user must validate in a web browser.
        case OAUTH2_LOGIN_PROGRESS: {
            ProgressDialog working_dialog = new ProgressDialog(this);
            try {
                if (codeResponseJson != null && codeResponseJson.has(OAuth2GetCodeRunnable.CODE_VERIFICATION_URL)) {
                    working_dialog.setMessage(String.format(getString(R.string.oauth_code_validation_message), 
                            codeResponseJson.getString(OAuth2GetCodeRunnable.CODE_VERIFICATION_URL), 
                            codeResponseJson.getString(OAuth2GetCodeRunnable.CODE_USER_CODE)));
                } else {
                    working_dialog.setMessage(String.format("Getting authorization"));
                }
            } catch (JSONException e) {
                Log.e(TAG, "onCreateDialog->JSONException: " + e.toString());
            }
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(true);
            working_dialog
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Log.i(TAG, "Login canceled");
                    if (mOAuth2GetCodeThread != null) {
                        mOAuth2GetCodeThread.interrupt();
                        finish();
                    } 
                    if (tokenReceiver != null) {
                        unregisterReceiver(tokenReceiver);
                        tokenReceiver = null;
                        finish();
                    }
                }
            });
            dialog = working_dialog;
            break;
        }
        case DIALOG_SSL_VALIDATOR: {
            dialog = SslValidatorDialog.newInstance(this, mLastSslUntrustedServerResult, this);
            break;
        }
        case DIALOG_CERT_NOT_SAVED: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.ssl_validator_not_saved));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    };
                });
            dialog = builder.create();
            break;
        }
        default:
            Log.e(TAG, "Incorrect dialog called with id = " + id);
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
        case DIALOG_LOGIN_PROGRESS:
        case DIALOG_CERT_NOT_SAVED:
        case OAUTH2_LOGIN_PROGRESS:
            break;
        case DIALOG_SSL_VALIDATOR: {
            ((SslValidatorDialog)dialog).updateResult(mLastSslUntrustedServerResult);
            break;
        }
        default:
            Log.e(TAG, "Incorrect dialog called with id = " + id);
        }
    }
    
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() start");
        // (old oauth code) Registering token receiver. We must listening to the service that is pooling to the oAuth server for a token.
        if (tokenReceiver == null) {
            IntentFilter tokenFilter = new IntentFilter(OAuth2GetTokenService.TOKEN_RECEIVED_MESSAGE);                
            tokenReceiver = new TokenReceiver();
            this.registerReceiver(tokenReceiver,tokenFilter);
        }
        // (new oauth code)
        /*if (mNewRedirectUriCaptured) {
            mNewRedirectUriCaptured = false;*/
        if (mNewCapturedUriFromOAuth2Redirection != null) {
            getOAuth2AccessTokenFromCapturedRedirection();            
            
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() start");
        super.onPause();
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
                    AccountAuthenticator.KEY_OC_VERSION, mDiscoveredVersion.toString());
            
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
            try {
                dismissDialog(DIALOG_LOGIN_PROGRESS);
            } catch (IllegalArgumentException e) {
                // NOTHING TO DO ; can't find out what situation that leads to the exception in this code, but user logs signal that it happens
            }
            TextView tv = (TextView) findViewById(R.id.account_username);
            tv.setError(message + "        ");  // the extra spaces are a workaround for an ugly bug: 
                                                // 1. insert wrong credentials and connect
                                                // 2. put the focus on the user name field with using hardware controls (don't touch the screen); the error is shown UNDER the field
                                                // 3. touch the user name field; the software keyboard appears; the error popup is moved OVER the field and SHRINKED in width, losing the last word
                                                // Seen, at least, in Android 2.x devices
        }
    }
    public void onCancelClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
    
    public void onOkClick(View view) {
        String prefix = "";
        String url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString().trim();
        if (mIsSslConn) {
            prefix = "https://";
        } else {
            prefix = "http://";
        }
        if (url.toLowerCase().startsWith("http://")
                || url.toLowerCase().startsWith("https://")) {
            prefix = "";
        }
        CheckBox oAuth2Check = (CheckBox) findViewById(R.id.oauth_onOff_check);
        if (oAuth2Check != null && oAuth2Check.isChecked()) {
            startOauthorization();
            
        } else {
            continueConnection(prefix);
        }
    }
    
    private void startOauthorization() {
        // We start a thread to get an authorization code from the oAuth2 server.
        setOAuth2ResultIconAndText(R.drawable.progress_small, R.string.oauth_login_connection);
        mOAuth2GetCodeRunnable = new OAuth2GetCodeRunnable(OAuth2Context.OAUTH2_F_AUTHORIZATION_ENDPOINT_URL, this);
        //mOAuth2GetCodeRunnable = new OAuth2GetCodeRunnable(OAuth2Context.OAUTH2_G_DEVICE_GETCODE_URL, this);
        mOAuth2GetCodeRunnable.setListener(this, mHandler);
        mOAuth2GetCodeThread = new Thread(mOAuth2GetCodeRunnable);
        mOAuth2GetCodeThread.start();
    }

    public void onRegisterClick(View view) {
        Intent register = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_account_register)));
        setResult(RESULT_CANCELED);
        startActivity(register);
    }

    private void continueConnection(String prefix) {
        String url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString().trim();
        String username = ((TextView) findViewById(R.id.account_username))
                .getText().toString();
        String password = ((TextView) findViewById(R.id.account_password))
                .getText().toString();
        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        URL uri = null;
        mDiscoveredVersion = mConnChkRunnable.getDiscoveredVersion();
        String webdav_path = AccountUtils.getWebdavPath(mDiscoveredVersion, false);
        
        if (webdav_path == null) {
            onAuthenticationResult(false, getString(R.string.auth_bad_oc_version_title));
            return;
        }
        
        try {
            mBaseUrl = prefix + url;
            String url_str = prefix + url + webdav_path;
            uri = new URL(url_str);
        } catch (MalformedURLException e) {
            // should never happen
            onAuthenticationResult(false, getString(R.string.auth_incorrect_address_title));
            return;
        }

        showDialog(DIALOG_LOGIN_PROGRESS);
        mAuthRunnable = new AuthenticationRunnable(uri, username, password, this);
        mAuthRunnable.setOnAuthenticationResultListener(this, mHandler);
        mAuthThread = new Thread(mAuthRunnable);
        mAuthThread.start();
    }

    @Override
    public void onConnectionCheckResult(ResultType type) {
        mStatusText = mStatusIcon = 0;
        mStatusCorrect = false;
        String t_url = ((TextView) findViewById(R.id.host_URL)).getText()
                .toString().trim().toLowerCase();

        switch (type) {
        case OK_SSL:
            mIsSslConn = true;
            mStatusIcon = android.R.drawable.ic_secure;
            mStatusText = R.string.auth_secure_connection;
            mStatusCorrect = true;
            break;
        case OK_NO_SSL:
            mIsSslConn = false;
            mStatusCorrect = true;
            if (t_url.startsWith("http://") ) {
                mStatusText = R.string.auth_connection_established;
                mStatusIcon = R.drawable.ic_ok;
            } else {
                mStatusText = R.string.auth_nossl_plain_ok_title;
                mStatusIcon = android.R.drawable.ic_partial_secure;
            }
            break;
        case BAD_OC_VERSION:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_bad_oc_version_title;
            break;
        case WRONG_CONNECTION:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_wrong_connection_title;
            break;
        case TIMEOUT:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_timeout_title;
            break;
        case INCORRECT_ADDRESS:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_incorrect_address_title;
            break;
        case SSL_UNVERIFIED_SERVER:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_ssl_unverified_server_title;
            break;
        case SSL_INIT_ERROR:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_ssl_general_error_title;
            break;
        case HOST_NOT_AVAILABLE:
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_unknown_host_title;
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
            mStatusText = R.string.auth_unknown_error_title;
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

    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.host_URL) {
            if (!hasFocus) {
                TextView tv = ((TextView) findViewById(R.id.host_URL));
                String uri = tv.getText().toString().trim();
                if (uri.length() != 0) {
                    setResultIconAndText(R.drawable.progress_small,
                            R.string.auth_testing_connection);
                    //mConnChkRunnable = new ConnectionCheckerRunnable(uri, this);
                    mConnChkRunnable = new  ConnectionCheckOperation(uri, this);
                    //mConnChkRunnable.setListener(this, mHandler);
                    //mAuthThread = new Thread(mConnChkRunnable);
                    //mAuthThread.start();
            		WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(uri), this);
            		mDiscoveredVersion = null;
                    mAuthThread = mConnChkRunnable.execute(client, this, mHandler);
                } else {
                    findViewById(R.id.refreshButton).setVisibility(
                            View.INVISIBLE);
                    setResultIconAndText(0, 0);
                }
            } else {
                // avoids that the 'connect' button can be clicked if the test was previously passed
                findViewById(R.id.buttonOK).setEnabled(false); 
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
            EditText view = (EditText) findViewById(R.id.account_password);
            int selectionStart = view.getSelectionStart();
            int selectionEnd = view.getSelectionEnd();
            int input_type = view.getInputType();
            if ((input_type & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                input_type = InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD;
            } else {
                input_type = InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            }
            view.setInputType(input_type);
            view.setSelection(selectionStart, selectionEnd);
        }
    }
    
    @Override protected void onDestroy() {       
        // We must stop the service thats it's pooling to oAuth2 server for a token.
        Intent tokenService = new Intent(this, OAuth2GetTokenService.class);
        stopService(tokenService);
        
        // We stop listening the result of the pooling service.
        if (tokenReceiver != null) {
            unregisterReceiver(tokenReceiver);
            tokenReceiver = null;
            finish();
        }

        super.onDestroy();
    }    
    
    // Controlling the oAuth2 checkbox on the activity: hide and show widgets.
    public void onOff_check_Click(View view) {
        CheckBox oAuth2Check = (CheckBox)view;      
        changeViewByOAuth2Check(oAuth2Check.isChecked());

    }
    
    public void changeViewByOAuth2Check(Boolean checked) {
        
        EditText oAuth2Url = (EditText) findViewById(R.id.oAuth_URL);
        EditText accountUsername = (EditText) findViewById(R.id.account_username);
        EditText accountPassword = (EditText) findViewById(R.id.account_password);
        ImageView viewPassword = (ImageView) findViewById(R.id.viewPassword); 
        ImageView auth2ActionIndicator = (ImageView) findViewById(R.id.auth2_action_indicator); 
        TextView oauth2StatusText = (TextView) findViewById(R.id.oauth2_status_text);         

        if (checked) {
            oAuth2Url.setVisibility(View.VISIBLE);
            accountUsername.setVisibility(View.GONE);
            accountPassword.setVisibility(View.GONE);
            viewPassword.setVisibility(View.GONE);
            auth2ActionIndicator.setVisibility(View.INVISIBLE);
            oauth2StatusText.setVisibility(View.INVISIBLE);
        } else {
            oAuth2Url.setVisibility(View.GONE);
            accountUsername.setVisibility(View.VISIBLE);
            accountPassword.setVisibility(View.VISIBLE);
            viewPassword.setVisibility(View.INVISIBLE);
            auth2ActionIndicator.setVisibility(View.GONE);
            oauth2StatusText.setVisibility(View.GONE);
        }     

    }    
    
    // Controlling the oAuth2 result of server connection.
    private void setOAuth2ResultIconAndText(int drawable_id, int text_id) {
        ImageView iv = (ImageView) findViewById(R.id.auth2_action_indicator);
        TextView tv = (TextView) findViewById(R.id.oauth2_status_text);

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
    
    // Results from the first call to oAuth2 server : getting the user_code and verification_url.
    @Override
    public void onOAuth2GetCodeResult(ResultOAuthType type, JSONObject responseJson) {
        if ((type == ResultOAuthType.OK_SSL)||(type == ResultOAuthType.OK_NO_SSL)) {
            codeResponseJson = responseJson;
            if (codeResponseJson != null) {
                getOAuth2AccessTokenFromJsonResponse();
            }  // else - nothing to do here - wait for callback !!!
        
        } else if (type == ResultOAuthType.HOST_NOT_AVAILABLE) {
            setOAuth2ResultIconAndText(R.drawable.common_error, R.string.oauth_connection_url_unavailable);
        }
    }

    // If the results of getting the user_code and verification_url are OK, we get the received data and we start
    // the polling service to oAuth2 server to get a valid token.
    private void getOAuth2AccessTokenFromJsonResponse() {
        String deviceCode = null;
        String verificationUrl = null;
        String userCode = null;
        int expiresIn = -1;
        int interval = -1;

        Log.d(TAG, "ResponseOAuth2->" + codeResponseJson.toString());

        try {
            // We get data that we must show to the user or we will use internally.
            verificationUrl = codeResponseJson.getString(OAuth2GetCodeRunnable.CODE_VERIFICATION_URL);
            userCode = codeResponseJson.getString(OAuth2GetCodeRunnable.CODE_USER_CODE);
            expiresIn = codeResponseJson.getInt(OAuth2GetCodeRunnable.CODE_EXPIRES_IN);                

            // And we get data that we must use to get a token.
            deviceCode = codeResponseJson.getString(OAuth2GetCodeRunnable.CODE_DEVICE_CODE);
            interval = codeResponseJson.getInt(OAuth2GetCodeRunnable.CODE_INTERVAL);

        } catch (JSONException e) {
            Log.e(TAG, "Exception accesing data in Json object" + e.toString());
        }

        // Updating status widget to OK.
        setOAuth2ResultIconAndText(R.drawable.ic_ok, R.string.auth_connection_established);
        
        // Showing the dialog with instructions for the user.
        showDialog(OAUTH2_LOGIN_PROGRESS);

        // Loggin all the data.
        Log.d(TAG, "verificationUrl->" + verificationUrl);
        Log.d(TAG, "userCode->" + userCode);
        Log.d(TAG, "deviceCode->" + deviceCode);
        Log.d(TAG, "expiresIn->" + expiresIn);
        Log.d(TAG, "interval->" + interval);

        // Starting the pooling service.
        try {
            Intent tokenService = new Intent(this, OAuth2GetTokenService.class);
            tokenService.putExtra(OAuth2GetTokenService.TOKEN_URI, OAuth2Context.OAUTH2_G_DEVICE_GETTOKEN_URL);
            tokenService.putExtra(OAuth2GetTokenService.TOKEN_DEVICE_CODE, deviceCode);
            tokenService.putExtra(OAuth2GetTokenService.TOKEN_INTERVAL, interval);

            startService(tokenService);
        }
        catch (Exception e) {
            Log.e(TAG, "tokenService creation problem :", e);
        }
        
    }   
    
    private void getOAuth2AccessTokenFromCapturedRedirection() {
        Map<String, String> responseValues = new HashMap<String, String>();
        //String queryParameters = getIntent().getData().getQuery();
        String queryParameters = mNewCapturedUriFromOAuth2Redirection.getQuery();
        mNewCapturedUriFromOAuth2Redirection = null;
        
        Log.v(TAG, "Queryparameters (Code) = " + queryParameters);

        String[] pairs = queryParameters.split("&");
        Log.v(TAG, "Pairs (Code) = " + pairs.toString());

        int i = 0;
        String key = "";
        String value = "";

        StringBuilder sb = new StringBuilder();

        while (pairs.length > i) {
            int j = 0;
            String[] part = pairs[i].split("=");

            while (part.length > j) {
                String p = part[j];
                if (j == 0) {
                    key = p;
                    sb.append(key + " = ");
                } else if (j == 1) {
                    value = p;
                    responseValues.put(key, value);
                    sb.append(value + "\n");
                }

                Log.v(TAG, "[" + i + "," + j + "] = " + p);
                j++;
            }
            i++;
        }
        
        
        // Updating status widget to OK.
        setOAuth2ResultIconAndText(R.drawable.ic_ok, R.string.auth_connection_established);
        
        // Showing the dialog with instructions for the user.
        showDialog(OAUTH2_LOGIN_PROGRESS);

        // 
        RemoteOperation operation = new GetOAuth2AccessToken(responseValues);
        WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(OAuth2Context.OAUTH2_F_TOKEN_ENDPOINT_URL), getApplicationContext());
        operation.execute(client, this, mHandler);
    }

    

    // We get data from the oAuth2 token service with this broadcast receiver.
    private class TokenReceiver extends BroadcastReceiver {
        /**
         * The token is received.
         *  @author
         * {@link BroadcastReceiver} to enable oAuth2 token receiving.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> tokenResponse = (HashMap<String, String>)intent.getExtras().get(OAuth2GetTokenService.TOKEN_RECEIVED_DATA);
            Log.d(TAG, "TokenReceiver->" + tokenResponse.get(OAuth2GetTokenService.TOKEN_ACCESS_TOKEN));
            dismissDialog(OAUTH2_LOGIN_PROGRESS);

        }
    }

	@Override
	public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
		if (operation instanceof ConnectionCheckOperation) {
		    
	        mStatusText = mStatusIcon = 0;
	        mStatusCorrect = false;
	        String t_url = ((TextView) findViewById(R.id.host_URL)).getText()
	                .toString().trim().toLowerCase();
	        
	        switch (result.getCode()) {
	        case OK_SSL:
	            mIsSslConn = true;
	            mStatusIcon = android.R.drawable.ic_secure;
	            mStatusText = R.string.auth_secure_connection;
	            mStatusCorrect = true;
	            break;
	            
	        case OK_NO_SSL:
	        case OK:
	            mIsSslConn = false;
	            mStatusCorrect = true;
	            if (t_url.startsWith("http://") ) {
	                mStatusText = R.string.auth_connection_established;
	                mStatusIcon = R.drawable.ic_ok;
	            } else {
	                mStatusText = R.string.auth_nossl_plain_ok_title;
	                mStatusIcon = android.R.drawable.ic_partial_secure;
	            }
	            break;
	        
	            
	        case BAD_OC_VERSION:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_bad_oc_version_title;
	            break;
	        case WRONG_CONNECTION:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_wrong_connection_title;
	            break;
	        case TIMEOUT:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_timeout_title;
	            break;
	        case INCORRECT_ADDRESS:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_incorrect_address_title;
	            break;
	            
            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mStatusIcon = R.drawable.common_error;
                mStatusText = R.string.auth_ssl_unverified_server_title;
                mLastSslUntrustedServerResult = result;
                showDialog(DIALOG_SSL_VALIDATOR); 
                break;
	            
            case SSL_ERROR:
                mStatusIcon = R.drawable.common_error;
                mStatusText = R.string.auth_ssl_general_error_title;
                break;
                
	        case HOST_NOT_AVAILABLE:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_unknown_host_title;
	            break;
	        case NO_NETWORK_CONNECTION:
	            mStatusIcon = R.drawable.no_network;
	            mStatusText = R.string.auth_no_net_conn_title;
	            break;
	        case INSTANCE_NOT_CONFIGURED:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_not_configured_title;
	            break;
	        case FILE_NOT_FOUND:
	            mStatusIcon = R.drawable.common_error;
	            mStatusText = R.string.auth_incorrect_path_title;
	            break;
            case UNHANDLED_HTTP_CODE:
            case UNKNOWN_ERROR:
                mStatusIcon = R.drawable.common_error;
                mStatusText = R.string.auth_unknown_error_title;
                break;
	        default:
	            Log.e(TAG, "Incorrect connection checker result type: " + result.getHttpCode());
	        }
	        setResultIconAndText(mStatusIcon, mStatusText);
	        if (!mStatusCorrect)
	            findViewById(R.id.refreshButton).setVisibility(View.VISIBLE);
	        else
	            findViewById(R.id.refreshButton).setVisibility(View.INVISIBLE);
	        findViewById(R.id.buttonOK).setEnabled(mStatusCorrect);
	        
		} else if (operation instanceof GetOAuth2AccessToken) {

            try {
                dismissDialog(OAUTH2_LOGIN_PROGRESS);
            } catch (IllegalArgumentException e) {
                // NOTHING TO DO ; can't find out what situation that leads to the exception in this code, but user logs signal that it happens
            }

		    if (result.isSuccess()) {
		        
		        /// time to test the retrieved access token on the ownCloud server
		        String url = ((TextView) findViewById(R.id.host_URL)).getText()
		                .toString().trim();
		        if (url.endsWith("/"))
		            url = url.substring(0, url.length() - 1);

		        Uri uri = null;
		        /*String webdav_path = AccountUtils.getWebdavPath(mDiscoveredVersion);
		        
		        if (webdav_path == null) {
		            onAuthenticationResult(false, getString(R.string.auth_bad_oc_version_title));
		            return;
		        }*/
		        
		        String prefix = "";
		        if (mIsSslConn) {
		            prefix = "https://";
		        } else {
		            prefix = "http://";
		        }
		        if (url.toLowerCase().startsWith("http://")
		                || url.toLowerCase().startsWith("https://")) {
		            prefix = "";
		        }
		        
		        try {
		            mBaseUrl = prefix + url;
		            //String url_str = prefix + url + webdav_path;
		            String url_str = prefix + url + "/remote.php/odav";
		            uri = Uri.parse(url_str);
		            
		        } catch (Exception e) {
		            // should never happen
		            onAuthenticationResult(false, getString(R.string.auth_incorrect_address_title));
		            return;
		        }

		        showDialog(DIALOG_LOGIN_PROGRESS);
                String accessToken = ((GetOAuth2AccessToken)operation).getResultTokenMap().get(OAuth2Context.KEY_ACCESS_TOKEN);
                Log.d(TAG, "Got ACCESS TOKEN: " + accessToken);
		        mAuthChkOperation = new ExistenceCheckOperation("", this, accessToken);
		        WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(uri, getApplicationContext());
		        mAuthChkOperation.execute(client, this, mHandler);
		        
                
            } else {
                TextView tv = (TextView) findViewById(R.id.oAuth_URL);
                tv.setError("A valid authorization could not be obtained");

            }
		        
		} else if (operation instanceof ExistenceCheckOperation)  {
		        
		    try {
		        dismissDialog(DIALOG_LOGIN_PROGRESS);
		    } catch (IllegalArgumentException e) {
		        // NOTHING TO DO ; can't find out what situation that leads to the exception in this code, but user logs signal that it happens
		    }
		    
		    if (result.isSuccess()) {
                TextView tv = (TextView) findViewById(R.id.oAuth_URL);
		        Log.d(TAG, "Checked access - time to save the account");
		        
		        Uri uri = Uri.parse(mBaseUrl);
		        String username = "OAuth_user" + (new java.util.Random(System.currentTimeMillis())).nextLong(); 
		        String accountName = username + "@" + uri.getHost();
		        if (uri.getPort() >= 0) {
		            accountName += ":" + uri.getPort();
		        }
		        // TODO - check that accountName does not exist
		        Account account = new Account(accountName, AccountAuthenticator.ACCOUNT_TYPE);
		        AccountManager accManager = AccountManager.get(this);
		        accManager.addAccountExplicitly(account, "", null);  // with our implementation, the password is never input in the app

		        // Add this account as default in the preferences, if there is none
		        Account defaultAccount = AccountUtils.getCurrentOwnCloudAccount(this);
		        if (defaultAccount == null) {
		            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		            editor.putString("select_oc_account", accountName);
	                editor.commit();
		        }

		        /// account data to save by the AccountManager
		        final Intent intent = new Intent();
		        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.ACCOUNT_TYPE);
		        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
		        intent.putExtra(AccountManager.KEY_USERDATA, username);

                accManager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, ((ExistenceCheckOperation) operation).getAccessToken());
                
		        accManager.setUserData(account, AccountAuthenticator.KEY_OC_VERSION, mConnChkRunnable.getDiscoveredVersion().toString());
		        accManager.setUserData(account, AccountAuthenticator.KEY_OC_BASE_URL, mBaseUrl);
		        accManager.setUserData(account, AccountAuthenticator.KEY_SUPPORTS_OAUTH2, "TRUE");

		        setAccountAuthenticatorResult(intent.getExtras());
		        setResult(RESULT_OK, intent);
	                
		        /// enforce the first account synchronization
		        Bundle bundle = new Bundle();
		        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		        ContentResolver.requestSync(account, "org.owncloud", bundle);

		        finish();
	                
		    } else {      
		        TextView tv = (TextView) findViewById(R.id.oAuth_URL);
		        tv.setError(result.getLogMessage());
                Log.d(TAG, "Access failed: " + result.getLogMessage());
		    }
		}
	}

	
    public void onSavedCertificate() {
        mAuthThread = mConnChkRunnable.retry(this, mHandler);                
    }

    @Override
    public void onFailedSavingCertificate() {
        showDialog(DIALOG_CERT_NOT_SAVED);
    }

}
