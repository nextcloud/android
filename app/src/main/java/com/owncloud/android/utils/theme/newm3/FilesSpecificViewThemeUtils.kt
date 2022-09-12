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
import android.graphics.PorterDuff
import android.preference.PreferenceCategory
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.android.common.ui.theme.ViewThemeUtilsBase
import com.nextcloud.android.common.ui.theme.utils.AndroidViewThemeUtils
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import javax.inject.Inject

class FilesSpecificViewThemeUtils @Inject constructor(
    schemes: MaterialSchemes,
    private val androidViewThemeUtils: AndroidViewThemeUtils
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
