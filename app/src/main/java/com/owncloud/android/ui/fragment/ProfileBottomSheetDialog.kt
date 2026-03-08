/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.nextcloud.android.lib.resources.profile.Action
import com.nextcloud.android.lib.resources.profile.HoverCard
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.databinding.ProfileBottomSheetFragmentBinding
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

private const val TEXT_SIZE = 16f

/**
 * Show actions of an user
 */
class ProfileBottomSheetDialog(
    private val fileActivity: FragmentActivity,
    private val user: User,
    private val hoverCard: HoverCard,
    private val viewThemeUtils: ViewThemeUtils
) : BottomSheetDialog(fileActivity),
    DisplayUtils.AvatarGenerationListener {
    private var _binding: ProfileBottomSheetFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ProfileBottomSheetFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (window != null) {
            window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        viewThemeUtils.platform.themeDialog(binding.root)

        binding.icon.tag = hoverCard.userId
        DisplayUtils.setAvatar(
            user,
            hoverCard.userId,
            hoverCard.displayName,
            this,
            context.resources.getDimension(R.dimen.list_item_avatar_icon_radius),
            context.resources,
            binding.icon,
            context
        )

        binding.displayName.text = hoverCard.displayName

        val itemHeight = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_height)
        val standardPadding = context.resources.getDimensionPixelSize(R.dimen.standard_padding)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.iconized_single_line_item_icon_size)
        val primaryColor = viewThemeUtils.getColorScheme(context).primary.toArgb()
        val textColor = ContextCompat.getColor(context, R.color.text_color)

        for (action in hoverCard.actions) {
            if (action.appId == "email") {
                action.hyperlink = action.title
                action.title = context.resources.getString(R.string.write_email)
            }

            val iconRes = when (action.appId) {
                "profile" -> R.drawable.ic_user_outline
                "email" -> R.drawable.ic_email
                "spreed" -> R.drawable.ic_talk
                else -> R.drawable.ic_edit
            }

            val config = ProfileButtonConfig(
                itemHeight = itemHeight,
                standardPadding = standardPadding,
                textColor = textColor,
                iconRes = iconRes,
                iconSize = iconSize,
                primaryColor = primaryColor
            )
            binding.creators.addView(createProfileButton(config, action))
        }

        setOnShowListener { d: DialogInterface? ->
            BottomSheetBehavior.from(binding.root.parent as View)
                .setPeekHeight(binding.root.measuredHeight)
        }
    }

    private fun createProfileButton(config: ProfileButtonConfig, action: Action): MaterialButton = MaterialButton(
        ContextThemeWrapper(context, R.style.ThemeOverlay_App_Button_BottomSheetItem),
        null,
        com.google.android.material.R.attr.materialButtonStyle
    ).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            config.itemHeight
        )
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setPaddingRelative(config.standardPadding, 0, config.standardPadding, 0)
        text = action.title
        setTextColor(config.textColor)
        textSize = TEXT_SIZE
        isAllCaps = false
        icon = ResourcesCompat.getDrawable(context.resources, config.iconRes, null)
        this.iconSize = config.iconSize
        this.iconPadding = config.standardPadding
        iconGravity = MaterialButton.ICON_GRAVITY_START
        iconTint = ColorStateList.valueOf(config.primaryColor)
        setOnClickListener {
            send(hoverCard.userId, action)
            dismiss()
        }
    }

    private fun send(userId: String, action: Action) {
        when (action.appId) {
            "profile" -> openWebsite(action.hyperlink)
            "core" -> sendEmail(action.hyperlink)
            "spreed" -> openTalk(userId, action.hyperlink)
        }
    }

    private fun openWebsite(url: String) {
        DisplayUtils.startLinkIntent(fileActivity, url)
    }

    private fun sendEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }

        DisplayUtils.startIntentIfAppAvailable(intent, fileActivity, R.string.no_email_app_available)
    }

    private fun openTalk(userId: String, hyperlink: String) {
        try {
            val sharingIntent = Intent(Intent.ACTION_VIEW)
            sharingIntent.setClassName(
                "com.nextcloud.talk2",
                "com.nextcloud.talk.activities.MainActivity"
            )
            sharingIntent.putExtra("server", user.server.uri)
            sharingIntent.putExtra("userId", userId)
            fileActivity.startActivity(sharingIntent)
        } catch (e: ActivityNotFoundException) {
            openWebsite(hyperlink)
        }
    }

    override fun onStop() {
        super.onStop()
        _binding = null
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        if (callContext is ImageView) {
            callContext.setImageDrawable(avatarDrawable)
        }
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        if (callContext is ImageView) {
            // needs to be changed once federated users have avatars
            return callContext.tag.toString() == tag!!.split("@").toTypedArray()[0]
        }
        return false
    }

    private data class ProfileButtonConfig(
        val itemHeight: Int,
        val standardPadding: Int,
        val textColor: Int,
        val iconRes: Int,
        val iconSize: Int,
        val primaryColor: Int
    )
}
