/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.extensions.highlightNavigationView
import com.owncloud.android.R
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer

class SidebarManager(private val hostView: View) {
    private var sidebarMenu: View? = null
    private var sidebarNavigationView: NavigationView? = null
    private var sidebarQuotaView: LinearLayout? = null
    private var sidebarQuotaProgressBar: LinearProgressIndicator? = null
    private var sidebarQuotaTextPercentage: TextView? = null
    private var sidebarQuotaTextLink: TextView? = null

    fun setup(
        viewThemeUtils: ViewThemeUtils,
        onHeaderReady: Consumer<View>,
        filterMenu: Consumer<Menu>,
        onNavigationItemClicked: Consumer<MenuItem>
    ) {
        if (bindNavigationView() == null) {
            return
        }

        colorNavigationView(viewThemeUtils)
        getHeaderView()?.let { onHeaderReady.accept(it) }
        setupMenu(filterMenu, onNavigationItemClicked)
        setupQuotaElements(viewThemeUtils)
    }

    fun highlight(bottomNavigationView: BottomNavigationView?, menuItemId: Int) {
        sidebarNavigationView?.let {
            highlightNavigationView(it, bottomNavigationView, menuItemId)
        }
    }

    fun showQuota(show: Boolean) {
        sidebarQuotaView?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setQuotaText(text: String) {
        sidebarQuotaTextPercentage?.text = text
    }

    fun setQuotaProgress(relative: Int) {
        sidebarQuotaProgressBar?.progress = relative
    }

    fun colorQuotaProgressPrimary(viewThemeUtils: ViewThemeUtils) {
        sidebarQuotaProgressBar?.let { viewThemeUtils.material.colorProgressBar(it, ColorRole.PRIMARY) }
    }

    fun colorQuotaProgressWarning(viewThemeUtils: ViewThemeUtils, @ColorInt color: Int) {
        sidebarQuotaProgressBar?.let { viewThemeUtils.material.colorProgressBar(it, color) }
    }

    fun hideQuotaLink() {
        sidebarQuotaTextLink?.visibility = View.GONE
    }

    fun updateQuotaLink(
        firstQuota: ExternalLink,
        iconSize: Int,
        onQuotaLinkClick: Consumer<ExternalLink>,
        createQuotaDrawableTarget: BiFunction<Int, TextView, Target<Drawable>>,
        loadQuotaIcon: BiConsumer<Target<Drawable>, String>
    ) {
        val quotaTextLink = sidebarQuotaTextLink ?: return
        quotaTextLink.text = firstQuota.name
        quotaTextLink.isClickable = true
        quotaTextLink.visibility = View.VISIBLE
        quotaTextLink.setOnClickListener { onQuotaLinkClick.accept(firstQuota) }

        val quotaTarget = createQuotaDrawableTarget.apply(iconSize, quotaTextLink)
        loadQuotaIcon.accept(quotaTarget, firstQuota.iconUrl)
    }

    fun applyLayoutMode(isDrawerLayout: Boolean, actionBar: ActionBar?, onSidebarVisible: Runnable) {
        if (isDrawerLayout) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
            sidebarMenu?.visibility = View.GONE
        } else {
            actionBar?.setDisplayHomeAsUpEnabled(false)
            sidebarMenu?.visibility = View.VISIBLE
            onSidebarVisible.run()
        }
    }

    fun updateActionBarForFile(isDrawerLayout: Boolean, actionBar: ActionBar?, isRoot: Boolean) {
        if (!isDrawerLayout) {
            actionBar?.setDisplayHomeAsUpEnabled(!isRoot)
        }
    }

    private fun bindNavigationView(): NavigationView? {
        if (sidebarMenu == null) {
            sidebarMenu = hostView.findViewById(R.id.sidebar_menu)
        }

        if (sidebarMenu != null && sidebarNavigationView == null) {
            sidebarNavigationView = sidebarMenu?.findViewById(R.id.sidebar_view)
        }

        return sidebarNavigationView
    }

    private fun getHeaderView(): View? = sidebarNavigationView?.getHeaderView(0)

    private fun setupMenu(filterMenu: Consumer<Menu>, onNavigationItemClicked: Consumer<MenuItem>) {
        sidebarNavigationView?.setNavigationItemSelectedListener { menuItem ->
            onNavigationItemClicked.accept(menuItem)
            true
        }

        sidebarNavigationView?.menu?.let { filterMenu.accept(it) }
    }

    private fun colorNavigationView(viewThemeUtils: ViewThemeUtils) {
        sidebarNavigationView?.let { viewThemeUtils.files.colorNavigationView(it) }
    }

    private fun setupQuotaElements(viewThemeUtils: ViewThemeUtils) {
        sidebarQuotaView = findQuotaViewById(R.id.sidebar_quota) as? LinearLayout
        sidebarQuotaProgressBar = findQuotaViewById(R.id.sidebar_quota_ProgressBar) as? LinearProgressIndicator
        sidebarQuotaTextPercentage = findQuotaViewById(R.id.sidebar_quota_percentage) as? TextView
        sidebarQuotaTextLink = findQuotaViewById(R.id.sidebar_quota_link) as? TextView

        sidebarQuotaProgressBar?.let {
            viewThemeUtils.material.colorProgressBar(it, ColorRole.PRIMARY)
            it.trackStopIndicatorSize = 0
        }
        sidebarQuotaView?.let { viewThemeUtils.platform.colorViewBackground(it) }
    }

    private fun findQuotaViewById(@IdRes id: Int): View? {
        val navigationView = hostView.findViewById<NavigationView>(R.id.sidebar_view)
        val headerView = navigationView?.getHeaderView(0)
        return headerView?.findViewById(id) ?: hostView.findViewById(id)
    }
}

