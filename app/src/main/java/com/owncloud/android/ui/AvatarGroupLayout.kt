/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Stefan Niedermann
 * Copyright (C) 2021 Andy Scherzinger
 * Copyright (C) 2021 Stefan Niedermann
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.utils.GlideHelper.loadCircularBitmapIntoImageView
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlin.math.min

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
import com.owncloud.android.utils.theme.ViewThemeUtils;

    @Px
    private val avatarBorderSize: Int = DisplayUtils.convertDpToPixel(2f, context)

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

    init {
        checkNotNull(borderDrawable)
        DrawableCompat.setTint(borderDrawable, ContextCompat.getColor(context, R.color.bg_default))
    }

    @Suppress("LongMethod", "TooGenericExceptionCaught")
    fun setAvatars(user: User, sharees: MutableList<ShareeUser>, viewThemeUtils: ViewThemeUtils) {
        val context = getContext()
        removeAllViews()
        var avatarLayoutParams: LayoutParams?
        val shareeSize = min(sharees.size, MAX_AVATAR_COUNT)
        val resources = context.resources
        val avatarRadius = resources.getDimension(R.dimen.list_item_avatar_icon_radius)
        var sharee: ShareeUser

        var avatarCount = 0
        while (avatarCount < shareeSize) {
            avatarLayoutParams = LayoutParams(avatarSize, avatarSize).apply {
                setMargins(0, 0, avatarCount * overlapPx, 0)
                addRule(ALIGN_PARENT_RIGHT)
            }

            val avatar = ImageView(context).apply {
                layoutParams = avatarLayoutParams
                setPadding(avatarBorderSize, avatarBorderSize, avatarBorderSize, avatarBorderSize)
                background = borderDrawable
            }

            addView(avatar)
            avatar.requestLayout()

            if (avatarCount == 0 && sharees.size > MAX_AVATAR_COUNT) {
                avatar.setImageResource(R.drawable.ic_people)
                viewThemeUtils.platform.tintDrawable(context, avatar.drawable, ColorRole.ON_SURFACE)
            } else {
                sharee = sharees[avatarCount]
                when (sharee.shareType) {
                    ShareType.GROUP, ShareType.EMAIL, ShareType.ROOM, ShareType.CIRCLE ->
                        viewThemeUtils.files.createAvatar(
                            sharee.shareType,
                            avatar,
                            context
                        )

                    ShareType.FEDERATED -> showFederatedShareAvatar(
                        context,
                        sharee.userId!!,
                        avatarRadius,
                        resources,
                        avatar,
                        viewThemeUtils
                    )

                    else -> {
                        avatar.tag = sharee
                        DisplayUtils.setAvatar(
                            user,
                            sharee.userId!!,
                            sharee.displayName,
                            this,
                            avatarRadius,
                            resources,
                            avatar,
                            context
                        )
                    }
                }
            }
            avatarCount++
        }

        // Recalculate container size based on avatar count
        val size = overlapPx * (avatarCount - 1) + avatarSize
        val rememberParam = layoutParams
        rememberParam.width = size
        layoutParams = rememberParam
    }

    @Suppress("TooGenericExceptionCaught")
    private fun showFederatedShareAvatar(
        context: Context,
        user: String,
        avatarRadius: Float,
        resources: Resources,
        avatar: ImageView,
        viewThemeUtils: ViewThemeUtils
    ) {
        // maybe federated share
        val split = user.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val userId: String? = split[0]
        val server = split[1]

        val url = "https://" + server + "/index.php/avatar/" + userId + "/" +
            resources.getInteger(R.integer.file_avatar_px)
        var placeholder: Drawable?
        try {
            placeholder = TextDrawable.createAvatarByUserId(userId, avatarRadius)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e)
            placeholder = viewThemeUtils.platform.colorDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.account_circle_white,
                    null
                )!!,
                ContextCompat.getColor(context, R.color.black)
            )
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

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any) {
        (callContext as ImageView).setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any): Boolean =
        (callContext as ImageView).tag == tag

    companion object {
        private val TAG: String = AvatarGroupLayout::class.java.simpleName
        private const val MAX_AVATAR_COUNT = 3
    }
}
