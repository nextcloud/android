/*
 * Nextcloud Android client application
 *
 * SPDX-FileCopyrightText: 2021-2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
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

class AvatarGroupLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes),
    AvatarGenerationListener {
    private val borderDrawable = checkNotNull(ContextCompat.getDrawable(context, R.drawable.round_bgnd)).apply {
        setTint(ContextCompat.getColor(context, R.color.bg_default))
    }

    @Px
    private val avatarSize: Int = DisplayUtils.convertDpToPixel(40f, context)

    @Px
    private val avatarBorderSize: Int = DisplayUtils.convertDpToPixel(2f, context)

    @Px
    private val overlapPx: Int = DisplayUtils.convertDpToPixel(24f, context)

    var boundFileId: Long? = null
        set(value) {
            if (field != value) {
                displayedSharees = null
            }
            field = value
        }

    private var displayedSharees: List<ShareeUser>? = null

    init {
        checkNotNull(borderDrawable)
        DrawableCompat.setTint(borderDrawable, ContextCompat.getColor(context, R.color.bg_default))
    }

    @Suppress("LongMethod", "TooGenericExceptionCaught")
    fun setAvatars(user: User, sharees: List<ShareeUser>, viewThemeUtils: ViewThemeUtils) {
        if (sharees == displayedSharees) {
            return
        }
        displayedSharees = sharees

        removeAllViews()

        if (sharees.isEmpty()) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        val avatarCount = sharees.size.coerceAtMost(MAX_AVATAR_COUNT)
        val hasOverflow = sharees.size > MAX_AVATAR_COUNT
        val serverName = user.accountName.substringAfterLast('@')

        repeat(avatarCount) { index ->
            val avatar = addAvatarView(index)

            if (index == 0 && hasOverflow) {
                avatar.setImageResource(R.drawable.ic_people)
                viewThemeUtils.platform.tintDrawable(context, avatar.drawable, ColorRole.ON_SURFACE)
            } else {
                sharee = sharees[avatarCount]
                when (sharee.shareType) {
                    ShareType.GROUP, ShareType.EMAIL, ShareType.ROOM, ShareType.CIRCLE, ShareType.PUBLIC_LINK ->
                        viewThemeUtils.files.createAvatar(
                            sharee.shareType,
                            avatar,
                            context
                        )

                    ShareType.FEDERATED, ShareType.FEDERATED_GROUP -> showFederatedShareAvatar(
                        context,
                        sharee.userId!!,
                        avatarRadius,
                        resources,
                        avatar,
                        viewThemeUtils
                    )

                    else -> {
                        avatar.tag = "${sharee.userId}@$serverName"
                        DisplayUtils.setAvatar(
                            user,
                            sharee.userId!!,
                            sharee.displayName,
                            this,
                            avatarRadius,
                            resources,
                            avatar,
                            context,
                            avatarBorderSize
                        )
                    }
                }
            }
        }

        updateLayoutParams { width = overlapPx * (avatarCount - 1) + avatarSize }
    }

    private fun addAvatarView(index: Int): ImageView {
        val avatar = ImageView(context).apply {
            layoutParams = LayoutParams(avatarSize, avatarSize).apply {
                setMargins(0, 0, index * overlapPx, 0)
                addRule(ALIGN_PARENT_RIGHT)
            }
            setPadding(avatarBorderSize, avatarBorderSize, avatarBorderSize, avatarBorderSize)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = borderDrawable
        }

        addView(avatar)
        avatar.requestLayout()
        return avatar
    }

    @Suppress("LongParameterList")
    private fun bindSharee(
        avatar: ImageView,
        sharee: ShareeUser,
        user: User,
        serverName: String,
        viewThemeUtils: ViewThemeUtils,
        avatarGenerator: AvatarGenerator
    ) {
        when (sharee.shareType) {
            ShareType.GROUP, ShareType.EMAIL, ShareType.ROOM, ShareType.CIRCLE, ShareType.PUBLIC_LINK ->
                viewThemeUtils.files.createAvatar(sharee.shareType, avatar, context)

            ShareType.FEDERATED, ShareType.FEDERATED_GROUP ->
                showFederatedShareAvatar(checkNotNull(sharee.userId), avatar, viewThemeUtils)

            else -> {
                val userId = checkNotNull(sharee.userId)
                avatar.tag = "$userId@$serverName"
                avatarGenerator.setUserAvatar(
                    userId,
                    this,
                    avatarRadius,
                    avatar,
                    displayName = sharee.displayName,
                    user = user,
                    avatarBorder = avatarBorderSize
                )
            }
        }
    }

    private fun showFederatedShareAvatar(federatedId: String, avatar: ImageView, viewThemeUtils: ViewThemeUtils) {
        val parts = federatedId.split("@")
        val userId = parts.first()
        val url = parts.getOrNull(1)?.let { server ->
            "https://$server/index.php/avatar/$userId/${resources.getInteger(R.integer.file_avatar_px)}"
        }
        val placeholder = createFederatedPlaceholder(userId, viewThemeUtils)

        avatar.tag = null
        if (url != null) {
            loadCircularBitmapIntoImageView(context, url, avatar, placeholder)
        } else {
            avatar.setImageDrawable(placeholder)
        }
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any) {
        (callContext as ImageView).setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any): Boolean =
        (callContext as ImageView).tag == tag

    companion object {
        private val TAG: String = AvatarGroupLayout::class.java.simpleName
        private const val MAX_AVATAR_COUNT = 3
        private const val AVATAR_SIZE_DP = 40f
        private const val AVATAR_BORDER_DP = 2f
        private const val AVATAR_OVERLAP_DP = 24f
    }
}
