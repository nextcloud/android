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
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.client.onboarding.OnboardingService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.mdm.MDMConfig
import com.nextcloud.utils.text.LinkFormatter
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.server.AuthenticatorUrlUtils.normalizeScheme
import com.owncloud.android.authentication.server.AuthenticatorUrlUtils.normalizeUrlSuffix
import com.owncloud.android.authentication.server.AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles
import com.owncloud.android.authentication.server.AuthenticatorUrlUtils.trimWebdavSuffix
import com.owncloud.android.authentication.server.EnforcedServerPolicy
import com.owncloud.android.authentication.status.AuthenticatorStatus
import com.owncloud.android.authentication.status.AuthenticatorStatusMapper
import com.owncloud.android.authentication.account.AuthenticatorAccountStore
import com.owncloud.android.authentication.account.CredentialsVerifier
import com.owncloud.android.authentication.dialog.LoginDialog
import com.owncloud.android.authentication.loginflow.LoginBrowserLauncher
import com.owncloud.android.authentication.loginflow.LoginFlowV2
import com.owncloud.android.authentication.signup.SignupWebViewLogin
import com.owncloud.android.authentication.loginurl.LoginDataUrlParser
import com.owncloud.android.authentication.loginurl.LoginUrlInfo
import com.owncloud.android.databinding.AccountSetupBinding
import com.owncloud.android.databinding.AccountSetupWebviewBinding
import com.owncloud.android.datamodel.FileDataStorageManager
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
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    lateinit var clientRepository: ClientRepository

    @get:VisibleForTesting
    var accountSetupBinding: AccountSetupBinding? = null
        private set
    private var accountSetupWebviewBinding: AccountSetupWebviewBinding? = null

    protected val loginDialog = LoginDialog(this)
    private lateinit var viewThemeUtils: ViewThemeUtils
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val statusMapper by lazy { AuthenticatorStatusMapper(resources) }
    private val accountStore by lazy { AuthenticatorAccountStore(this, AccountManager.get(this), accountManager) }
    private val loginFlow by lazy { LoginFlowV2(clientRepository, lifecycleScope, ::completeLoginFlow) }

    private var authenticatorAction = ACTION_CREATE
    private var account: Account? = null
    private var onlyAdd = false
    private var isFirstAuthAttempt = false
    private var waitingForOpId = NO_PENDING_OPERATION

    private var operationsServiceConnection: ServiceConnection? = null
    private var operationsServiceBinder: OperationsServiceBinder? = null
    private var credentialsCheckJob: Job? = null

    private var serverInfo = ServerInfo()
    private var serverStatus = AuthenticatorStatus.NONE
    private var authStatus = AuthenticatorStatus.NONE

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
        loginFlow.pendingAuthObjectJson?.let { outState.putString(KEY_LOGIN_FLOW_AUTH_OBJECT, it) }

        credentialsCheckJob?.cancel()
        outState.putBoolean(KEY_CREDENTIALS_CHECK_IN_PROGRESS, credentialsCheckJob != null)
        credentialsCheckJob = null
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.getBoolean(KEY_CREDENTIALS_CHECK_IN_PROGRESS)) {
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
            startSignupWebViewLogin()
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
            authStatus = AuthenticatorStatus(it.getInt(KEY_AUTH_STATUS_ICON), it.getString(KEY_AUTH_STATUS_TEXT))
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

        val enforcedServers = EnforcedServerPolicy.parse(getString(R.string.enforce_servers))
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

        serverInfo.mBaseUrl = accountStore.baseUrl(account)
        serverInfo.mIsSslConn = serverInfo.mBaseUrl.startsWith(HTTPS_PROTOCOL)
        serverInfo.mVersion = accountManager.getServerVersion(account)
    }

    private fun restoreServerInfo(savedInstanceState: Bundle) {
        serverStatus = AuthenticatorStatus(
            savedInstanceState.getInt(KEY_SERVER_STATUS_ICON),
            savedInstanceState.getString(KEY_SERVER_STATUS_TEXT)
        )

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
    private var isRedirectedToTheDefaultBrowser = false
    private var isServerCheckPendingServiceBinding = false

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START && loginFlow.hasPollToken) {
            Log_OC.d(TAG, "Login flow v2: app is in foreground, polling started")
            loginFlow.poll()
        }
    }

    private fun stopLoginFlowPolling() {
        loginFlow.stopPolling()
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
        Log_OC.d(TAG, "Login flow v2: requesting login URL from $url")

        lifecycleScope.launch {
            val response = loginFlow.requestLoginResponse(url)
            if (response.isEmpty()) {
                Log_OC.e(TAG, "Login flow v2: login URL request returned an empty response")
                SnackbarUtil.show(this@AuthenticatorActivity, R.string.authenticator_activity_empty_response_message)
                return@launch
            }

            val loginUrl = loginFlow.parseLoginUrl(response) ?: defaultLoginUrl()
            initLoginInfoView()
            loginBrowserLauncher.open(loginUrl)
        }
    }

    private fun defaultLoginUrl(): String {
        Log_OC.e(TAG, "Login flow v2: no login URL in response, using default login URL")
        SnackbarUtil.show(this, R.string.authenticator_activity_login_error)
        return getString(R.string.webview_login_url)
    }

    private fun completeLoginFlow(response: String, status: Int) {
        try {
            val loginUrlInfo: LoginUrlInfo? = gson.fromJson(response, LoginUrlInfo::class.java)
            if (loginUrlInfo == null) {
                Log_OC.e(TAG, "Login flow v2: cannot complete, poll response has no credentials")
                return
            }
            loginFlow.isCompleted = loginUrlInfo.isValid(status)
            Log_OC.d(
                TAG,
                "Login flow v2: credentials received for ${loginUrlInfo.server}, valid=${loginFlow.isCompleted}"
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

        loginFlow.restore(authObjectJson)
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
        loginFlow.clearPendingLogin()
        recreate()
    }
    // endregion

    // region WebView login
    private fun startSignupWebViewLogin() {
        val binding = inflateWebViewLayout()
        SignupWebViewLogin(
            activity = this,
            viewThemeUtils = viewThemeUtils,
            currentBinding = { accountSetupWebviewBinding },
            loginDataPrefix = loginDataPrefix,
            onLoginDataUrl = ::parseAndLoginFromWebView
        ).start(binding)
    }

    fun parseAndLoginFromWebView(dataString: String) {
        try {
            val loginUrlInfo = LoginDataUrlParser.parse(loginDataPrefix, dataString)

            val isServerAllowed = EnforcedServerPolicy.isAllowed(
                server = loginUrlInfo.server,
                webViewLoginUrl = getString(R.string.webview_login_url),
                enforcedServersJson = getString(R.string.enforce_servers)
            )
            if (!isServerAllowed) {
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
            serverStatus = AuthenticatorStatus(R.drawable.progress_small, getString(R.string.auth_testing_connection))
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
            updateServerStatus(result)
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

        if (loginFlow.isCompleted) {
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
    private val isHostUrlPlainHttp: Boolean
        get() = accountSetupBinding?.hostUrlInput?.text
            ?.toString()
            ?.trim { it <= ' ' }
            ?.lowercase()
            ?.startsWith(HTTP_PROTOCOL) == true

    private fun updateServerStatus(result: RemoteOperationResult<*>) {
        serverStatus = statusMapper.forServerCheck(result, isHostUrlPlainHttp)
    }

    private fun updateAuthStatus(result: RemoteOperationResult<*>) {
        authStatus = statusMapper.forAuthentication(result, isHostUrlPlainHttp, showWebViewLoginUrl, authStatus.text)

        if (!showWebViewLoginUrl && statusMapper.isFatalAuthenticationError(result)) {
            SnackbarUtil.show(this, authStatus.text)
            finish()
        }
    }

    private fun showServerStatusAlert(message: String) {
        serverStatus = AuthenticatorStatus(R.drawable.ic_alert, message)
        showServerStatus()
    }

    private fun showServerStatus() {
        val binding = accountSetupBinding ?: return
        showStatus(binding.serverStatusText, serverStatus)
    }

    private fun showAuthStatus() {
        val binding = accountSetupBinding ?: return
        showStatus(binding.authStatusText, authStatus)
    }

    private fun showStatus(statusView: TextView, status: AuthenticatorStatus) {
        if (status.isNone) {
            statusView.visibility = View.INVISIBLE
            return
        }

        statusView.text = status.text
        statusView.setCompoundDrawablesWithIntrinsicBounds(status.icon, 0, 0, 0)
        statusView.visibility = View.VISIBLE
    }
    // endregion

    // region Credentials
    private fun checkBasicAuthorization(webViewUsername: String?, webViewPassword: String?) {
        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(webViewUsername, webViewPassword)
        accessRootFolder(credentials)
    }

    private fun accessRootFolder(credentials: OwnCloudCredentials) {
        val baseUrl = serverInfo.mBaseUrl
        Log_OC.d(TAG, "Credential check: started for $baseUrl")
        credentialsCheckJob = lifecycleScope.launch {
            val result = CredentialsVerifier(applicationContext).verify(baseUrl, credentials)
            onCredentialsChecked(result)
        }
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
        authStatus = AuthenticatorStatus(R.drawable.ic_alert, getString(statusText))
        showAuthStatus()
        Log_OC.e(TAG, "User info: request failed, code=${result.code}, ${result.logMessage}")
    }

    private fun onCredentialsChecked(result: RemoteOperationResult<UserInfo?>) {
        waitingForOpId = NO_PENDING_OPERATION
        dismissWaitingDialog()
        credentialsCheckJob = null

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

        updateServerStatus(result)
        showServerStatus()
        authStatus = AuthenticatorStatus.NONE

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
            updateAuthStatus(result)
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
     */
    @Throws(AccountUtils.AccountNotFoundException::class)
    private fun updateAccountAuthentication() {
        val account = checkNotNull(account)
        val response = accountStore.updateCredentials(account, webViewPassword)

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
            updateAuthStatus(RemoteOperationResult<Any?>(ResultCode.ACCOUNT_NOT_NEW))
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
        val authenticatorResult = accountStore.addAccount(newAccount, webViewPassword, loginName, serverInfo, userInfo)

        setAccountAuthenticatorResult(authenticatorResult.extras)
        setResult(RESULT_OK, authenticatorResult)

        DocumentsStorageProvider.notifyRootsChanged(this)

        Log_OC.d(TAG, "Account: created $accountName")
        return true
    }
    // endregion

    // region QR code and deep link login
    private val loginBrowserLauncher = LoginBrowserLauncher(this)

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
        val loginUrlInfo = LoginDataUrlParser.parse(oneTimeLoginPrefix, scannedData)
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
            login(LoginDataUrlParser.parse(loginDataPrefix, data))
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
        private const val KEY_CREDENTIALS_CHECK_IN_PROGRESS = "AUTH_IN_PROGRESS"

        private const val NO_PENDING_OPERATION = Long.MAX_VALUE
        private const val LOGIN_PATH = "login/"
        private const val ONE_TIME_LOGIN_PATH = "onetime-login/"
        private const val QR_SCAN_RESULT_EXTRA = "com.blikoon.qrcodescanner.got_qr_scan_relult"

        const val WEB_LOGIN: String = "/index.php/login/v2"

        const val PROTOCOL_SUFFIX: String = "://"
        const val HTTPS_PROTOCOL: String = "https://"
        const val HTTP_PROTOCOL: String = "http://"

        const val EMPTY_STRING: String = ""
        const val REQUEST_CODE_FIRST_RUN: Int = 102
    }
}
