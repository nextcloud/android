/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013  ownCloud Inc.
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

import java.util.HashMap;
import java.util.Map;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.authenticator.oauth2.OAuth2Context;
import com.owncloud.android.ui.dialog.SslValidatorDialog;
import com.owncloud.android.ui.dialog.SslValidatorDialog.OnSslValidatorListener;
import com.owncloud.android.utils.OwnCloudVersion;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OwnCloudServerCheckOperation;
import com.owncloud.android.operations.ExistenceCheckOperation;
import com.owncloud.android.operations.OAuth2GetAccessToken;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
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
 * @author David A. Velasco
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements  OnRemoteOperationListener, OnSslValidatorListener, OnFocusChangeListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_USER_NAME = "USER_NAME";
    public static final String EXTRA_HOST_NAME = "HOST_NAME";

    private static final String KEY_HOST_URL_TEXT = "HOST_URL_TEXT";
    private static final String KEY_OC_VERSION = "OC_VERSION";
    private static final String KEY_STATUS_TEXT = "STATUS_TEXT";
    private static final String KEY_STATUS_ICON = "STATUS_ICON";
    private static final String KEY_STATUS_CORRECT = "STATUS_CORRECT";
    private static final String KEY_IS_SSL_CONN = "IS_SSL_CONN";
    private static final String KEY_OAUTH2_STATUS_TEXT = "OAUTH2_STATUS_TEXT";
    private static final String KEY_OAUTH2_STATUS_ICON = "OAUTH2_STATUS_ICON";

    private static final int DIALOG_LOGIN_PROGRESS = 0;
    private static final int DIALOG_SSL_VALIDATOR = 1;
    private static final int DIALOG_CERT_NOT_SAVED = 2;
    private static final int DIALOG_OAUTH2_LOGIN_PROGRESS = 3;

    
    private String mHostBaseUrl;
    private OwnCloudVersion mDiscoveredVersion;
    
    private int mStatusText, mStatusIcon;
    private boolean mStatusCorrect, mIsSslConn;
    private int mOAuth2StatusText, mOAuth2StatusIcon;    
    
    private final Handler mHandler = new Handler();
    private Thread mOperationThread;
    private OwnCloudServerCheckOperation mOcServerChkOperation;
    private ExistenceCheckOperation mAuthCheckOperation;
    private RemoteOperationResult mLastSslUntrustedServerResult;

    //private Thread mOAuth2GetCodeThread;
    //private OAuth2GetAuthorizationToken mOAuth2GetCodeRunnable;     
    //private TokenReceiver tokenReceiver;
    //private JSONObject mCodeResponseJson; 
    private Uri mNewCapturedUriFromOAuth2Redirection;
    
    private AccountManager mAccountMgr;
    
    private ImageView mRefreshButton;
    private ImageView mViewPasswordButton;
    private EditText mHostUrlInput;
    private EditText mUsernameInput;
    private EditText mPasswordInput;
    private CheckBox mOAuth2Check;
    private String mOAuthAccessToken;
    private View mOkButton;
    
    private TextView mOAuthAuthEndpointText;
    private TextView mOAuthTokenEndpointText;
    
    
    /**
     * {@inheritDoc}
     * 
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
        /// set view and get references to view elements
        setContentView(R.layout.account_setup);
        mRefreshButton = (ImageView) findViewById(R.id.refreshButton);
        mViewPasswordButton = (ImageView) findViewById(R.id.viewPasswordButton);
        mHostUrlInput = (EditText) findViewById(R.id.hostUrlInput);
        mUsernameInput = (EditText) findViewById(R.id.account_username);
        mPasswordInput = (EditText) findViewById(R.id.account_password);
        mOAuthAuthEndpointText = (TextView)findViewById(R.id.oAuthEntryPoint_1);
        mOAuthTokenEndpointText = (TextView)findViewById(R.id.oAuthEntryPoint_2);
        mOAuth2Check = (CheckBox) findViewById(R.id.oauth_onOff_check);
        mOkButton = findViewById(R.id.buttonOK);

        /// complete label for 'register account' button
        Button b = (Button) findViewById(R.id.account_register);
        if (b != null) {
            b.setText(String.format(getString(R.string.auth_register), getString(R.string.app_name)));
        }

        /// bind view elements to listeners
        mHostUrlInput.setOnFocusChangeListener(this);
        mPasswordInput.setOnFocusChangeListener(this);
        
        /// initialization
        mAccountMgr = AccountManager.get(this);
        mNewCapturedUriFromOAuth2Redirection = null;    // TODO save?

        if (savedInstanceState == null) {
            /// connection state and info
            mStatusText = mStatusIcon = 0;
            mStatusCorrect = false;
            mIsSslConn = false;
            
            /// retrieve extras from intent
            String tokenType = getIntent().getExtras().getString(AccountAuthenticator.KEY_AUTH_TOKEN_TYPE);
            boolean oAuthRequired = AccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN.equals(tokenType);
            mOAuth2Check.setChecked(oAuthRequired);
            changeViewByOAuth2Check(oAuthRequired);
            
            Account account = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
            if (account != null) {
                String ocVersion = mAccountMgr.getUserData(account, AccountAuthenticator.KEY_OC_VERSION);
                if (ocVersion != null) {
                    mDiscoveredVersion = new OwnCloudVersion(ocVersion);
                }
                mHostBaseUrl = mAccountMgr.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
                mHostUrlInput.setText(mHostBaseUrl);
                String userName = account.name.substring(0, account.name.lastIndexOf('@'));
                mUsernameInput.setText(userName);
            }

        } else {
            loadSavedInstanceState(savedInstanceState);
        }
        
        mPasswordInput.setText("");     // clean password to avoid social hacking (disadvantage: password in removed if the device is turned aside)
    }


    /**
     * Saves relevant state before {@link #onPause()}
     * 
     * Do NOT save {@link #mNewCapturedUriFromOAuth2Redirection}; it keeps a temporal flag, intended to defer the 
     * processing of the redirection caught in {@link #onNewIntent(Intent)} until {@link #onResume()} 
     * 
     * See {@link #loadSavedInstanceState(Bundle)}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        /// connection state and info
        outState.putInt(KEY_STATUS_TEXT, mStatusText);
        outState.putInt(KEY_STATUS_ICON, mStatusIcon);
        outState.putBoolean(KEY_STATUS_CORRECT, mStatusCorrect);
        outState.putBoolean(KEY_IS_SSL_CONN, mIsSslConn);

        /// server data
        if (mDiscoveredVersion != null) 
            outState.putString(KEY_OC_VERSION, mDiscoveredVersion.toString());
        outState.putString(KEY_HOST_URL_TEXT, mHostBaseUrl);
        
        // Saving the state of oAuth2 components.
        outState.putInt(KEY_OAUTH2_STATUS_ICON, mOAuth2StatusIcon);
        outState.putInt(KEY_OAUTH2_STATUS_TEXT, mOAuth2StatusText);
        
        /* Leave old OAuth flow
        if (codeResponseJson != null){
            outState.putString(KEY_OAUTH2_CODE_RESULT, codeResponseJson.toString());
        }
        */
    }


    /**
     * Loads saved state
     * 
     * See {@link #onSaveInstanceState(Bundle)}.
     * 
     * @param savedInstanceState    Saved state, as received in {@link #onCreate(Bundle)}.
     */
    private void loadSavedInstanceState(Bundle savedInstanceState) {
        /// connection state and info
        mStatusCorrect = savedInstanceState.getBoolean(KEY_STATUS_CORRECT);
        mIsSslConn = savedInstanceState.getBoolean(KEY_IS_SSL_CONN);
        mStatusText = savedInstanceState.getInt(KEY_STATUS_TEXT);
        mStatusIcon = savedInstanceState.getInt(KEY_STATUS_ICON);
        updateOcServerCheckIconAndText();
        
        /// UI settings depending upon connection
        mOkButton.setEnabled(mStatusCorrect);   // TODO really necessary?
        if (!mStatusCorrect)
            mRefreshButton.setVisibility(View.VISIBLE); // seems that setting visibility is necessary
        else
            mRefreshButton.setVisibility(View.INVISIBLE);
        
        /// server data
        String ocVersion = savedInstanceState.getString(KEY_OC_VERSION);
        if (ocVersion != null)
            mDiscoveredVersion = new OwnCloudVersion(ocVersion);
        mHostBaseUrl = savedInstanceState.getString(KEY_HOST_URL_TEXT);
        
        // state of oAuth2 components
        mOAuth2StatusIcon = savedInstanceState.getInt(KEY_OAUTH2_STATUS_ICON);
        mOAuth2StatusText = savedInstanceState.getInt(KEY_OAUTH2_STATUS_TEXT);
        
        /* Leave old OAuth flow
        // We store a JSon object with all the data returned from oAuth2 server when we get user_code.
        // Is better than store variable by variable. We use String object to serialize from/to it.
           try {
            if (savedInstanceState.containsKey(KEY_OAUTH2_CODE_RESULT)) {
                codeResponseJson = new JSONObject(savedInstanceState.getString(KEY_OAUTH2_CODE_RESULT));
            }
        } catch (JSONException e) {
            Log.e(TAG, "onCreate->JSONException: " + e.toString());
        }*/
        // END of getting the state of oAuth2 components.
        
    }

    
    /**
     * The redirection triggered by the OAuth authentication server as response to the GET AUTHORIZATION request
     * is caught here.
     * 
     * To make this possible, this activity needs to be qualified with android:launchMode = "singleTask" in the
     * AndroidManifest.xml file.
     */
    @Override
    protected void onNewIntent (Intent intent) {
        Log.d(TAG, "onNewIntent()");
        Uri data = intent.getData();
        if (data != null && data.toString().startsWith(OAuth2Context.MY_REDIRECT_URI)) {
            mNewCapturedUriFromOAuth2Redirection = data;
        }
    }

    
    /**
     * The redirection triggered by the OAuth authentication server as response to the GET AUTHORIZATION, and 
     * deferred in {@link #onNewIntent(Intent)}, is processed here.
     */
    @Override
    protected void onResume() {
        super.onResume();
        changeViewByOAuth2Check(mOAuth2Check.isChecked());  
            // the state of mOAuth2Check is automatically recovered between configuration changes, but not before onCreate() finishes
        
        /* LEAVE OLD OAUTH FLOW ; 
        // (old oauth code) Registering token receiver. We must listening to the service that is pooling to the oAuth server for a token.
        if (tokenReceiver == null) {
            IntentFilter tokenFilter = new IntentFilter(OAuth2GetTokenService.TOKEN_RECEIVED_MESSAGE);                
            tokenReceiver = new TokenReceiver();
            this.registerReceiver(tokenReceiver,tokenFilter);
        } */
        // (new oauth code)
        if (mNewCapturedUriFromOAuth2Redirection != null) {
            getOAuth2AccessTokenFromCapturedRedirection();            
        }
    }
    
    
    @Override protected void onDestroy() {       
        super.onDestroy();

        /* LEAVE OLD OAUTH FLOW
        // We must stop the service thats it's pooling to oAuth2 server for a token.
        Intent tokenService = new Intent(this, OAuth2GetTokenService.class);
        stopService(tokenService);
        
        // We stop listening the result of the pooling service.
        if (tokenReceiver != null) {
            unregisterReceiver(tokenReceiver);
            tokenReceiver = null;
        }*/

    }    
    
    
    /**
     * Parses the redirection with the response to the GET AUTHORIZATION request to the 
     * oAuth server and requests for the access token (GET ACCESS TOKEN)
     */
    private void getOAuth2AccessTokenFromCapturedRedirection() {
        /// Parse data from OAuth redirection
        Map<String, String> responseValues = new HashMap<String, String>();
        String queryParameters = mNewCapturedUriFromOAuth2Redirection.getQuery();
        mNewCapturedUriFromOAuth2Redirection = null;
        String[] pairs = queryParameters.split("&");
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
        
        /// Updating status widget to OK.
        updateOAuth2IconAndText(R.drawable.ic_ok, R.string.auth_connection_established);
        
        /// Showing the dialog with instructions for the user.
        showDialog(DIALOG_OAUTH2_LOGIN_PROGRESS);

        /// GET ACCESS TOKEN to the oAuth server 
        RemoteOperation operation = new OAuth2GetAccessToken(responseValues);
        WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(getString(R.string.oauth_url_endpoint_access)), getApplicationContext());
        operation.execute(client, this, mHandler);
    }
    

    
    /**
     * Handles the change of focus on the text inputs for the server URL and the password
     */
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.hostUrlInput) {
            onUrlInputFocusChanged((TextView) view, hasFocus);
            
        } else if (view.getId() == R.id.account_password) {
            onPasswordFocusChanged((TextView) view, hasFocus);
        }
    }
    

    /**
     * Handles changes in focus on the text input for the server URL.
     * 
     * IMPORTANT ENTRY POINT 2: When (!hasFocus), user wrote the server URL and changed to 
     * other field. The operation to check the existence of the server in the entered URL is
     * started. 
     * 
     * When hasFocus:    user 'comes back' to write again the server URL.
     * 
     * @param hostInput     TextView with the URL input field receiving the change of focus.
     * @param hasFocus      'True' if focus is received, 'false' if is lost
     */
    private void onUrlInputFocusChanged(TextView hostInput, boolean hasFocus) {
        if (!hasFocus) {
            String uri = hostInput.getText().toString().trim();
            if (uri.length() != 0) {
                mStatusText = R.string.auth_testing_connection;
                mStatusIcon = R.drawable.progress_small;
                updateOcServerCheckIconAndText();
                /** TODO cancel previous connection check if the user tries to ammend a wrong URL  
                if(mConnChkOperation != null) {
                    mConnChkOperation.cancel();
                } */
                mOcServerChkOperation = new  OwnCloudServerCheckOperation(uri, this);
                WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(uri), this);
                mHostBaseUrl = "";
                mDiscoveredVersion = null;
                mOperationThread = mOcServerChkOperation.execute(client, this, mHandler);
            } else {
                mRefreshButton.setVisibility(View.INVISIBLE);
                mStatusText = 0;
                mStatusIcon = 0;
                updateOcServerCheckIconAndText();
            }
        } else {
            // avoids that the 'connect' button can be clicked if the test was previously passed
            mOkButton.setEnabled(false); 
        }
    }


    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     * 
     * When (hasFocus), the button to toggle password visibility is shown.
     * 
     * When (!hasFocus), the button is made invisible and the password is hidden.
     * 
     * @param passwordInput    TextView with the password input field receiving the change of focus.
     * @param hasFocus          'True' if focus is received, 'false' if is lost
     */
    private void onPasswordFocusChanged(TextView passwordInput, boolean hasFocus) {
        if (hasFocus) {
            mViewPasswordButton.setVisibility(View.VISIBLE);
        } else {
            int input_type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
            passwordInput.setInputType(input_type);
            mViewPasswordButton.setVisibility(View.INVISIBLE);
        }
    }


    
    /**
     * Cancels the authenticator activity
     * 
     * IMPORTANT ENTRY POINT 3: Never underestimate the importance of cancellation
     * 
     * This method is bound in the layout/acceoun_setup.xml resource file.
     * 
     * @param view      Cancel button
     */
    public void onCancelClick(View view) {
        setResult(RESULT_CANCELED);     // TODO review how is this related to AccountAuthenticator
        finish();
    }
    
    
    
    /**
     * Checks the credentials of the user in the root of the ownCloud server
     * before creating a new local account.
     * 
     * For basic authorization, a check of existence of the root folder is
     * performed.
     * 
     * For OAuth, starts the flow to get an access token; the credentials test 
     * is postponed until it is available.
     * 
     * IMPORTANT ENTRY POINT 4
     * 
     * @param view      OK button
     */
    public void onOkClick(View view) {
        // this check should be unnecessary
        if (mDiscoveredVersion == null || !mDiscoveredVersion.isVersionValid()  || mHostBaseUrl == null || mHostBaseUrl.length() == 0) {
            mStatusIcon = R.drawable.common_error;
            mStatusText = R.string.auth_wtf_reenter_URL;
            updateOcServerCheckIconAndText();
            mOkButton.setEnabled(false);
            Log.wtf(TAG,  "The user was allowed to click 'connect' to an unchecked server!!");
            return;
        }
        
        if (mOAuth2Check.isChecked()) {
            startOauthorization();
            
        } else {
            checkBasicAuthorization();
        }
    }
    
    
    /**
     * Tests the credentials entered by the user performing a check of existence on 
     * the root folder of the ownCloud server.
     */
    private void checkBasicAuthorization() {
        /// get the path to the root folder through WebDAV from the version server
        String webdav_path = AccountUtils.getWebdavPath(mDiscoveredVersion, false);
        
        /// get basic credentials entered by user
        String username = mUsernameInput.getText().toString();
        String password = mPasswordInput.getText().toString();
        
        /// be gentle with the user
        showDialog(DIALOG_LOGIN_PROGRESS);
        
        /// test credentials accessing the root folder
        mAuthCheckOperation = new  ExistenceCheckOperation("", this, false);
        WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(mHostBaseUrl + webdav_path), this);
        client.setBasicCredentials(username, password);
        mOperationThread = mAuthCheckOperation.execute(client, this, mHandler);
    }


    /**
     * Starts the OAuth 'grant type' flow to get an access token, with 
     * a GET AUTHORIZATION request to the BUILT-IN authorization server. 
     */
    private void startOauthorization() {
        // be gentle with the user
        updateOAuth2IconAndText(R.drawable.progress_small, R.string.oauth_login_connection);
        
        // GET AUTHORIZATION request
        /*
        mOAuth2GetCodeRunnable = new OAuth2GetAuthorizationToken(, this);
        mOAuth2GetCodeRunnable.setListener(this, mHandler);
        mOAuth2GetCodeThread = new Thread(mOAuth2GetCodeRunnable);
        mOAuth2GetCodeThread.start();
        */
        
        //if (mGrantType.equals(OAuth2Context.OAUTH2_AUTH_CODE_GRANT_TYPE)) {
        Uri uri = Uri.parse(getString(R.string.oauth_url_endpoint_auth));
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(OAuth2Context.CODE_RESPONSE_TYPE, OAuth2Context.OAUTH2_CODE_RESPONSE_TYPE);
        uriBuilder.appendQueryParameter(OAuth2Context.CODE_REDIRECT_URI, OAuth2Context.MY_REDIRECT_URI);   
        uriBuilder.appendQueryParameter(OAuth2Context.CODE_CLIENT_ID, OAuth2Context.OAUTH2_F_CLIENT_ID);
        uriBuilder.appendQueryParameter(OAuth2Context.CODE_SCOPE, OAuth2Context.OAUTH2_F_SCOPE);
        //uriBuilder.appendQueryParameter(OAuth2Context.CODE_STATE, whateverwewant);
        uri = uriBuilder.build();
        Log.d(TAG, "Starting browser to view " + uri.toString());
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
        //}
    }

    
    /**
     * Callback method invoked when a RemoteOperation executed by this Activity finishes.
     * 
     * Dispatches the operation flow to the right method.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {

        if (operation instanceof OwnCloudServerCheckOperation) {
            onOcServerCheckFinish((OwnCloudServerCheckOperation) operation, result);
            
        } else if (operation instanceof OAuth2GetAccessToken) {
            onGetOAuthAccessTokenFinish((OAuth2GetAccessToken)operation, result);
                
        } else if (operation instanceof ExistenceCheckOperation)  {
            onAuthorizationCheckFinish((ExistenceCheckOperation)operation, result);
                
        }
    }
    

    /**
     * Processes the result of the server check performed when the user finishes the enter of the
     * server URL.
     * 
     * @param operation     Server check performed.
     * @param result        Result of the check.
     */
    private void onOcServerCheckFinish(OwnCloudServerCheckOperation operation, RemoteOperationResult result) {
        /// update status connection icon and text
        mStatusText = mStatusIcon = 0;
        mStatusCorrect = false;
        
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
                if (mHostUrlInput.getText().toString().trim().toLowerCase().startsWith("http://") ) {
                    mStatusText = R.string.auth_connection_established;
                    mStatusIcon = R.drawable.ic_ok;
                } else {
                    mStatusText = R.string.auth_nossl_plain_ok_title;
                    mStatusIcon = android.R.drawable.ic_partial_secure;
                }
                break;
                
            /// very special case (TODO: move to a common place for all the remote operations)
            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                mStatusIcon = R.drawable.common_error;
                mStatusText = R.string.auth_ssl_unverified_server_title;
                mLastSslUntrustedServerResult = result;
                showDialog(DIALOG_SSL_VALIDATOR); 
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
        updateOcServerCheckIconAndText();
        
        /// update the visibility of the 'retry connection' button
        if (!mStatusCorrect)
            mRefreshButton.setVisibility(View.VISIBLE);
        else
            mRefreshButton.setVisibility(View.INVISIBLE);
        
        /// retrieve discovered version and normalize server URL
        mDiscoveredVersion = operation.getDiscoveredVersion();
        mHostBaseUrl = mHostUrlInput.getText().toString().trim();
        if (!mHostBaseUrl.toLowerCase().startsWith("http://") &&
            !mHostBaseUrl.toLowerCase().startsWith("https://")) {
            
            if (mIsSslConn) {
                mHostBaseUrl = "https://" + mHostBaseUrl;
            } else {
                mHostBaseUrl = "http://" + mHostBaseUrl;
            }
            
        }
        if (mHostBaseUrl.endsWith("/"))
            mHostBaseUrl = mHostBaseUrl.substring(0, mHostBaseUrl.length() - 1);
        
        /// allow or not the user try to access the server
        mOkButton.setEnabled(mStatusCorrect);
    }


    /**
     * Processes the result of the request for and access token send 
     * to an OAuth authorization server.
     * 
     * @param operation     Operation performed requesting the access token.
     * @param result        Result of the operation.
     */
    private void onGetOAuthAccessTokenFinish(OAuth2GetAccessToken operation, RemoteOperationResult result) {
        try {
            dismissDialog(DIALOG_OAUTH2_LOGIN_PROGRESS);
        } catch (IllegalArgumentException e) {
            // NOTHING TO DO ; can't find out what situation that leads to the exception in this code, but user logs signal that it happens
        }

        String webdav_path = AccountUtils.getWebdavPath(mDiscoveredVersion, false);
        if (result.isSuccess() && webdav_path != null) {
            /// be gentle with the user
            showDialog(DIALOG_LOGIN_PROGRESS);
            
            /// time to test the retrieved access token on the ownCloud server
            mOAuthAccessToken = ((OAuth2GetAccessToken)operation).getResultTokenMap().get(OAuth2Context.KEY_ACCESS_TOKEN);
            Log.d(TAG, "Got ACCESS TOKEN: " + mOAuthAccessToken);
            mAuthCheckOperation = new ExistenceCheckOperation("", this, false);
            WebdavClient client = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(mHostBaseUrl + webdav_path), this);
            client.setBearerCredentials(mOAuthAccessToken);
            mAuthCheckOperation.execute(client, this, mHandler);
            
        } else {
            if (webdav_path != null) {
                mOAuthAuthEndpointText.setError("A valid authorization could not be obtained");
            } else {
                mOAuthAuthEndpointText.setError(getString(R.string.auth_bad_oc_version_title)); // should never happen 
            }
        }
    }

    
    /**
     * Processes the result of the access check performed to try the user credentials.
     * 
     * Creates a new account through the AccountManager.
     * 
     * @param operation     Access check performed.
     * @param result        Result of the operation.
     */
    private void onAuthorizationCheckFinish(ExistenceCheckOperation operation, RemoteOperationResult result) {
        try {
            dismissDialog(DIALOG_LOGIN_PROGRESS);
        } catch (IllegalArgumentException e) {
            // NOTHING TO DO ; can't find out what situation that leads to the exception in this code, but user logs signal that it happens
        }
        
        boolean isOAuth = mOAuth2Check.isChecked();

        if (result.isSuccess()) {
            Log.d(TAG, "Successful access - time to save the account");

            /// create and save new ownCloud account
            Uri uri = Uri.parse(mHostBaseUrl);
            String username = isOAuth ?
                                "OAuth_user" + (new java.util.Random(System.currentTimeMillis())).nextLong() :
                                 mUsernameInput.getText().toString().trim();
                             // TODO a better way to set an account name
            String accountName = username + "@" + uri.getHost();
            if (uri.getPort() >= 0) {
                accountName += ":" + uri.getPort();
            }
            Account account = new Account(accountName, AccountAuthenticator.ACCOUNT_TYPE);
            AccountManager accManager = AccountManager.get(this);
            if (isOAuth) {
                accManager.addAccountExplicitly(account, "", null);  // with our implementation, the password is never input in the app
            } else {
                accManager.addAccountExplicitly(account, mPasswordInput.getText().toString(), null);
            }

            /// add the new account as default in preferences, if there is none already
            Account defaultAccount = AccountUtils.getCurrentOwnCloudAccount(this);
            if (defaultAccount == null) {
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.commit();
            }


            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same effect as addAccountExplicitly, but it's not well done
            final Intent intent = new Intent();    // TODO check if the intent can be retrieved from getIntent(), passed from AccountAuthenticator     
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,    AccountAuthenticator.ACCOUNT_TYPE);
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME,    account.name);
            if (!isOAuth)
                intent.putExtra(AccountManager.KEY_AUTHTOKEN,   AccountAuthenticator.ACCOUNT_TYPE); // TODO check this; not sure it's right; maybe
            intent.putExtra(AccountManager.KEY_USERDATA,        username);
            if (isOAuth) {
                accManager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN, mOAuthAccessToken);
            }
            /// add user data to the new account; TODO probably can be done in the last parameter addAccountExplicitly, or in KEY_USERDATA
            accManager.setUserData(account, AccountAuthenticator.KEY_OC_VERSION,    mDiscoveredVersion.toString());
            accManager.setUserData(account, AccountAuthenticator.KEY_OC_BASE_URL,   mHostBaseUrl);
            if (isOAuth)
                accManager.setUserData(account, AccountAuthenticator.KEY_SUPPORTS_OAUTH2, "TRUE");  // TODO this flag should be unnecessary

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            
            /// immediately request for the synchronization of the new account
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, AccountAuthenticator.AUTHORITY, bundle);

            finish();
                
        } else {
            if (!isOAuth) {
                mUsernameInput.setError(result.getLogMessage() + "        ");  
                                                    // the extra spaces are a workaround for an ugly bug: 
                                                    // 1. insert wrong credentials and connect
                                                    // 2. put the focus on the user name field with using hardware controls (don't touch the screen); the error is shown UNDER the field
                                                    // 3. touch the user name field; the software keyboard appears; the error popup is moved OVER the field and SHRINKED in width, losing the last word
                                                    // Seen, at least, in Android 2.x devices            
            
            } else {
                mOAuthAuthEndpointText.setError(result.getLogMessage() + "        ");
            }
            Log.d(TAG, "Access failed: " + result.getLogMessage());
        }
    }

    
    
    /**
     * {@inheritDoc}
     * 
     * Necessary to update the contents of the SSL Dialog
     * 
     * TODO move to some common place for all possible untrusted SSL failures
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
        case DIALOG_LOGIN_PROGRESS:
        case DIALOG_CERT_NOT_SAVED:
        case DIALOG_OAUTH2_LOGIN_PROGRESS:
            break;
        case DIALOG_SSL_VALIDATOR: {
            ((SslValidatorDialog)dialog).updateResult(mLastSslUntrustedServerResult);
            break;
        }
        default:
            Log.e(TAG, "Incorrect dialog called with id = " + id);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_LOGIN_PROGRESS: {
            /// simple progress dialog
            ProgressDialog working_dialog = new ProgressDialog(this);
            working_dialog.setMessage(getResources().getString(R.string.auth_trying_to_login));
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(true);
            working_dialog
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            /// TODO study if this is enough
                            Log.i(TAG, "Login canceled");
                            if (mOperationThread != null) {
                                mOperationThread.interrupt();
                                finish();
                            }
                        }
                    });
            dialog = working_dialog;
            break;
        }
        case DIALOG_OAUTH2_LOGIN_PROGRESS: {
            /// oAuth2 dialog. We show here to the user the URL and user_code that the user must validate in a web browser. - OLD!
            // TODO optimize this dialog
            ProgressDialog working_dialog = new ProgressDialog(this);
            /* Leave the old OAuth flow
            try {
                if (mCodeResponseJson != null && mCodeResponseJson.has(OAuth2GetCodeRunnable.CODE_VERIFICATION_URL)) {
                    working_dialog.setMessage(String.format(getString(R.string.oauth_code_validation_message), 
                            mCodeResponseJson.getString(OAuth2GetCodeRunnable.CODE_VERIFICATION_URL), 
                            mCodeResponseJson.getString(OAuth2GetCodeRunnable.CODE_USER_CODE)));
                } else {*/
                    working_dialog.setMessage(String.format("Getting authorization")); 
                /*}
            } catch (JSONException e) {
                Log.e(TAG, "onCreateDialog->JSONException: " + e.toString());
            }*/
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(true);
            working_dialog
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Log.i(TAG, "Login canceled");
                    /*if (mOAuth2GetCodeThread != null) {
                        mOAuth2GetCodeThread.interrupt();
                        finish();
                    } */
                    /*if (tokenReceiver != null) {
                        unregisterReceiver(tokenReceiver);
                        tokenReceiver = null;
                        finish();
                    }*/
                    finish();
                }
            });
            dialog = working_dialog;
            break;
        }
        case DIALOG_SSL_VALIDATOR: {
            /// TODO start to use new dialog interface, at least for this (it is a FragmentDialog already)
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

    
    /**
     * Starts and activity to open the 'new account' page in the ownCloud web site
     * 
     * @param view      'Account register' button
     */
    public void onRegisterClick(View view) {
        Intent register = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_account_register)));
        setResult(RESULT_CANCELED);
        startActivity(register);
    }

    
    /**
     * Updates the content and visibility state of the icon and text associated
     * to the last check on the ownCloud server.
     */
    private void updateOcServerCheckIconAndText() {
        ImageView iv = (ImageView) findViewById(R.id.action_indicator);
        TextView tv = (TextView) findViewById(R.id.status_text);

        if (mStatusIcon == 0 && mStatusText == 0) {
            iv.setVisibility(View.INVISIBLE);
            tv.setVisibility(View.INVISIBLE);
        } else {
            iv.setImageResource(mStatusIcon);
            tv.setText(mStatusText);
            iv.setVisibility(View.VISIBLE);
            tv.setVisibility(View.VISIBLE);
        }
    }

    
    /**
     * Called when the refresh button in the input field for ownCloud host is clicked.
     * 
     * Performs a new check on the URL in the input field.
     * 
     * @param view      Refresh 'button'
     */
    public void onRefreshClick(View view) {
        onFocusChange(mRefreshButton, false);
    }
    
    
    /**
     * Called when the eye icon in the password field is clicked.
     * 
     * Toggles the visibility of the password in the field. 
     * 
     * @param view      'View password' 'button'
     */
    public void onViewPasswordClick(View view) {
        int selectionStart = mPasswordInput.getSelectionStart();
        int selectionEnd = mPasswordInput.getSelectionEnd();
        int input_type = mPasswordInput.getInputType();
        if ((input_type & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            input_type = InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        } else {
            input_type = InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        }
        mPasswordInput.setInputType(input_type);
        mPasswordInput.setSelection(selectionStart, selectionEnd);
    }    
    
    
    /**
     * Called when the checkbox for OAuth authorization is clicked.
     * 
     * Hides or shows the input fields for user & password. 
     * 
     * @param view      'View password' 'button'
     */
    public void onCheckClick(View view) {
        CheckBox oAuth2Check = (CheckBox)view;      
        changeViewByOAuth2Check(oAuth2Check.isChecked());

    }
    
    /**
     * Changes the visibility of input elements depending upon the kind of authorization
     * chosen by the user: basic or OAuth
     * 
     * @param checked       'True' when OAuth is selected.
     */
    public void changeViewByOAuth2Check(Boolean checked) {
        
        ImageView auth2ActionIndicator = (ImageView) findViewById(R.id.auth2_action_indicator); 
        TextView oauth2StatusText = (TextView) findViewById(R.id.oauth2_status_text);         

        if (checked) {
            mOAuthAuthEndpointText.setVisibility(View.VISIBLE);
            mOAuthTokenEndpointText.setVisibility(View.VISIBLE);
            mUsernameInput.setVisibility(View.GONE);
            mPasswordInput.setVisibility(View.GONE);
            mViewPasswordButton.setVisibility(View.GONE);
            auth2ActionIndicator.setVisibility(View.INVISIBLE);
            oauth2StatusText.setVisibility(View.INVISIBLE);
        } else {
            mOAuthAuthEndpointText.setVisibility(View.GONE);
            mOAuthTokenEndpointText.setVisibility(View.GONE);
            mUsernameInput.setVisibility(View.VISIBLE);
            mPasswordInput.setVisibility(View.VISIBLE);
            mViewPasswordButton.setVisibility(View.INVISIBLE);
            auth2ActionIndicator.setVisibility(View.GONE);
            oauth2StatusText.setVisibility(View.GONE);
        }     

    }    
    
    /**
     * Updates the content and visibility state of the icon and text associated
     * to the interactions with the OAuth authorization server.
     * 
     * @param   drawable_id     Resource id for the icon.
     * @param   text_id         Resource id for the text.
     */
    private void updateOAuth2IconAndText(int drawable_id, int text_id) {
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
    
    /* Leave the old OAuth flow
    // Results from the first call to oAuth2 server : getting the user_code and verification_url.
    @Override
    public void onOAuth2GetCodeResult(ResultOAuthType type, JSONObject responseJson) {
        if ((type == ResultOAuthType.OK_SSL)||(type == ResultOAuthType.OK_NO_SSL)) {
            mCodeResponseJson = responseJson;
            if (mCodeResponseJson != null) {
                getOAuth2AccessTokenFromJsonResponse();
            }  // else - nothing to do here - wait for callback !!!
        
        } else if (type == ResultOAuthType.HOST_NOT_AVAILABLE) {
            updateOAuth2IconAndText(R.drawable.common_error, R.string.oauth_connection_url_unavailable);
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

        Log.d(TAG, "ResponseOAuth2->" + mCodeResponseJson.toString());

        try {
            // We get data that we must show to the user or we will use internally.
            verificationUrl = mCodeResponseJson.getString(OAuth2GetAuthorizationToken.CODE_VERIFICATION_URL);
            userCode = mCodeResponseJson.getString(OAuth2GetAuthorizationToken.CODE_USER_CODE);
            expiresIn = mCodeResponseJson.getInt(OAuth2GetAuthorizationToken.CODE_EXPIRES_IN);                

            // And we get data that we must use to get a token.
            deviceCode = mCodeResponseJson.getString(OAuth2GetAuthorizationToken.CODE_DEVICE_CODE);
            interval = mCodeResponseJson.getInt(OAuth2GetAuthorizationToken.CODE_INTERVAL);

        } catch (JSONException e) {
            Log.e(TAG, "Exception accesing data in Json object" + e.toString());
        }

        // Updating status widget to OK.
        updateOAuth2IconAndText(R.drawable.ic_ok, R.string.auth_connection_established);
        
        // Showing the dialog with instructions for the user.
        showDialog(DIALOG_OAUTH2_LOGIN_PROGRESS);

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
    */
    
    /* Leave the old OAuth flow
    // We get data from the oAuth2 token service with this broadcast receiver.
    private class TokenReceiver extends BroadcastReceiver {
        /**
         * The token is received.
         *  @author
         * {@link BroadcastReceiver} to enable oAuth2 token receiving.
         *-/
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> tokenResponse = (HashMap<String, String>)intent.getExtras().get(OAuth2GetTokenService.TOKEN_RECEIVED_DATA);
            Log.d(TAG, "TokenReceiver->" + tokenResponse.get(OAuth2GetTokenService.TOKEN_ACCESS_TOKEN));
            dismissDialog(DIALOG_OAUTH2_LOGIN_PROGRESS);

        }
    }
    */

    
    /**
     * Called from SslValidatorDialog when a new server certificate was correctly saved.
     */
    public void onSavedCertificate() {
        mOperationThread = mOcServerChkOperation.retry(this, mHandler);                
    }

    /**
     * Called from SslValidatorDialog when a new server certificate could not be saved 
     * when the user requested it.
     */
    @Override
    public void onFailedSavingCertificate() {
        showDialog(DIALOG_CERT_NOT_SAVED);
    }

}
