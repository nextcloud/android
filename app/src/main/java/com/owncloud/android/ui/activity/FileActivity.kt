/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.NetworkChangeListener
import com.nextcloud.client.player.ui.audio.AudioPlayerActivity
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.providers.UsersAndGroupsSearchConfig
import com.owncloud.android.providers.UsersAndGroupsSearchProvider
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.fileactivity.DevVersionChecker
import com.owncloud.android.ui.activity.fileactivity.FileOperationResultHandler
import com.owncloud.android.ui.activity.fileactivity.LoadingDialogController
import com.owncloud.android.ui.activity.fileactivity.OperationsServiceConnector
import com.owncloud.android.ui.activity.fileactivity.ShareOperationResultHandler
import com.owncloud.android.ui.activity.fileactivity.ShareWithIntentHandler
import com.owncloud.android.ui.asynctasks.CheckRemoteWipeTask
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask.VersionDevInterface
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener
import com.owncloud.android.ui.events.DialogEvent
import com.owncloud.android.ui.events.DialogEventType
import com.owncloud.android.ui.fragment.FileDetailFragment
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
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import javax.inject.Inject

@Suppress("TooManyFunctions")
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
    private val operationsServiceConnector = OperationsServiceConnector(this, this, ::doOnResumeAndBound)
    private val loadingDialogController = LoadingDialogController(this)
    private val operationResultHandler = FileOperationResultHandler(this)
    private val shareOperationResultHandler = ShareOperationResultHandler(this)
    private val shareWithIntentHandler = ShareWithIntentHandler(this, ::doShareWith)

    protected val fileUploadHelper: FileUploadHelper = FileUploadHelper.instance()
    protected var isFileDisplayActivityResumed = false

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

        operationsServiceConnector.bind()
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
        operationsServiceConnector.onResume()
    }

    override fun onPause() {
        operationsServiceConnector.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        operationsServiceConnector.unbind()
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

    override fun getOperationsServiceBinder(): OperationsServiceBinder? = operationsServiceConnector.binder

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
        operationResultHandler.onOperationFinish(operation, result)
        shareOperationResultHandler.onOperationFinish(operation, result)
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

    @JvmOverloads
    fun requestCredentialsUpdate(account: Account? = null) {
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

    fun updateFileFromDB() {
        val currentFile = file ?: return
        file = storageManager.getFileByPath(currentFile.remotePath)
    }

    fun showLoadingDialog(message: String?) = loadingDialogController.show(message)

    fun dismissLoadingDialog() = loadingDialogController.dismiss()

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
        if (DevVersionChecker.isCheckNecessary(resources, arbitraryDataProvider)) {
            DevVersionChecker.checkForNewDevVersion(this, context)
        }
    }

    override fun returnVersion(latestVersion: Int?) {
        val version = latestVersion ?: return
        DevVersionChecker.showDevSnackbar(this, version, openDirectly = false, inBackground = true)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != UsersAndGroupsSearchProvider.ACTION_SHARE_WITH) return

        val remotePath = fileFromDetailFragment?.remotePath ?: return
        shareWithIntentHandler.handle(intent, remotePath)
    }

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

        private const val DIALOG_UNTRUSTED_CERT = "DIALOG_UNTRUSTED_CERT"
        private const val DIALOG_CERT_NOT_SAVED = "DIALOG_CERT_NOT_SAVED"

        private const val KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID"
        private const val KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE"

        private const val NO_TITLE_RES = 0
        private const val NO_BUTTON_RES = -1
    }
}
