/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.nextcloud.android.common.ui.color.ColorUtil
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
    private val colorUtil: ColorUtil,
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

    private fun getThumbDrawable(context: Context): Drawable {
        val thumbDrawable =
            ResourcesCompat.getDrawable(
                context.resources,
                me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb,
                null
            )
        return androidViewThemeUtils.tintPrimaryDrawable(context, thumbDrawable)!!
    }

    private fun getHomeAsUpIcon(isMenu: Boolean): Int {
        val icon = if (isMenu) {
            R.drawable.ic_menu
        } else {
            R.drawable.ic_arrow_back
        }
        return icon
    }

    /**
     * Sets title and colors the actionbar, the title and the back arrow
     */
    // TODO move back arrow resource to lib and use lib method directly?
    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, title: String, isMenu: Boolean = false) {
        val icon = getHomeAsUpIcon(isMenu)
        val backArrow = ResourcesCompat.getDrawable(
            context.resources,
            icon,
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
    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, @StringRes titleRes: Int, isMenu: Boolean = false) {
        val title = context.getString(titleRes)
        themeActionBar(
            context,
            actionBar,
            title,
            isMenu
        )
    }

    /**
     * Colors actionbar background and back arrow but not the title
     */
    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, isMenu: Boolean = false) {
        val backArrow = ResourcesCompat.getDrawable(
            context.resources,
            getHomeAsUpIcon(isMenu),
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

    fun themeStatusCardView(cardView: MaterialCardView) {
        withScheme(cardView) { scheme ->
            val background = cardView.context.getColor(R.color.grey_200)
            cardView.backgroundTintList =
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        scheme.secondaryContainer,
                        background
                    )
                )
            cardView.setStrokeColor(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        scheme.onSecondaryContainer,
                        scheme.surface
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
            colorUtil.colorToHexString(scheme.primary)
        }
    }

    fun setWhiteBackButton(context: Context, supportActionBar: ActionBar) {
        val backArrow = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_arrow_back, null)
        val tinted = androidViewThemeUtils.colorDrawable(backArrow!!, Color.WHITE)
        supportActionBar.setHomeAsUpIndicator(tinted)
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
