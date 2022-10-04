/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
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
    fun applyFastScroll(
        recyclerView: RecyclerView,
        viewHelper: FastScroller.ViewHelper? = null
    ) {
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
