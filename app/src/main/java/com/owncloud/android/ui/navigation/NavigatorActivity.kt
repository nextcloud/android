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
import androidx.fragment.app.FragmentContainerView
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.ui.activity.DrawerActivity

class NavigatorActivity : DrawerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigator)
        setupToolbar()
        setupDrawer(menuItemId)

        if (savedInstanceState == null) {
            val screen = intent.getParcelableArgument(EXTRA_SCREEN, NavigatorScreen::class.java) ?: return
            val fragmentContainerView = findViewById<FragmentContainerView>(R.id.fragment_container_view)
            val navigator = Navigator(supportFragmentManager, fragmentContainerView)
            navigator.push(screen)
        }
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

    override fun getMenuItemId(): Int = when (intent.getParcelableArgument(EXTRA_SCREEN, NavigatorScreen::class.java)) {
        NavigatorScreen.Community -> R.id.nav_community
        else -> super.getMenuItemId()
    }

    override fun onResume() {
        super.onResume()
        highlightNavigationViewItem(menuItemId)
    }

    companion object {
        const val EXTRA_SCREEN = "extra_screen"

        fun intent(context: Context, screen: NavigatorScreen): Intent =
            Intent(context, NavigatorActivity::class.java).apply {
                putExtra(EXTRA_SCREEN, screen)
            }
    }
}
