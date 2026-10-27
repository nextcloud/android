/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023-2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2019-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2013-2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.authentication

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.browser.auth.AuthTabIntent
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blikoon.qrcodescanner.QrCodeActivity
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.client.onboarding.OnboardingService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.HTTPStatusCodes
import com.nextcloud.operations.PostMethod
import com.nextcloud.utils.RawResourceReader
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.mdm.MDMConfig
import com.nextcloud.utils.text.LinkFormatter
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorAsyncTask.OnAuthenticatorTaskListener
import com.owncloud.android.authentication.AuthenticatorUrlUtils.normalizeScheme
import com.owncloud.android.authentication.AuthenticatorUrlUtils.normalizeUrlSuffix
import com.owncloud.android.authentication.AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles
import com.owncloud.android.authentication.AuthenticatorUrlUtils.trimWebdavSuffix
import com.owncloud.android.authentication.dialog.LoginDialog
import com.owncloud.android.databinding.AccountSetupBinding
import com.owncloud.android.databinding.AccountSetupWebviewBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.operations.GetServerInfoOperation
import com.owncloud.android.operations.GetServerInfoOperation.ServerInfo
import com.owncloud.android.providers.DocumentsStorageProvider
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.NextcloudWebViewClient
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.ProxyUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import java.lang.ref.WeakReference
import java.net.URLDecoder
import javax.inject.Inject

/**
 * This Activity is used to add an ownCloud account to the App
 */
