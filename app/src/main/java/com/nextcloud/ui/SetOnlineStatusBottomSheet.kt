/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

        currentStatus?.let {
            updateCurrentStatusViews(it)
        }

        setupStatusViews()

        viewThemeUtils.platform.themeDialog(binding.root)

        val capability = CapabilityUtils.getCapability(context)
        val busyStatus = capability.userStatusSupportsBusy.isTrue
        binding.busyStatus.setVisibleIf(busyStatus)
    }

    private fun setupStatusViews() {
        val statuses = listOf(
            binding.onlineStatus to StatusType.ONLINE,
            binding.awayStatus to StatusType.AWAY,
            binding.busyStatus to StatusType.BUSY,
            binding.dndStatus to StatusType.DND,
            binding.invisibleStatus to StatusType.INVISIBLE
        )

        statuses.forEach { (view, status) ->
            view.setOnClickListener { setStatus(status) }
            viewThemeUtils.files.themeStatusCardView(view)
        }
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
        lifecycleScope.launch(Dispatchers.Main) {
            if (!isAdded) {
                return@launch
            }

            activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.set_online_status_bottom_sheet_error_message)
            }
            clearTopStatus()
        }
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
        viewThemeUtils.platform.colorTextView(views.second, ColorRole.ON_SECONDARY_CONTAINER)
    }

    private fun clearTopStatus() {
        val ctx = context ?: return
        val defaultColor = ContextCompat.getColor(
            ctx,
            com.nextcloud.android.common.ui.R.color.high_emphasis_text
        )

        listOf(
            binding.onlineHeadline,
            binding.awayHeadline,
            binding.busyHeadline,
            binding.dndHeadline,
            binding.invisibleHeadline
        ).forEach { it.setTextColor(defaultColor) }

        listOf(
            binding.awayIcon,
            binding.dndIcon,
            binding.invisibleIcon
        ).forEach { it.imageTintList = null }

        listOf(
            binding.onlineStatus,
            binding.awayStatus,
            binding.busyStatus,
            binding.dndStatus,
            binding.invisibleStatus
        ).forEach { it.isChecked = false }
    }

    companion object {
        private val TAG = SetOnlineStatusBottomSheet::class.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SetOnlineStatusBottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}
