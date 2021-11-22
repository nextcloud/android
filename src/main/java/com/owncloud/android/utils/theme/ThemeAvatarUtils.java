/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils.theme;

import android.content.Context;
import android.graphics.PorterDuff;
import android.widget.ImageView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.shares.ShareType;

import androidx.core.content.res.ResourcesCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class with methods for client side button theming.
 */
public final class ThemeAvatarUtils {
    public static void colorIconImageViewWithBackground(ImageView imageView, Context context) {
        int primaryColor = ThemeColorUtils.primaryColor(null, true, false, context);

        imageView.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN);
        imageView.getDrawable().mutate().setColorFilter(ThemeColorUtils.getColorForPrimary(primaryColor, context),
                                                        PorterDuff.Mode.SRC_IN);
    }

    @SuppressFBWarnings(
        value = "SF_SWITCH_NO_DEFAULT",
        justification = "We only create avatars for a subset of share types")
    public static void createAvatar(ShareType type, ImageView avatar, Context context) {
        switch (type) {
            case GROUP:
                avatar.setImageResource(R.drawable.ic_group);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(4, 4, 4, 4);
                ThemeAvatarUtils.colorIconImageViewWithBackground(avatar, context);
                break;

            case ROOM:
                avatar.setImageResource(R.drawable.first_run_talk);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(8, 8, 8, 8);
                ThemeAvatarUtils.colorIconImageViewWithBackground(avatar, context);
                break;

            case CIRCLE:
                avatar.setImageResource(R.drawable.ic_circles);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.getBackground().setColorFilter(context.getResources().getColor(R.color.nc_grey),
                                                      PorterDuff.Mode.SRC_IN);
                avatar.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.icon_on_nc_grey),
                                                             PorterDuff.Mode.SRC_IN);
                avatar.setCropToPadding(true);
                avatar.setPadding(4, 4, 4, 4);
                break;

            case EMAIL:
                avatar.setImageResource(R.drawable.ic_email);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(8, 8, 8, 8);
                avatar.getBackground().setColorFilter(context.getResources().getColor(R.color.nc_grey),
                                                      PorterDuff.Mode.SRC_IN);
                avatar.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.icon_on_nc_grey),
                                                             PorterDuff.Mode.SRC_IN);
                break;
        }
    }
}
