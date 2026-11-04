/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.view.Menu
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemManager
import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.nextcloud.ui.dialog.account.ChooseAccountDialogFragment
import com.nextcloud.utils.avatar.AvatarGenerationListener
import com.nextcloud.utils.avatar.AvatarGenerator
import com.nextcloud.utils.extensions.getSelectedMenuItemId
import com.nextcloud.utils.extensions.highlightNavigationView
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.ExternalLinksProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ManageAccountsActivity
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.activity.drawer.avatar.DrawerAvatarGenerationListener
import com.owncloud.android.ui.activity.drawer.back.DrawerOnBackPressedCallback
import com.owncloud.android.ui.activity.drawer.externallinks.DrawerExternalLinksMenu
import com.owncloud.android.ui.activity.drawer.externallinks.ExternalLinkOpener
import com.owncloud.android.ui.activity.drawer.externallinks.ExternalLinksSynchronizer
import com.owncloud.android.ui.activity.drawer.fragment.DrawerFragmentFinder
import com.owncloud.android.ui.activity.drawer.header.DrawerHeaderController
import com.owncloud.android.ui.activity.drawer.navigation.DrawerDeepLinkHandler
import com.owncloud.android.ui.activity.drawer.navigation.DrawerMenuFilter
import com.owncloud.android.ui.activity.drawer.navigation.DrawerMenuItemClickHandler
import com.owncloud.android.ui.activity.drawer.navigation.DrawerNavigator
import com.owncloud.android.ui.activity.drawer.quota.DrawerQuotaController
import com.owncloud.android.ui.events.AccountRemovedEvent
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.navigation.NavigatorScreen
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Optional
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

