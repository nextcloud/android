/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.nextcloud.utils.extensions.isActive
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.LoadingDialog

class LoadingDialogController(private val activity: AppCompatActivity) {

    fun show(message: String?) {
        activity.runOnUiThread {
            if (!activity.isActive()) {
                Log_OC.w(TAG, "cannot show loading dialog, activity is finishing")
                return@runOnUiThread
            }

            val fragmentManager = activity.supportFragmentManager
            fragmentManager.executePendingTransactions()
            fragmentManager.dismissExistingLoadingDialog("dismiss previous loading dialog")

            if (fragmentManager.isStateSaved) return@runOnUiThread

            Log_OC.d(TAG, "show loading dialog")
            LoadingDialog.newInstance(message).show(fragmentManager, DIALOG_WAIT_TAG)
        }
    }

    fun dismiss() {
        activity.runOnUiThread {
            if (!activity.isActive()) {
                Log_OC.w(TAG, "cannot dismiss loading dialog, activity is finishing")
                return@runOnUiThread
            }

            val fragmentManager = activity.supportFragmentManager
            fragmentManager.executePendingTransactions()
            fragmentManager.dismissExistingLoadingDialog("dismiss loading dialog")
        }
    }

    private fun FragmentManager.dismissExistingLoadingDialog(logMessage: String) {
        val loadingDialog = findFragmentByTag(DIALOG_WAIT_TAG) as? LoadingDialog ?: return
        Log_OC.d(TAG, logMessage)

        if (isStateSaved) {
            loadingDialog.dismissAllowingStateLoss()
        } else {
            loadingDialog.dismiss()
        }
    }

    companion object {
        private const val TAG = "LoadingDialogController"
        private const val DIALOG_WAIT_TAG = "DIALOG_WAIT"
    }
}
