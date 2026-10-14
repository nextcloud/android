/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022-2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.preference.PreferenceCategory
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.android.common.ui.theme.ViewThemeUtilsBase
import com.nextcloud.android.common.ui.theme.utils.AndroidViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.AndroidXViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.android.common.ui.util.buildColorStateList
import com.nextcloud.utils.view.FastScrollPopupBackground
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import dynamiccolor.MaterialDynamicColors
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles
import javax.inject.Inject

@Suppress("TooManyFunctions")
class FilesSpecificViewThemeUtils @Inject constructor(
    schemes: MaterialSchemes,
    private val colorUtil: ColorUtil,
    private val androidViewThemeUtils: AndroidViewThemeUtils,
    private val androidXViewThemeUtils: AndroidXViewThemeUtils
) : ViewThemeUtilsBase(schemes) {
    private val dynamicColor = MaterialDynamicColors()

    // not ported to common lib because PreferenceCategory is deprecated
    @Suppress("DEPRECATION")
    fun themePreferenceCategory(category: PreferenceCategory) {
        withScheme(category.context) { scheme ->
            category.title = SpannableString(category.title).apply {
                setSpan(ForegroundColorSpan(scheme.primary), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }
    }

    fun createAvatar(type: ShareType?, avatar: ImageView, context: Context) {
        fun createAvatarBase(@DrawableRes icon: Int, @Px padding: Int = AvatarPadding.SMALL) = avatar.apply {
            setImageResource(icon)
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.round_bgnd, null)
            cropToPadding = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(padding, padding, padding, padding)
        }

        fun createThemedAvatar(@DrawableRes icon: Int, @Px padding: Int = AvatarPadding.SMALL) {
            createAvatarBase(icon, padding)
            androidViewThemeUtils.colorImageViewBackgroundAndIcon(avatar)
        }

        when (type) {
            ShareType.GROUP, ShareType.FEDERATED_GROUP -> createThemedAvatar(R.drawable.ic_group)

            ShareType.ROOM -> createThemedAvatar(R.drawable.ic_talk, AvatarPadding.LARGE)

            ShareType.EMAIL -> createThemedAvatar(R.drawable.ic_email, AvatarPadding.LARGE)

            ShareType.PUBLIC_LINK -> createThemedAvatar(R.drawable.ic_link, AvatarPadding.LARGE)

            ShareType.CIRCLE -> createAvatarBase(R.drawable.ic_circles).run {
                background.colorFilter = srcInColorFilter(context.getColor(R.color.nc_grey))
                drawable.mutate().colorFilter = srcInColorFilter(context.getColor(R.color.icon_on_nc_grey))
            }

            else -> Log_OC.d(TAG, "Unknown share type")
        }
    }

    fun themeFastScrollerBuilder(context: Context, builder: FastScrollerBuilder): FastScrollerBuilder =
        withScheme(context) { scheme ->
            builder
                .useMd2Style()
                .setThumbDrawable(getThumbDrawable(context))
                .setPopupStyle {
                    PopupStyles.MD2.accept(it)
                    it.background = FastScrollPopupBackground(context, scheme.primary)
                }
        }

    private fun getThumbDrawable(context: Context): Drawable = checkNotNull(
        androidViewThemeUtils.tintDrawable(
            context,
            me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb,
            ColorRole.PRIMARY
        )
    ) { "Fast scroller thumb drawable not found" }

    private fun getHomeAsUpDrawable(context: Context, isMenu: Boolean): Drawable {
        val icon = if (isMenu) R.drawable.ic_menu else R.drawable.ic_arrow_back
        return getDrawable(context, icon)
    }

    private fun getDrawable(context: Context, @DrawableRes id: Int): Drawable =
        checkNotNull(ResourcesCompat.getDrawable(context.resources, id, null)) { "Drawable $id not found" }

    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, title: String, isMenu: Boolean = false) {
        androidXViewThemeUtils.themeActionBar(context, actionBar, title, getHomeAsUpDrawable(context, isMenu))
    }

    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, @StringRes titleRes: Int, isMenu: Boolean = false) {
        themeActionBar(context, actionBar, context.getString(titleRes), isMenu)
    }

    @JvmOverloads
    fun themeActionBar(context: Context, actionBar: ActionBar, isMenu: Boolean = false) {
        androidXViewThemeUtils.themeActionBar(context, actionBar, getHomeAsUpDrawable(context, isMenu))
    }

    fun themeTemplateCardView(view: MaterialCardView) {
        withScheme(view.context) { scheme ->
            view.setStrokeColor(checkedColors(scheme.primary, scheme.outline))
        }
    }

    fun themeStatusCardView(view: MaterialCardView) {
        withScheme(view) { scheme ->
            view.backgroundTintList = checkedColors(scheme.surfaceContainerHighest, scheme.surface)
            view.setStrokeColor(checkedColors(scheme.onSecondaryContainer, scheme.outlineVariant))
        }
    }

    fun themeFilledCardView(view: MaterialCardView) {
        withScheme(view) { scheme ->
            view.backgroundTintList = checkedColors(scheme.secondaryContainer, view.context.getColor(R.color.grey_200))
        }
    }

    fun primaryColorToHexString(context: Context): String = withScheme(context) { scheme ->
        colorUtil.colorToHexString(scheme.primary)
    }

    fun setWhiteBackButton(context: Context, actionBar: ActionBar) {
        val backArrow = getDrawable(context, R.drawable.ic_arrow_back)
        actionBar.setHomeAsUpIndicator(androidViewThemeUtils.colorDrawable(backArrow, Color.WHITE))
    }

    fun themeContentSearchView(view: SearchView) {
        withScheme(view) { scheme ->
            view.findViewById<AppCompatAutoCompleteTextView>(androidx.appcompat.R.id.search_src_text).run {
                setHintTextColor(scheme.onSurfaceVariant)
                highlightColor = scheme.inverseOnSurface
                setTextColor(scheme.onSurface)
            }
            view.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn).setColorFilter(scheme.onSurface)
            view.findViewById<ImageView>(androidx.appcompat.R.id.search_button).setColorFilter(scheme.onSurface)
            view.findViewById<LinearLayout>(androidx.appcompat.R.id.search_plate)
                .setBackgroundColor(scheme.surfaceContainerHigh)
        }
    }

    @JvmOverloads
    fun colorNavigationView(view: NavigationView, colorIcons: Boolean = true) {
        withScheme(view) { scheme ->
            view.run {
                itemBackground?.setTintList(
                    checkedColors(dynamicColor.secondaryContainer().getArgb(scheme), Color.TRANSPARENT)
                )

                background.setTintList(ColorStateList.valueOf(dynamicColor.surface().getArgb(scheme)))

                if (colorIcons) {
                    itemIconTintList = checkedColors(
                        dynamicColor.onSecondaryContainer().getArgb(scheme),
                        dynamicColor.onSurfaceVariant().getArgb(scheme)
                    )
                }
            }
        }
    }

    fun themeAutoCompleteTextView(view: MaterialAutoCompleteTextView) {
        withScheme(view) { scheme ->
            view.setTextColor(dynamicColor.onSurface().getArgb(scheme))
            view.setHintTextColor(dynamicColor.onSurfaceVariant().getArgb(scheme))
            view.dropDownBackgroundTintList = ColorStateList.valueOf(dynamicColor.surfaceContainer().getArgb(scheme))
        }
    }

    private fun checkedColors(@ColorInt checked: Int, @ColorInt unchecked: Int): ColorStateList = buildColorStateList(
        android.R.attr.state_checked to checked,
        -android.R.attr.state_checked to unchecked
    )

    private fun srcInColorFilter(@ColorInt color: Int) = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

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
