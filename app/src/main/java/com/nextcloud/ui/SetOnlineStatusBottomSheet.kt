/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.SetOnlineStatusBottomSheetBinding
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class SetOnlineStatusBottomSheet(val currentStatus: Status?) :
    BottomSheetDialogFragment(R.layout.set_online_status_bottom_sheet),
    Injectable {

    private lateinit var binding: SetOnlineStatusBottomSheetBinding

    private lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var asyncRunner: AsyncRunner

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

        currentStatus?.let {
            updateCurrentStatusViews(it)
        }

        binding.onlineStatus.setOnClickListener { setStatus(StatusType.ONLINE) }
        binding.awayStatus.setOnClickListener { setStatus(StatusType.AWAY) }
        binding.busyStatus.setOnClickListener { setStatus(StatusType.BUSY) }
        binding.dndStatus.setOnClickListener { setStatus(StatusType.DND) }
        binding.invisibleStatus.setOnClickListener { setStatus(StatusType.INVISIBLE) }

        viewThemeUtils.files.themeStatusCardView(binding.onlineStatus)
        viewThemeUtils.files.themeStatusCardView(binding.awayStatus)
        viewThemeUtils.files.themeStatusCardView(binding.busyStatus)
        viewThemeUtils.files.themeStatusCardView(binding.dndStatus)
        viewThemeUtils.files.themeStatusCardView(binding.invisibleStatus)

        viewThemeUtils.platform.themeDialog(binding.root)

        binding.busyStatus.setVisibleIf(CapabilityUtils.getCapability(context).userStatusSupportsBusy.isTrue)
    }

    private fun updateCurrentStatusViews(it: Status) {
        visualizeStatus(it.status)
    }

    private fun setStatus(statusType: StatusType) {
        asyncRunner.postQuickTask(
            SetStatusTask(
                statusType,
                accountManager.currentOwnCloudAccount?.savedAccount,
                context
            ),
            {
                if (it) {
                    dismiss()
                } else {
                    showErrorSnackbar()
                }
            },
            {
                showErrorSnackbar()
            }
        )
    }

    private fun showErrorSnackbar() {
        DisplayUtils.showSnackMessage(view, "Failed to set status!")
        clearTopStatus()
    }

    private fun visualizeStatus(statusType: StatusType) {
        clearTopStatus()
        val views: Triple<MaterialCardView, TextView, ImageView> = when (statusType) {
            StatusType.ONLINE -> Triple(binding.onlineStatus, binding.onlineHeadline, binding.onlineIcon)
            StatusType.AWAY -> Triple(binding.awayStatus, binding.awayHeadline, binding.awayIcon)
            StatusType.BUSY -> Triple(binding.busyStatus, binding.busyHeadline, binding.busyIcon)
            StatusType.DND -> Triple(binding.dndStatus, binding.dndHeadline, binding.dndIcon)
            StatusType.INVISIBLE -> Triple(binding.invisibleStatus, binding.invisibleHeadline, binding.invisibleIcon)
            else -> {
                Log.d(TAG, "unknown status")
                return
            }
        }
        views.first.isChecked = true
        viewThemeUtils.platform.colorOnSecondaryContainerTextViewElement(views.second)
    }

    private fun clearTopStatus() {
        context?.let {
            binding.onlineHeadline.setTextColor(
                resources.getColor(com.nextcloud.android.common.ui.R.color.high_emphasis_text)
            )
            binding.awayHeadline.setTextColor(
                resources.getColor(com.nextcloud.android.common.ui.R.color.high_emphasis_text)
            )
            binding.busyHeadline.setTextColor(
                resources.getColor(com.nextcloud.android.common.ui.R.color.high_emphasis_text)
            )
            binding.dndHeadline.setTextColor(
                resources.getColor(com.nextcloud.android.common.ui.R.color.high_emphasis_text)
            )
            binding.invisibleHeadline.setTextColor(
                resources.getColor(com.nextcloud.android.common.ui.R.color.high_emphasis_text)
            )

            binding.awayIcon.imageTintList = null
            binding.dndIcon.imageTintList = null
            binding.invisibleIcon.imageTintList = null

            binding.onlineStatus.isChecked = false
            binding.awayStatus.isChecked = false
            binding.busyStatus.isChecked = false
            binding.dndStatus.isChecked = false
            binding.invisibleStatus.isChecked = false
        }
    }

    companion object {
        private val TAG = SetOnlineStatusBottomSheet::class.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SetOnlineStatusBottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}
