/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.ui.activity.DrawerActivity
import dagger.android.support.AndroidSupportInjection

class NavigatorActivity : DrawerActivity() {

    private lateinit var navigator: Navigator

    // region Lifecycle Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigator)

        val screen = intent.getParcelableArgument(EXTRA_SCREEN, NavigatorScreen::class.java) ?: return
        val fragmentContainerView = findViewById<FragmentContainerView>(R.id.fragment_container_view)
        navigator = Navigator(supportFragmentManager, fragmentContainerView)
        setupBackPressedHandler()
        pushOrRestoreScreen(savedInstanceState, screen)
    }

    // addFragmentOnAttachListener or via registerFragmentLifecycleCallbacks not providing same result
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onAttachFragment(fragment: Fragment) {
        AndroidSupportInjection.inject(fragment)
        super.onAttachFragment(fragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else {
                openDrawer()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getMenuItemId(): Int {
        val screen = intent.getParcelableArgument(EXTRA_SCREEN, NavigatorScreen::class.java)
        return screen?.menuItemId() ?: super.getMenuItemId()
    }

    override fun onResume() {
        super.onResume()
        highlightOrRestoreNavigationViewItem()
    }
    // endregion

    // region Public Methods
    fun pop() {
        val previousScreen = navigator.pop() ?: return
        setupActionBar(previousScreen)
    }

    fun push(screen: NavigatorScreen) {
        setupActionBar(screen)
        navigator.push(screen)
    }
    // endregion

    // region Private Methods
    private fun pushOrRestoreScreen(savedInstanceState: Bundle?, screen: NavigatorScreen) {
        if (savedInstanceState == null) {
            push(screen)
        } else {
            val currentScreen = navigator.getTopScreen() ?: screen
            setupActionBar(currentScreen)
            highlightNavigationViewItem(currentScreen.menuItemId())
        }
    }

    private fun setupActionBar(screen: NavigatorScreen) {
        val (style, titleId) = screen.actionBarStyle()
        if (style == ActionBarStyle.Plain) {
            setupToolbar()
        } else {
            setupHomeSearchToolbarWithSortAndListButtons()
        }
        updateActionBarTitleAndHomeButtonByString(getString(titleId))
        setupDrawer(menuItemId)
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        isDrawerOpen -> closeDrawer()
                        supportFragmentManager.backStackEntryCount == 1 -> finish()
                        else -> pop()
                    }
                }
            }
        )
    }

    private fun highlightOrRestoreNavigationViewItem() {
        val currentScreen = navigator.getTopScreen()
        if (currentScreen != null) {
            highlightNavigationViewItem(currentScreen.menuItemId())
        } else {
            highlightNavigationViewItem(menuItemId)
        }
    }
    // endregion

    companion object {
        const val EXTRA_SCREEN = "extra_screen"

        fun intent(context: Context, screen: NavigatorScreen): Intent =
            Intent(context, NavigatorActivity::class.java).apply {
                putExtra(EXTRA_SCREEN, screen)
            }
    }
}