@Suppress("LargeClass")
open class AuthenticatorActivity :
    AccountAuthenticatorActivity(),
    OnRemoteOperationListener,
    OnEditorActionListener,
    OnSslUntrustedCertListener,
    OnAuthenticatorTaskListener,
    Injectable {

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var onboarding: OnboardingService

    @Inject
    lateinit var deviceInfo: DeviceInfo

    @Inject
    lateinit var passCodeManager: PassCodeManager

    @Inject
    lateinit var viewThemeUtilsFactory: ViewThemeUtils.Factory

    @Inject
    lateinit var clientFactory: ClientFactory

    @get:VisibleForTesting
    var accountSetupBinding: AccountSetupBinding? = null
        private set
    private var accountSetupWebviewBinding: AccountSetupWebviewBinding? = null

    protected val loginDialog = LoginDialog(this)
    private lateinit var viewThemeUtils: ViewThemeUtils
    private lateinit var platformAccountManager: AccountManager
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    private var authenticatorAction = ACTION_CREATE
    private var account: Account? = null
    private var onlyAdd = false
    private var isFirstAuthAttempt = false
    private var waitingForOpId = NO_PENDING_OPERATION

    private var operationsServiceConnection: ServiceConnection? = null
    private var operationsServiceBinder: OperationsServiceBinder? = null
    private var authenticatorTask: AuthenticatorAsyncTask? = null

    private var serverInfo = ServerInfo()
    private var serverStatusText: String? = EMPTY_STRING
    private var serverStatusIcon = NO_ICON
    private var authStatusText: String? = EMPTY_STRING
    private var authStatusIcon = NO_ICON

    private var showWebViewLoginUrl = false
    private var webViewUser: String? = null
    private var webViewPassword: String? = null

    private val loginDataScheme: String
        get() = getString(R.string.login_data_own_scheme)

    private val loginDataPrefix: String
        get() = loginDataScheme + PROTOCOL_SUFFIX + LOGIN_PATH

    private val isAdditionalAccountForbidden: Boolean
        get() = !MDMConfig.multiAccountSupport(this) && accountManager.accounts.size == 1

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewThemeUtils = viewThemeUtilsFactory.withPrimaryAsBackground()
        viewThemeUtils.platform.colorStatusBar(this, ContextCompat.getColor(this, R.color.primary))

        val isDirectLogin = intent.data?.toString()?.let(::isLoginData) == true
        if (savedInstanceState == null && !isDirectLogin) {
            onboarding.launchFirstRunIfNeeded(this)
        }

        onlyAdd = isOnlyAddRequest(intent)
        deleteCookies()
        hideActionBar()

        isFirstAuthAttempt = true
        platformAccountManager = AccountManager.get(this)
        authenticatorAction = intent.getByteExtra(EXTRA_ACTION, ACTION_CREATE)
        account = intent.extras?.getParcelableArgument(EXTRA_ACCOUNT, Account::class.java)

        savedInstanceState?.let {
            waitingForOpId = it.getLong(KEY_WAITING_FOR_OP_ID)
            isFirstAuthAttempt = it.getBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG)
        }

        val webLoginUrl = mdmWebLoginUrl() ?: providerWebLoginUrl() ?: brandedWebLoginUrl()
        if (webLoginUrl != null) {
            inflateWebViewLayout()
            anonymouslyPostLoginRequest(webLoginUrl)
        } else {
            initServerUrlLogin(savedInstanceState)
        }

        val actionName = if (authenticatorAction == ACTION_CREATE) "create account" else "update credentials"
        Log_OC.d(
            TAG,
            "Started, action=$actionName, recreated=${savedInstanceState != null}, webViewLogin=${webLoginUrl != null}"
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(KEY_WAITING_FOR_OP_ID, waitingForOpId)
        outState.putBoolean(KEY_IS_SSL_CONN, serverInfo.mIsSslConn)
        outState.putString(KEY_HOST_URL_TEXT, serverInfo.mBaseUrl)
        serverInfo.mVersion?.let { outState.putString(KEY_OC_VERSION, it.version) }
        outState.putString(KEY_SERVER_AUTH_METHOD, serverInfo.mAuthMethod.name)
        outState.putBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG, isFirstAuthAttempt)

        val pendingAuthObject = authObject
        if (pendingAuthObject != null && !isLoginProcessCompleted) {
            outState.putString(KEY_LOGIN_FLOW_AUTH_OBJECT, AuthObject.toJson(pendingAuthObject))
        }

        authenticatorTask?.cancel(true)
        outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, authenticatorTask != null)
        authenticatorTask = null
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.getBoolean(KEY_ASYNC_TASK_IN_PROGRESS)) {
            return
        }

        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            savedInstanceState.getString(KEY_USERNAME),
            savedInstanceState.getString(KEY_PASSWORD)
        )
        accessRootFolder(credentials)
    }

    /**
     * The redirection triggered by the OAuth authentication server as response to the GET AUTHORIZATION request is
     * caught here.
     *
     * To make this possible, this activity needs to be qualified with android:launchMode = "singleTask" in the
     * AndroidManifest.xml file.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(FirstRunActivity.EXTRA_EXIT, false)) {
            super.finish()
        }

        onlyAdd = isOnlyAddRequest(intent)
        passCodeManager.onActivityResumed(this)

        val data = intent.data?.toString()
        if (data != null && isLoginData(data)) {
            Log_OC.d(TAG, "Login data received via deep link")
            if (isAdditionalAccountForbidden) {
                Log_OC.w(TAG, "Login rejected, multiple accounts are not allowed")
                SnackbarUtil.show(this, R.string.no_mutliple_accounts_allowed)
                finish()
                return
            }
            parseAndLoginFromWebView(data)
        }

        if (intent.getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
            initSimpleSignupLogin(inflateWebViewLayout())
        }
    }

    override fun onResume() {
        super.onResume()

        val connection = OperationsServiceConnection()
        operationsServiceConnection = connection
        if (!bindService(Intent(this, OperationsService::class.java), connection, BIND_AUTO_CREATE)) {
            SnackbarUtil.show(accountSetupBinding?.scroll, R.string.error_cant_bind_to_operations_service)
            finish()
        }

        if (operationsServiceBinder != null) {
            doOnResumeAndBound()
        }
    }

    override fun onPause() {
        operationsServiceBinder?.removeOperationListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        operationsServiceConnection?.let {
            unbindService(it)
            operationsServiceBinder = null
        }
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val webViewBinding = accountSetupWebviewBinding
        val isBackPressed = event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK
        if (webViewBinding == null || !isBackPressed) {
            return super.onKeyDown(keyCode, event)
        }

        if (webViewBinding.loginWebview.canGoBack()) {
            webViewBinding.loginWebview.goBack()
        } else {
            finish()
        }
        return true
    }
    // endregion

    // region Initial UI
    private fun hideActionBar() {
        supportActionBar?.run {
            hide()
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun mdmWebLoginUrl(): String? {
        if (!MainApp.isClientBrandedPlus()) {
            return null
        }
        return MDMConfig.getBaseUrl(this).takeIf { it.isNotEmpty() }?.plus(WEB_LOGIN)
    }

    private fun providerWebLoginUrl(): String? = if (intent.getBooleanExtra(EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)) {
        getString(R.string.provider_registration_server)
    } else {
        null
    }

    private fun brandedWebLoginUrl(): String? = getString(R.string.webview_login_url)
        .takeIf { it.isNotEmpty() }
        ?.also { showWebViewLoginUrl = resources.getBoolean(R.bool.show_server_url_input) }

    private fun inflateWebViewLayout(): AccountSetupWebviewBinding =
        AccountSetupWebviewBinding.inflate(layoutInflater).also {
            accountSetupWebviewBinding = it
            setContentView(it.root)
        }

    private fun inflateAccountSetupLayout(): AccountSetupBinding = AccountSetupBinding.inflate(layoutInflater).also {
        accountSetupBinding = it
        setContentView(it.root)
    }

    private fun initServerUrlLogin(savedInstanceState: Bundle?) {
        val binding = inflateAccountSetupLayout()
        initOverallUi(binding)

        if (getString(R.string.enforce_servers).isEmpty()) {
            initAuthorizationUi(binding, savedInstanceState)
        } else {
            showEnforcedServers(binding)
        }

        initServerInfo(savedInstanceState)
        restorePendingLoginFlow(savedInstanceState)
    }

    private fun initOverallUi(binding: AccountSetupBinding) {
        binding.hostUrlContainer.setEndIconOnClickListener { checkOcServer() }
        binding.hostUrlInputHelperText.text =
            getString(R.string.login_url_helper_text, getString(R.string.app_name))

        viewThemeUtils.platform.colorTextView(binding.hostUrlInputHelperText, ColorRole.ON_PRIMARY)
        viewThemeUtils.platform.colorTextView(binding.serverStatusText, ColorRole.ON_PRIMARY)
        viewThemeUtils.platform.colorTextView(binding.authStatusText, ColorRole.ON_PRIMARY)
        viewThemeUtils.material.colorTextInputLayout(binding.hostUrlContainer, ColorRole.ON_PRIMARY)
        viewThemeUtils.platform.colorEditTextOnPrimary(binding.hostUrlInput)

        if (!deviceInfo.hasCamera(this)) {
            binding.scanQr.visibility = View.GONE
            return
        }

        binding.scanQr.setOnClickListener { onScan() }
        viewThemeUtils.platform.tintDrawable(this, binding.scanQr.drawable, ColorRole.ON_PRIMARY)
    }

    private fun initAuthorizationUi(binding: AccountSetupBinding, savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            authStatusText = it.getString(KEY_AUTH_STATUS_TEXT)
            authStatusIcon = it.getInt(KEY_AUTH_STATUS_ICON)
        }

        showAuthStatus()

        binding.hostUrlInput.imeOptions = EditorInfo.IME_ACTION_NEXT
        binding.hostUrlInput.setOnEditorActionListener(this)
    }

    private fun showEnforcedServers(binding: AccountSetupBinding) {
        showAuthStatus()
        binding.hostUrlFrame.visibility = View.GONE
        binding.hostUrlInputHelperText.visibility = View.GONE
        binding.serversSpinner.visibility = View.VISIBLE

        val enforcedServers = parseEnforcedServers(getString(R.string.enforce_servers))
        val serverUrls = listOf(EMPTY_STRING) + enforcedServers.map { it.url }

        binding.serversSpinner.adapter = ArrayAdapter<String>(this, R.layout.enforced_servers_spinner).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            add(getString(R.string.please_select_a_server))
            enforcedServers.forEach { add(it.name) }
        }

        binding.serversSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val url = serverUrls[position]
                if (!URLUtil.isValidUrl(url)) {
                    return
                }
                accountSetupBinding?.hostUrlInput?.setText(url)
                checkOcServer()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun initServerInfo(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreServerInfo(savedInstanceState)
            return
        }

        val account = account
        if (account == null) {
            serverInfo.mBaseUrl = getString(R.string.webview_login_url).trim { it <= ' ' }
            serverInfo.mIsSslConn = serverInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL)
            return
        }

        serverInfo.mBaseUrl = platformAccountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)
            .orEmpty()
        serverInfo.mIsSslConn = serverInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL)
        serverInfo.mVersion = accountManager.getServerVersion(account)
    }

    private fun restoreServerInfo(savedInstanceState: Bundle) {
        serverStatusText = savedInstanceState.getString(KEY_SERVER_STATUS_TEXT)
        serverStatusIcon = savedInstanceState.getInt(KEY_SERVER_STATUS_ICON)

        serverInfo.mIsSslConn = savedInstanceState.getBoolean(KEY_IS_SSL_CONN)
        serverInfo.mBaseUrl = savedInstanceState.getString(KEY_HOST_URL_TEXT)
        savedInstanceState.getString(KEY_OC_VERSION)?.let { serverInfo.mVersion = OwnCloudVersion(it) }
        serverInfo.mAuthMethod = AuthenticationMethod.valueOf(savedInstanceState.getString(KEY_SERVER_AUTH_METHOD)!!)
    }

    private fun isOnlyAddRequest(intent: Intent): Boolean =
        intent.getBooleanExtra(KEY_ONLY_ADD, false) || isSsoRequest(intent)

    private fun isSsoRequest(intent: Intent): Boolean =
        intent.extras?.getString(KEY_AUTH_TOKEN_TYPE) == SSO_AUTH_TOKEN_TYPE

    private fun isLoginData(value: String): Boolean = value.startsWith(loginDataScheme)

    private fun deleteCookies() {
        try {
            CookieManager.getInstance().removeAllCookies(null)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to delete web view cookies", e)
        }
    }
    // endregion

    // region LoginFlow
    private var authObject: AuthObject? = null
    private var fallbackToken: String? = null
    private var loginFlowBaseUrl: String? = null
    private var isLoginProcessCompleted = false
    private var isRedirectedToTheDefaultBrowser = false
    private var isServerCheckPendingServiceBinding = false
    private var loginFlowPollingJob: Job? = null
    private val loginFlowPollRequests = Channel<Unit>(Channel.CONFLATED)

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START && !authObject?.poll?.token.isNullOrEmpty()) {
            Log_OC.d(TAG, "Login flow v2: app is in foreground, polling started")
            pollLoginFlow()
        }
    }

    private fun pollLoginFlow() {
        loginFlowPollRequests.trySend(Unit)
        if (loginFlowPollingJob?.isActive == true) {
            return
        }

        loginFlowPollingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                withTimeoutOrNull(LOGIN_FLOW_POLL_INTERVAL_MS) { loginFlowPollRequests.receive() }
                if (!isLoginProcessCompleted && !pollLoginFlowOnce()) {
                    return@launch
                }
            }
        }
    }

    private fun stopLoginFlowPolling() {
        loginFlowPollingJob?.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
    }

    /**
     * Anonymously posts a login flow v2 request to [url] (e.g. `https://example.com/index.php/login/v2`) and opens
     * the returned login URL in the browser.
     */
    private fun anonymouslyPostLoginRequest(url: String) {
        if (url.isEmpty()) {
            Log_OC.e(TAG, "Login flow v2: cannot start, login request URL is empty")
            SnackbarUtil.show(this, R.string.authenticator_activity_empty_base_url)
            return
        }
        loginFlowBaseUrl = url
        Log_OC.d(TAG, "Login flow v2: requesting login URL from $url")

        lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) { requestLoginFlowResponse(url) }
            if (response.isEmpty()) {
                Log_OC.e(TAG, "Login flow v2: login URL request returned an empty response")
                SnackbarUtil.show(this@AuthenticatorActivity, R.string.authenticator_activity_empty_response_message)
                return@launch
            }

            val loginUrl = extractLoginUrl(response)
            initLoginInfoView()
            launchDefaultWebBrowser(loginUrl)
        }
    }

    private fun requestLoginFlowResponse(url: String): String = try {
        val post = PostMethod(url, false, FormBody.Builder().build())
        val status = post.execute(clientFactory.createPlainClient())
        Log_OC.d(TAG, "Login flow v2: login URL request finished with status $status")
        post.getResponseBodyAsString()
    } catch (e: Exception) {
        Log_OC.e(TAG, "Login flow v2: login URL request failed: ${e.javaClass.simpleName}")
        EMPTY_STRING
    }

    private fun extractLoginUrl(response: String): String =
        loginUrlFromAuthObject(response) ?: loginUrlFromJsonObject(response) ?: defaultLoginUrl()

    private fun loginUrlFromAuthObject(response: String): String? {
        try {
            val parsedAuthObject = AuthObject.fromJson(response)
            authObject = parsedAuthObject
            if (parsedAuthObject.login.isNotEmpty()) {
                return parsedAuthObject.login
            }
            Log_OC.e(TAG, "Login flow v2: login URL missing in response, trying fallback parser")
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login flow v2: response parsing failed, trying fallback parser: ${e.javaClass.simpleName}")
        }
        return null
    }

    private fun loginUrlFromJsonObject(response: String): String? {
        try {
            val jsonObject = JsonParser.parseString(response).asJsonObject
            fallbackToken = jsonObject.getAsJsonObject(JSON_KEY_POLL).get(JSON_KEY_TOKEN).asString
            val loginUrl: String? = jsonObject.get(JSON_KEY_LOGIN).asString
            if (!loginUrl.isNullOrEmpty()) {
                return loginUrl
            }
            Log_OC.e(TAG, "Login flow v2: fallback parser found no login URL")
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login flow v2: fallback parser failed: ${e.javaClass.simpleName}")
        }
        return null
    }

    private fun defaultLoginUrl(): String {
        Log_OC.e(TAG, "Login flow v2: no login URL in response, using default login URL")
        SnackbarUtil.show(this, R.string.authenticator_activity_login_error)
        return getString(R.string.webview_login_url)
    }

    private fun launchDefaultWebBrowser(url: String) {
        if (url.isBlank()) {
            Log_OC.e(TAG, "Login flow v2: cannot open browser, login URL is blank")
            SnackbarUtil.show(this, R.string.invalid_url)
            return
        }

        val uri = url.toUri()
        if (launchAuthTab(uri) || launchExternalBrowser(uri)) {
            return
        }

        Log_OC.e(TAG, "Login flow v2: no browser available to open login page")
        SnackbarUtil.show(this, R.string.authenticator_activity_no_web_browser_found)
    }

    private fun launchAuthTab(uri: Uri): Boolean = try {
        val toolbarColor = ContextCompat.getColor(this, R.color.primary)
        val authTabIntent = AuthTabIntent.Builder().setColorScheme(toolbarColor).build()
        authTabIntent.launch(authTabResultLauncher, uri, loginDataScheme)
        Log_OC.d(TAG, "Login flow v2: login page opened in Auth Tab")
        true
    } catch (e: Exception) {
        Log_OC.e(TAG, "Login flow v2: Auth Tab launch failed: ${e.javaClass.simpleName}")
        false
    }

    private fun launchExternalBrowser(uri: Uri): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log_OC.d(TAG, "Login flow v2: login page opened in external browser")
                return true
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login flow v2: external browser launch failed: ${e.javaClass.simpleName}")
        }
        return false
    }

    private fun loginFlowPollEndpointAndToken(): Pair<String, String?> {
        val poll = authObject?.poll
        if (poll != null && poll.endpoint.isNotEmpty() && poll.token.isNotEmpty()) {
            return poll.endpoint to poll.token
        }

        Log_OC.w(TAG, "Login flow v2: poll endpoint or token missing, using fallback endpoint and token")
        return "$loginFlowBaseUrl$LOGIN_FLOW_POLL_PATH" to fallbackToken
    }

    private suspend fun pollLoginFlowOnce(): Boolean {
        val (status, response) = try {
            requestLoginFlowCredentials()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login flow v2: poll failed, polling stopped: ${e.javaClass.simpleName}")
            return false
        }

        if (status == HTTPStatusCodes.NOT_FOUND.code) {
            Log_OC.d(TAG, "Login flow v2: access not granted in browser yet, polling continues")
            return true
        }

        Log_OC.d(TAG, "Login flow v2: poll finished with status $status")
        if (status == HTTPStatusCodes.SUCCESS.code && response.isNotEmpty()) {
            withContext(Dispatchers.Main) { completeLoginFlow(response, status) }
        }
        return true
    }

    private fun requestLoginFlowCredentials(): Pair<Int, String> {
        val (pollUrl, token) = loginFlowPollEndpointAndToken()
        val requestBody = FormBody.Builder()
            .add(JSON_KEY_TOKEN, requireNotNull(token))
            .build()

        val post = PostMethod(pollUrl, false, requestBody)
        val status = post.execute(clientFactory.createPlainClient())
        return status to post.getResponseBodyAsString()
    }

    private fun completeLoginFlow(response: String, status: Int) {
        try {
            val loginUrlInfo: LoginUrlInfo? = gson.fromJson(response, LoginUrlInfo::class.java)
            if (loginUrlInfo == null) {
                Log_OC.e(TAG, "Login flow v2: cannot complete, poll response has no credentials")
                return
            }
            isLoginProcessCompleted = loginUrlInfo.isValid(status)
            Log_OC.d(
                TAG,
                "Login flow v2: credentials received for ${loginUrlInfo.server}, valid=$isLoginProcessCompleted"
            )

            accountSetupBinding?.hostUrlInput?.setText(EMPTY_STRING)
            applyLoginCredentials(loginUrlInfo)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login flow v2: poll response parsing failed: ${e.javaClass.simpleName}")
            showServerStatusAlert(getString(R.string.qr_could_not_be_read))
        }

        if (operationsServiceBinder == null) {
            Log_OC.d(TAG, "Login flow v2: operations service not bound yet, server check deferred")
            isServerCheckPendingServiceBinding = true
        } else {
            checkOcServer()
        }
        stopLoginFlowPolling()
        Log_OC.d(TAG, "Login flow v2: polling stopped")
    }

    private fun restorePendingLoginFlow(savedInstanceState: Bundle?) {
        val authObjectJson = savedInstanceState?.getString(KEY_LOGIN_FLOW_AUTH_OBJECT) ?: return

        authObject = AuthObject.fromJson(authObjectJson)
        Log_OC.d(TAG, "Login flow v2: pending flow restored after activity recreation")
        inflateWebViewLayout()
        initLoginInfoView()
    }

    private fun initLoginInfoView() {
        val loginFlowBinding = accountSetupWebviewBinding?.loginFlowV2 ?: return
        val loginFlowLayout = loginFlowBinding.root
        loginFlowLayout.visibility = View.VISIBLE

        ViewCompat.setOnApplyWindowInsetsListener(loginFlowLayout) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }
        // the listener is attached after the initial insets dispatch, so request a fresh pass
        ViewCompat.requestApplyInsets(loginFlowLayout)

        loginFlowBinding.cancelButton.setOnClickListener { cancelLoginFlow() }
    }

    private fun cancelLoginFlow() {
        Log_OC.d(TAG, "Login flow v2: cancelled by user")
        stopLoginFlowPolling()
        authObject = null
        recreate()
    }
    // endregion

    // region WebView login
    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT")
    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    private fun initSimpleSignupLogin(binding: AccountSetupWebviewBinding) {
        viewThemeUtils.platform.colorCircularProgressBar(
            binding.loginWebviewProgressBar,
            ColorRole.ON_PRIMARY_CONTAINER
        )
        binding.loginWebview.visibility = View.GONE
        binding.loginWebview.settings.run {
            allowFileAccess = false
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = MainApp.getUserAgent()
            saveFormData = false
            savePassword = false
        }

        val headers = mapOf(RemoteOperation.OCS_API_HEADER to RemoteOperation.OCS_API_HEADER_VALUE)
        ProxyUtil(WeakReference<Context>(this)).applyProxy {
            accountSetupWebviewBinding?.loginWebview?.loadUrl(getString(R.string.provider_registration_server), headers)
        }
        binding.loginFlowV2.loginFlowInfoV2.visibility = View.GONE

        setLoginWebViewClient(binding)
    }

    private fun setLoginWebViewClient(binding: AccountSetupWebviewBinding) {
        binding.loginWebview.webViewClient = object : NextcloudWebViewClient(supportFragmentManager) {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (!url.startsWith(loginDataPrefix)) {
                    return false
                }
                parseAndLoginFromWebView(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                showLoadedWebView()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                showLoadedWebView()

                val customError = RawResourceReader.readText(resources, R.raw.custom_error)
                if (customError.isNotEmpty()) {
                    accountSetupWebviewBinding?.loginWebview?.loadData(customError, HTML_MIME_TYPE, null)
                }
            }
        }
    }

    private fun showLoadedWebView() {
        accountSetupWebviewBinding?.run {
            loginWebviewProgressBar.visibility = View.GONE
            loginWebview.visibility = View.VISIBLE
        }
    }

    fun parseAndLoginFromWebView(dataString: String) {
        try {
            val loginUrlInfo = parseLoginDataUrl(loginDataPrefix, dataString)

            if (!isServerAllowed(loginUrlInfo.server)) {
                Log_OC.w(TAG, "Login rejected, server is not allowed: ${loginUrlInfo.server}")
                showServerStatusAlert(getString(R.string.server_not_allowed))
                return
            }

            accountSetupBinding?.hostUrlInput?.setText(EMPTY_STRING)
            applyLoginCredentials(loginUrlInfo)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login data could not be parsed: ${e.javaClass.simpleName}")
            showServerStatusAlert(getString(R.string.qr_could_not_be_read))
            return
        }
        checkOcServer()
    }

    private fun isServerAllowed(server: String): Boolean {
        val webViewLoginUrl = getString(R.string.webview_login_url)
        if (webViewLoginUrl.isNotEmpty() && webViewLoginUrl.startsWith(server)) {
            return true
        }

        val enforcedServers = getString(R.string.enforce_servers)
        if (enforcedServers.isEmpty()) {
            return false
        }

        return parseEnforcedServers(enforcedServers).any { it.url.startsWith(server) }
    }

    private fun parseEnforcedServers(json: String): List<EnforcedServer> =
        gson.fromJson(json, object : TypeToken<List<EnforcedServer>>() {}.type)
    // endregion

    // region Server check
    @SuppressFBWarnings("NP")
    private fun checkOcServer() {
        val binding = accountSetupBinding
        val hostUrl = binding?.hostUrlInput?.text?.toString()
        var uri: String = if (hostUrl.isNullOrEmpty()) serverInfo.mBaseUrl else hostUrl.trim { it <= ' ' }

        serverInfo = ServerInfo()

        if (uri.isEmpty()) {
            return
        }

        if (binding != null) {
            uri = stripIndexPhpOrAppsFiles(uri)
            binding.hostUrlInput.setText(uri)
        }

        uri = toAsciiDomainOrKeep(normalizeSchemeOrKeep(uri))

        if (binding != null) {
            serverStatusText = getString(R.string.auth_testing_connection)
            serverStatusIcon = R.drawable.progress_small
            showServerStatus()
        }

        queueServerInfoCheck(uri)
    }

    private fun normalizeSchemeOrKeep(uri: String): String = try {
        normalizeScheme(uri)
    } catch (e: IllegalArgumentException) {
        Log_OC.e(TAG, "Server check: cannot normalize scheme of $uri", e)
        uri
    }

    private fun toAsciiDomainOrKeep(uri: String): String = try {
        LinkFormatter.toAsciiDomain(uri)
    } catch (e: IllegalArgumentException) {
        Log_OC.e(TAG, "Server check: cannot convert internationalized domain name $uri", e)
        uri
    }

    private fun queueServerInfoCheck(uri: String) {
        val getServerInfoIntent = Intent(OperationsService.ACTION_GET_SERVER_INFO)
            .putExtra(OperationsService.EXTRA_SERVER_URL, normalizeUrlSuffix(uri))

        val binder = operationsServiceBinder
        if (binder == null) {
            Log_OC.e(TAG, "Server check: skipped, operations service is not bound")
            return
        }

        waitingForOpId = binder.queueNewOperation(getServerInfoIntent)
        Log_OC.d(TAG, "Server check: started for $uri")
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when (operation) {
            is GetServerInfoOperation -> if (operation.hashCode().toLong() == waitingForOpId) {
                onGetServerInfoFinish(result)
            }

            is GetUserInfoRemoteOperation -> {
                @Suppress("UNCHECKED_CAST")
                onGetUserNameFinish(result as RemoteOperationResult<UserInfo?>)
            }
        }
    }

    private fun onGetServerInfoFinish(result: RemoteOperationResult<*>) {
        waitingForOpId = NO_PENDING_OPERATION

        if (result.isSuccess) {
            onServerInfoReceived(result.data[0] as ServerInfo)
        } else {
            Log_OC.e(TAG, "Server check: failed, code=${result.code}, ${result.logMessage}")
            updateServerStatusIconAndText(result)
            showServerStatus()
        }

        if (result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result)
        }
    }

    private fun onServerInfoReceived(receivedServerInfo: ServerInfo) {
        serverInfo = receivedServerInfo
        Log_OC.d(TAG, "Server check: succeeded for ${serverInfo.mBaseUrl}")

        val user = webViewUser
        val password = webViewPassword
        if (!user.isNullOrEmpty() && !password.isNullOrEmpty()) {
            checkBasicAuthorization(user, password)
            return
        }

        inflateWebViewLayout()

        if (isLoginProcessCompleted) {
            return
        }

        if (isRedirectedToTheDefaultBrowser) {
            initLoginInfoView()
            return
        }

        anonymouslyPostLoginRequest(serverInfo.mBaseUrl + WEB_LOGIN)
        isRedirectedToTheDefaultBrowser = true
    }

    private fun showUntrustedCertDialog(result: RemoteOperationResult<*>) {
        val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(
            result.exception as CertificateCombinedException?
        )
        val transaction = supportFragmentManager.beginTransaction().addToBackStack(null)
        dialog.show(transaction, UNTRUSTED_CERT_DIALOG_TAG)
    }

    override fun onSavedCertificate() {
        checkOcServer()
    }

    override fun onFailedSavingCertificate() {
        SnackbarUtil.show(this, R.string.ssl_validator_not_saved)
    }
    // endregion

    // region Status
    private fun updateServerStatusIconAndText(result: RemoteOperationResult<*>) {
        val (icon, text) = when (val code = result.code) {
            ResultCode.OK_SSL -> R.drawable.ic_lock_white to getString(R.string.auth_secure_connection)

            ResultCode.OK_NO_SSL, ResultCode.OK -> plainConnectionStatus()

            ResultCode.NO_NETWORK_CONNECTION -> R.drawable.no_network to getString(R.string.auth_no_net_conn_title)

            ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION ->
                R.drawable.ic_lock_open_white to getString(R.string.auth_redirect_non_secure_connection_title)

            ResultCode.UNKNOWN_ERROR -> R.drawable.ic_alert to unknownErrorMessage(result)

            else -> SERVER_ERROR_MESSAGES[code]?.let { R.drawable.ic_alert to getString(it) }
                ?: (NO_ICON to EMPTY_STRING)
        }
        serverStatusIcon = icon
        serverStatusText = text
    }

    private fun updateAuthStatusIconAndText(result: RemoteOperationResult<*>) {
        authStatusIcon = R.drawable.ic_alert

        when (result.code) {
            ResultCode.OK_SSL -> setAuthStatus(R.drawable.ic_lock_white to getString(R.string.auth_secure_connection))

            ResultCode.OK_NO_SSL, ResultCode.OK -> if (showWebViewLoginUrl) {
                setAuthStatus(plainConnectionStatus())
            }

            ResultCode.NO_NETWORK_CONNECTION ->
                setAuthStatus(R.drawable.no_network to getString(R.string.auth_no_net_conn_title))

            ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED ->
                authStatusText = getString(R.string.auth_ssl_unverified_server_title)

            ResultCode.TIMEOUT -> authStatusText = getString(R.string.auth_timeout_title)

            ResultCode.HOST_NOT_AVAILABLE -> authStatusText = getString(R.string.auth_unknown_host_title)

            ResultCode.ACCOUNT_NOT_NEW -> {
                authStatusText = getString(R.string.auth_account_not_new)
                finishWithAuthErrorIfServerUrlHidden()
            }

            else -> {
                authStatusText = ErrorMessageAdapter.getErrorCauseMessage(result, null, resources)
                finishWithAuthErrorIfServerUrlHidden()
            }
        }
    }

    private fun setAuthStatus(status: Pair<Int, String>) {
        authStatusIcon = status.first
        authStatusText = status.second
    }

    private fun plainConnectionStatus(): Pair<Int, String> = if (isHostUrlPlainHttp) {
        R.drawable.ic_ok to getString(R.string.auth_connection_established)
    } else {
        R.drawable.ic_lock_open_white to getString(R.string.auth_nossl_plain_ok_title)
    }

    private val isHostUrlPlainHttp: Boolean
        get() = accountSetupBinding?.hostUrlInput?.text
            ?.toString()
            ?.trim { it <= ' ' }
            ?.lowercase()
            ?.startsWith(HTTP_PROTOCOL) == true

    private fun unknownErrorMessage(result: RemoteOperationResult<*>): String {
        val exceptionMessage = result.exception?.message
        return if (exceptionMessage.isNullOrEmpty()) {
            getString(R.string.auth_unknown_error_title)
        } else {
            getString(R.string.auth_unknown_error_exception_title, exceptionMessage)
        }
    }

    private fun finishWithAuthErrorIfServerUrlHidden() {
        if (showWebViewLoginUrl) {
            return
        }
        SnackbarUtil.show(this, authStatusText)
        finish()
    }

    private fun updateStatusIconFailUserName(failedStatusText: Int) {
        authStatusIcon = R.drawable.ic_alert
        authStatusText = getString(failedStatusText)
    }

    private fun showServerStatusAlert(message: String) {
        serverStatusIcon = R.drawable.ic_alert
        serverStatusText = message
        showServerStatus()
    }

    private fun showServerStatus() {
        val binding = accountSetupBinding ?: return
        showStatus(binding.serverStatusText, serverStatusIcon, serverStatusText)
    }

    private fun showAuthStatus() {
        val binding = accountSetupBinding ?: return
        showStatus(binding.authStatusText, authStatusIcon, authStatusText)
    }

    private fun showStatus(statusView: TextView, @DrawableRes icon: Int, text: String?) {
        if (icon == NO_ICON && text == EMPTY_STRING) {
            statusView.visibility = View.INVISIBLE
            return
        }

        statusView.text = text
        statusView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        statusView.visibility = View.VISIBLE
    }
    // endregion

    // region Credentials
    private fun checkBasicAuthorization(webViewUsername: String?, webViewPassword: String?) {
        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(webViewUsername, webViewPassword)
        accessRootFolder(credentials)
    }

    @Suppress("DEPRECATION")
    private fun accessRootFolder(credentials: OwnCloudCredentials?) {
        Log_OC.d(TAG, "Credential check: started for ${serverInfo.mBaseUrl}")
        authenticatorTask = AuthenticatorAsyncTask(this).apply { execute(serverInfo.mBaseUrl, credentials) }
    }

    private fun onGetUserNameFinish(result: RemoteOperationResult<UserInfo?>) {
        waitingForOpId = NO_PENDING_OPERATION

        if (result.isSuccess) {
            if (saveAccount(result)) {
                finish()
            }
            return
        }

        val statusText = if (result.code == ResultCode.MAINTENANCE_MODE) {
            R.string.maintenance_mode
        } else {
            R.string.auth_fail_get_user_name
        }
        updateStatusIconFailUserName(statusText)
        showAuthStatus()
        Log_OC.e(TAG, "User info: request failed, code=${result.code}, ${result.logMessage}")
    }

    override fun onAuthenticatorTaskCallback(result: RemoteOperationResult<UserInfo?>?) {
        result ?: return

        waitingForOpId = NO_PENDING_OPERATION
        dismissWaitingDialog()
        authenticatorTask = null

        when {
            result.isSuccess -> onCredentialsAccepted(result)
            result.isServerFail || result.isException -> onCredentialsServerError(result)
            else -> onCredentialsRejected(result)
        }
    }

    private fun onCredentialsAccepted(result: RemoteOperationResult<UserInfo?>) {
        Log_OC.d(TAG, "Credential check: succeeded, saving account")

        val isAccountSaved = saveAccount(result)
        resetWebViewCredentials()

        if (isAccountSaved) {
            val accountName = requireNotNull(account).name
            accountManager.setCurrentOwnCloudAccount(accountName)
            fetchCapabilitiesAndFinish(accountName)
            return
        }

        val binding = inflateAccountSetupLayout()
        initOverallUi(binding)
        binding.hostUrlInput.setText(serverInfo.mBaseUrl)
        binding.serverStatusText.visibility = View.GONE
        showAuthStatus()
    }

    private fun onCredentialsServerError(result: RemoteOperationResult<UserInfo?>) {
        Log_OC.e(TAG, "Credential check: server error, code=${result.code}, ${result.logMessage}")
        serverInfo = ServerInfo()

        updateServerStatusIconAndText(result)
        showServerStatus()
        authStatusIcon = NO_ICON
        authStatusText = EMPTY_STRING

        if (result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result)
        }
    }

    private fun onCredentialsRejected(result: RemoteOperationResult<UserInfo?>) {
        Log_OC.e(TAG, "Credential check: rejected, code=${result.code}, ${result.logMessage}")
        if (accountSetupWebviewBinding != null) {
            Log_OC.d(TAG, "Login flow v2: restarting flow after rejected credentials")
            anonymouslyPostLoginRequest(serverInfo.mBaseUrl + WEB_LOGIN)
        } else {
            SnackbarUtil.show(this, R.string.auth_access_failed, result.getLogMessage(this))
            updateAuthStatusIconAndText(result)
        }

        resetWebViewCredentials()
    }

    private fun resetWebViewCredentials() {
        webViewPassword = null
        webViewUser = null
        deleteCookies()
    }

    private fun applyLoginCredentials(loginUrlInfo: LoginUrlInfo, password: String = loginUrlInfo.appPassword) {
        serverInfo.mBaseUrl = normalizeUrlSuffix(loginUrlInfo.server)
        webViewUser = loginUrlInfo.loginName
        webViewPassword = password
    }

    private fun dismissWaitingDialog() {
        val dialog = supportFragmentManager.findFragmentByTag(WAIT_DIALOG_TAG) as? DialogFragment ?: return
        try {
            dialog.dismiss()
        } catch (e: IllegalStateException) {
            Log_OC.e(TAG, "Waiting dialog dismiss failed, dismissing with state loss", e)
            dialog.dismissAllowingStateLoss()
        }
    }
    // endregion

    // region Account
    private fun saveAccount(result: RemoteOperationResult<UserInfo?>): Boolean {
        if (authenticatorAction == ACTION_CREATE) {
            return createAccount(result)
        }

        return try {
            updateAccountAuthentication()
            true
        } catch (e: AccountUtils.AccountNotFoundException) {
            Log_OC.e(TAG, "Account: ${account?.name} no longer exists", e)
            SnackbarUtil.show(accountSetupBinding?.scroll, R.string.auth_account_does_not_exist)
            finish()
            false
        }
    }

    private fun fetchCapabilitiesAndFinish(accountName: String) {
        val user = accountManager.getUser(accountName)
        if (!user.isPresent) {
            Log_OC.w(TAG, "Account: $accountName not found, capabilities not fetched")
            endSuccess()
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val storageManager = FileDataStorageManager(user.get(), contentResolver)
                    GetCapabilitiesOperation(storageManager).execute(MainApp.getAppContext())
                } catch (e: Exception) {
                    Log_OC.e(TAG, "Account: failed to fetch capabilities for $accountName", e)
                }
            }
            endSuccess()
        }
    }

    private fun endSuccess() {
        if (!onlyAdd) {
            startActivity(postLoginIntent())
        }
        finish()
    }

    private fun postLoginIntent(): Intent {
        if (MDMConfig.enforceProtection(this) && preferences.lockPreference == SettingsActivity.LOCK_NONE) {
            return Intent(this, SettingsActivity::class.java)
        }

        return Intent(this, FileDisplayActivity::class.java).apply {
            action = FileDisplayActivity.RESTART
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Sets the proper response so that the AccountAuthenticator that started this activity saves a new authorization
     * token for [account].
     *
     * Kills the session kept by OwnCloudClientManager so that a new one will created with the new credentials when
     * needed.
     */
    @Throws(AccountUtils.AccountNotFoundException::class)
    private fun updateAccountAuthentication() {
        val account = checkNotNull(account)
        val response = bundleOf(
            AccountManager.KEY_ACCOUNT_NAME to account.name,
            AccountManager.KEY_ACCOUNT_TYPE to account.type,
            AccountManager.KEY_AUTHTOKEN to webViewPassword
        )
        platformAccountManager.setPassword(account, webViewPassword)

        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(OwnCloudAccount(account, this))

        setAccountAuthenticatorResult(response)
        setResult(RESULT_OK, Intent().putExtras(response))
        Log_OC.d(TAG, "Account: credentials updated for ${account.name}")
    }

    @SuppressFBWarnings("DMI")
    @SuppressLint("TrulyRandom")
    protected fun createAccount(authResult: RemoteOperationResult<UserInfo?>): Boolean {
        val accountType = MainApp.getAccountType(this)

        authResult.lastPermanentLocation?.let { serverInfo.mBaseUrl = trimWebdavSuffix(it) }

        val loginName = webViewUser
        val accountName = AccountUtils.buildAccountName(serverInfo.mBaseUrl.toUri(), loginName)
        val newAccount = Account(accountName, accountType)
        if (accountManager.exists(newAccount)) {
            updateAuthStatusIconAndText(RemoteOperationResult<Any?>(ResultCode.ACCOUNT_NOT_NEW))
            showAuthStatus()
            Log_OC.w(TAG, "Account: $accountName already exists")
            return false
        }

        val userInfo = authResult.resultData
        if (userInfo == null) {
            Log_OC.e(TAG, "Account: cannot create $accountName, user info is missing")
            return false
        }

        account = newAccount
        platformAccountManager.addAccountExplicitly(newAccount, webViewPassword, null)
        platformAccountManager.notifyAccountAuthenticated(newAccount)
        selectAccountIfNoneSelected(accountName)

        val intent = Intent().apply {
            putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType)
            putExtra(AccountManager.KEY_ACCOUNT_NAME, newAccount.name)
            putExtra(AccountManager.KEY_USERDATA, loginName)
        }
        storeAccountUserData(newAccount, userInfo)

        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)

        DocumentsStorageProvider.notifyRootsChanged(this)

        Log_OC.d(TAG, "Account: created $accountName")
        return true
    }

    private fun selectAccountIfNoneSelected(accountName: String) {
        if (!accountManager.user.isAnonymous) {
            return
        }

        @Suppress("DEPRECATION")
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putString(PREF_SELECTED_ACCOUNT_NAME, accountName)
        }
    }

    private fun storeAccountUserData(newAccount: Account, userInfo: UserInfo) {
        platformAccountManager.run {
            setUserData(newAccount, AccountUtils.Constants.KEY_OC_VERSION, serverInfo.mVersion.version)
            setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.mBaseUrl)
            setUserData(newAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.displayName)
            setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, userInfo.id)
            setUserData(
                newAccount,
                AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                UserAccountManager.ACCOUNT_VERSION_WITH_PROPER_ID.toString()
            )
        }
    }
    // endregion

    // region QR code and deep link login
    private val authTabResultLauncher = AuthTabIntent.registerActivityResultLauncher(this) { result ->
        Log_OC.d(TAG, "Login flow v2: Auth Tab closed with result code ${result.resultCode}")
    }

    private val qrScanResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }
            val data = result.data ?: return@registerForActivityResult
            onQrCodeScanned(data.getStringExtra(QR_SCAN_RESULT_EXTRA))
        }

    fun onScan() {
        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startQRScanner()
        } else {
            PermissionUtil.requestCameraPermission(this, PermissionUtil.PERMISSIONS_CAMERA)
        }
    }

    private fun startQRScanner() {
        qrScanResultLauncher.launch(Intent(this, QrCodeActivity::class.java))
    }

    private fun onQrCodeScanned(scannedData: String?) {
        if (scannedData == null || !isLoginData(scannedData)) {
            Log_OC.w(TAG, "QR login: scanned code does not contain login data")
            showServerStatusAlert("QR Code could not be read!")
            return
        }

        if (isAdditionalAccountForbidden) {
            Log_OC.w(TAG, "Login rejected, multiple accounts are not allowed")
            SnackbarUtil.show(this, R.string.no_mutliple_accounts_allowed)
            return
        }

        val oneTimeLoginPrefix = loginDataScheme + PROTOCOL_SUFFIX + ONE_TIME_LOGIN_PATH
        val loginUrlInfo = parseLoginDataUrl(oneTimeLoginPrefix, scannedData)
        loginDialog.showLoginConfirmationDialog(loginUrlInfo, scannedData)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PermissionUtil.PERMISSIONS_CAMERA) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startQRScanner()
        }
    }

    fun onOTPCompleted(loginUrlInfo: LoginUrlInfo, otpResult: String) {
        applyLoginCredentials(loginUrlInfo, otpResult)
        checkOcServer()
    }

    fun onOTPFailed() {
        showServerStatusAlert(getString(R.string.qr_could_not_be_read))
    }

    fun login(loginUrlInfo: LoginUrlInfo) {
        applyLoginCredentials(loginUrlInfo)
        doOnResumeAndBound()
        checkOcServer()
    }
    // endregion

    /**
     * Called when the 'action' button in an IME is pressed ('enter' in software keyboard).
     */
    override fun onEditorAction(inputField: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        val isConfirmAction = actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL
        if (isConfirmAction && inputField != null && inputField == accountSetupBinding?.hostUrlInput) {
            checkOcServer()
        }
        return false // always return false to grant that the software keyboard is hidden anyway
    }

    // region Operations service
    private fun doOnResumeAndBound() {
        val binder = operationsServiceBinder ?: return
        binder.addOperationListener(this, handler)
        if (waitingForOpId <= Int.MAX_VALUE) {
            binder.dispatchResultIfFinished(waitingForOpId.toInt(), this)
        }
    }

    private fun loginFromDeepLink(data: String) {
        try {
            login(parseLoginDataUrl(loginDataPrefix, data))
        } catch (e: Exception) {
            Log_OC.e(TAG, "Login data from deep link could not be parsed: ${e.javaClass.simpleName}")
            showServerStatusAlert(getString(R.string.qr_could_not_be_read))
        }
    }

    private fun onOperationsServiceBound() {
        doOnResumeAndBound()
        if (!isServerCheckPendingServiceBinding) {
            return
        }

        Log_OC.d(TAG, "Login flow v2: operations service bound, running deferred server check")
        isServerCheckPendingServiceBinding = false
        checkOcServer()
    }

    private inner class OperationsServiceConnection : ServiceConnection {
        private val operationsServiceComponent: ComponentName
            get() = ComponentName(this@AuthenticatorActivity, OperationsService::class.java)

        override fun onServiceConnected(component: ComponentName, service: IBinder?) {
            if (component != operationsServiceComponent) {
                return
            }

            operationsServiceBinder = service as OperationsServiceBinder?

            val data = intent?.data?.toString()
            if (data != null && isLoginData(data)) {
                loginFromDeepLink(data)
            } else {
                onOperationsServiceBound()
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component != operationsServiceComponent) {
                return
            }

            Log_OC.e(TAG, "Operations service disconnected unexpectedly")
            operationsServiceBinder = null
        }
    }
    // endregion

    companion object {
        private const val TAG = "AuthenticatorActivity"

        const val EXTRA_ACTION: String = "ACTION"
        const val EXTRA_ACCOUNT: String = "ACCOUNT"
        const val EXTRA_USE_PROVIDER_AS_WEBLOGIN: String = "USE_PROVIDER_AS_WEBLOGIN"

        private const val KEY_HOST_URL_TEXT = "HOST_URL_TEXT"
        private const val KEY_OC_VERSION = "OC_VERSION"
        private const val KEY_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT"
        private const val KEY_SERVER_STATUS_ICON = "SERVER_STATUS_ICON"
        private const val KEY_IS_SSL_CONN = "IS_SSL_CONN"
        private const val KEY_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT"
        private const val KEY_AUTH_STATUS_ICON = "AUTH_STATUS_ICON"
        private const val KEY_SERVER_AUTH_METHOD = "SERVER_AUTH_METHOD"
        private const val KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID"
        private const val KEY_ONLY_ADD = "onlyAdd"
        private const val KEY_LOGIN_FLOW_AUTH_OBJECT = "LOGIN_FLOW_AUTH_OBJECT"
        private const val KEY_AUTH_TOKEN_TYPE = "authTokenType"
        private const val SSO_AUTH_TOKEN_TYPE = "SSO"

        const val ACTION_CREATE: Byte = 0
        const val ACTION_UPDATE_EXPIRED_TOKEN: Byte = 2

        const val UNTRUSTED_CERT_DIALOG_TAG: String = "UNTRUSTED_CERT_DIALOG"
        private const val WAIT_DIALOG_TAG = "WAIT_DIALOG"
        private const val KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT"

        private const val KEY_USERNAME = "USERNAME"
        private const val KEY_PASSWORD = "PASSWORD"
        private const val KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS"

        private const val NO_PENDING_OPERATION = Long.MAX_VALUE
        private const val LOGIN_FLOW_POLL_INTERVAL_MS = 30_000L
        private const val LOGIN_FLOW_POLL_PATH = "/poll"
        private const val JSON_KEY_POLL = "poll"
        private const val JSON_KEY_TOKEN = "token"
        private const val JSON_KEY_LOGIN = "login"
        private const val LOGIN_PATH = "login/"
        private const val ONE_TIME_LOGIN_PATH = "onetime-login/"
        private const val QR_SCAN_RESULT_EXTRA = "com.blikoon.qrcodescanner.got_qr_scan_relult"
        private const val PREF_SELECTED_ACCOUNT_NAME = "select_oc_account"
        private const val HTML_MIME_TYPE = "text/html; charset=UTF-8"

        private const val LOGIN_URL_ELEMENT_SEPARATOR = "&"
        private const val MAX_LOGIN_URL_ELEMENTS = 3

        const val WEB_LOGIN: String = "/index.php/login/v2"

        const val PROTOCOL_SUFFIX: String = "://"
        const val LOGIN_URL_DATA_KEY_VALUE_SEPARATOR: String = ":"
        private const val LOGIN_URL_USER_KEY = "user$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR"
        private const val LOGIN_URL_PASSWORD_KEY = "password$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR"
        private const val LOGIN_URL_SERVER_KEY = "server$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR"
        const val HTTPS_PROTOCOL: String = "https://"
        const val HTTP_PROTOCOL: String = "http://"

        const val NO_ICON: Int = 0
        const val EMPTY_STRING: String = ""
        const val REQUEST_CODE_FIRST_RUN: Int = 102

        private val SERVER_ERROR_MESSAGES = mapOf(
            ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED to R.string.auth_ssl_unverified_server_title,
            ResultCode.BAD_OC_VERSION to R.string.auth_bad_oc_version_title,
            ResultCode.WRONG_CONNECTION to R.string.auth_wrong_connection_title,
            ResultCode.TIMEOUT to R.string.auth_timeout_title,
            ResultCode.INCORRECT_ADDRESS to R.string.auth_incorrect_address_title,
            ResultCode.SSL_ERROR to R.string.auth_ssl_general_error_title,
            ResultCode.UNAUTHORIZED to R.string.auth_unauthorized,
            ResultCode.HOST_NOT_AVAILABLE to R.string.auth_unknown_host_title,
            ResultCode.INSTANCE_NOT_CONFIGURED to R.string.auth_not_configured_title,
            ResultCode.FILE_NOT_FOUND to R.string.auth_incorrect_path_title,
            ResultCode.OAUTH2_ERROR to R.string.auth_oauth_error,
            ResultCode.OAUTH2_ERROR_ACCESS_DENIED to R.string.auth_oauth_error_access_denied,
            ResultCode.UNHANDLED_HTTP_CODE to R.string.auth_unknown_error_http_title,
            ResultCode.MAINTENANCE_MODE to R.string.maintenance_mode,
            ResultCode.UNTRUSTED_DOMAIN to R.string.untrusted_domain
        )

        /**
         * Parses a login URI of the format `<prefix>server:xxx&user:xxx&password:xxx`, where every element is
         * optional.
         *
         * @param prefix     URI beginning, e.g. cloud://login/
         * @param dataString the complete URI
         * @throws IllegalArgumentException when the URI is shorter than [prefix] or has more than three elements
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun parseLoginDataUrl(prefix: String, dataString: String): LoginUrlInfo {
            require(dataString.length >= prefix.length) { "Invalid login URL detected" }

            val values = splitLoginUrlElements(dataString.substring(prefix.length))
            require(values.size in 1..MAX_LOGIN_URL_ELEMENTS) {
                "Illegal number of login URL elements detected: ${values.size}"
            }

            val loginUrlInfo = LoginUrlInfo(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING)
            for (value in values) {
                when {
                    value.startsWith(LOGIN_URL_USER_KEY) ->
                        loginUrlInfo.loginName = decodeLoginUrlValue(value, LOGIN_URL_USER_KEY)

                    value.startsWith(LOGIN_URL_PASSWORD_KEY) ->
                        loginUrlInfo.appPassword = decodeLoginUrlValue(value, LOGIN_URL_PASSWORD_KEY)

                    value.startsWith(LOGIN_URL_SERVER_KEY) ->
                        loginUrlInfo.server = decodeLoginUrlValue(value, LOGIN_URL_SERVER_KEY)
                }
            }
            return loginUrlInfo
        }

        private fun splitLoginUrlElements(data: String): List<String> {
            val elements = data.split(LOGIN_URL_ELEMENT_SEPARATOR)
            if (elements.size == 1) {
                return elements
            }
            return elements.dropLastWhile { it.isEmpty() }
        }

        @Suppress("DEPRECATION")
        private fun decodeLoginUrlValue(value: String, key: String): String =
            URLDecoder.decode(value.substring(key.length))
    }
}
