/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AnimatorRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.utils.theme.ThemeColorUtils
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

abstract class ToolbarActivity :
    BaseActivity(),
    Injectable {

    @Inject
    lateinit var themeColorUtils: ThemeColorUtils

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    protected lateinit var mMenuButton: MaterialButton
    protected lateinit var mSearchText: MaterialTextView
    protected lateinit var mSwitchAccountButton: MaterialButton
    protected lateinit var mNotificationButton: MaterialButton
    protected lateinit var mToolbarSpinner: AppCompatSpinner

    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBar: AppBarLayout
    private lateinit var defaultToolbar: RelativeLayout
    private lateinit var homeSearchToolbar: LinearLayout
    private lateinit var homeSearchContainer: MaterialCardView

    var previewImageView: ImageView? = null
        private set
    private var previewImageContainer: FrameLayout? = null
    private var infoBox: LinearLayout? = null
    private var infoBoxMessage: TextView? = null

    private var isHomeSearchToolbarShow = false

    private val isToolbarBound: Boolean
        get() = ::appBar.isInitialized

    private val sortListButtonGroup: View?
        get() = findViewById(R.id.sort_list_button_group)

    private fun setupToolbar(isHomeSearchToolbarShow: Boolean, showSortListButtonGroup: Boolean) {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        bindToolbarViews()

        if (showSortListButtonGroup) findViewById<View>(R.id.sort_list_button_group).isVisible = true

        this.isHomeSearchToolbarShow = isHomeSearchToolbarShow
        updateActionBarTitleAndHomeButton(null)

        infoBox = findViewById(R.id.info_box)
        infoBoxMessage = findViewById(R.id.info_box_message)
        previewImageView = findViewById(R.id.preview_image)
        previewImageContainer = findViewById(R.id.preview_image_frame)
        mToolbarSpinner = findViewById(R.id.toolbar_spinner)

        themeToolbarViews()
    }

    private fun bindToolbarViews() {
        appBar = findViewById(R.id.appbar)
        defaultToolbar = findViewById(R.id.default_toolbar)
        homeSearchToolbar = findViewById(R.id.home_toolbar)
        homeSearchContainer = findViewById(R.id.home_search_container)
        mMenuButton = findViewById(R.id.menu_button)
        mSearchText = findViewById(R.id.search_text)
        mSwitchAccountButton = findViewById(R.id.switch_account_button)
        mNotificationButton = findViewById(R.id.notification_button)
    }

    private fun themeToolbarViews() {
        viewThemeUtils.run {
            material.themeToolbar(toolbar)
            material.colorToolbarOverflowIcon(toolbar)
            platform.themeStatusBar(this@ToolbarActivity)
            material.colorMaterialTextButton(mSwitchAccountButton)
            material.themeSearchCardView(homeSearchContainer)
            material.colorMaterialButtonContent(mMenuButton, ColorRole.ON_SURFACE)
            material.colorMaterialButtonContent(mNotificationButton, ColorRole.ON_SURFACE)
            platform.colorTextView(mSearchText, ColorRole.ON_SURFACE_VARIANT)
        }
    }

    fun setupToolbarShowOnlyMenuButtonAndTitle(title: String, toggleDrawer: View.OnClickListener) {
        setupToolbar(isHomeSearchToolbarShow = false, showSortListButtonGroup = false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toolbarLayout = findViewById<LinearLayout>(R.id.toolbar_linear_layout)
        val menuButton = findViewById<MaterialButton>(R.id.toolbar_menu_button)
        findViewById<MaterialTextView>(R.id.toolbar_title).run {
            text = title
            setTextColor(ContextCompat.getColor(this@ToolbarActivity, R.color.foreground_highlight))
        }
        menuButton.iconTint = ContextCompat.getColorStateList(this, R.color.foreground_highlight)

        toolbarLayout.isVisible = true
        menuButton.setOnClickListener(toggleDrawer)
    }

    fun setupToolbar() {
        if (isToolbarBound && homeSearchToolbar.isGone && defaultToolbar.isVisible) {
            Log_OC.d(TAG, "Search toolbar is already hidden, skipping update.")
            return
        }

        setupToolbar(isHomeSearchToolbarShow = false, showSortListButtonGroup = false)
    }

    fun setupHomeSearchToolbarWithSortAndListButtons() {
        if (isToolbarBound && homeSearchToolbar.isVisible && defaultToolbar.isGone) {
            Log_OC.d(TAG, "Search toolbar is already visible, skipping update.")
            return
        }

        setupToolbar(isHomeSearchToolbarShow = true, showSortListButtonGroup = true)
    }

    private val ocFileListFragment: OCFileListFragment?
        get() = (this as? FileDisplayActivity)?.listOfFilesFragment

    private val isCurrentDirRoot: Boolean
        get() = ocFileListFragment?.fileDepth == OCFileDepth.Root

    private val searchType: SearchType
        get() = ocFileListFragment?.currentSearchType ?: SearchType.NO_SEARCH

    val actionBarRootTitle: String
        get() = searchType.titleId()?.let { getString(it) } ?: themeUtils.getDefaultDisplayNameForRootFolder(this)

    fun getActionBarTitle(chosenFile: OCFile, isRoot: Boolean): String =
        if (isRoot) actionBarRootTitle else getActionBarTitleFromFile(chosenFile)

    private fun getActionBarTitleFromFile(file: OCFile): String =
        fileDataStorageManager.getFilenameConsideringOfflineOperation(file)

    protected open fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        val isRoot = isRoot(chosenFile) || isCurrentDirRoot
        val title = if (chosenFile == null || isRoot) actionBarRootTitle else getActionBarTitleFromFile(chosenFile)
        updateActionBarTitleAndHomeButtonByString(title)

        val isToolbarStyleSearch = (this as? DrawerActivity)?.isToolbarStyleSearch == true
        showHomeSearchToolbar(isHomeSearchToolbarShow && isRoot && isToolbarStyleSearch)

        if (isToolbarBound) {
            mSearchText.text = getString(R.string.appbar_search_in, title)
        }

        supportActionBar?.let { viewThemeUtils.files.themeActionBar(this, it, title, isRoot) }
    }

    protected fun updateActionBarForFile(file: OCFile?) {
        if (file == null) return

        val title = getActionBarTitleFromFile(file)
        updateActionBarTitleAndHomeButtonByString(title)
        showHomeSearchToolbar(false)
        supportActionBar?.let { viewThemeUtils.files.themeActionBar(this, it, title, false) }
    }

    fun showSearchView() {
        if (!isHomeSearchToolbarShow) return
        showHomeSearchToolbar(false)
    }

    fun hideSearchView(chosenFile: OCFile?) {
        if (!isHomeSearchToolbarShow) return
        showHomeSearchToolbar(isRoot(chosenFile))
    }

    @SuppressLint("PrivateResource")
    private fun showHomeSearchToolbar(isShow: Boolean) {
        if (!isToolbarBound) return

        viewThemeUtils.material.themeToolbar(toolbar)

        if (isShow) {
            viewThemeUtils.platform.resetStatusBar(this)
            setAppBarStateListAnimator(R.animator.appbar_elevation_off)
            defaultToolbar.isVisible = false
            homeSearchToolbar.isVisible = true
            viewThemeUtils.material.themeSearchCardView(homeSearchContainer)
            viewThemeUtils.material.themeSearchBarText(mSearchText)
        } else {
            setAppBarStateListAnimator(R.animator.appbar_elevation_on)
            viewThemeUtils.platform.themeStatusBar(this)
            defaultToolbar.isVisible = true
            homeSearchToolbar.isVisible = false
        }

        SystemBarBackgroundCallbacks.apply(this, viewThemeUtils)
    }

    private fun setAppBarStateListAnimator(@AnimatorRes animatorId: Int) {
        appBar.stateListAnimator = AnimatorInflater.loadStateListAnimator(appBar.context, animatorId)
    }

    val isHomeSearchToolbarVisible: Boolean
        get() = isToolbarBound && homeSearchToolbar.isVisible

    fun updateActionBarTitleAndHomeButtonByString(title: String?) {
        val actionBar = supportActionBar ?: return

        if (title != null) {
            actionBar.title = title
        }
        actionBar.setDisplayShowTitleEnabled(title != null)
    }

    fun isRoot(file: OCFile?): Boolean =
        file == null || (file.isFolder && file.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong())

    protected fun showInfoBox(@StringRes text: Int) {
        val infoBox = infoBox ?: return
        val infoBoxMessage = infoBoxMessage ?: return

        infoBox.isVisible = true
        infoBoxMessage.setText(text)
    }

    fun hideInfoBox() {
        infoBox?.isVisible = false
    }

    fun setPreviewImageVisibility(isVisible: Boolean) {
        previewImageContainer?.isVisible = isVisible
    }

    fun hidePreviewImage() = setPreviewImageVisibility(false)

    open fun showSortListGroup(show: Boolean) {
        sortListButtonGroup?.isVisible = show
    }

    fun sortListGroupVisibility(): Boolean = sortListButtonGroup?.isVisible == true

    fun updateToolbarSubtitle(subtitle: String) {
        val actionBar = supportActionBar ?: return
        actionBar.subtitle = subtitle
        viewThemeUtils.androidx.themeActionBarSubtitle(this, actionBar)
    }

    fun clearToolbarSubtitle() {
        supportActionBar?.subtitle = null
    }

    companion object {
        private const val TAG = "ToolbarActivity"
    }
}
