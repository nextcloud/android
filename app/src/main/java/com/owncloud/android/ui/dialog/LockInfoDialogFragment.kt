/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
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
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.R
import com.owncloud.android.databinding.LockInfoDialogBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeButtonUtils

/**
 * Dialog that shows lock information for a locked file
 */
class LockInfoDialogFragment(private val user: User, private val file: OCFile) :
    DialogFragment(),
    DisplayUtils.AvatarGenerationListener {
    private lateinit var binding: LockInfoDialogBinding

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val alertDialog = it as AlertDialog
            ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = LockInfoDialogBinding.inflate(inflater, null, false)

        val view = binding.root

        // Setup layout
        if (file.isLocked) {
            binding.lockedByUsername.text = getUserAndTimestampText()
            loadAvatar()
            setRemainingTime()
        } else {
            setNotLocked()
        }

        // Build the dialog
        val dialog = MaterialAlertDialogBuilder(requireActivity(), R.style.Theme_ownCloud_Dialog)
            .setTitle(R.string.file_lock_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.dismiss) { _, _ ->
                dismiss()
            }
            .create()

        return dialog
    }

    private fun getUserAndTimestampText(): CharSequence {
        val username = file.lockOwnerDisplayName ?: file.lockOwnerId
        val lockTimestampMillis = file.lockTimestamp * TimeConstants.MILLIS_PER_SECOND
        return DisplayUtils.createTextWithSpan(
            getString(
                R.string.locked_by_user_date, username, DisplayUtils.unixTimeToHumanReadable(lockTimestampMillis)
            ),
            username,
            StyleSpan(Typeface.BOLD)
        )
    }

    private fun loadAvatar() {
        DisplayUtils.setAvatar(
            user,
            file.lockOwnerId!!,
            file.lockOwnerDisplayName,
            this,
            requireContext().resources.getDimension(R.dimen.list_item_avatar_icon_radius),
            resources,
            binding.lockedByAvatar,
            context
        )
    }

    private fun setRemainingTime() {
        if (file.lockTimestamp == 0L || file.lockTimeout == 0L) {
            binding.lockExpiration.visibility = View.GONE
        } else {
            val expirationText = getExpirationRelativeText()
            binding.lockExpiration.text = getString(R.string.lock_expiration_info, expirationText)
            binding.lockExpiration.visibility = View.VISIBLE
        }
    }

    private fun getExpirationRelativeText(): CharSequence? {
        val expirationTimestamp = (file.lockTimestamp + file.lockTimeout) * TimeConstants.MILLIS_PER_SECOND
        return DisplayUtils.getRelativeTimestamp(context, expirationTimestamp, true)
    }

    private fun setNotLocked() {
        binding.lockExpiration.visibility = View.GONE
        binding.lockedByAvatar.visibility = View.GONE
        binding.lockedByUsername.text = getString(R.string.file_not_locked)
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any) {
        (callContext as ImageView).setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String, callContext: Any): Boolean {
        return (callContext as ImageView).tag == tag
    }
}
