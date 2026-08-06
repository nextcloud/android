/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nextcloud.android.lib.resources.profile.GetHoverCardRemoteOperation
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.fragment.ProfileBottomSheetDialog
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetrieveHoverCardAsyncTask(
    private val user: User,
    private val userId: String,
    private val activity: FragmentActivity,
    private val clientFactory: ClientFactory,
    private val viewThemeUtils: ViewThemeUtils
) {
    companion object {
        private const val TAG = "RetrieveHoverCardAsyncTask"
    }

    @Suppress("TooGenericExceptionCaught")
    fun execute() {
        activity.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val client = clientFactory.createNextcloudClient(user)
                val operationResult = GetHoverCardRemoteOperation(userId).execute(client)
                return@withContext try {
                    if (operationResult.isSuccess) {
                        operationResult.getResultData()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log_OC.e(TAG, "exception: $e")
                    null
                }
            }

            withContext(Dispatchers.Main) {
                if (result?.actions.isNullOrEmpty()) {
                    DisplayUtils.showSnackMessage(activity, R.string.no_actions)
                    return@withContext
                }

                ProfileBottomSheetDialog(
                    activity,
                    user,
                    result,
                    viewThemeUtils
                )
                    .show()
            }
        }
    }
}
