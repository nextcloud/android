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

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.owncloud.android.utils.theme.ThemeColorUtils
import com.owncloud.android.utils.theme.ThemeDrawableUtils
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles

object FastScroll {

    @JvmStatic
    @JvmOverloads
    fun applyFastScroll(
        context: Context,
        themeColorUtils: ThemeColorUtils,
        themeDrawableUtils: ThemeDrawableUtils,
        recyclerView: RecyclerView,
        viewHelper: FastScroller.ViewHelper? = null
    ) {
        val primaryColor = themeColorUtils.primaryColor(context)
        val builder = FastScrollerBuilder(recyclerView)
            .useMd2Style()
            .setThumbDrawable(getThumbDrawable(context, themeDrawableUtils, primaryColor))
            .setPopupStyle {
                PopupStyles.MD2.accept(it)
                it.background = FastScrollPopupBackground(context, primaryColor)
            }
        if (viewHelper != null) {
            builder.setViewHelper(viewHelper)
        }
        builder.build()
    }

    private fun getThumbDrawable(
        context: Context,
        themeDrawableUtils: ThemeDrawableUtils,
        @ColorInt color: Int
    ): Drawable {
        val thumbDrawable =
            ResourcesCompat.getDrawable(
                context.resources,
                me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb,
                null
            )
        themeDrawableUtils.tintDrawable(thumbDrawable, color)
        return thumbDrawable!!
    }

    @JvmStatic
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
