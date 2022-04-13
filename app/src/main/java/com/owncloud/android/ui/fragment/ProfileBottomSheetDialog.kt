/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nextcloud.android.lib.resources.profile.Action
import com.nextcloud.android.lib.resources.profile.HoverCard
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.databinding.ProfileBottomSheetActionBinding
import com.owncloud.android.databinding.ProfileBottomSheetFragmentBinding
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import com.owncloud.android.utils.theme.ThemeDrawableUtils

/**
 * Show actions of an user
 */
class ProfileBottomSheetDialog(
    private val fileActivity: FragmentActivity,
    private val user: User,
    private val hoverCard: HoverCard
) : BottomSheetDialog(fileActivity), DisplayUtils.AvatarGenerationListener {
    private var _binding: ProfileBottomSheetFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ProfileBottomSheetFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (window != null) {
            window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val primaryColor = ThemeColorUtils.primaryColor(context, true)

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

        for (action in hoverCard.actions) {
            val actionBinding = ProfileBottomSheetActionBinding.inflate(
                layoutInflater
            )
            val creatorView: View = actionBinding.root

            if (action.appId == "email") {
                action.hyperlink = action.title
                action.title = context.resources.getString(R.string.write_email)
            }

            actionBinding.name.text = action.title

            val icon = when (action.appId) {
                "profile" -> R.drawable.ic_user
                "email" -> R.drawable.ic_email
                "spreed" -> R.drawable.ic_talk
                else -> R.drawable.ic_edit
            }
            actionBinding.icon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    icon,
                    null
                )
            )
            ThemeDrawableUtils.tintDrawable(actionBinding.icon.drawable, primaryColor)

            creatorView.setOnClickListener { v: View? ->
                send(hoverCard.userId, action)
                dismiss()
            }
            binding.creators.addView(creatorView)
        }

        setOnShowListener { d: DialogInterface? ->
            BottomSheetBehavior.from(binding.root.parent as View)
                .setPeekHeight(binding.root.measuredHeight)
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
            data = Uri.parse("mailto:")
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
}
