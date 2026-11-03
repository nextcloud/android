/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.animation.AnimatorInflater
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
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

abstract class ToolbarActivity :
    BaseActivity(),
    Injectable {

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    protected lateinit var menuButton: MaterialButton
    protected lateinit var searchText: MaterialTextView
    protected lateinit var switchAccountButton: MaterialButton
    protected lateinit var notificationButton: MaterialButton
    protected lateinit var toolbarSpinner: AppCompatSpinner

    private lateinit var toolbar: MaterialToolbar
    private lateinit var defaultToolbar: RelativeLayout
    private lateinit var homeSearchToolbar: LinearLayout
    private lateinit var homeSearchContainer: MaterialCardView

    private var appBar: AppBarLayout? = null
    var previewImageView: ImageView? = null
        private set
    private var previewImageContainer: FrameLayout? = null
    private var infoBox: LinearLayout? = null
    private var infoBoxMessage: TextView? = null

    private var actionBarStyle = ActionBarStyle.Plain

    private val isToolbarBound: Boolean
        get() = ::homeSearchToolbar.isInitialized

    // region Action bar setup
    fun showActionBar(style: ActionBarStyle) {
        if (isActionBarDisplayed(style)) {
            Log_OC.d(TAG, "$style action bar is already displayed, skipping update.")
            return
        }

        configureActionBar(style)
    }

    fun showMenuAndTitleActionBar(title: String, onMenuClick: View.OnClickListener) {
        configureActionBar(ActionBarStyle.Plain)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toolbarLayout = findViewById<LinearLayout>(R.id.toolbar_linear_layout)
        val toolbarMenuButton = findViewById<MaterialButton>(R.id.toolbar_menu_button)
        findViewById<MaterialTextView>(R.id.toolbar_title).run {
            text = title
            setTextColor(ContextCompat.getColor(this@ToolbarActivity, R.color.foreground_highlight))
        }
        toolbarMenuButton.iconTint = ContextCompat.getColorStateList(this, R.color.foreground_highlight)
        toolbarLayout.isVisible = true
        toolbarMenuButton.setOnClickListener(onMenuClick)
    }

    private fun isActionBarDisplayed(style: ActionBarStyle): Boolean {
        if (!isToolbarBound) return false
        val (displayed, hidden) = when (style) {
            ActionBarStyle.Plain -> defaultToolbar to homeSearchToolbar
            ActionBarStyle.Search -> homeSearchToolbar to defaultToolbar
        }
        return displayed.isVisible && hidden.isGone
    }

    private fun configureActionBar(style: ActionBarStyle) {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        bindToolbarViews()

        if (style == ActionBarStyle.Search) sortListButtonGroup?.isVisible = true

        actionBarStyle = style
        updateActionBarTitleAndHomeButton(null)

        infoBox = findViewById(R.id.info_box)
        infoBoxMessage = findViewById(R.id.info_box_message)
        previewImageView = findViewById(R.id.preview_image)
        previewImageContainer = findViewById(R.id.preview_image_frame)

        themeToolbarViews()
    }

    private fun bindToolbarViews() {
        appBar = findViewById(R.id.appbar)
        defaultToolbar = findViewById(R.id.default_toolbar)
        homeSearchToolbar = findViewById(R.id.home_toolbar)
        homeSearchContainer = findViewById(R.id.home_search_container)
        menuButton = findViewById(R.id.menu_button)
        searchText = findViewById(R.id.search_text)
        switchAccountButton = findViewById(R.id.switch_account_button)
        notificationButton = findViewById(R.id.notification_button)
        toolbarSpinner = findViewById(R.id.toolbar_spinner)
    }

    private fun themeToolbarViews() {
        viewThemeUtils.run {
            material.themeToolbar(toolbar)
            material.colorToolbarOverflowIcon(toolbar)
            platform.themeStatusBar(this@ToolbarActivity)
            material.colorMaterialTextButton(switchAccountButton)
            material.themeSearchCardView(homeSearchContainer)
            material.colorMaterialButtonContent(menuButton, ColorRole.ON_SURFACE)
            material.colorMaterialButtonContent(notificationButton, ColorRole.ON_SURFACE)
            platform.colorTextView(searchText, ColorRole.ON_SURFACE_VARIANT)
        }
    }
    // endregion

    // region Title and subtitle
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
        val title = chosenFile?.let { getActionBarTitle(it, isRoot) } ?: actionBarRootTitle
        setActionBarTitle(title)

        val isToolbarStyleSearch = (this as? DrawerActivity)?.isToolbarStyleSearch == true
        val canShowSearchBar = actionBarStyle == ActionBarStyle.Search && isRoot && isToolbarStyleSearch
        switchActionBar(if (canShowSearchBar) ActionBarStyle.Search else ActionBarStyle.Plain)

        if (isToolbarBound) searchText.text = getString(R.string.appbar_search_in, title)

        supportActionBar?.let { viewThemeUtils.files.themeActionBar(this, it, title, isRoot) }
    }

    protected fun updateActionBarForFile(file: OCFile?) {
        if (file == null) return
        val title = getActionBarTitleFromFile(file)
        setActionBarTitle(title)
        switchActionBar(ActionBarStyle.Plain)
        supportActionBar?.let { viewThemeUtils.files.themeActionBar(this, it, title, false) }
    }

    fun setActionBarTitle(title: String?) {
        val actionBar = supportActionBar ?: return
        title?.let { actionBar.title = it }
        actionBar.setDisplayShowTitleEnabled(title != null)
    }

    fun isRoot(file: OCFile?): Boolean =
        file == null || (file.isFolder && file.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong())

    fun updateToolbarSubtitle(subtitle: String) {
        val actionBar = supportActionBar ?: return
        actionBar.subtitle = subtitle
        viewThemeUtils.androidx.themeActionBarSubtitle(this, actionBar)
    }

    fun clearToolbarSubtitle() {
        supportActionBar?.subtitle = null
    }
    // endregion

    // region Search action bar
    val isSearchActionBarVisible: Boolean
        get() = isToolbarBound && homeSearchToolbar.isVisible

    fun showSearchView() {
        if (actionBarStyle != ActionBarStyle.Search) return
        switchActionBar(ActionBarStyle.Plain)
    }

    fun hideSearchView(chosenFile: OCFile?) {
        if (actionBarStyle != ActionBarStyle.Search) return
        switchActionBar(if (isRoot(chosenFile)) ActionBarStyle.Search else ActionBarStyle.Plain)
    }

    private fun switchActionBar(style: ActionBarStyle) {
        val appBar = appBar ?: return

        viewThemeUtils.material.themeToolbar(toolbar)

        when (style) {
            ActionBarStyle.Search -> {
                viewThemeUtils.platform.resetStatusBar(this)
                setAppBarStateListAnimator(appBar, R.animator.appbar_elevation_off)
                defaultToolbar.isVisible = false
                homeSearchToolbar.isVisible = true
                viewThemeUtils.material.themeSearchCardView(homeSearchContainer)
                viewThemeUtils.material.themeSearchBarText(searchText)
            }

            ActionBarStyle.Plain -> {
                setAppBarStateListAnimator(appBar, R.animator.appbar_elevation_on)
                viewThemeUtils.platform.themeStatusBar(this)
                defaultToolbar.isVisible = true
                homeSearchToolbar.isVisible = false
            }
        }

        SystemBarBackgroundCallbacks.apply(this, viewThemeUtils)
    }

    private fun setAppBarStateListAnimator(appBar: AppBarLayout, @AnimatorRes animatorId: Int) {
        appBar.stateListAnimator = AnimatorInflater.loadStateListAnimator(appBar.context, animatorId)
    }
    // endregion

    // region Info box
    protected fun showInfoBox(@StringRes text: Int) {
        val infoBox = infoBox ?: return
        val infoBoxMessage = infoBoxMessage ?: return
        infoBox.isVisible = true
        infoBoxMessage.setText(text)
    }

    fun hideInfoBox() {
        infoBox?.isVisible = false
    }
    // endregion

    // region Preview image
    fun setPreviewImageVisibility(isVisible: Boolean) {
        previewImageContainer?.isVisible = isVisible
    }

    fun hidePreviewImage() = setPreviewImageVisibility(false)
    // endregion

    // region Sort list group
    private val sortListButtonGroup: View?
        get() = findViewById(R.id.sort_list_button_group)

    val isSortListGroupVisible: Boolean
        get() = sortListButtonGroup?.isVisible == true

    open fun showSortListGroup(show: Boolean) {
        sortListButtonGroup?.isVisible = show
    }
    // endregion

    companion object {
        private const val TAG = "ToolbarActivity"
    }
}
