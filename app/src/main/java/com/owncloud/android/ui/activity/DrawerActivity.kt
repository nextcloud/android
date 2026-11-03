/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemApp
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemManager
import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.android.common.ui.util.PlatformThemeUtil
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.files.DeepLinkConstants
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeActivity.ComposeDestination
import com.nextcloud.ui.dialog.account.ChooseAccountDialogFragment
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.LinkHelper
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.avatar.AvatarGenerationListener
import com.nextcloud.utils.avatar.AvatarGenerator
import com.nextcloud.utils.extensions.getSelectedMenuItemId
import com.nextcloud.utils.extensions.highlightNavigationView
import com.nextcloud.utils.extensions.lastFragment
import com.nextcloud.utils.extensions.navigateToAllFiles
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.mdm.MDMConfig
import com.nextcloud.utils.view.ScreenMetrics
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.ExternalLinksProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.common.accounts.ExternalLinksOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.ui.events.AccountRemovedEvent
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DrawableUtil
import com.owncloud.android.utils.DrawerMenuUtil
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.CapabilityUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Optional
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Suppress("LargeClass")
abstract class DrawerActivity :
    ToolbarActivity(),
    AvatarGenerationListener,
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

    private var drawerLayout: DrawerLayout? = null
    private var drawerNavigationView: NavigationView? = null
    private var navigationViewHeader: View? = null

    private var quotaView: LinearLayout? = null
    private var quotaProgressBar: LinearProgressIndicator? = null
    private var quotaTextPercentage: TextView? = null
    private var quotaTextLink: TextView? = null

    private var pendingRunnable: Runnable? = null

    private lateinit var externalLinksProvider: ExternalLinksProvider
    private lateinit var appOpenedCountProvider: ArbitraryDataProvider

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
        get() = lastFragment() as? OCFileListFragment
            ?: supportFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES) as? OCFileListFragment

    val isToolbarStyleSearch: Boolean
        get() = when (selectedMenuItemId) {
            Menu.NONE, R.id.nav_all_files, R.id.nav_personal_files -> true
            else -> false
        }

    val isAlbumItemsFragment: Boolean
        get() = isFragmentVisible<AlbumItemsFragment>(AlbumItemsFragment.TAG)

    val isAlbumsFragment: Boolean
        get() = isFragmentVisible<AlbumsFragment>(AlbumsFragment.TAG)

    private val selectedMenuItemId: Int
        get() = drawerNavigationView?.getSelectedMenuItemId() ?: R.id.nav_all_files

    private val isAssistantAvailable: Boolean
        get() = capabilities.getOrNull()?.assistant?.isTrue == true

    private val serverTextColor: Int
        get() {
            val serverTextColor = CapabilityUtils.getCapability(this).serverTextColor
            val fallbackColor = if (PlatformThemeUtil.isDarkMode(this)) Color.WHITE else Color.BLACK
            return colorUtil.getNullSafeColor(serverTextColor, fallbackColor)
        }

    private val ecosystemIconColor: Int
        get() {
            val account = account ?: return getColor(R.color.grey_800_transparent)
            val isServerPrimaryColorDark =
                Hct.fromInt(getServerPrimaryColor(account)).tone < ECOSYSTEM_ICON_LIGHT_TONE_THRESHOLD
            return if (isServerPrimaryColorDark) Color.WHITE else getColor(R.color.grey_800_transparent)
        }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        addOnBackPressedCallback()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalLinksProvider = ExternalLinksProvider(contentResolver)
        appOpenedCountProvider = ArbitraryDataProviderImpl(this)
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
        updateExternalLinksInDrawer()
        updateQuotaLink()
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
        setupDrawerMenu(navigationView)
        fetchAndDisplayUserQuota()
        setupQuotaElement(navigationView)
        highlightNavigationViewItem(id)
    }

    private fun setupBottomNavigationView(bottomNavigation: BottomNavigationView, id: Int) {
        viewThemeUtils.platform.colorBottomNavigationView(bottomNavigation)
        showAssistantInBottomNavigation(bottomNavigation)
        bottomNavigation.setOnItemSelectedListener(::onBottomNavigationItemSelected)
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

    private fun setupDrawerMenu(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout?.closeDrawers()
            pendingRunnable = Runnable { onNavigationItemClicked(menuItem) }
            true
        }
        filterDrawerMenu(navigationView.menu, accountManager.user)
    }

    private fun filterDrawerMenu(menu: Menu, user: User) {
        capabilities.getOrNull()?.let { capability ->
            DrawerMenuUtil.filterTrashbinMenuItem(menu, capability)
            DrawerMenuUtil.filterActivityMenuItem(menu, capability)
            DrawerMenuUtil.filterGroupfoldersMenuItem(menu, capability)
            DrawerMenuUtil.filterAssistantMenuItem(menu, capability, resources)
        }

        DrawerMenuUtil.filterSearchMenuItems(menu, user)
        DrawerMenuUtil.setupHomeMenuItem(menu, resources)
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_community, !resources.getBoolean(R.bool.participate_enabled))
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_shared, !resources.getBoolean(R.bool.shared_enabled))
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_logout, !resources.getBoolean(R.bool.show_drawer_logout))
    }
    // endregion

    // region Drawer header
    fun updateHeader() {
        val header = navigationViewHeader ?: return
        val isClientBranded = resources.getBoolean(R.bool.is_branded_client)
        if (!isClientBranded) {
            showServerBranding(header)
        }

        val banner = header.findViewById<ConstraintLayout>(R.id.drawer_ecosystem_apps)
        if (isClientBranded || !preferences.isShowEcosystemApps) {
            banner.visibility = View.GONE
        } else {
            showTopBanner(banner)
        }
    }

    private fun showServerBranding(header: View) {
        val account = account ?: return
        val capability = capabilities.getOrNull() ?: return
        if (capability.serverBackground == null) return

        val primaryColor = getServerPrimaryColor(account)
        header.findViewById<LinearLayout>(R.id.drawer_header_view).setBackgroundColor(primaryColor)

        val serverLogoURL = capability.serverLogo
        if (serverLogoURL.isNullOrEmpty() || !URLUtil.isValidUrl(serverLogoURL)) return

        GlideHelper.loadIntoTarget(
            this,
            accountManager.currentOwnCloudAccount,
            serverLogoURL,
            createServerLogoTarget(primaryColor, capability),
            R.drawable.background
        )
    }

    private fun createServerLogoTarget(primaryColor: Int, capability: OCCapability): Target<Drawable> =
        object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                val bitmap = resource.toServerLogoBitmap() ?: return
                val layers = arrayOf(primaryColor.toDrawable(), bitmap.scaledToLogoSize().toDrawable(resources))
                setDrawerHeaderLogo(LayerDrawable(layers), capability.serverName)
            }

            override fun onLoadCleared(placeholder: Drawable?) = Unit
        }

    private fun Drawable.toServerLogoBitmap(): Bitmap? = when (this) {
        is PictureDrawable -> createBitmap(intrinsicWidth, intrinsicHeight).also { Canvas(it).drawPicture(picture) }

        is BitmapDrawable -> bitmap

        else -> {
            Log_OC.e(TAG, "Unsupported drawable type: " + javaClass.name)
            null
        }
    }

    private fun Bitmap.scaledToLogoSize(): Bitmap {
        val maxDimension = max(width, height)
        if (maxDimension <= MAX_LOGO_SIZE_PX) return this
        return BitmapUtils.scaleBitmap(this, MAX_LOGO_SIZE_PX.toFloat(), width, height, maxDimension)
    }

    private fun setDrawerHeaderLogo(drawable: Drawable, serverName: String?) {
        val header = navigationViewHeader ?: return
        header.findViewById<ImageView>(R.id.drawer_header_logo).run {
            setImageDrawable(drawable)
            adjustViewBounds = true
        }

        if (serverName.isNullOrEmpty()) return
        header.findViewById<TextView>(R.id.drawer_header_server_name).run {
            visibility = View.VISIBLE
            text = serverName
            setTextColor(serverTextColor)
        }
    }

    private fun showTopBanner(banner: ConstraintLayout) {
        val notesView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_notes)
        val talkView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_talk)
        val moreView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_more)
        val assistantView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_assistant)

        user.getOrNull()?.accountName?.let { accountName ->
            notesView.setOnClickListener { ecosystemManager.openApp(EcosystemApp.NOTES, accountName) }
            talkView.setOnClickListener { ecosystemManager.openApp(EcosystemApp.TALK, accountName) }
        }
        moreView.setOnClickListener { LinkHelper.openAppStore(ECOSYSTEM_APP_STORE_QUERY, true, this) }
        assistantView.setOnClickListener { startAssistantScreen() }
        assistantView.isVisible = isAssistantAvailable

        val iconColor = ecosystemIconColor
        listOf(notesView, talkView, moreView, assistantView).forEach { tintEcosystemAppView(it, iconColor) }

        banner.visibility = View.VISIBLE
    }

    private fun tintEcosystemAppView(appView: LinearLayout, color: Int) {
        val imageView = appView.getChildAt(0) as ImageView
        imageView.imageTintList = ColorStateList.valueOf(color)
        (imageView.background as GradientDrawable).setStroke(ScreenMetrics.dpToPx(1f, this), color)
        (appView.getChildAt(1) as TextView).setTextColor(color)
    }

    private fun getServerPrimaryColor(account: Account): Int {
        val serverColor = CapabilityUtils.getCapability(account, this).serverColor
        return colorUtil.getNullSafeColor(serverColor, getColor(R.color.primary))
    }
    // endregion

    // region Navigation item click
    private fun onNavigationItemClicked(menuItem: MenuItem) {
        val itemId = menuItem.itemId
        when (itemId) {
            R.id.nav_all_files, R.id.nav_personal_files -> openAllFiles(itemId == R.id.nav_personal_files)
            R.id.nav_favorites -> openFavoritesTab()
            R.id.nav_gallery -> openMediaTab()
            R.id.nav_album -> openAlbums()
            R.id.nav_on_device -> showOnDeviceFiles()
            in navigatorScreensByMenuItemId -> openNavigatorScreen(itemId)
            R.id.nav_settings -> openSettings()
            R.id.nav_logout -> openAccountRemovalDialog(menuItem)
            R.id.nav_shared -> openSharedTab()
            R.id.nav_recent_files -> openRecentFiles()
            R.id.nav_assistant -> openAssistant()
            R.id.nav_groupfolders -> openGroupfolders()
            else -> onOtherNavigationItemClicked(menuItem)
        }

        resetFileDepth()
        highlightNavigationViewItem(itemId)
    }

    @SuppressFBWarnings("RV")
    private fun onBottomNavigationItemSelected(menuItem: MenuItem): Boolean {
        val itemId = menuItem.itemId
        exitSelectionMode()
        resetOnlyPersonalAndOnDevice()

        when (itemId) {
            R.id.nav_all_files -> {
                navigateToAllFiles()
                EventBus.getDefault().post(ChangeMenuEvent())
            }

            R.id.nav_favorites -> openFavoritesTab()

            R.id.nav_assistant -> if (this !is ComposeActivity) startAssistantScreen()

            R.id.nav_gallery -> openMediaTab()

            R.id.nav_album -> replaceAlbumFragment()
        }

        supportActionBar?.setIcon(null)
        resetFileDepth()
        highlightNavigationViewItem(itemId)
        return false
    }

    private fun openAllFiles(onlyPersonal: Boolean) {
        closeDrawer()
        navigateToAllFiles(onlyPersonal)
        EventBus.getDefault().post(ChangeMenuEvent())
    }

    private fun openFavoritesTab() {
        resetOnlyPersonalAndOnDevice()
        showActionBar(ActionBarStyle.Plain)
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH))
    }

    private fun openMediaTab() {
        GalleryFragment.clearSavedViewState()
        resetOnlyPersonalAndOnDevice()
        showActionBar(ActionBarStyle.Plain)
        startPhotoSearch()
    }

    private fun openAlbums() {
        if (this is FileDisplayActivity) {
            replaceAlbumFragment()
        } else {
            startActivity(fileDisplayActivityIntent(FileDisplayActivity.ALBUMS))
        }
    }

    private fun openNavigatorScreen(itemId: Int) {
        val screen = navigatorScreensByMenuItemId[itemId] ?: return
        resetOnlyPersonalAndOnDevice()
        pushFragment(screen)
    }

    private fun openSettings() {
        resetOnlyPersonalAndOnDevice()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openAccountRemovalDialog(menuItem: MenuItem) {
        resetOnlyPersonalAndOnDevice()
        val uncheckedMenuItem = menuItem.setChecked(false)
        Log_OC.d(TAG, "onNavigationItemClicked nav_logout setChecked $uncheckedMenuItem")
        user.getOrNull()?.let { UserInfoActivity.openAccountRemovalDialog(it, supportFragmentManager) }
    }

    private fun openSharedTab() {
        resetOnlyPersonalAndOnDevice()
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER))
    }

    private fun openRecentFiles() {
        resetOnlyPersonalAndOnDevice()
        launchActivityForSearch(SearchEvent("", SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH))
    }

    private fun openAssistant() {
        resetOnlyPersonalAndOnDevice()
        startAssistantScreen()
    }

    private fun openGroupfolders() {
        resetOnlyPersonalAndOnDevice()
        startActivity(fileDisplayActivityIntent(FileDisplayActivity.LIST_GROUPFOLDERS))
    }

    private fun onOtherNavigationItemClicked(menuItem: MenuItem) {
        if (menuItem.groupId == R.id.drawer_menu_external_links) {
            externalLinkClicked(menuItem)
            return
        }
        Log_OC.w(TAG, "Unknown drawer menu item clicked: " + menuItem.title)
    }
    // endregion

    // region Navigation
    fun highlightNavigationViewItem(menuItemId: Int) {
        highlightNavigationView(drawerNavigationView, bottomNavigationView, menuItemId)
        SystemBarBackgroundCallbacks.apply(this, viewThemeUtils)
        Log_OC.d(TAG, "New menu item is: $menuItemId")
    }

    fun pushFragment(screen: NavigatorScreen) {
        if (this is NavigatorActivity) {
            push(screen)
            return
        }
        startActivity(NavigatorActivity.intent(this, screen))
    }

    fun startPhotoSearch() {
        MainApp.showOnlyFilesOnDevice(false)
        launchActivityForSearch(SearchEvent(PHOTO_SEARCH_MIME_TYPE, SearchRemoteOperation.SearchType.PHOTO_SEARCH))
    }

    open fun showFiles(onDeviceOnly: Boolean, onlyPersonalFiles: Boolean) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly)
        MainApp.showOnlyPersonalFiles(onlyPersonalFiles)
        startActivity(fileDisplayActivityIntent(FileDisplayActivity.ALL_FILES))
    }

    fun replaceAlbumFragment() {
        if (isAlbumsFragment) return
        supportFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.left_fragment_container, AlbumsFragment.newInstance(false), AlbumsFragment.TAG)
        }
    }

    fun <T : Fragment> getFragment(tag: String?, clazz: Class<T>): Optional<T> =
        Optional.ofNullable(supportFragmentManager.findFragmentByTag(tag))
            .filter(clazz::isInstance)
            .map(clazz::cast)

    fun showBottomNavigationBar(show: Boolean) {
        bottomNavigationView.setVisibleIf(show)
    }

    protected fun handleDeepLink(uri: Uri) {
        val path = uri.lastPathSegment ?: return
        val deepLinkType = DeepLinkConstants.fromPath(path)
        if (deepLinkType == null) {
            SnackbarUtil.show(this, getString(R.string.invalid_url))
            return
        }

        when (deepLinkType) {
            DeepLinkConstants.OPEN_AUTO_UPLOAD -> pushFragment(NavigatorScreen.AutoUpload)
            DeepLinkConstants.OPEN_EXTERNAL_URL -> openDeepLinkExternalUrl(uri)
            DeepLinkConstants.ACTION_CREATE_NEW -> findViewById<View>(R.id.fab_main)?.callOnClick()
            DeepLinkConstants.ACTION_APP_UPDATE -> LinkHelper.openAppStore(packageName, false, this)
            DeepLinkConstants.OPEN_NOTIFICATIONS -> pushFragment(NavigatorScreen.Notifications)
            else -> handleNavItemClickEvent(deepLinkType.navId)
        }
    }

    private fun openDeepLinkExternalUrl(uri: Uri) {
        val url = uri.getQueryParameter(DEEP_LINK_URL_QUERY_PARAMETER) ?: return
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun handleNavItemClickEvent(@IdRes menuItemId: Int) {
        val menuItem = drawerNavigationView?.menu?.findItem(menuItemId) ?: return
        onNavigationItemClicked(menuItem)
    }

    private fun launchActivityForSearch(searchEvent: SearchEvent) {
        val intent = fileDisplayActivityIntent(Intent.ACTION_SEARCH)
            .putExtra(OCFileListFragment.SEARCH_EVENT, searchEvent)
        startActivity(intent)
    }

    private fun startAssistantScreen() {
        val intent = Intent(applicationContext, ComposeActivity::class.java)
            .putExtra(ComposeActivity.DESTINATION, ComposeDestination.getAssistantScreen(this))
        startActivity(intent)
    }

    private fun showOnDeviceFiles() {
        MainApp.showOnlyFilesOnDevice(true)
        MainApp.showOnlyPersonalFiles(false)
        startActivity(fileDisplayActivityIntent(FileDisplayActivity.ON_DEVICE))
    }

    private fun restart() {
        startActivity(fileDisplayActivityIntent(FileDisplayActivity.RESTART))
        fetchExternalLinks(false)
    }

    private fun fileDisplayActivityIntent(action: String): Intent =
        Intent(applicationContext, FileDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.action = action
        }

    private fun resetOnlyPersonalAndOnDevice() {
        MainApp.showOnlyFilesOnDevice(false)
        MainApp.showOnlyPersonalFiles(false)
    }

    private fun resetFileDepth() {
        ocFileListFragment?.resetFileDepth()
    }

    private fun exitSelectionMode() {
        ocFileListFragment?.exitSelectionMode()
    }

    private inline fun <reified T : Fragment> isFragmentVisible(tag: String): Boolean =
        (supportFragmentManager.findFragmentByTag(tag) as? T)?.isVisible == true
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
        updateExternalLinksInDrawer()
        updateQuotaLink()
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

    private fun addOnBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isDrawerOpen) {
                        closeDrawer()
                        return
                    }

                    val fragment = supportFragmentManager.findFragmentByTag(FileDetailsSharingProcessFragment.TAG)
                    if (fragment is FileDetailsSharingProcessFragment) {
                        fragment.onBackPressed()
                        return
                    }

                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }
    // endregion

    // region Accounts
    fun showManageAccountsDialog() {
        ChooseAccountDialogFragment.newInstance(accountManager.user)
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
        val currentUser = accountManager.user
        if (currentUser.nameEquals(user) || !accountManager.setCurrentOwnCloudAccount(user)) return
        fetchExternalLinks(true)
        restart()
    }

    private fun onManageAccountsResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val data = result.data ?: return
        if (!data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) return
        if (!data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) return

        setAccount(accountManager.currentAccount, false)
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

    // region Quota
    private fun setupQuotaElement(navigationView: NavigationView) {
        quotaView = findQuotaViewById(navigationView, R.id.drawer_quota)
        quotaProgressBar = findQuotaViewById(navigationView, R.id.drawer_quota_ProgressBar)
        quotaTextPercentage = findQuotaViewById(navigationView, R.id.drawer_quota_percentage)
        quotaTextLink = findQuotaViewById(navigationView, R.id.drawer_quota_link)

        quotaProgressBar?.let {
            viewThemeUtils.material.colorProgressBar(it, ColorRole.PRIMARY)
            it.trackStopIndicatorSize = 0
        }
        quotaView?.let { viewThemeUtils.platform.colorViewBackground(it) }
    }

    private fun <T : View> findQuotaViewById(navigationView: NavigationView, @IdRes id: Int): T? =
        navigationView.getHeaderView(0)?.findViewById(id) ?: findViewById(id)

    private fun fetchAndDisplayUserQuota() {
        lifecycleScope.launch {
            val quota = fetchUserQuota() ?: return@launch
            if (quota.isDisplayable) {
                showQuotaInformation(quota)
            } else {
                quotaView?.isVisible = false
            }
        }
    }

    private suspend fun fetchUserQuota(): Quota? = withContext(Dispatchers.IO) {
        if (accountManager.user.isAnonymous) return@withContext null
        val client = clientRepository.getNextcloudClient() ?: return@withContext null
        val result = GetUserInfoRemoteOperation().execute(client)
        if (result.isSuccess) result.resultData?.quota else null
    }

    private val Quota.isDisplayable: Boolean
        get() = quota > 0 ||
            quota == GetUserInfoRemoteOperation.SPACE_UNLIMITED ||
            quota == GetUserInfoRemoteOperation.QUOTA_LIMIT_INFO_NOT_AVAILABLE

    private fun showQuotaInformation(quota: Quota) {
        quotaTextPercentage?.text = if (quota.quota == GetUserInfoRemoteOperation.SPACE_UNLIMITED) {
            getString(R.string.drawer_quota_unlimited, HumanReadableFormatter.formatBytes(quota.used))
        } else {
            getString(
                R.string.drawer_quota,
                HumanReadableFormatter.formatBytes(quota.used),
                HumanReadableFormatter.formatBytes(quota.total)
            )
        }

        val relative = ceil(quota.relative).toInt()
        quotaProgressBar?.let { progressBar ->
            progressBar.progress = relative
            if (relative < RELATIVE_THRESHOLD_WARNING) {
                viewThemeUtils.material.colorProgressBar(progressBar, ColorRole.PRIMARY)
            } else {
                viewThemeUtils.material.colorProgressBar(
                    progressBar,
                    resources.getColor(R.color.infolevel_warning, null)
                )
            }
        }

        updateQuotaLink()
        quotaView?.isVisible = true
    }

    private fun updateQuotaLink() {
        val quotaTextLink = quotaTextLink ?: return
        if (!MDMConfig.externalSiteSupport(this)) {
            quotaTextLink.visibility = View.GONE
            return
        }

        externalLinksProvider.getExternalLink(ExternalLinkType.QUOTA) { quotas ->
            val firstQuota = quotas.firstOrNull()
            if (firstQuota == null) {
                quotaTextLink.visibility = View.GONE
            } else {
                showQuotaLink(quotaTextLink, firstQuota)
            }
        }
    }

    private fun showQuotaLink(quotaTextLink: TextView, quota: ExternalLink) {
        quotaTextLink.text = quota.name
        quotaTextLink.isClickable = true
        quotaTextLink.visibility = View.VISIBLE
        quotaTextLink.setOnClickListener { openExternalSiteWebView(quota) }

        val iconSize = (QUOTA_LINK_ICON_SIZE_DP * resources.displayMetrics.density).roundToInt()
        GlideHelper.loadIntoTarget(
            this,
            accountManager.currentOwnCloudAccount,
            quota.iconUrl,
            createQuotaDrawableTarget(iconSize, quotaTextLink),
            R.drawable.ic_link
        )
    }

    private fun createQuotaDrawableTarget(size: Int, quotaTextLink: TextView): Target<Drawable> =
        object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                setQuotaLinkIcon(quotaTextLink, resource, size)
            }

            override fun onLoadCleared(placeholder: Drawable?) = Unit

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                errorDrawable?.let { setQuotaLinkIcon(quotaTextLink, it, size) }
            }
        }

    private fun setQuotaLinkIcon(quotaTextLink: TextView, drawable: Drawable, size: Int) {
        val icon = drawable.current.apply { setBounds(0, 0, size, size) }
        quotaTextLink.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }
    // endregion

    // region External links
    fun fetchExternalLinks(force: Boolean) {
        if (!MDMConfig.externalSiteSupport(this)) return

        val user = accountManager.user
        if (user.isAnonymous) {
            Log_OC.d(TAG, "Trying to execute a sync operation with a storage manager for an anonymous account")
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                refreshCapabilities()
                syncExternalLinks(user, force)
            }
            updateExternalLinksInDrawer()
        }
    }

    private fun refreshCapabilities() {
        val capability = capabilities.getOrNull() ?: return
        if (capability.accountName?.isEmpty() == true) return
        val storageManager = storageManager ?: return
        GetCapabilitiesOperation(storageManager).execute(baseContext)
    }

    private fun syncExternalLinks(user: User, force: Boolean) {
        val isExternalLinksEnabled =
            storageManager != null && CapabilityUtils.getCapability(user, this).externalLinks.isTrue
        if (!isExternalLinksEnabled) {
            externalLinksProvider.deleteAllExternalLinks()
            Log_OC.d(EXTERNAL_LINKS_TAG, "links disabled")
            return
        }

        val appOpenedCount =
            appOpenedCountProvider.getIntegerValue(FilesSyncHelper.GLOBAL, FileActivity.APP_OPENED_COUNT)
        val shouldUpdate = force ||
            appOpenedCount == APP_OPENED_COUNT_NOT_SET ||
            appOpenedCount > APP_OPENED_COUNT_UPDATE_THRESHOLD
        if (!shouldUpdate) {
            storeAppOpenedCount(appOpenedCount + 1)
            return
        }

        if (force) {
            Log_OC.d(EXTERNAL_LINKS_TAG, "force update")
        }
        storeAppOpenedCount(0)

        Log_OC.d(EXTERNAL_LINKS_TAG, "update via api")
        val result = ExternalLinksOperation().execute(user, this)
        if (!result.isSuccess) return
        val externalLinks = result.data ?: return

        externalLinksProvider.deleteAllExternalLinks()
        externalLinks.filterIsInstance<ExternalLink>().forEach(externalLinksProvider::storeExternalLink)
    }

    private fun storeAppOpenedCount(count: Int) {
        appOpenedCountProvider.storeOrUpdateKeyValue(FilesSyncHelper.GLOBAL, FileActivity.APP_OPENED_COUNT, "$count")
    }

    private fun updateExternalLinksInDrawer() {
        val navigationView = drawerNavigationView ?: return
        if (!MDMConfig.externalSiteSupport(this)) return

        navigationView.menu.removeGroup(R.id.drawer_menu_external_links)

        val greyColor = ContextCompat.getColor(this, R.color.drawer_menu_icon)
        externalLinksProvider.getExternalLink(ExternalLinkType.LINK) { externalLinks ->
            externalLinks.forEach { addExternalLinkMenuItem(navigationView, it, greyColor) }
        }
    }

    private fun addExternalLinkMenuItem(navigationView: NavigationView, link: ExternalLink, tintColor: Int) {
        val menuItemId = navigationView.menu
            .add(
                R.id.drawer_menu_external_links,
                MENU_ITEM_EXTERNAL_LINK + link.id,
                MENU_ORDER_EXTERNAL_LINKS,
                link.name
            )
            .setCheckable(true)
            .itemId

        GlideHelper.loadIntoTarget(
            this,
            accountManager.currentOwnCloudAccount,
            link.iconUrl,
            createMenuItemTarget(menuItemId, tintColor),
            R.drawable.ic_link
        )
    }

    private fun createMenuItemTarget(menuItemId: Int, tintColor: Int): Target<Drawable> =
        object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                setExternalLinkIcon(menuItemId, resource, tintColor)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                setExternalLinkIcon(menuItemId, errorDrawable, tintColor)
            }

            override fun onLoadCleared(placeholder: Drawable?) = Unit
        }

    private fun setExternalLinkIcon(menuItemId: Int, drawable: Drawable?, tintColor: Int) {
        val menuItem = drawerNavigationView?.menu?.findItem(menuItemId) ?: return
        if (drawable == null) {
            menuItem.setIcon(R.drawable.ic_link)
            return
        }

        val resizedDrawable = DrawableUtil.getResizedDrawable(this, drawable, EXTERNAL_LINK_ICON_SIZE_PX)
        menuItem.icon = viewThemeUtils.platform.colorDrawable(resizedDrawable, tintColor)
    }

    private fun externalLinkClicked(menuItem: MenuItem) {
        val linkId = menuItem.itemId - MENU_ITEM_EXTERNAL_LINK
        externalLinksProvider.getExternalLink(ExternalLinkType.LINK) { externalLinks ->
            val link = externalLinks.firstOrNull { it.id == linkId }
            if (link == null) {
                Log_OC.w(TAG, "No external link found for menu item: " + menuItem.title)
            } else {
                openExternalLink(link)
            }
        }
    }

    private fun openExternalLink(link: ExternalLink) {
        if (link.redirect) {
            IntentUtil.startLinkIntent(this, link.url)
            return
        }
        openExternalSiteWebView(link)
    }

    private fun openExternalSiteWebView(link: ExternalLink) {
        val intent = Intent(applicationContext, ExternalSiteWebView::class.java)
            .putExtra(ExternalSiteWebView.EXTRA_TITLE, link.name)
            .putExtra(ExternalSiteWebView.EXTRA_URL, link.url)
            .putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true)
        startActivity(intent)
    }
    // endregion

    // region Avatar
    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        when (callContext) {
            is MenuItem -> {
                val newIcon = callContext.setIcon(avatarDrawable)
                Log_OC.d(TAG, "avatarGenerated new icon: $newIcon")
            }

            is ImageView -> callContext.setImageDrawable(avatarDrawable)

            is MaterialButton -> callContext.icon = avatarDrawable
        }
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        val contextTag: Any? = when (callContext) {
            is MenuItem -> callContext.title
            is ImageView -> callContext.tag
            is MaterialButton -> callContext.tag
            else -> return false
        }
        return contextTag.toString() == tag
    }
    // endregion

    companion object {
        private const val TAG = "DrawerActivity"
        private const val EXTERNAL_LINKS_TAG = "ExternalLinks"
        private const val CHOOSE_ACCOUNT_DIALOG_TAG = "fragment_chose_account"
        private const val ECOSYSTEM_APP_STORE_QUERY = "Nextcloud"
        private const val PHOTO_SEARCH_MIME_TYPE = "image/%"
        private const val DEEP_LINK_URL_QUERY_PARAMETER = "url"
        private const val MENU_ORDER_EXTERNAL_LINKS = 3
        private const val MENU_ITEM_EXTERNAL_LINK = 111
        private const val MAX_LOGO_SIZE_PX = 1000
        private const val RELATIVE_THRESHOLD_WARNING = 80
        private const val ECOSYSTEM_ICON_LIGHT_TONE_THRESHOLD = 80.0
        private const val QUOTA_LINK_ICON_SIZE_DP = 24
        private const val EXTERNAL_LINK_ICON_SIZE_PX = 32
        private const val APP_OPENED_COUNT_NOT_SET = -1
        private const val APP_OPENED_COUNT_UPDATE_THRESHOLD = 10

        const val REQ_ALL_FILES_ACCESS = 3001
        const val REQ_MEDIA_ACCESS = 3000

        private val navigatorScreensByMenuItemId: Map<Int, NavigatorScreen> by lazy {
            mapOf(
                R.id.nav_uploads to NavigatorScreen.UploadList,
                R.id.nav_trashbin to NavigatorScreen.Trashbin,
                R.id.nav_activity to NavigatorScreen.Activities,
                R.id.nav_community to NavigatorScreen.Community
            )
        }
    }
}