abstract class DrawerActivity :
    ToolbarActivity(),
    AvatarGenerationListener by DrawerAvatarGenerationListener(),
    Injectable {

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var avatarGenerator: AvatarGenerator

    @Inject
    lateinit var colorUtil: ColorUtil

    @JvmField
    protected var mDrawerToggle: ActionBarDrawerToggle? = null

    var bottomNavigationView: BottomNavigationView? = null
        private set

    lateinit var ecosystemManager: EcosystemManager
        private set

    internal var drawerNavigationView: NavigationView? = null
        private set

    internal lateinit var externalLinksProvider: ExternalLinksProvider
        private set

    private var drawerLayout: DrawerLayout? = null
    private var navigationViewHeader: View? = null
    private var pendingRunnable: Runnable? = null
    private lateinit var externalLinksSynchronizer: ExternalLinksSynchronizer

    private val fragmentFinder = DrawerFragmentFinder(this)
    private val drawerNavigator = DrawerNavigator(this)
    private val externalLinkOpener = ExternalLinkOpener(this)
    private val externalLinksMenu by lazy { DrawerExternalLinksMenu(this, externalLinkOpener) }
    private val quotaController by lazy { DrawerQuotaController(this, externalLinkOpener) }
    private val headerController by lazy { DrawerHeaderController(this, drawerNavigator) }
    private val menuItemClickHandler by lazy { DrawerMenuItemClickHandler(this, drawerNavigator, externalLinksMenu) }
    private val deepLinkHandler by lazy { DrawerDeepLinkHandler(this, drawerNavigator, menuItemClickHandler) }

    private val manageAccountsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onManageAccountsResult)

    protected open val menuItemId: Int
        get() = R.id.nav_all_files

    open val isDrawerIndicatorAvailable: Boolean
        get() = true

    val appPreferences: AppPreferences
        get() = preferences

    val isDrawerOpen: Boolean
        get() = drawerLayout?.isDrawerOpen(GravityCompat.START) == true

    val ocFileListFragment: OCFileListFragment?
        get() = fragmentFinder.ocFileListFragment

    val isAlbumItemsFragment: Boolean
        get() = fragmentFinder.isAlbumItemsFragmentVisible

    val isAlbumsFragment: Boolean
        get() = fragmentFinder.isAlbumsFragmentVisible

    val isToolbarStyleSearch: Boolean
        get() = when (selectedMenuItemId) {
            Menu.NONE, R.id.nav_all_files, R.id.nav_personal_files -> true
            else -> false
        }

    internal val isAssistantAvailable: Boolean
        get() = capabilities.getOrNull()?.assistant?.isTrue == true

    private val selectedMenuItemId: Int
        get() = drawerNavigationView?.getSelectedMenuItemId() ?: R.id.nav_all_files

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onBackPressedDispatcher.addCallback(DrawerOnBackPressedCallback(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalLinksProvider = ExternalLinksProvider(contentResolver)
        externalLinksSynchronizer =
            ExternalLinksSynchronizer(this, externalLinksProvider, ArbitraryDataProviderImpl(this))
        ecosystemManager = EcosystemManager(this)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        highlightNavigationViewItem(selectedMenuItemId)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle?.let { toggle ->
            toggle.syncState()
            if (isDrawerOpen) {
                toggle.isDrawerIndicatorEnabled = true
            }
        }
        externalLinksMenu.update()
        quotaController.updateQuotaLink()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if (preferences.lockTimestamp != 0L) {
            preferences.lockTimestamp = SystemClock.elapsedRealtime()
        }
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        externalLinksProvider.cleanup()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PassCodeManager.PASSCODE_ACTIVITY -> data?.let(::onPassCodeResult)
            REQ_ALL_FILES_ACCESS, REQ_MEDIA_ACCESS -> checkStoragePermissionWarningBannerVisibility()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onAccountRemovedEvent(event: AccountRemovedEvent) {
        restart()
    }
    // endregion

    // region Drawer setup
    protected fun setupDrawer(id: Int) {
        drawerLayout = drawerLayout ?: findViewById(R.id.drawer_layout)
        drawerNavigationView = drawerNavigationView ?: findViewById(R.id.nav_view)
        drawerNavigationView?.let { setupDrawerNavigationView(it, id) }

        setupDrawerToggle()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bottomNavigationView = bottomNavigationView ?: findViewById(R.id.bottom_navigation)
        bottomNavigationView?.let { setupBottomNavigationView(it, id) }
    }

    private fun setupDrawerNavigationView(navigationView: NavigationView, id: Int) {
        viewThemeUtils.files.colorNavigationView(navigationView)
        navigationViewHeader = navigationView.getHeaderView(0)
        updateHeader()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout?.closeDrawers()
            pendingRunnable = Runnable { menuItemClickHandler.onNavigationItemClicked(menuItem) }
            true
        }
        DrawerMenuFilter.filter(navigationView.menu, userAccountManager.user, capabilities.getOrNull(), resources)

        quotaController.fetchAndDisplay()
        quotaController.setupViews(navigationView)
        highlightNavigationViewItem(id)
    }

    private fun setupBottomNavigationView(bottomNavigation: BottomNavigationView, id: Int) {
        viewThemeUtils.platform.colorBottomNavigationView(bottomNavigation)
        showAssistantInBottomNavigation(bottomNavigation)
        bottomNavigation.setOnItemSelectedListener(menuItemClickHandler::onBottomNavigationItemSelected)
        highlightNavigationViewItem(id)
    }

    @SuppressFBWarnings("RV")
    private fun showAssistantInBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.menu.findItem(R.id.nav_assistant)?.isVisible = isAssistantAvailable
    }

    private fun setupDrawerToggle() {
        val drawerLayout = drawerLayout ?: return
        val toggle = object : ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                runPendingNavigation()
                closeDrawer()
            }
        }
        mDrawerToggle = toggle

        drawerLayout.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.isDrawerSlideAnimationEnabled = true
        ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_back, null)?.let { backArrow ->
            viewThemeUtils.platform.tintToolbarArrowDrawable(this, toggle, backArrow)
        }
    }

    private fun runPendingNavigation() {
        val runnable = pendingRunnable ?: return
        Handler(Looper.getMainLooper()).post(runnable)
        pendingRunnable = null
    }

    fun updateHeader() {
        val header = navigationViewHeader ?: return
        headerController.update(header)
    }
    // endregion

    // region Navigation
    fun highlightNavigationViewItem(menuItemId: Int) {
        highlightNavigationView(drawerNavigationView, bottomNavigationView, menuItemId)
        SystemBarBackgroundCallbacks.apply(this, viewThemeUtils)
        Log_OC.d(TAG, "New menu item is: $menuItemId")
    }

    fun pushFragment(screen: NavigatorScreen) = drawerNavigator.pushFragment(screen)

    fun startPhotoSearch() = drawerNavigator.startPhotoSearch()

    open fun showFiles(onDeviceOnly: Boolean, onlyPersonalFiles: Boolean) =
        drawerNavigator.showFiles(onDeviceOnly, onlyPersonalFiles)

    fun replaceAlbumFragment() = drawerNavigator.replaceAlbumFragment()

    fun <T : Fragment> getFragment(tag: String?, clazz: Class<T>): Optional<T> = fragmentFinder.getFragment(tag, clazz)

    fun showBottomNavigationBar(show: Boolean) {
        bottomNavigationView.setVisibleIf(show)
    }

    protected fun handleDeepLink(uri: Uri) = deepLinkHandler.handle(uri)

    private fun restart() {
        drawerNavigator.restartFileDisplayActivity()
        fetchExternalLinks(false)
    }
    // endregion

    // region Drawer state
    fun toggleDrawer() {
        if (isDrawerOpen) {
            closeDrawer()
        } else {
            openDrawer()
        }
    }

    fun closeDrawer() {
        drawerLayout?.closeDrawer(GravityCompat.START)
    }

    fun openDrawer() {
        val drawerLayout = drawerLayout ?: return
        drawerLayout.openDrawer(GravityCompat.START)
        externalLinksMenu.update()
        quotaController.updateQuotaLink()
    }

    fun setDrawerLockMode(lockMode: Int) {
        drawerLayout?.setDrawerLockMode(lockMode)
    }

    fun setDrawerIndicatorEnabled(enable: Boolean) {
        mDrawerToggle?.isDrawerIndicatorEnabled = enable
    }

    fun addDrawerListener(listener: DrawerLayout.DrawerListener?) {
        val drawerLayout = drawerLayout
        if (drawerLayout == null) {
            Log_OC.e(TAG, "Drawer layout not ready to add drawer listener")
            return
        }
        listener?.let(drawerLayout::addDrawerListener)
    }

    override fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        super.updateActionBarTitleAndHomeButton(chosenFile)
        mDrawerToggle?.isDrawerIndicatorEnabled = chosenFile != null && isRoot(chosenFile)
    }
    // endregion

    // region Accounts
    fun showManageAccountsDialog() {
        ChooseAccountDialogFragment.newInstance(userAccountManager.user)
            .show(supportFragmentManager, CHOOSE_ACCOUNT_DIALOG_TAG)
    }

    fun openManageAccounts() {
        manageAccountsLauncher.launch(Intent(applicationContext, ManageAccountsActivity::class.java))
    }

    fun openAddAccount() {
        stopMediaPlayerAndHidePip()
        if (!MDMConfig.showIntro(this)) {
            startAccountCreation()
            return
        }

        val intent = Intent(applicationContext, FirstRunActivity::class.java)
            .putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true)
        startActivity(intent)
    }

    protected fun stopMediaPlayerAndHidePip() {
        playbackModel.release()
    }

    fun accountClicked(user: User?) {
        val accountManager = userAccountManager
        if (accountManager.user.nameEquals(user) || !accountManager.setCurrentOwnCloudAccount(user)) return
        fetchExternalLinks(true)
        restart()
    }

    fun fetchExternalLinks(force: Boolean) {
        if (!MDMConfig.externalSiteSupport(this)) return

        val user = userAccountManager.user
        if (user.isAnonymous) {
            Log_OC.d(TAG, "Trying to execute a sync operation with a storage manager for an anonymous account")
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { externalLinksSynchronizer.sync(user, force) }
            externalLinksMenu.update()
        }
    }

    private fun onManageAccountsResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val data = result.data ?: return
        if (!data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) return
        if (!data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) return

        setAccount(userAccountManager.currentAccount, false)
        restart()
    }

    private fun onPassCodeResult(data: Intent) {
        val result = data.getIntExtra(
            RequestCredentialsActivity.KEY_CHECK_RESULT,
            RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE
        )
        if (result != RequestCredentialsActivity.KEY_CHECK_RESULT_CANCEL) return

        Log_OC.d(TAG, "PassCodeManager cancelled")
        preferences.lockTimestamp = 0
        finish()
    }

    private fun checkStoragePermissionWarningBannerVisibility() {
        (this as? UploadFilesActivity)?.setupStoragePermissionWarningBanner()
    }
    // endregion

    companion object {
        private const val TAG = "DrawerActivity"
        private const val CHOOSE_ACCOUNT_DIALOG_TAG = "fragment_chose_account"

        const val REQ_ALL_FILES_ACCESS = 3001
        const val REQ_MEDIA_ACCESS = 3000
    }
}
