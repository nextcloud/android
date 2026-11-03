/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.quota

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.drawer.externallinks.ExternalLinkOpener
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

class DrawerQuotaController(private val activity: DrawerActivity, private val externalLinkOpener: ExternalLinkOpener) {
    private val quotaFetcher = UserQuotaFetcher(activity)

    private var quotaView: LinearLayout? = null
    private var quotaProgressBar: LinearProgressIndicator? = null
    private var quotaTextPercentage: TextView? = null
    private var quotaTextLink: TextView? = null

    private val Quota.isDisplayable: Boolean
        get() = quota > 0 ||
            quota == GetUserInfoRemoteOperation.SPACE_UNLIMITED ||
            quota == GetUserInfoRemoteOperation.QUOTA_LIMIT_INFO_NOT_AVAILABLE

    fun setupViews(navigationView: NavigationView) {
        quotaView = findQuotaViewById(navigationView, R.id.drawer_quota)
        quotaProgressBar = findQuotaViewById(navigationView, R.id.drawer_quota_ProgressBar)
        quotaTextPercentage = findQuotaViewById(navigationView, R.id.drawer_quota_percentage)
        quotaTextLink = findQuotaViewById(navigationView, R.id.drawer_quota_link)

        quotaProgressBar?.let {
            activity.viewThemeUtils.material.colorProgressBar(it, ColorRole.PRIMARY)
            it.trackStopIndicatorSize = 0
        }
        quotaView?.let { activity.viewThemeUtils.platform.colorViewBackground(it) }
    }

    fun fetchAndDisplay() {
        activity.lifecycleScope.launch {
            val quota = quotaFetcher.fetch() ?: return@launch
            if (quota.isDisplayable) {
                showQuotaInformation(quota)
            } else {
                quotaView?.isVisible = false
            }
        }
    }

    fun updateQuotaLink() {
        val quotaTextLink = quotaTextLink ?: return
        if (!MDMConfig.externalSiteSupport(activity)) {
            quotaTextLink.visibility = View.GONE
            return
        }

        activity.externalLinksProvider.getExternalLink(ExternalLinkType.QUOTA) { quotas ->
            val firstQuota = quotas.firstOrNull()
            if (firstQuota == null) {
                quotaTextLink.visibility = View.GONE
            } else {
                showQuotaLink(quotaTextLink, firstQuota)
            }
        }
    }

    private fun <T : View> findQuotaViewById(navigationView: NavigationView, @IdRes id: Int): T? =
        navigationView.getHeaderView(0)?.findViewById(id) ?: activity.findViewById(id)

    private fun showQuotaInformation(quota: Quota) {
        quotaTextPercentage?.text = if (quota.quota == GetUserInfoRemoteOperation.SPACE_UNLIMITED) {
            activity.getString(R.string.drawer_quota_unlimited, HumanReadableFormatter.formatBytes(quota.used))
        } else {
            activity.getString(
                R.string.drawer_quota,
                HumanReadableFormatter.formatBytes(quota.used),
                HumanReadableFormatter.formatBytes(quota.total)
            )
        }

        val relative = ceil(quota.relative).toInt()
        quotaProgressBar?.let { progressBar ->
            progressBar.progress = relative
            if (relative < RELATIVE_THRESHOLD_WARNING) {
                activity.viewThemeUtils.material.colorProgressBar(progressBar, ColorRole.PRIMARY)
            } else {
                activity.viewThemeUtils.material.colorProgressBar(
                    progressBar,
                    activity.resources.getColor(R.color.infolevel_warning, null)
                )
            }
        }

        updateQuotaLink()
        quotaView?.isVisible = true
    }

    private fun showQuotaLink(quotaTextLink: TextView, quota: ExternalLink) {
        quotaTextLink.text = quota.name
        quotaTextLink.isClickable = true
        quotaTextLink.visibility = View.VISIBLE
        quotaTextLink.setOnClickListener { externalLinkOpener.openInWebView(quota) }

        val iconSize = (LINK_ICON_SIZE_DP * activity.resources.displayMetrics.density).roundToInt()
        GlideHelper.loadIntoTarget(
            activity,
            activity.userAccountManager.currentOwnCloudAccount,
            quota.iconUrl,
            QuotaLinkIconTarget(iconSize, quotaTextLink),
            R.drawable.ic_link
        )
    }

    companion object {
        private const val RELATIVE_THRESHOLD_WARNING = 80
        private const val LINK_ICON_SIZE_DP = 24
    }
}
