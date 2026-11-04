/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.header

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemApp
import com.nextcloud.utils.LinkHelper
import com.nextcloud.utils.view.ScreenMetrics
import com.owncloud.android.R
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.drawer.navigation.DrawerNavigator
import hct.Hct
import kotlin.jvm.optionals.getOrNull

class EcosystemAppsBanner(
    private val activity: DrawerActivity,
    private val navigator: DrawerNavigator,
    private val brandingColors: ServerBrandingColors
) {

    private val iconColor: Int
        get() {
            val account = activity.account ?: return activity.getColor(R.color.grey_800_transparent)
            val isServerPrimaryColorDark =
                Hct.fromInt(brandingColors.getPrimaryColor(account)).tone < LIGHT_TONE_THRESHOLD
            return if (isServerPrimaryColorDark) Color.WHITE else activity.getColor(R.color.grey_800_transparent)
        }

    fun show(banner: ConstraintLayout) {
        val notesView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_notes)
        val talkView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_talk)
        val moreView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_more)
        val assistantView = banner.findViewById<LinearLayout>(R.id.drawer_ecosystem_assistant)

        activity.user.getOrNull()?.accountName?.let { accountName ->
            notesView.setOnClickListener { activity.ecosystemManager.openApp(EcosystemApp.NOTES, accountName) }
            talkView.setOnClickListener { activity.ecosystemManager.openApp(EcosystemApp.TALK, accountName) }
        }
        moreView.setOnClickListener { LinkHelper.openAppStore(APP_STORE_QUERY, true, activity) }
        assistantView.setOnClickListener { navigator.startAssistantScreen() }
        assistantView.isVisible = activity.isAssistantAvailable

        val color = iconColor
        listOf(notesView, talkView, moreView, assistantView).forEach { tintAppView(it, color) }

        banner.visibility = View.VISIBLE
    }

    private fun tintAppView(appView: LinearLayout, color: Int) {
        val imageView = appView.getChildAt(0) as ImageView
        imageView.imageTintList = ColorStateList.valueOf(color)
        (imageView.background as GradientDrawable).setStroke(ScreenMetrics.dpToPx(1f, activity), color)
        (appView.getChildAt(1) as TextView).setTextColor(color)
    }

    companion object {
        private const val APP_STORE_QUERY = "Nextcloud"
        private const val LIGHT_TONE_THRESHOLD = 80.0
    }
}
