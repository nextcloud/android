/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.owncloud.android.utils.theme.ViewThemeUtils
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import javax.inject.Inject

class FastScrollUtils @Inject constructor(private val viewThemeUtils: ViewThemeUtils) {
    @JvmOverloads
    fun applyFastScroll(recyclerView: RecyclerView, viewHelper: FastScroller.ViewHelper? = null) {
        val builder =
            FastScrollerBuilder(recyclerView).let {
                viewThemeUtils.files.themeFastScrollerBuilder(
                    recyclerView.context,
                    it
                )
            }
        if (viewHelper != null) {
            builder.setViewHelper(viewHelper)
        }
        builder.build()
    }

    fun fixAppBarForFastScroll(appBarLayout: AppBarLayout, content: ViewGroup) {
        val contentLayoutInitialPaddingBottom = content.paddingBottom
        appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, offset ->
                content.setPadding(
                    content.paddingLeft,
                    content.paddingTop,
                    content.paddingRight,
                    contentLayoutInitialPaddingBottom + appBarLayout.totalScrollRange + offset
                )
            }
        )
    }
}
