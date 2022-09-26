/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils.theme.newm3

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.preference.PreferenceCategory
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.card.MaterialCardView
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.android.common.ui.theme.ViewThemeUtilsBase
import com.nextcloud.android.common.ui.theme.utils.AndroidViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.AndroidXViewThemeUtils
import com.nextcloud.utils.view.FastScrollPopupBackground
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles
import javax.inject.Inject

class FilesSpecificViewThemeUtils @Inject constructor(
    schemes: MaterialSchemes,
    private val androidViewThemeUtils: AndroidViewThemeUtils,
    private val androidXViewThemeUtils: AndroidXViewThemeUtils
) : ViewThemeUtilsBase(schemes) {
    // not ported to common lib because PreferenceCategory is deprecated
    fun themePreferenceCategory(category: PreferenceCategory) {
        withScheme(category.context) {
            val text: Spannable = SpannableString(category.title)
            text.setSpan(
                ForegroundColorSpan(it.primary),
                0,
                text.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            category.title = text
        }
    }

    fun createAvatar(type: ShareType?, avatar: ImageView, context: Context) {
        fun createAvatarBase(@DrawableRes icon: Int, padding: Int = AvatarPadding.SMALL) {
            avatar.setImageResource(icon)
            avatar.background = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.round_bgnd,
                null
            )
            avatar.cropToPadding = true
            avatar.setPadding(padding, padding, padding, padding)
        }

        // TODO figure out why circle and email use grey background instead of primary
        when (type) {
            ShareType.GROUP -> {
                createAvatarBase(R.drawable.ic_group)
                androidViewThemeUtils.colorImageViewBackgroundAndIcon(avatar)
            }
            ShareType.ROOM -> {
                createAvatarBase(R.drawable.first_run_talk, AvatarPadding.LARGE)
                androidViewThemeUtils.colorImageViewBackgroundAndIcon(avatar)
            }
            ShareType.CIRCLE -> {
                createAvatarBase(R.drawable.ic_circles)
                avatar.background.setColorFilter(
                    context.resources.getColor(R.color.nc_grey),
                    PorterDuff.Mode.SRC_IN
                )
                avatar.drawable.mutate().setColorFilter(
                    context.resources.getColor(R.color.icon_on_nc_grey),
                    PorterDuff.Mode.SRC_IN
                )
            }
            ShareType.EMAIL -> {
                createAvatarBase(R.drawable.ic_email, AvatarPadding.LARGE)
                androidViewThemeUtils.colorImageViewBackgroundAndIcon(avatar)
            }
            else -> Log_OC.d(TAG, "Unknown share type")
        }
    }

    fun themeFastScrollerBuilder(context: Context, builder: FastScrollerBuilder): FastScrollerBuilder {
        return withScheme(context) { scheme ->
            builder
                .useMd2Style()
                .setThumbDrawable(getThumbDrawable(context))
                .setPopupStyle {
                    PopupStyles.MD2.accept(it)
                    it.background = FastScrollPopupBackground(context, scheme.primary)
                }
        }
    }

    private fun getThumbDrawable(
        context: Context
    ): Drawable {
        val thumbDrawable =
            ResourcesCompat.getDrawable(
                context.resources,
                me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb,
                null
            )
        return androidViewThemeUtils.tintPrimaryDrawable(context, thumbDrawable)!!
    }

    /**
     * Sets title and colors the actionbar, the title and the back arrow
     */
    // TODO move back arrow resource to lib and use lib method directly?
    fun themeActionBar(context: Context, actionBar: ActionBar, title: String) {
        val backArrow = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ic_arrow_back,
            null
        )!!
        androidXViewThemeUtils.themeActionBar(
            context,
            actionBar,
            title,
            backArrow
        )
    }

    /**
     * Sets title and colors the actionbar, the title and the back arrow
     */
    fun themeActionBar(context: Context, actionBar: ActionBar, @StringRes titleRes: Int) {
        val title = context.getString(titleRes)
        themeActionBar(
            context,
            actionBar,
            title
        )
    }

    /**
     * Colors actionbar background and back arrow but not the title
     */
    fun themeActionBar(context: Context, actionBar: ActionBar) {
        val backArrow = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ic_arrow_back,
            null
        )!!
        androidXViewThemeUtils.themeActionBar(context, actionBar, backArrow)
    }

    fun themeTemplateCardView(cardView: MaterialCardView) {
        withScheme(cardView.context) { scheme ->
            cardView.setStrokeColor(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        scheme.primary,
                        scheme.outline
                    )
                )
            )
        }
    }

    fun themeAvatarButton(shareImageView: ImageView) {
        withScheme(shareImageView.context) { scheme ->
            shareImageView.background.setColorFilter(scheme.primary, PorterDuff.Mode.SRC_IN)
            shareImageView.drawable.mutate().setColorFilter(scheme.onPrimary, PorterDuff.Mode.SRC_IN)
        }
    }

    fun primaryColorToHexString(context: Context): String {
        return withScheme(context) { scheme ->
            String.format("#%06X", 0xFFFFFF and scheme.primary)
        }
    }

    companion object {
        private val TAG = FilesSpecificViewThemeUtils::class.simpleName

        private object AvatarPadding {
            @Px
            const val SMALL = 4

            @Px
            const val LARGE = 8
        }
    }
}
