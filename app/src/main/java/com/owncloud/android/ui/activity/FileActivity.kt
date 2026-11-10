/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.NetworkChangeListener
import com.nextcloud.client.player.ui.audio.AudioPlayerActivity
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.isActive
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.CreateShareViaLinkOperation
import com.owncloud.android.operations.CreateShareWithShareeOperation
import com.owncloud.android.operations.GetSharesForFileOperation
import com.owncloud.android.operations.SetFilesDownloadLimitOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.UnshareOperation
import com.owncloud.android.operations.UpdateNoteForShareOperation
import com.owncloud.android.operations.UpdateShareInfoOperation
import com.owncloud.android.operations.UpdateSharePermissionsOperation
import com.owncloud.android.operations.UpdateShareViaLinkOperation
import com.owncloud.android.providers.UsersAndGroupsSearchConfig
import com.owncloud.android.providers.UsersAndGroupsSearchProvider
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.asynctasks.CheckRemoteWipeTask
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask.VersionDevInterface
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.dialog.ShareLinkToDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener
import com.owncloud.android.ui.events.DialogEvent
import com.owncloud.android.ui.events.DialogEventType
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment.OnEditShareListener
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.ui.fragment.filesRepository.FilesRepository
import com.owncloud.android.ui.fragment.filesRepository.RemoteFilesRepository
import com.owncloud.android.ui.helpers.CredentialsUpdateHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import com.owncloud.android.ui.preview.media.PreviewMediaActivity
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.MimeType
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
abstract class FileActivity :
    DrawerActivity(),
    OnRemoteOperationListener,
    ComponentsGetter,
    OnSslUntrustedCertListener,
    VersionDevInterface,
    OnEditShareListener,
    NetworkChangeListener {

    private var handledFile: OCFile? = null

    open var file: OCFile?
        get() = handledFile
        set(value) {
            handledFile = value
        }

    var isFromNotification = false
        private set

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    open val handler: Handler
        get() = mainThreadHandler

    private lateinit var operationsHelper: FileOperationsHelper
    private var operationsServiceConnection: ServiceConnection? = null
    private var operationsBinder: OperationsServiceBinder? = null
    private var isActivityResumed = false

    protected val fileUploadHelper: FileUploadHelper = FileUploadHelper.instance()
    protected var isFileDisplayActivityResumed = false

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var editorUtils: EditorUtils

    @Inject
    lateinit var usersAndGroupsSearchConfig: UsersAndGroupsSearchConfig

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    lateinit var filesRepository: FilesRepository
        private set

    private val shareFileFragment: FileDetailSharingFragment?
        get() {
            val fragment = supportFragmentManager.findFragmentByTag(ShareActivity.TAG_SHARE_FRAGMENT)
                ?: supportFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)

            return when (fragment) {
                is FileDetailSharingFragment -> fragment
                is FileDetailFragment -> fragment.fileDetailSharingFragment
                else -> null
            }
        }

    private val fileDetailFragment: FileDetailFragment?
        get() = supportFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES) as? FileDetailFragment

    private val fileFromDetailFragment: OCFile?
        get() {
            val fragment = fileDetailFragment ?: return file
            return fragment.file
        }

    override fun showFiles(onDeviceOnly: Boolean, onlyPersonalFiles: Boolean) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly)
        MainApp.showOnlyPersonalFiles(onlyPersonalFiles)
        showActionBar(if (onDeviceOnly) ActionBarStyle.Plain else ActionBarStyle.Search)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usersAndGroupsSearchConfig.reset()
        operationsHelper = FileOperationsHelper(this, userAccountManager, connectivityService, editorUtils)

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        } else {
            readIntentExtras()
        }

        bindOperationsService()
        filesRepository = RemoteFilesRepository(clientRepository, this)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        handledFile = savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        isFromNotification = savedInstanceState.getBoolean(EXTRA_FROM_NOTIFICATION)
        operationsHelper.opIdWaitingFor = savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)

        val actionBar = supportActionBar ?: return
        if (this is PreviewMediaActivity) return
        val actionBarTitle = savedInstanceState.getString(KEY_ACTION_BAR_TITLE) ?: return
        viewThemeUtils.files.themeActionBar(this, actionBar, actionBarTitle)
    }

    private fun readIntentExtras() {
        val user = intent.getParcelableArgument(EXTRA_USER, User::class.java)
        handledFile = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        isFromNotification = intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)
        user?.let { setUser(it) }
    }

    private fun bindOperationsService() {
        val connection = OperationsServiceConnection()
        operationsServiceConnection = connection
        bindService(Intent(this, OperationsService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun networkAndServerConnectionListener(isNetworkAndServerAvailable: Boolean) {
        when {
            isNetworkAndServerAvailable -> {
                hideInfoBox()
                if (!isFileDisplayActivityResumed) {
                    refreshList()
                }
            }

            this is AudioPlayerActivity -> hideInfoBox()

            else -> showInfoBox(R.string.offline_mode)
        }
    }

    override fun onStart() {
        super.onStart()
        connectivityService.addListener(this)
        fetchExternalLinks(false)
    }

    override fun onStop() {
        super.onStop()
        connectivityService.removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        operationsBinder?.let { doOnResumeAndBound(it) }
    }

    override fun onPause() {
        operationsBinder?.removeOperationListener(this)
        isActivityResumed = false
        super.onPause()
    }

    override fun onDestroy() {
        operationsServiceConnection?.let {
            unbindService(it)
            operationsBinder = null
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        handledFile.logFileSize(TAG)
        outState.run {
            putParcelable(EXTRA_FILE, handledFile)
            putBoolean(EXTRA_FROM_NOTIFICATION, isFromNotification)
            putLong(KEY_WAITING_FOR_OP_ID, operationsHelper.opIdWaitingFor)
            supportActionBar?.title?.let { putString(KEY_ACTION_BAR_TITLE, it.toString()) }
        }
    }

    override fun getOperationsServiceBinder(): OperationsServiceBinder? = operationsBinder

    protected open fun newTransferenceServiceConnection(): ServiceConnection? = null

    override fun getFileOperationsHelper(): FileOperationsHelper = operationsHelper

    override fun getFileUploaderHelper(): FileUploadHelper = fileUploadHelper

    fun getCurrentDir(): OCFile? {
        val currentFile = file ?: return null
        if (currentFile.isFolder) return currentFile
        return storageManager?.getFileByPath(currentFile.parentRemotePath)
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        Log_OC.d(TAG, "Received result of operation in FileActivity - common behaviour for all the FileActivities ")
        operationsHelper.opIdWaitingFor = Long.MAX_VALUE
        dismissLoadingDialog()
        onCommonOperationFinish(operation, result)
        onShareOperationFinish(operation, result)
    }

    private fun onCommonOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when {
            result.isAuthenticationFailure() -> onAuthenticationFailure(operation, result)
            !result.isSuccess && result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED ->
                showUntrustedCertDialog(result)
            operation == null || operation is SynchronizeFolderOperation ->
                onSynchronizeFolderOperationFinish(operation, result)
            operation is SynchronizeFileOperation -> onSynchronizeFileOperationFinish(operation, result)
            operation is GetSharesForFileOperation -> onGetSharesForFileOperationFinish(operation, result)
        }
    }

    private fun onShareOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when (operation) {
            is CreateShareViaLinkOperation -> onCreateShareViaLinkOperationFinish(operation, result)
            is CreateShareWithShareeOperation -> onUpdateShareInformation(result, R.string.sharee_add_failed)
            is UpdateShareViaLinkOperation,
            is UpdateShareInfoOperation,
            is UpdateSharePermissionsOperation -> onUpdateShareInformation(result, R.string.updating_share_failed)
            is SetFilesDownloadLimitOperation -> onUpdateShareInformation(result, R.string.set_download_limit_failed)
            is UnshareOperation -> onUpdateShareInformation(result, R.string.unsharing_failed)
            is UpdateNoteForShareOperation -> onUpdateNoteForShareOperationFinish(result)
        }
    }

    private fun RemoteOperationResult<*>.isAuthenticationFailure(): Boolean =
        !isSuccess && (code == ResultCode.UNAUTHORIZED || (isException && exception is AuthenticatorException))

    private fun onAuthenticationFailure(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        requestCredentialsUpdate()
        if (result.code == ResultCode.UNAUTHORIZED) {
            showErrorCause(operation, result)
        }
    }

    private fun onSynchronizeFolderOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when {
            result.isSuccess -> updateFileFromDB()
            result.code != ResultCode.CANCELLED -> showErrorCause(operation, result)
        }
    }

    private fun onGetSharesForFileOperationFinish(
        operation: GetSharesForFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess || result.code == ResultCode.SHARE_NOT_FOUND) {
            updateFileFromDB()
        } else {
            showErrorCause(operation, result)
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            if (!operation.transferWasRequested) {
                showErrorCause(operation, result)
            }
            supportInvalidateOptionsMenu()
            return
        }

        if (result.code != ResultCode.SYNC_CONFLICT) return

        val currentUser = user.orElseThrow { RuntimeException() }
        val intent = ConflictsResolveActivity.createIntent(
            operation.localFile,
            currentUser,
            NO_CONFLICT_UPLOAD_ID,
            null,
            this
        )
        startActivity(intent)
    }

    private fun showErrorCause(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        SnackbarUtil.show(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources))
    }

    @JvmOverloads
    protected fun requestCredentialsUpdate(account: Account? = null) {
        val targetAccount = account ?: this.account
        CheckRemoteWipeTask(backgroundJobManager, targetAccount, WeakReference(this)).execute()
    }

    fun performCredentialsUpdate(account: Account) {
        val credentialsUpdateHelper = CredentialsUpdateHelper(this)
        try {
            credentialsUpdateHelper.invalidateCredentials(account)
            startActivityForResult(
                credentialsUpdateHelper.createUpdateCredentialsIntent(account),
                REQUEST_CODE__UPDATE_CREDENTIALS
            )
        } catch (e: AccountUtils.AccountNotFoundException) {
            Log_OC.e(TAG, "Account not found while updating credentials", e)
            SnackbarUtil.show(this, R.string.auth_account_does_not_exist)
        }
    }

    fun showUntrustedCertDialog(result: RemoteOperationResult<*>) {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag(DIALOG_UNTRUSTED_CERT) != null) return

        SslUntrustedCertDialog
            .newInstanceForFullSslError(result.exception as CertificateCombinedException?)
            .show(fragmentManager.beginTransaction(), DIALOG_UNTRUSTED_CERT)
    }

    protected fun updateFileFromDB() {
        val currentFile = file ?: return
        file = storageManager.getFileByPath(currentFile.remotePath)
    }

    fun showLoadingDialog(message: String?) {
        runOnUiThread {
            if (!isActive()) {
                Log_OC.w(TAG, "cannot show loading dialog, activity is finishing")
                return@runOnUiThread
            }

            val fragmentManager = supportFragmentManager
            fragmentManager.executePendingTransactions()
            fragmentManager.dismissExistingLoadingDialog("dismiss previous loading dialog")

            if (fragmentManager.isStateSaved) return@runOnUiThread

            Log_OC.d(TAG, "show loading dialog")
            LoadingDialog.newInstance(message).show(fragmentManager, DIALOG_WAIT_TAG)
        }
    }

    fun dismissLoadingDialog() {
        runOnUiThread {
            if (!isActive()) {
                Log_OC.w(TAG, "cannot dismiss loading dialog, activity is finishing")
                return@runOnUiThread
            }

            val fragmentManager = supportFragmentManager
            fragmentManager.executePendingTransactions()
            fragmentManager.dismissExistingLoadingDialog("dismiss loading dialog")
        }
    }

    private fun FragmentManager.dismissExistingLoadingDialog(logMessage: String) {
        val loadingDialog = findFragmentByTag(DIALOG_WAIT_TAG) as? LoadingDialog ?: return
        Log_OC.d(TAG, logMessage)

        if (isStateSaved) {
            loadingDialog.dismissAllowingStateLoss()
        } else {
            loadingDialog.dismiss()
        }
    }

    fun showSyncLoadingDialog(isFolder: Boolean) {
        if (isFolder) return
        showLoadingDialog(applicationContext.getString(R.string.wait_a_moment))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSyncDialogEvent(event: DialogEvent) {
        if (event.type == DialogEventType.SYNC) {
            dismissLoadingDialog()
        }
    }

    private fun doOnResumeAndBound(binder: OperationsServiceBinder) {
        binder.addOperationListener(this, mainThreadHandler)

        val waitingForOpId = operationsHelper.opIdWaitingFor
        if (waitingForOpId > Int.MAX_VALUE) return

        val isOperationDispatchedOrPending = binder.dispatchResultIfFinished(waitingForOpId.toInt(), this)
        if (!isOperationDispatchedOrPending) {
            dismissLoadingDialog()
        }
    }

    private inner class OperationsServiceConnection : ServiceConnection {
        private val operationsServiceComponent = ComponentName(this@FileActivity, OperationsService::class.java)

        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component != operationsServiceComponent) return

            Log_OC.d(TAG, "Operations service connected")
            val binder = service as OperationsServiceBinder
            operationsBinder = binder
            if (isActivityResumed) {
                doOnResumeAndBound(binder)
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component != operationsServiceComponent) return

            Log_OC.d(TAG, "Operations service disconnected")
            operationsBinder = null
        }
    }

    override fun onSavedCertificate() = Unit

    override fun onFailedSavingCertificate() {
        ConfirmationDialogFragment.newInstance(
            messageResId = R.string.ssl_validator_not_saved,
            messageArguments = emptyArray(),
            titleResId = NO_TITLE_RES,
            positiveButtonTextId = R.string.common_ok,
            negativeButtonTextId = NO_BUTTON_RES,
            neutralButtonTextId = NO_BUTTON_RES
        ).show(supportFragmentManager, DIALOG_CERT_NOT_SAVED)
    }

    fun checkForNewDevVersionNecessary(context: Context) {
        if (!resources.getBoolean(R.bool.dev_version_direct_download_enabled)) return

        val appOpenedCount = arbitraryDataProvider.getIntegerValue(FilesSyncHelper.GLOBAL, APP_OPENED_COUNT)
        if (appOpenedCount > MIN_APP_OPENED_COUNT_FOR_DEV_VERSION_CHECK || appOpenedCount == UNKNOWN_VALUE) {
            checkForNewDevVersion(this, context)
        }
    }

    override fun returnVersion(latestVersion: Int?) {
        val version = latestVersion ?: return
        showDevSnackbar(this, version, openDirectly = false, inBackground = true)
    }

    private fun onUpdateNoteForShareOperationFinish(result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            SnackbarUtil.show(this, R.string.note_could_not_sent)
            return
        }
        shareFileFragment?.onUpdateShareInformation(result)
    }

    private fun onUpdateShareInformation(result: RemoteOperationResult<*>, @StringRes defaultError: Int) {
        val sharingFragment = shareFileFragment

        if (result.isSuccess) {
            updateFileFromDB()
            sharingFragment?.onUpdateShareInformation(result)
            return
        }

        val message = result.message.takeUnless { it.isNullOrEmpty() } ?: getString(defaultError)
        val sharingFragmentView = sharingFragment?.view
        if (sharingFragmentView == null) {
            SnackbarUtil.show(this, message)
            return
        }

        Snackbar.make(sharingFragmentView, message, Snackbar.LENGTH_LONG).showThemed()
    }

    fun refreshList() {
        when {
            isAlbumsFragment -> getFragment(AlbumsFragment.TAG, AlbumsFragment::class.java)
                .ifPresent { it.refreshAlbums() }

            isAlbumItemsFragment -> getFragment(AlbumItemsFragment.TAG, AlbumItemsFragment::class.java)
                .ifPresent { it.refreshData() }

            else -> refreshListOfFilesFragment()
        }
    }

    private fun refreshListOfFilesFragment() {
        when (val fragment = supportFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)) {
            is OCFileListFragment -> fragment.onRefresh()
            is FileDetailFragment -> fragment.goBackToOCFileListFragment()
        }
    }

    private fun onCreateShareViaLinkOperationFinish(
        operation: CreateShareViaLinkOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            onShareViaLinkCreated(result)
        } else {
            onShareViaLinkFailed(operation, result)
        }
    }

    private fun onShareViaLinkCreated(result: RemoteOperationResult<*>) {
        val sharingFragment = shareFileFragment
        val fileListFragment = supportFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)
        updateFileFromDB()

        val publicLinkShare = result.data
            .filterIsInstance<OCShare>()
            .firstOrNull { FileDisplayActivity.TAG_PUBLIC_LINK.equals(it.shareType?.name, ignoreCase = true) }
        val sharedFile = publicLinkShare?.let { storageManager.getFileByEncryptedRemotePath(it.path) }

        if (publicLinkShare != null && sharedFile != null) {
            copyAndShareFileLink(this, sharedFile, publicLinkShare.shareLink)
        }
        sharingFragment?.onUpdateShareInformation(result, sharedFile)

        if (fileListFragment !is OCFileListFragment || sharedFile == null) return

        if (fileListFragment.adapterFiles.contains(sharedFile)) {
            fileListFragment.updateOCFile(sharedFile)
        } else {
            SnackbarUtil.show(this, R.string.file_activity_shared_file_cannot_be_updated)
        }
    }

    private fun onShareViaLinkFailed(operation: CreateShareViaLinkOperation, result: RemoteOperationResult<*>) {
        val sharingFragment = shareFileFragment
        val isForbiddenWithoutPassword =
            result.code == ResultCode.SHARE_FORBIDDEN && operation.password.isNullOrEmpty()

        if (!isForbiddenWithoutPassword) {
            sharingFragment?.refreshSharesFromDB()
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources)
            Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG).showThemed()
            return
        }

        val capability = capabilities.orElse(null) ?: return
        if (!capability.filesSharingPublicEnabled.isUnknown) return
        if (sharingFragment == null || !sharingFragment.isAdded) return

        sharingFragment.requestPasswordForShareViaLink(true, capability.filesSharingPublicAskForOptionalPassword.isTrue)
    }

    private fun Snackbar.showThemed() {
        viewThemeUtils.material.themeSnackbar(this)
        show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == UsersAndGroupsSearchProvider.ACTION_SHARE_WITH) {
            onShareWithIntent(intent)
        }
    }

    private fun onShareWithIntent(intent: Intent) {
        val data = intent.data ?: return
        val remotePath = fileFromDetailFragment?.remotePath ?: return
        val shareWith = data.toString().substringAfterLast('/')
        val shareType = UsersAndGroupsSearchProvider.getShareType(data.authority)

        val isAlreadyShared = storageManager
            .getSharesWithForAFile(remotePath, account.name)
            .any { shareeKey(it.shareType, it.shareWith) == shareeKey(shareType, shareWith) }

        if (isAlreadyShared) {
            SnackbarUtil.show(this, getString(R.string.sharee_already_added_to_file))
        } else {
            doShareWith(shareWith, shareType)
        }
    }

    private fun shareeKey(shareType: ShareType?, shareWith: String?): String = "${shareType}_$shareWith"

    protected open fun doShareWith(shareeName: String, shareType: ShareType) {
        fileDetailFragment?.initiateSharingProcess(shareeName, shareType, usersAndGroupsSearchConfig.searchOnlyUsers)
    }

    override fun editExistingShare(share: OCShare, screenTypePermission: Int, isReshareShown: Boolean) {
        fileDetailFragment?.editExistingShare(share, screenTypePermission, isReshareShown)
    }

    override fun onShareProcessClosed() {
        fileDetailFragment?.showHideFragmentView(false)
    }

    companion object {
        const val EXTRA_FILE = "com.owncloud.android.ui.activity.FILE"
        const val EXTRA_FILE_REMOTE_PATH = "com.owncloud.android.ui.activity.FILE_REMOTE_PATH"
        const val EXTRA_LIVE_PHOTO_FILE = "com.owncloud.android.ui.activity.LIVE.PHOTO.FILE"
        const val EXTRA_USER = "com.owncloud.android.ui.activity.USER"
        const val EXTRA_FROM_NOTIFICATION = "com.owncloud.android.ui.activity.FROM_NOTIFICATION"
        const val APP_OPENED_COUNT = "APP_OPENED_COUNT"
        const val EXTRA_SEARCH = "com.owncloud.android.ui.activity.SEARCH"
        const val EXTRA_SEARCH_QUERY = "com.owncloud.android.ui.activity.SEARCH_QUERY"

        @JvmField
        val TAG: String = FileActivity::class.java.simpleName

        const val REQUEST_CODE__UPDATE_CREDENTIALS = 0
        const val REQUEST_CODE__LAST_SHARED = REQUEST_CODE__UPDATE_CREDENTIALS

        private const val DIALOG_WAIT_TAG = "DIALOG_WAIT"
        private const val DIALOG_UNTRUSTED_CERT = "DIALOG_UNTRUSTED_CERT"
        private const val DIALOG_CERT_NOT_SAVED = "DIALOG_CERT_NOT_SAVED"

        private const val KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID"
        private const val KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE"

        private const val NO_CONFLICT_UPLOAD_ID = -1L
        private const val NO_TITLE_RES = 0
        private const val NO_BUTTON_RES = -1
        private const val UNKNOWN_VALUE = -1
        private const val MIN_APP_OPENED_COUNT_FOR_DEV_VERSION_CHECK = 10
        private const val DEV_APK_EXTENSION = ".apk"

        @JvmStatic
        fun checkForNewDevVersion(callback: VersionDevInterface?, context: Context) {
            LoadingVersionNumberTask(callback).execute(context.getString(R.string.dev_latest))
        }

        @JvmStatic
        fun showDevSnackbar(activity: Activity, latestVersion: Int, openDirectly: Boolean, inBackground: Boolean) {
            val currentVersion = activity.installedVersionCode()

            if (latestVersion == UNKNOWN_VALUE || currentVersion == UNKNOWN_VALUE) {
                SnackbarUtil.show(activity, R.string.dev_version_no_information_available)
            }

            if (latestVersion <= currentVersion) {
                if (!inBackground) {
                    SnackbarUtil.show(activity, R.string.dev_version_no_new_version_available)
                }
                return
            }

            val devApkLink = activity.getString(R.string.dev_link) + latestVersion + DEV_APK_EXTENSION
            if (openDirectly) {
                IntentUtil.startLinkIntent(activity, devApkLink)
                return
            }

            Snackbar
                .make(
                    activity.findViewById(android.R.id.content),
                    R.string.dev_version_new_version_available,
                    Snackbar.LENGTH_LONG
                )
                .setAction(activity.getString(R.string.version_dev_download)) {
                    IntentUtil.startLinkIntent(activity, devApkLink)
                }
                .show()
        }

        @Suppress("DEPRECATION")
        private fun Activity.installedVersionCode(): Int = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log_OC.e(TAG, "Package not found", e)
            UNKNOWN_VALUE
        }

        fun copyAndShareFileLink(activity: FileActivity, file: OCFile, link: String?) {
            if (!MDMConfig.shareViaLink(activity) || !MDMConfig.clipBoardSupport(activity)) return

            ClipboardUtil.copyToClipboard(activity, link, false)
            Snackbar
                .make(activity.findViewById(android.R.id.content), R.string.clipboard_text_copied, Snackbar.LENGTH_LONG)
                .setAction(R.string.share) { showShareLinkDialog(activity, file, link) }
                .also { activity.viewThemeUtils.material.themeSnackbar(it) }
                .show()
        }

        fun showShareLinkDialog(activity: FileActivity, file: ServerFileInterface, link: String?) {
            val username = activity.sharingUsername()
            val subject = if (username != null) {
                activity.getString(R.string.subject_user_shared_with_you, username, file.fileName)
            } else {
                activity.getString(R.string.subject_shared_with_you, file.fileName)
            }

            val intentToShareLink = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, link)
                type = MimeType.TEXT_PLAIN
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            ShareLinkToDialog
                .newInstance(intentToShareLink, activity.packageName)
                .show(activity.supportFragmentManager, FileDisplayActivity.FTAG_CHOOSER_DIALOG)
        }

        @Suppress("TooGenericExceptionCaught")
        private fun FileActivity.sharingUsername(): String? {
            val displayName = try {
                OwnCloudAccount(account, this).displayName
            } catch (e: Exception) {
                Log_OC.e(TAG, "Could not read account display name", e)
                null
            }
            return displayName.takeUnless { it.isNullOrEmpty() } ?: AccountUtils.getUsernameForAccount(account)
        }
    }
}
