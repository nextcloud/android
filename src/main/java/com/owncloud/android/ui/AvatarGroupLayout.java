/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Stefan Niedermann
 * Copyright (C) 2021 Andy Scherzinger
 * Copyright (C) 2021 Stefan Niedermann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeAvatarUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

public class AvatarGroupLayout extends RelativeLayout implements DisplayUtils.AvatarGenerationListener {
    private static final String TAG = AvatarGroupLayout.class.getSimpleName();

    private final static int MAX_AVATAR_COUNT = 3;

    private final Drawable borderDrawable;
    @Px private final int avatarSize;
    @Px private final int avatarBorderSize;
    @Px private final int overlapPx;

    public AvatarGroupLayout(Context context) {
        this(context, null);
    }

    public AvatarGroupLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AvatarGroupLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AvatarGroupLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        avatarBorderSize = DisplayUtils.convertDpToPixel(2, context);
        avatarSize = DisplayUtils.convertDpToPixel(40, context);
        overlapPx = DisplayUtils.convertDpToPixel(24, context);
        borderDrawable = ContextCompat.getDrawable(context, R.drawable.round_bgnd);
        assert borderDrawable != null;
        DrawableCompat.setTint(borderDrawable, ContextCompat.getColor(context, R.color.bg_default));
    }

    public void setAvatars(@NonNull User user, @NonNull List<ShareeUser> sharees) {
        @NonNull Context context = getContext();
        removeAllViews();
        RelativeLayout.LayoutParams avatarLayoutParams;
        int avatarCount;
        int shareeSize = Math.min(sharees.size(), MAX_AVATAR_COUNT);

        Resources resources = context.getResources();
        float avatarRadius = resources.getDimension(R.dimen.list_item_avatar_icon_radius);
        ShareeUser sharee;

        for (avatarCount = 0; avatarCount < shareeSize; avatarCount++) {
            avatarLayoutParams = new RelativeLayout.LayoutParams(avatarSize, avatarSize);
            avatarLayoutParams.setMargins(0, 0, avatarCount * overlapPx, 0);
            avatarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            final ImageView avatar = new ImageView(context);
            avatar.setLayoutParams(avatarLayoutParams);
            avatar.setPadding(avatarBorderSize, avatarBorderSize, avatarBorderSize, avatarBorderSize);

            avatar.setBackground(borderDrawable);
            addView(avatar);
            avatar.requestLayout();

            if (avatarCount == 0 && sharees.size() > MAX_AVATAR_COUNT) {
                avatar.setImageResource(R.drawable.ic_people);
                ThemeDrawableUtils.setIconColor(avatar.getDrawable());
            } else {
                sharee = sharees.get(avatarCount);
                switch (sharee.getShareType()) {
                    case GROUP:
                    case EMAIL:
                    case ROOM:
                    case CIRCLE:
                        ThemeAvatarUtils.createAvatar(sharee.getShareType(), avatar, context);
                        break;
                    case FEDERATED:
                        showFederatedShareAvatar(context, sharee.getUserId(), avatarRadius, resources, avatar);
                        break;
                    default:
                        avatar.setTag(sharee);
                        DisplayUtils.setAvatar(user,
                                               sharee.getUserId(),
                                               sharee.getDisplayName(),
                                               this,
                                               avatarRadius,
                                               resources,
                                               avatar,
                                               context);
                        break;
                }
            }
        }

        // Recalculate container size based on avatar count
        int size = overlapPx * (avatarCount - 1) + avatarSize;
        ViewGroup.LayoutParams rememberParam = getLayoutParams();
        rememberParam.width = size;
        setLayoutParams(rememberParam);
    }

    private void showFederatedShareAvatar(Context context, String user, float avatarRadius, Resources resources,
                                          ImageView avatar) {
        // maybe federated share
        String[] split = user.split("@");
        String userId = split[0];
        String server = split[1];

        String url = "https://" + server + "/index.php/avatar/" + userId + "/" +
            DisplayUtils.convertDpToPixel(avatarRadius, context);

        Drawable placeholder;
        try {
            placeholder = TextDrawable.createAvatarByUserId(userId, avatarRadius);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
            placeholder = ThemeDrawableUtils.tintDrawable(ResourcesCompat.getDrawable(resources,
                                                                                      R.drawable.account_circle_white,
                                                                                      null),
                                                          R.color.black);
        }

        avatar.setTag(null);
        Glide.with(context).load(url)
            .asBitmap()
            .placeholder(placeholder)
            .error(placeholder)
            .into(new BitmapImageViewTarget(avatar) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources,
                                                                                                       resource);
                    circularBitmapDrawable.setCircular(true);
                    avatar.setImageDrawable(circularBitmapDrawable);
                }
            });
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ImageView) callContext).setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return ((ImageView) callContext).getTag().equals(tag);
    }
}
