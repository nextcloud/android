/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation.animator

import android.transition.Fade
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar
import com.owncloud.android.R
import com.owncloud.android.ui.navigation.model.ActionBarStyle

class SearchBarTransition(private val toolbar: MaterialToolbar, homeToolbar: ViewGroup) {

    private val toolbarParent = toolbar.parent as ViewGroup
    private val toolbarIndex = toolbarParent.indexOfChild(toolbar)
    private val toolbarLayoutParams = toolbar.layoutParams

    private val searchCard: ViewGroup = homeToolbar.findViewById(R.id.home_search_container)
    private val searchCardContent: View = homeToolbar.findViewById(R.id.home_search_content)

    private val durationMillis = homeToolbar.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    private val sideButtonsAnimator = SearchBarSideButtonsAnimator(homeToolbar, durationMillis)
    private val expandFade = Fade(Fade.IN).setDuration(durationMillis)
    private val collapseFade = Fade()
        .setDuration(durationMillis)
        .excludeTarget(toolbar, true)

    // if parent fully search card means it's already expanded
    val isExpanded: Boolean
        get() = (toolbar.parent === searchCard)

    private val isSearchViewExpanded: Boolean
        get() = toolbar.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)?.isVisible == true

    fun expand() {
        if (isExpanded) return

        TransitionManager.beginDelayedTransition(searchCard, expandFade)
        fadeOutSearchCardContent()
        toolbarParent.removeView(toolbar)
        toolbar.background = null
        searchCard.addView(toolbar, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        sideButtonsAnimator.hide()
    }

    fun syncWith(style: ActionBarStyle) {
        when (style) {
            ActionBarStyle.Search -> if (!isSearchViewExpanded) collapse()
            ActionBarStyle.Plain -> collapse()
        }
    }

    private fun collapse() {
        if (!isExpanded) return

        TransitionManager.beginDelayedTransition(searchCard, collapseFade)
        searchCard.removeView(toolbar)
        toolbarParent.addView(toolbar, toolbarIndex, toolbarLayoutParams)
        searchCardContent.animate().cancel()
        searchCardContent.alpha = OPAQUE
        searchCardContent.isVisible = true

        sideButtonsAnimator.show()
    }

    private fun fadeOutSearchCardContent() {
        searchCardContent.animate()
            .alpha(TRANSPARENT)
            .setDuration(durationMillis)
            .withEndAction {
                searchCardContent.isVisible = false
                searchCardContent.alpha = OPAQUE
            }
    }

    private companion object {
        const val OPAQUE = 1f
        const val TRANSPARENT = 0f
    }
}
